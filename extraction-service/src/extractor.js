/*
 * yt-dlp wrapper.
 *
 * Spawns yt-dlp via execFile (NOT exec) so the URL is passed as argv and
 * cannot trigger shell interpolation — the workflow snippet that builds a
 * shell string with `exec(`yt-dlp -g ${url}`)` is unsafe and is replaced
 * here.
 *
 * yt-dlp's `--dump-single-json` gives us a rich payload (formats, thumbnail,
 * width/height, duration, filesize) which we normalize to the MediaItem shape
 * the Android client expects from /resolve.
 */

const { execFile } = require('node:child_process');
const fs = require('node:fs');

const YTDLP_BIN = process.env.YTDLP_BIN || 'yt-dlp';
const YTDLP_TIMEOUT_MS = Number(process.env.YTDLP_TIMEOUT_MS || 60_000);
const YTDLP_MAX_BUFFER = 8 * 1024 * 1024;
// Optional cookies file (Netscape format) — required for many Instagram
// posts that are public but gated behind a login wall for datacenter IPs.
// Generate with a browser extension and mount the file into the container.
const YTDLP_COOKIES_PATH = process.env.YTDLP_COOKIES_PATH || '';
// Optional outbound proxy (residential proxy recommended for production
// datacenter deploys — Instagram aggressively rate-limits cloud IPs).
const YTDLP_PROXY = process.env.YTDLP_PROXY || '';
// User-agent override; recent Chrome works better than yt-dlp default for IG.
const YTDLP_USER_AGENT =
  process.env.YTDLP_USER_AGENT ||
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36';

const COOKIES_USABLE = (() => {
  if (!YTDLP_COOKIES_PATH) return false;
  try {
    return fs.statSync(YTDLP_COOKIES_PATH).isFile();
  } catch {
    // eslint-disable-next-line no-console
    console.warn(`[reelgrab-extractor] YTDLP_COOKIES_PATH set but file missing: ${YTDLP_COOKIES_PATH}`);
    return false;
  }
})();

class ExtractError extends Error {
  constructor(reason, cause) {
    super(reason);
    this.reason = reason;
    if (cause) this.cause = cause;
  }
}

function classifyStderr(stderr = '') {
  const s = stderr.toLowerCase();
  if (s.includes('login required') || s.includes('private') || s.includes('this video is private')) {
    return 'private';
  }
  if (s.includes('rate-limit') || s.includes('rate limit') || s.includes('http error 429')) {
    return 'rate_limited';
  }
  if (s.includes('unsupported url') || s.includes('no video formats') || s.includes('http error 404')) {
    return 'unsupported';
  }
  return 'internal';
}

function buildArgs(url) {
  const args = [
    '--dump-single-json',
    '--no-warnings',
    '--no-playlist',
    '--no-call-home',
    '--socket-timeout', '20',
    '--retries', '3',
    '--user-agent', YTDLP_USER_AGENT,
  ];
  if (COOKIES_USABLE) args.push('--cookies', YTDLP_COOKIES_PATH);
  if (YTDLP_PROXY) args.push('--proxy', YTDLP_PROXY);
  args.push('--', url);
  return args;
}

function runYtDlp(url) {
  return new Promise((resolve, reject) => {
    execFile(
      YTDLP_BIN,
      buildArgs(url),
      { timeout: YTDLP_TIMEOUT_MS, maxBuffer: YTDLP_MAX_BUFFER },
      (err, stdout, stderr) => {
        if (err) {
          const reason = classifyStderr(stderr);
          return reject(new ExtractError(reason, err));
        }
        try {
          resolve(JSON.parse(stdout));
        } catch (parseErr) {
          reject(new ExtractError('internal', parseErr));
        }
      },
    );
  });
}

function pickBestFormat(info) {
  if (!Array.isArray(info.formats) || info.formats.length === 0) return null;
  const progressive = info.formats.filter(
    (f) => f.url && f.vcodec && f.vcodec !== 'none' && f.acodec && f.acodec !== 'none',
  );
  const candidates = progressive.length > 0 ? progressive : info.formats.filter((f) => f.url);
  candidates.sort((a, b) => (b.height || 0) - (a.height || 0) || (b.tbr || 0) - (a.tbr || 0));
  return candidates[0] || null;
}

function bestThumbnail(info) {
  if (info.thumbnail) return info.thumbnail;
  if (Array.isArray(info.thumbnails) && info.thumbnails.length > 0) {
    const sorted = [...info.thumbnails].sort(
      (a, b) => (b.width || 0) - (a.width || 0),
    );
    return sorted[0]?.url ?? null;
  }
  return null;
}

function toMediaItem(info, sourceUrl, idx = 0) {
  const isImage = info._type === 'image' || (info.ext && /^(jpg|jpeg|png|webp)$/i.test(info.ext));
  if (isImage) {
    const directUrl = info.url || bestThumbnail(info);
    if (!directUrl) throw new ExtractError('unsupported');
    // Android MediaDto declares width/height/thumbnailUrl non-null. Coerce here so a
    // missing field from yt-dlp never trips kotlinx.serialization on the client.
    return {
      id: info.id ? `${info.id}-${idx}` : `img-${idx}`,
      sourceUrl,
      directUrl,
      thumbnailUrl: bestThumbnail(info) || directUrl,
      type: 'IMAGE',
      durationMs: null,
      width: info.width ?? 0,
      height: info.height ?? 0,
      sizeBytes: info.filesize ?? info.filesize_approx ?? null,
    };
  }

  const fmt = pickBestFormat(info);
  const directUrl = fmt?.url || info.url;
  if (!directUrl) throw new ExtractError('unsupported');

  return {
    id: info.id ? `${info.id}-${idx}` : `vid-${idx}`,
    sourceUrl,
    directUrl,
    thumbnailUrl: bestThumbnail(info) || directUrl,
    type: 'VIDEO',
    durationMs: typeof info.duration === 'number' ? Math.round(info.duration * 1000) : null,
    width: fmt?.width ?? info.width ?? 0,
    height: fmt?.height ?? info.height ?? 0,
    sizeBytes: fmt?.filesize ?? fmt?.filesize_approx ?? info.filesize ?? info.filesize_approx ?? null,
  };
}

async function extract(url) {
  const info = await runYtDlp(url);

  // Carousel / multi-entry post.
  if (Array.isArray(info.entries) && info.entries.length > 0) {
    return info.entries.map((entry, idx) => toMediaItem(entry, url, idx));
  }
  return [toMediaItem(info, url, 0)];
}

module.exports = { extract, ExtractError };

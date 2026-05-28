/*
 * ReelGrab extraction service.
 *
 * Two endpoints:
 *   POST /resolve  — full contract used by Android RemoteMediaRepository,
 *                    returns { items: MediaItem[] }.
 *   POST /download — simpler shape from AppWorkFlow.md, returns { video }
 *                    (direct MP4 URL of the first/best video item).
 *
 * Both call the same yt-dlp extractor (src/extractor.js). The mock keywords
 * (private / ratelimit / unsupported / carousel / mock) are kept so the
 * client error-mapping tests still pass without a network call.
 *
 * Set USE_MOCK=1 to force mock responses even for non-keyword URLs (useful
 * for CI / dev machines without yt-dlp installed).
 */

const express = require('express');
const cors = require('cors');
const swaggerUi = require('swagger-ui-express');
const rateLimit = require('express-rate-limit');
const { z } = require('zod');
const { healthRouter } = require('./healthcheck');
const { extract, ExtractError } = require('./extractor');
const openapiSpec = require('./openapi');

const INSTAGRAM = /^https?:\/\/(www\.)?instagram\.com\/(p|reel|tv|stories)\/[\w\-]+\/?.*$/;
const FACEBOOK = /^https?:\/\/(www\.|m\.|web\.)?(facebook\.com|fb\.watch)\/.*$/;

const ResolveRequest = z.object({
  url: z
    .string()
    .url()
    .refine((u) => INSTAGRAM.test(u) || FACEBOOK.test(u), {
      message: 'unsupported_host',
    }),
});

const app = express();

// Render / Heroku / any reverse proxy: trust X-Forwarded-* so
// express-rate-limit keys on the real client IP, not the proxy.
app.set('trust proxy', 1);

// CORS: lock down via env in production; default permissive for dev.
const CORS_ORIGINS = (process.env.CORS_ORIGINS || '*')
  .split(',')
  .map((s) => s.trim())
  .filter(Boolean);
app.use(
  cors({
    origin: CORS_ORIGINS.includes('*') ? true : CORS_ORIGINS,
  }),
);
app.use(express.json({ limit: '8kb' }));
app.use(healthRouter);

// API key middleware. Skipped when API_KEY env is unset (dev mode) and on
// /health, /docs, /openapi.json, / so probes and the UI stay reachable.
const API_KEY = process.env.API_KEY || '';
const OPEN_PATHS = new Set(['/health', '/openapi.json', '/']);
app.use((req, res, next) => {
  if (!API_KEY) return next();
  if (OPEN_PATHS.has(req.path) || req.path.startsWith('/docs')) return next();
  const provided = req.get('X-API-Key') || req.query.api_key;
  if (provided && provided === API_KEY) return next();
  return res.status(401).json({ error: 'unauthorized' });
});

// Rate limit extraction endpoints. yt-dlp is expensive; protect the box
// and reduce platform-side rate-limit triggers.
const limiter = rateLimit({
  windowMs: Number(process.env.RATE_LIMIT_WINDOW_MS || 60_000),
  max: Number(process.env.RATE_LIMIT_MAX || 30),
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'rate_limited' },
});

// OpenAPI / Swagger UI — browse the contract at GET /docs, fetch the raw
// spec at GET /openapi.json (handy for codegen).
app.get('/openapi.json', (_req, res) => res.json(openapiSpec));
app.use(
  '/docs',
  swaggerUi.serve,
  swaggerUi.setup(openapiSpec, {
    customSiteTitle: 'ReelGrab extractor — API docs',
    swaggerOptions: { persistAuthorization: true, displayRequestDuration: true },
  }),
);
// Convenience: visiting the root in a browser jumps to /docs.
app.get('/', (_req, res) => res.redirect('/docs'));

const USE_MOCK = process.env.USE_MOCK === '1';
const SAMPLE_VIDEO = 'https://samplelib.com/lib/preview/mp4/sample-5s.mp4';
const SAMPLE_IMAGE_THUMB = 'https://picsum.photos/seed/reelgrab/600/600';

function mockVideoItem(id, sourceUrl) {
  return {
    id,
    sourceUrl,
    directUrl: SAMPLE_VIDEO,
    thumbnailUrl: SAMPLE_IMAGE_THUMB,
    type: 'VIDEO',
    durationMs: 5000,
    width: 1280,
    height: 720,
    sizeBytes: 2000000,
  };
}

function mockImageItem(id, sourceUrl, seed) {
  return {
    id,
    sourceUrl,
    directUrl: `https://picsum.photos/seed/${seed}/1080/1080`,
    thumbnailUrl: `https://picsum.photos/seed/${seed}/600/600`,
    type: 'IMAGE',
    durationMs: null,
    width: 1080,
    height: 1080,
    sizeBytes: 800000,
  };
}

function parseUrl(req, res) {
  const parsed = ResolveRequest.safeParse(req.body);
  if (!parsed.success) {
    const reason = parsed.error.issues[0]?.message ?? 'invalid_url';
    if (reason === 'unsupported_host') {
      res.status(404).json({ error: 'unsupported' });
    } else {
      res.status(400).json({ error: 'invalid_url' });
    }
    return null;
  }
  return parsed.data.url;
}

function mockFor(url) {
  const lower = url.toLowerCase();
  if (lower.includes('private')) return { status: 403, body: { error: 'private' } };
  if (lower.includes('ratelimit')) return { status: 429, body: { error: 'rate_limited' } };
  if (lower.includes('unsupported')) return { status: 404, body: { error: 'unsupported' } };
  if (lower.includes('carousel')) {
    return {
      status: 200,
      body: {
        items: [
          mockImageItem('mock-c1', url, 'reelgrab-c1'),
          mockImageItem('mock-c2', url, 'reelgrab-c2'),
          { ...mockVideoItem('mock-c3', url), id: 'mock-c3' },
        ],
      },
    };
  }
  if (lower.includes('mock') || USE_MOCK) {
    return { status: 200, body: { items: [mockVideoItem('mock-1', url)] } };
  }
  return null;
}

function errorFor(reason) {
  switch (reason) {
    case 'private':       return { status: 403, body: { error: 'private' } };
    case 'rate_limited':  return { status: 429, body: { error: 'rate_limited' } };
    case 'unsupported':   return { status: 404, body: { error: 'unsupported' } };
    default:              return { status: 500, body: { error: 'internal' } };
  }
}

async function resolveItems(url) {
  const mock = mockFor(url);
  if (mock) return mock;
  try {
    const items = await extract(url);
    return { status: 200, body: { items } };
  } catch (err) {
    const reason = err instanceof ExtractError ? err.reason : 'internal';
    if (reason === 'internal') {
      // eslint-disable-next-line no-console
      console.error('[reelgrab-extractor] extract failed', err);
    }
    return errorFor(reason);
  }
}

app.post('/resolve', limiter, async (req, res) => {
  const url = parseUrl(req, res);
  if (!url) return;
  const { status, body } = await resolveItems(url);
  res.status(status).json(body);
});

// Workflow-style endpoint: { video } shape consumed by the simple
// DownloadManager flow described in AppWorkFlow.md.
app.post('/download', limiter, async (req, res) => {
  const url = parseUrl(req, res);
  if (!url) return;
  const { status, body } = await resolveItems(url);
  if (status !== 200) return res.status(status).json(body);
  const video = body.items.find((it) => it.type === 'VIDEO') ?? body.items[0];
  if (!video?.directUrl) return res.status(404).json({ error: 'unsupported' });
  res.json({
    video: video.directUrl,
    thumbnail: video.thumbnailUrl ?? null,
    durationMs: video.durationMs ?? null,
  });
});

// eslint-disable-next-line no-unused-vars
app.use((err, _req, res, _next) => {
  // eslint-disable-next-line no-console
  console.error('[reelgrab-extractor] unhandled', err);
  res.status(500).json({ error: 'internal' });
});

const PORT = process.env.PORT || 3000;
if (require.main === module) {
  app.listen(PORT, '0.0.0.0', () => {
    // eslint-disable-next-line no-console
    console.log(
      `[reelgrab-extractor] listening on :${PORT} ` +
        `(mock=${USE_MOCK ? 'on' : 'off'}, auth=${API_KEY ? 'on' : 'off'})`,
    );
  });
}

module.exports = { app };

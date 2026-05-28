# reelgrab-extractor

Node.js + Express service that extracts direct-media URLs from Instagram and
Facebook posts using [yt-dlp](https://github.com/yt-dlp/yt-dlp). Two
endpoints:

| Endpoint        | Shape                              | Consumer                            |
|-----------------|------------------------------------|-------------------------------------|
| `POST /resolve` | `{ items: MediaItem[] }`           | Android `RemoteMediaRepository`     |
| `POST /download`| `{ video, thumbnail, durationMs }` | Simple `DownloadManager` flow (AppWorkFlow.md) |

Both call the same extractor.

---

## Run locally

Requires **Node 20+**, **Python 3**, **yt-dlp**, and **ffmpeg** on PATH.

```bash
# macOS
brew install yt-dlp ffmpeg
cd extraction-service
npm install
npm start          # listens on :3000
# or
npm run dev        # nodemon hot reload
```

### With Docker

```bash
docker compose up --build
```

The Dockerfile installs `yt-dlp` + `ffmpeg` inside the image.

### Reaching the service from the Android app

The Android client picks up `EXTRACTION_BASE_URL_DEBUG` from
`<repo-root>/local.properties` (see `local.properties.example`).

| Target                       | Base URL                                      | Notes |
|------------------------------|-----------------------------------------------|-------|
| Emulator on dev machine      | `http://10.0.2.2:3000/`                       | default, no setup |
| Physical device same Wi-Fi   | `http://<dev-machine-LAN-IP>:3000/`           | `./expose.sh lan` to grab IP |
| Physical device anywhere     | `https://<id>.trycloudflare.com/`             | `./expose.sh cloudflared` |
| Physical device anywhere     | `https://<id>.ngrok-free.app/`                | `./expose.sh ngrok` |

After editing `local.properties`, rebuild: `./gradlew :app:assembleDebug`.

Cleartext over LAN works only for **debug** APKs — `network_security_config.xml`
opens `<debug-overrides cleartextTrafficPermitted="true">`. Release builds
require HTTPS, so use a tunnel (or a real TLS-terminated host) for QA on a
shipped artifact.

### Quick exposure

```bash
cd extraction-service
./expose.sh           # info: prints LAN URL + tunnel options
./expose.sh lan       # just the LAN URL
./expose.sh cloudflared
./expose.sh ngrok
```

### Mock mode (no yt-dlp needed)

Set `USE_MOCK=1` to bypass yt-dlp entirely and return canned `MediaItem`s.
Useful for CI or dev boxes without yt-dlp installed.

```bash
USE_MOCK=1 npm start
```

The mock keywords `private`, `ratelimit`, `unsupported`, `carousel`, `mock`
also work even when `USE_MOCK` is off — they short-circuit before yt-dlp is
called so error-path tests run fast and offline.

---

## Contract — `POST /resolve` (full)

```http
POST /resolve HTTP/1.1
Content-Type: application/json

{ "url": "https://www.instagram.com/reel/Cxyz123/" }
```

URL is validated against the same Instagram / Facebook regex the Android
app uses:

```
^https?://(www\.)?instagram\.com/(p|reel|tv|stories)/[\w\-]+/?.*$
^https?://(www\.|m\.|web\.)?(facebook\.com|fb\.watch)/.*$
```

**Success — 200**

```json
{
  "items": [
    {
      "id": "Cxyz123-0",
      "sourceUrl": "https://www.instagram.com/reel/Cxyz123/",
      "directUrl": "https://scontent.cdninstagram.com/.../video.mp4",
      "thumbnailUrl": "https://scontent.cdninstagram.com/.../thumb.jpg",
      "type": "VIDEO",
      "durationMs": 14820,
      "width": 1080,
      "height": 1920,
      "sizeBytes": 3284921
    }
  ]
}
```

`type` is `VIDEO` or `IMAGE`. `durationMs` and `sizeBytes` are nullable.
Carousels return one entry per slide.

---

## Contract — `POST /download` (workflow-style)

```http
POST /download HTTP/1.1
Content-Type: application/json

{ "url": "https://www.instagram.com/reel/Cxyz123/" }
```

**Success — 200**

```json
{
  "video": "https://scontent.cdninstagram.com/.../video.mp4",
  "thumbnail": "https://scontent.cdninstagram.com/.../thumb.jpg",
  "durationMs": 14820
}
```

`video` is the direct MP4 URL — pass it straight to Android
`DownloadManager`. For carousels this returns the first VIDEO item; use
`/resolve` if you need every slide.

---

## Error codes

| HTTP | Body                          | yt-dlp signal                          | Android `ErrorReason` |
|------|-------------------------------|----------------------------------------|-----------------------|
| 400  | `{"error":"invalid_url"}`     | Body fails Zod URL parse               | `InvalidUrl`          |
| 403  | `{"error":"private"}`         | "login required" / "private" in stderr | `PrivateContent`      |
| 404  | `{"error":"unsupported"}`     | Host not IG/FB, or "unsupported url"   | `Unsupported`         |
| 429  | `{"error":"rate_limited"}`    | "http error 429" / "rate limit"        | `RateLimited`         |
| 500  | `{"error":"internal"}`        | Anything else                          | `Unknown`             |

### Health

`GET /health` → `{"status":"ok"}` (used by Docker / k8s probes).

---

## Architecture

```
client URL
   │
   ▼
parseUrl ── Zod regex check ──► 400 / 404
   │
   ▼
mockFor(url) ── keyword? ──► canned MediaItem[]
   │ no
   ▼
extract(url) ── execFile yt-dlp --dump-single-json ──► MediaItem[]
   │
   ├─► /resolve  → { items }
   └─► /download → { video, thumbnail, durationMs }   (first VIDEO item)
```

`extractor.js` shells out to yt-dlp via `execFile` with an **argv array** —
no shell, no interpolation, no injection. (The AppWorkFlow.md snippet that
uses `exec(\`yt-dlp -g ${url}\`)` is intentionally **not** used; that form
is unsafe.)

### Tuning

| Env var               | Default      | Purpose                                              |
|-----------------------|--------------|------------------------------------------------------|
| `PORT`                | `3000`       | HTTP port                                            |
| `USE_MOCK`            | unset        | `1` to force mock responses                          |
| `YTDLP_BIN`           | `yt-dlp`     | Path / name of the binary                            |
| `YTDLP_TIMEOUT_MS`    | `60000`      | Per-request extraction timeout                       |
| `YTDLP_COOKIES_PATH`  | unset        | Netscape cookies.txt path (recommended for IG)       |
| `YTDLP_PROXY`         | unset        | Outbound proxy URL (residential recommended)         |
| `YTDLP_USER_AGENT`    | recent Chrome| Override yt-dlp User-Agent                           |
| `API_KEY`             | unset        | Require `X-API-Key` header when set                  |
| `CORS_ORIGINS`        | `*`          | Comma-separated allowed origins                      |
| `RATE_LIMIT_WINDOW_MS`| `60000`      | Rate limit window per IP                             |
| `RATE_LIMIT_MAX`      | `30`         | Max requests per window per IP                       |

---

## Deploy to Render

Repo includes `extraction-service/render.yaml` (Blueprint format).

1. Push the repo to GitHub.
2. Render dashboard → **New +** → **Blueprint** → pick repo.
3. Render reads `extraction-service/render.yaml`, builds the Dockerfile, exposes
   the service at `https://reelgrab-extractor-<slug>.onrender.com`.
4. In the service dashboard → **Environment** → set:
   - `API_KEY` = strong random string (32+ chars).
   - `YTDLP_COOKIES_PATH` (optional) = `/etc/secrets/instagram-cookies.txt`
     after uploading the cookies file under **Secret Files**.
   - `YTDLP_PROXY` (optional) = residential proxy URL.
5. Wait for first deploy (~3–5 min — yt-dlp + ffmpeg install).
6. Smoke test:
   ```bash
   curl https://<your-host>.onrender.com/health
   curl -X POST https://<your-host>.onrender.com/resolve \
     -H 'Content-Type: application/json' \
     -H "X-API-Key: $API_KEY" \
     -d '{"url":"https://www.instagram.com/reel/Cxyz123/"}'
   ```

### Why Instagram needs cookies

Anonymous yt-dlp requests from datacenter IPs (Render, AWS, GCP) get
`login required` / 429 for most reels. Workarounds:

- **Cookies (recommended):** Export a logged-in IG session as Netscape
  `cookies.txt` (browser extension like "Get cookies.txt LOCALLY"), upload
  via Render **Secret Files**, point `YTDLP_COOKIES_PATH` at it. Use a
  throwaway account — IG may shadow-ban or rate-limit it.
- **Residential proxy:** Set `YTDLP_PROXY=http://user:pass@host:port`. Bright
  Data / Smartproxy / Oxylabs etc. Costs $$ but no IG account needed.

### Render free tier caveats

- Spins down after 15 min idle → 30–60s cold start on first hit. Android
  client now allows 90s read timeout (see `OkHttpModule.kt`).
- 512 MB RAM — yt-dlp + ffmpeg can spike for large videos. Use **starter**
  ($7/mo) for non-sleeping + same RAM, **standard** if OOM appears.
- Shared egress IPs are flagged by Instagram more often. Cookies/proxy
  largely mitigate.

### Android wiring

After deploy, set in `local.properties`:

```
EXTRACTION_BASE_URL_RELEASE=https://reelgrab-extractor-<slug>.onrender.com/
EXTRACTION_BASE_URL_DEBUG=https://reelgrab-extractor-<slug>.onrender.com/
EXTRACTION_API_KEY=<same as Render API_KEY>
```

Rebuild: `./gradlew :app:assembleDebug`.

---

## Security caveats — read before deploying

This service is dev-friendly: open CORS, no auth, no rate limiting.
**Do not expose it publicly as-is.** Before production:

- Require an `X-API-Key` header signed per client; rotate frequently.
- Add IP rate limiting (`express-rate-limit`) and request-size caps.
- Front with TLS; the Android client enforces HTTPS via `CertificatePinner`
  — pin the production cert.
- Strip / hash inbound URLs in logs; they are PII-adjacent.
- Run yt-dlp in a sandboxed container with no outbound egress except the
  platform CDNs you support.
- Keep yt-dlp pinned and updated frequently — extractor breakages are
  routine when platforms change their HTML.

### Legal

Per AppWorkFlow.md: download only public content, do not bypass auth, do
not store user cookies. Respect platform ToS and copyright.

---

## License

Apache 2.0 — see project root `LICENSE`.

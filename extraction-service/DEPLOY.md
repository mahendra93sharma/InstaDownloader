# Deploy `reelgrab-extractor` to Render

Step-by-step guide to ship the extraction service as a public HTTPS API on
[Render](https://render.com). After deploy, the Android client (or any HTTP
client) hits `https://<your-host>.onrender.com/resolve` or `/download`.

> TL;DR — push repo to GitHub, create a Render Blueprint pointing at this
> folder, set `API_KEY` + optional `YTDLP_COOKIES_PATH`, done.

---

## 0. Prerequisites

- GitHub account + repo containing this project.
- Render account (free is enough to start: https://dashboard.render.com).
- Optional but **strongly recommended for Instagram**:
  - Netscape-format `cookies.txt` exported from a logged-in (throwaway) IG
    browser session. Browser extension: *Get cookies.txt LOCALLY*.
  - OR a residential proxy URL (Bright Data / Smartproxy / Oxylabs).
- Strong random `API_KEY` (32+ chars). Generate locally:
  ```bash
  openssl rand -hex 32
  ```

---

## 1. Files Render reads

Already in this folder — no edits needed for first deploy.

| File             | Purpose                                                  |
|------------------|----------------------------------------------------------|
| `render.yaml`    | Blueprint: service type, plan, env var keys, healthcheck |
| `Dockerfile`     | Image build: Node 20 + Python 3 + ffmpeg + pinned yt-dlp |
| `src/server.js`  | App entrypoint (Express, `/resolve`, `/download`, `/docs`) |
| `package.json`   | Node deps (`npm install --omit=dev` in image)            |

> Render builds from the Dockerfile, not from `npm install` on the host. Local
> `node_modules/` and `bin/` are ignored — see `.gitignore`.

---

## 2. Push to GitHub

From repo root (the parent of `extraction-service/`):

```bash
git init                       # if not already a repo
git add .
git commit -m "feat(extractor): initial Render deploy"
git branch -M main
git remote add origin git@github.com:<you>/<repo>.git
git push -u origin main
```

If the repo is already on GitHub, just commit + push:

```bash
git add extraction-service
git commit -m "chore(extractor): prepare Render deploy"
git push
```

---

## 3. Create the Render Blueprint

1. https://dashboard.render.com → **New +** → **Blueprint**.
2. Connect GitHub if not done; select the repo.
3. Render scans for `render.yaml`. Confirm it found
   `extraction-service/render.yaml`.
4. Service name auto-fills to `reelgrab-extractor`. Keep or rename.
5. Click **Apply**. Render starts building the Dockerfile (~3–5 min:
   apk add python3, pip install yt-dlp, npm install).

---

## 4. Set environment variables

Service dashboard → **Environment** tab.

### Required

| Key       | Value                                  |
|-----------|----------------------------------------|
| `API_KEY` | 32+ char random string (step 0 output) |

### Strongly recommended for Instagram

Without cookies or a residential proxy, anonymous yt-dlp requests from
Render's datacenter IPs get `login required` / 429 for most reels.

#### Option A — Cookies (cheapest, fragile)

1. Service dashboard → **Secret Files** → **Add Secret File**.
2. Filename: `instagram-cookies.txt`. Paste the Netscape cookies export.
3. Path becomes `/etc/secrets/instagram-cookies.txt`.
4. Back to **Environment** → set:
   ```
   YTDLP_COOKIES_PATH=/etc/secrets/instagram-cookies.txt
   ```
5. Use a **throwaway IG account** — IG may shadow-ban or 2FA-lock it.

#### Option B — Residential proxy (paid, stable)

```
YTDLP_PROXY=http://user:pass@host:port
```

Both options can be set together; yt-dlp uses both.

### Optional tuning

| Key                    | Default | Purpose                                  |
|------------------------|---------|------------------------------------------|
| `CORS_ORIGINS`         | `*`     | Lock to `https://yourapp.com,https://x.com` |
| `RATE_LIMIT_MAX`       | `30`    | Requests per window per IP               |
| `RATE_LIMIT_WINDOW_MS` | `60000` | Window length (ms)                       |
| `YTDLP_TIMEOUT_MS`     | `60000` | Per-request extractor timeout            |
| `YTDLP_USER_AGENT`     | Chrome  | UA string yt-dlp sends                   |

After saving env vars, Render redeploys automatically (~30 s).

---

## 5. Smoke test

URL pattern: `https://<service-slug>.onrender.com`.
Find the exact slug at the top of the service dashboard.

```bash
HOST=https://reelgrab-extractor-xxxx.onrender.com
KEY=<your API_KEY>

# Liveness — no auth required
curl $HOST/health
# → {"status":"ok"}

# Real extraction (replace URL with a public reel)
curl -X POST $HOST/resolve \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $KEY" \
  -d '{"url":"https://www.instagram.com/reel/Cxyz123/"}'
```

Expected `200` with `{ items: [...] }`. See `README.md` → *Error codes* for
non-200 responses (`private`, `rate_limited`, `unsupported`, `invalid_url`).

Swagger UI: `https://<host>/docs` — interactive testing in the browser.
Use the **Authorize** button to paste `X-API-Key`.

---

## 6. Wire up the Android client

In `<repo-root>/local.properties`:

```
EXTRACTION_BASE_URL_DEBUG=https://reelgrab-extractor-xxxx.onrender.com/
EXTRACTION_BASE_URL_RELEASE=https://reelgrab-extractor-xxxx.onrender.com/
EXTRACTION_API_KEY=<same value as Render API_KEY>
```

> Trailing slash matters — Retrofit base URL convention.

Rebuild:
```bash
./gradlew :app:assembleDebug
```

Release builds enforce HTTPS via `CertificatePinner`. Pin Render's cert
fingerprint (or LetsEncrypt root) before shipping a release APK.

---

## 7. Plan & cost notes

| Plan       | Price       | RAM     | Sleep?            | Use case                |
|------------|-------------|---------|-------------------|-------------------------|
| Free       | $0          | 512 MB  | Yes — 15 min idle | Demo / dev only         |
| Starter    | $7/mo       | 512 MB  | No                | Default for this repo   |
| Standard   | $25/mo      | 2 GB    | No                | If OOM appears          |

`render.yaml` ships with `plan: starter`. To use free tier instead, change
to `plan: free` and accept 30–60 s cold-start on first hit after idle.
Android `OkHttpModule.kt` already allows a 90 s read timeout.

---

## 8. Updating yt-dlp

Instagram/Facebook break the extractor every few weeks when they change
their HTML. When `/resolve` starts returning `500 internal`:

1. Check yt-dlp release notes: https://github.com/yt-dlp/yt-dlp/releases.
2. Bump `ARG YTDLP_VERSION=` in `Dockerfile` to the latest tag.
3. Commit + push — Render auto-rebuilds.

Pinning (vs `latest`) avoids surprise outages during a Render redeploy on
a yt-dlp bad-release window.

---

## 9. Security checklist before going public

- [ ] `API_KEY` set and **not** committed to the repo.
- [ ] `CORS_ORIGINS` locked to your domains (no `*` in prod).
- [ ] `RATE_LIMIT_MAX` tuned for your traffic (default 30/min/IP is dev-level).
- [ ] Cookies file (if used) uploaded as **Secret File**, not env var, not repo.
- [ ] HTTPS only — Render terminates TLS automatically; reject any `http://`
      callers via your reverse proxy if you front it.
- [ ] Watch logs for unusual URL patterns — `/resolve` accepts any IG/FB URL
      and runs yt-dlp against it. Abuse = your IP gets rate-limited.
- [ ] Pin yt-dlp version (already done) and rebuild deliberately.

---

## 10. Troubleshooting

| Symptom                           | Likely cause                    | Fix                                     |
|-----------------------------------|---------------------------------|-----------------------------------------|
| `401 unauthorized`                | Missing/wrong `X-API-Key`       | Re-check header value                   |
| `403 {"error":"private"}` always  | Datacenter IP blocked by IG     | Add cookies or proxy (step 4)           |
| `429 {"error":"rate_limited"}`    | yt-dlp hit platform rate limit  | Wait, or rotate proxy IP                |
| `500 {"error":"internal"}`        | yt-dlp extractor broken         | Bump `YTDLP_VERSION` in Dockerfile      |
| `502 Bad Gateway` from Render     | Container crashed at boot       | Check **Logs** tab — usually env-var typo |
| Cold start 30–60 s on first hit   | Free tier idle sleep            | Upgrade to Starter, or accept it        |
| Android client times out          | Server cold-starting > 90 s     | Bump `OkHttp` read timeout              |

Logs: service dashboard → **Logs** tab. Or stream via CLI:
```bash
brew install render
render login
render logs reelgrab-extractor --tail
```

---

## 11. Local Docker parity (optional)

Reproduce the production image locally before pushing:

```bash
cd extraction-service
docker compose up --build
curl -X POST http://localhost:3000/resolve \
  -H 'Content-Type: application/json' \
  -d '{"url":"https://www.instagram.com/reel/Cxyz123/"}'
```

Same Dockerfile Render uses — if it works here, it works there (modulo IG
geolocation blocks on datacenter IPs).

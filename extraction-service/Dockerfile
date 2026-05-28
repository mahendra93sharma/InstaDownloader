FROM node:20-alpine

# yt-dlp needs python3; ffmpeg used by yt-dlp for muxing separate
# video+audio streams. ca-certificates so HTTPS to platform CDNs works.
# yt-dlp is pinned — bump deliberately and rebuild; floating "latest"
# breaks extraction every few weeks when platforms change HTML.
ARG YTDLP_VERSION=2025.01.26
RUN apk add --no-cache python3 py3-pip ffmpeg ca-certificates wget \
  && pip3 install --no-cache-dir --break-system-packages "yt-dlp==${YTDLP_VERSION}" \
  && yt-dlp --version

WORKDIR /app

COPY package.json package-lock.json* ./
RUN npm install --omit=dev

COPY src ./src

ENV PORT=3000 \
    NODE_ENV=production \
    YTDLP_TIMEOUT_MS=60000
EXPOSE 3000

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -qO- http://127.0.0.1:${PORT}/health || exit 1

CMD ["node", "src/server.js"]

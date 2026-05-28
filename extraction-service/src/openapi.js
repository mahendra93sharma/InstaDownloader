/*
 * OpenAPI 3.0 spec for the extraction service.
 *
 * Served at:
 *   GET /openapi.json — raw spec
 *   GET /docs         — Swagger UI
 *
 * Keep this file in sync with src/server.js. The Android wire DTOs in
 * core/network/.../dto/ResolveDto.kt mirror the MediaItem schema below.
 */

const PORT = process.env.PORT || 3000;

module.exports = {
  openapi: '3.0.3',
  info: {
    title: 'ReelGrab extraction service',
    version: '0.2.0',
    description:
      'Extracts direct-media URLs from Instagram and Facebook posts via yt-dlp. ' +
      'Two endpoints: `/resolve` returns the full MediaItem[] consumed by the ' +
      'Android `RemoteMediaRepository`; `/download` is a simpler shape per ' +
      '`AppWorkFlow.md` that feeds Android `DownloadManager` directly.',
  },
  servers: [
    { url: `http://localhost:${PORT}`, description: 'Local dev' },
    { url: `http://10.0.2.2:${PORT}`, description: 'Android emulator loopback' },
  ],
  tags: [
    { name: 'extraction', description: 'Media URL extraction' },
    { name: 'system', description: 'Health / probes' },
  ],
  paths: {
    '/resolve': {
      post: {
        tags: ['extraction'],
        summary: 'Resolve a post URL into MediaItem[]',
        description:
          'Full contract used by the Android client. Returns one item per slide ' +
          'for carousels.',
        requestBody: {
          required: true,
          content: {
            'application/json': {
              schema: { $ref: '#/components/schemas/ResolveRequest' },
              examples: {
                reel:     { value: { url: 'https://www.instagram.com/reel/Cxyz123/' } },
                post:     { value: { url: 'https://www.instagram.com/p/Cxyz123/' } },
                carousel: { value: { url: 'https://www.instagram.com/p/carousel-test/' } },
                mock:     { value: { url: 'https://www.instagram.com/reel/mock-1/' } },
              },
            },
          },
        },
        responses: {
          200: {
            description: 'Extracted media',
            content: {
              'application/json': {
                schema: { $ref: '#/components/schemas/ResolveResponse' },
              },
            },
          },
          400: { $ref: '#/components/responses/InvalidUrl' },
          403: { $ref: '#/components/responses/Private' },
          404: { $ref: '#/components/responses/Unsupported' },
          429: { $ref: '#/components/responses/RateLimited' },
          500: { $ref: '#/components/responses/Internal' },
        },
      },
    },

    '/download': {
      post: {
        tags: ['extraction'],
        summary: 'Resolve a post URL into a single direct video URL',
        description:
          'Workflow-style shape from `AppWorkFlow.md`. Returns the direct MP4 ' +
          'URL of the first VIDEO item — pass straight to Android ' +
          '`DownloadManager`. Use `/resolve` if you need every carousel slide.',
        requestBody: {
          required: true,
          content: {
            'application/json': {
              schema: { $ref: '#/components/schemas/ResolveRequest' },
              examples: {
                reel: { value: { url: 'https://www.instagram.com/reel/Cxyz123/' } },
                mock: { value: { url: 'https://www.instagram.com/reel/mock-1/' } },
              },
            },
          },
        },
        responses: {
          200: {
            description: 'Direct video URL',
            content: {
              'application/json': {
                schema: { $ref: '#/components/schemas/DownloadResponse' },
              },
            },
          },
          400: { $ref: '#/components/responses/InvalidUrl' },
          403: { $ref: '#/components/responses/Private' },
          404: { $ref: '#/components/responses/Unsupported' },
          429: { $ref: '#/components/responses/RateLimited' },
          500: { $ref: '#/components/responses/Internal' },
        },
      },
    },

    '/health': {
      get: {
        tags: ['system'],
        summary: 'Liveness probe',
        responses: {
          200: {
            description: 'OK',
            content: {
              'application/json': {
                schema: {
                  type: 'object',
                  properties: { status: { type: 'string', example: 'ok' } },
                },
              },
            },
          },
        },
      },
    },
  },

  components: {
    schemas: {
      ResolveRequest: {
        type: 'object',
        required: ['url'],
        properties: {
          url: {
            type: 'string',
            format: 'uri',
            description: 'Public Instagram or Facebook post URL.',
            example: 'https://www.instagram.com/reel/Cxyz123/',
          },
        },
      },

      ResolveResponse: {
        type: 'object',
        required: ['items'],
        properties: {
          items: {
            type: 'array',
            items: { $ref: '#/components/schemas/MediaItem' },
          },
        },
      },

      DownloadResponse: {
        type: 'object',
        required: ['video'],
        properties: {
          video: {
            type: 'string',
            format: 'uri',
            example: 'https://cdn.example.com/abc.mp4',
          },
          thumbnail: { type: 'string', format: 'uri', nullable: true },
          durationMs: { type: 'integer', nullable: true, example: 14820 },
        },
      },

      MediaItem: {
        type: 'object',
        required: ['id', 'sourceUrl', 'directUrl', 'thumbnailUrl', 'type', 'width', 'height'],
        properties: {
          id:           { type: 'string', example: 'Cxyz123-0' },
          sourceUrl:    { type: 'string', format: 'uri' },
          directUrl:    { type: 'string', format: 'uri' },
          thumbnailUrl: { type: 'string', format: 'uri' },
          type:         { type: 'string', enum: ['VIDEO', 'IMAGE'] },
          durationMs:   { type: 'integer', nullable: true, description: 'VIDEO only.' },
          width:        { type: 'integer', example: 1080 },
          height:       { type: 'integer', example: 1920 },
          sizeBytes:    { type: 'integer', nullable: true },
        },
      },

      ErrorBody: {
        type: 'object',
        required: ['error'],
        properties: {
          error: {
            type: 'string',
            enum: ['invalid_url', 'private', 'unsupported', 'rate_limited', 'internal'],
          },
        },
      },
    },

    responses: {
      InvalidUrl: {
        description: 'Request body failed URL validation.',
        content: {
          'application/json': {
            schema: { $ref: '#/components/schemas/ErrorBody' },
            example: { error: 'invalid_url' },
          },
        },
      },
      Private: {
        description: 'Content requires login.',
        content: {
          'application/json': {
            schema: { $ref: '#/components/schemas/ErrorBody' },
            example: { error: 'private' },
          },
        },
      },
      Unsupported: {
        description: 'Host or media type not supported.',
        content: {
          'application/json': {
            schema: { $ref: '#/components/schemas/ErrorBody' },
            example: { error: 'unsupported' },
          },
        },
      },
      RateLimited: {
        description: 'Upstream throttled — back off and retry.',
        content: {
          'application/json': {
            schema: { $ref: '#/components/schemas/ErrorBody' },
            example: { error: 'rate_limited' },
          },
        },
      },
      Internal: {
        description: 'Unhandled extractor failure.',
        content: {
          'application/json': {
            schema: { $ref: '#/components/schemas/ErrorBody' },
            example: { error: 'internal' },
          },
        },
      },
    },
  },
};

/*
 * Liveness probe — used by Docker / Kubernetes / GitHub Actions to assert
 * the service is up. Intentionally cheap and side-effect free.
 */

const express = require('express');

const healthRouter = express.Router();

healthRouter.get('/health', (_req, res) => {
  res.json({ status: 'ok' });
});

module.exports = { healthRouter };

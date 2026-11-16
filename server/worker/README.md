# Kitsune API (Cloudflare Worker)

Verifies Google Play one-time purchases and serves signed, short-lived theme-pack downloads from a private R2 bucket.

Setup: `npm i`, create the R2 bucket `kitsune-theme-packs` and a KV namespace (put its id in `wrangler.toml`), then
`wrangler secret put GOOGLE_SERVICE_ACCOUNT_JSON` and `wrangler secret put URL_SIGNING_KEY`, `wrangler deploy`.
Grant the service account access to the app in Play Console (Users and permissions). Build the app with
`-PkitsuneApiBase=https://<your-worker>.workers.dev`. Until that is set the app fails closed: nothing unlocks.

Product ids: `kitsune_pro` (Pro editor + all packs) and `theme_pack_<name>`. Packs are validated data bundles, never code.
This is access control, not DRM.

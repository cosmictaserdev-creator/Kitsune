// Kitsune API: verifies Google Play one-time purchases and hands out short-lived signed theme-pack URLs.
// This is access control, not DRM: a pack on a user's device can always be copied by that user.

interface Env {
  PACKS: R2Bucket; ENTITLEMENTS: KVNamespace
  GOOGLE_SERVICE_ACCOUNT_JSON: string; URL_SIGNING_KEY: string; ANDROID_PACKAGE: string
}

const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json" } })

const enc = new TextEncoder()
const b64url = (buf: ArrayBuffer | Uint8Array) =>
  btoa(String.fromCharCode(...new Uint8Array(buf))).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "")

async function sha256Hex(s: string) {
  return [...new Uint8Array(await crypto.subtle.digest("SHA-256", enc.encode(s)))].map(b => b.toString(16).padStart(2, "0")).join("")
}

async function hmac(key: string, data: string) {
  const k = await crypto.subtle.importKey("raw", enc.encode(key), { name: "HMAC", hash: "SHA-256" }, false, ["sign"])
  return b64url(await crypto.subtle.sign("HMAC", k, enc.encode(data)))
}

/** OAuth access token for the Play Developer API from a service account (RS256 JWT). */
async function googleAccessToken(env: Env): Promise<string> {
  const sa = JSON.parse(env.GOOGLE_SERVICE_ACCOUNT_JSON)
  const now = Math.floor(Date.now() / 1000)
  const header = b64url(enc.encode(JSON.stringify({ alg: "RS256", typ: "JWT" })))
  const claim = b64url(enc.encode(JSON.stringify({
    iss: sa.client_email, scope: "https://www.googleapis.com/auth/androidpublisher",
    aud: "https://oauth2.googleapis.com/token", iat: now, exp: now + 3600 })))
  const pem = sa.private_key.replace(/-----[^-]+-----|\s/g, "")
  const der = Uint8Array.from(atob(pem), c => c.charCodeAt(0))
  const key = await crypto.subtle.importKey("pkcs8", der, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["sign"])
  const sig = b64url(await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, enc.encode(`${header}.${claim}`)))
  const res = await fetch("https://oauth2.googleapis.com/token", { method: "POST",
    headers: { "content-type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion: `${header}.${claim}.${sig}` }) })
  if (!res.ok) throw new Error("google auth failed")
  return (await res.json<{ access_token: string }>()).access_token
}

/** purchaseState 0 = purchased. Pending (2) and cancelled (1) are never entitled. */
async function verifyPurchase(env: Env, productId: string, token: string): Promise<boolean> {
  const access = await googleAccessToken(env)
  const url = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${env.ANDROID_PACKAGE}/purchases/products/${encodeURIComponent(productId)}/tokens/${encodeURIComponent(token)}`
  const res = await fetch(url, { headers: { authorization: `Bearer ${access}` } })
  if (!res.ok) return false
  const p = await res.json<{ purchaseState?: number; consumptionState?: number }>()
  return p.purchaseState === 0
}

export default {
  async fetch(req: Request, env: Env): Promise<Response> {
    const url = new URL(req.url)

    // POST /verify {packageName, productId, purchaseToken}  ->  200 when genuinely purchased
    if (req.method === "POST" && url.pathname === "/verify") {
      const { packageName, productId, purchaseToken } = await req.json<Record<string, string>>().catch(() => ({} as Record<string, string>))
      if (!productId || !purchaseToken || packageName !== env.ANDROID_PACKAGE) return json({ error: "bad request" }, 400)
      if (!(await verifyPurchase(env, productId, purchaseToken))) return json({ entitled: false }, 403)
      await env.ENTITLEMENTS.put(await sha256Hex(purchaseToken), JSON.stringify({ productId, at: Date.now() }))
      return json({ entitled: true })
    }

    // POST /pack-url {packId, purchaseToken} -> short-lived signed download URL (kitsune_pro unlocks every pack)
    if (req.method === "POST" && url.pathname === "/pack-url") {
      const { packId, purchaseToken } = await req.json<Record<string, string>>().catch(() => ({} as Record<string, string>))
      if (!packId || !/^[a-z0-9_-]+$/.test(packId) || !purchaseToken) return json({ error: "bad request" }, 400)
      const record = await env.ENTITLEMENTS.get(await sha256Hex(purchaseToken), "json") as { productId: string } | null
      if (!record || (record.productId !== "kitsune_pro" && record.productId !== `theme_pack_${packId}`)) return json({ error: "not entitled" }, 403)
      const exp = Math.floor(Date.now() / 1000) + 300
      const sig = await hmac(env.URL_SIGNING_KEY, `${packId}.${exp}`)
      return json({ url: `${url.origin}/download/${packId}?exp=${exp}&sig=${sig}`, expiresAt: exp })
    }

    // GET /download/:packId?exp&sig -> the pack from R2 if the signature is valid and unexpired
    if (req.method === "GET" && url.pathname.startsWith("/download/")) {
      const packId = url.pathname.slice("/download/".length)
      const exp = Number(url.searchParams.get("exp")), sig = url.searchParams.get("sig") ?? ""
      if (!/^[a-z0-9_-]+$/.test(packId) || !exp || exp < Date.now() / 1000 || sig !== await hmac(env.URL_SIGNING_KEY, `${packId}.${exp}`))
        return json({ error: "forbidden" }, 403)
      const obj = await env.PACKS.get(`${packId}.kpack`)
      if (!obj) return json({ error: "not found" }, 404)
      return new Response(obj.body, { headers: { "content-type": "application/octet-stream", "cache-control": "private, no-store" } })
    }

    return json({ error: "not found" }, 404)
  },
}

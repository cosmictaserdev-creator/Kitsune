// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.billing

import android.app.Activity
import android.content.Context
import androidx.core.content.edit
import com.android.billingclient.api.*
import helium314.keyboard.latin.BuildConfig
import helium314.keyboard.latin.utils.prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.coroutines.resume

/** Play product ids. Packs follow the pattern `theme_pack_<name>`; they must exist in Play Console before they can be bought. */
object Products {
    const val PRO = "kitsune_pro"
    fun pack(name: String) = "theme_pack_$name"
}

/**
 * One-time products only. A purchase is acknowledged **after** the Cloudflare Worker verifies the token with
 * Google; until then it stays pending. Entitlements are cached locally for offline use and re-verified on launch.
 * Nothing here grants access without a verified purchase.
 */
class KitsuneBilling private constructor(private val context: Context) : PurchasesUpdatedListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val http = OkHttpClient()
    private val client = BillingClient.newBuilder(context).setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()).build()
    private val _owned = MutableStateFlow(loadOwned())
    val owned = _owned.asStateFlow()
    private val _message = MutableStateFlow("")
    val message = _message.asStateFlow()

    fun isOwned(productId: String) = productId in _owned.value || Products.PRO in _owned.value
    val isPro get() = Products.PRO in _owned.value

    private fun loadOwned() = context.prefs().getStringSet("kitsune_owned_products", emptySet()).orEmpty()

    private suspend fun ensureConnected(): Boolean = client.isReady || suspendCancellableCoroutine { cont ->
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) { if (cont.isActive) cont.resume(result.responseCode == BillingClient.BillingResponseCode.OK) }
            override fun onBillingServiceDisconnected() {}
        })
    }

    /** Localized name/price from Play. Never hardcode prices. */
    suspend fun details(productId: String): ProductDetails? {
        if (!ensureConnected()) return null
        val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(
            QueryProductDetailsParams.Product.newBuilder().setProductId(productId).setProductType(BillingClient.ProductType.INAPP).build())).build()
        return suspendCancellableCoroutine { cont -> client.queryProductDetailsAsync(params) { _, list -> if (cont.isActive) cont.resume(list.firstOrNull()) } }
    }

    fun buy(activity: Activity, productId: String) {
        scope.launch {
            val d = details(productId) ?: run { _message.value = "This item is not available yet."; return@launch }
            val flow = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d).build())).build()
            activity.runOnUiThread { client.launchBillingFlow(activity, flow) }
        }
    }

    /** Restores purchases (new device, reinstall) by re-verifying every owned one-time product. */
    fun restore() = scope.launch {
        if (!ensureConnected()) return@launch
        val result = client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build())
        handle(result.purchasesList)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> scope.launch { handle(purchases.orEmpty()) }
            BillingClient.BillingResponseCode.USER_CANCELED -> _message.value = ""
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> restore()
            else -> _message.value = "Purchase failed. You were not charged."
        }
    }

    private suspend fun handle(purchases: List<Purchase>) {
        for (p in purchases) {
            if (p.purchaseState == Purchase.PurchaseState.PENDING) { _message.value = "Payment pending. Access unlocks when it clears."; continue }
            if (p.purchaseState != Purchase.PurchaseState.PURCHASED) continue
            if (!verify(p)) { _message.value = "Couldn't verify this purchase. Check your connection and tap Restore."; continue }
            if (!p.isAcknowledged) {
                val ack = suspendCancellableCoroutine { cont ->
                    client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()) { if (cont.isActive) cont.resume(it) } }
                if (ack.responseCode != BillingClient.BillingResponseCode.OK) continue
            }
            val ids = _owned.value + p.products
            context.prefs().edit { putStringSet("kitsune_owned_products", ids) }
            _owned.value = ids
            _message.value = ""
        }
    }

    /** The Worker asks Google (service account) whether the token is genuine and purchased. */
    private fun verify(p: Purchase): Boolean {
        val base = BuildConfig.KITSUNE_API_BASE
        if (base.isBlank()) return false // fail closed: no server, no entitlement
        val body = """{"packageName":"${context.packageName}","productId":"${p.products.first()}","purchaseToken":"${p.purchaseToken}"}"""
        return runCatching {
            http.newCall(Request.Builder().url("$base/verify").post(body.toRequestBody("application/json".toMediaType())).build()).execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }

    companion object {
        @Volatile private var instance: KitsuneBilling? = null
        fun get(context: Context) = instance ?: synchronized(this) { instance ?: KitsuneBilling(context.applicationContext).also { instance = it } }
    }
}

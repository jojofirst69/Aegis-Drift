package com.aegisdrift.bot.api

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import android.util.Base64
import com.aegisdrift.bot.data.TradingMode

data class Candle(
    val openTime: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)

class BitgetClient(
    private val apiKey: String,
    private val apiSecret: String,
    private val passphrase: String,
    private val tradingMode: String = TradingMode.PAPER
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson    = Gson()
    private val BASE    = "https://api.bitget.com"
    private val JSON_MT = "application/json; charset=utf-8".toMediaType()

    fun fetchCandles(symbol: String, granularity: String, limit: Int = 200): List<Candle> {
        val params = "symbol=$symbol&productType=USDT-FUTURES&granularity=$granularity&limit=$limit"
        val path   = "/api/v2/mix/market/history-candles?$params"
        val req    = Request.Builder()
            .url("$BASE$path")
            .addHeader("User-Agent", "Mozilla/5.0")
            .get().build()

        val body = client.newCall(req).execute().body?.string() ?: return emptyList()
        val map  = gson.fromJson<Map<String, Any>>(
            body, object : TypeToken<Map<String, Any>>() {}.type)
        if (map["code"] != "00000") return emptyList()

        @Suppress("UNCHECKED_CAST")
        val rows = map["data"] as? List<List<Any>> ?: return emptyList()
        return rows.mapNotNull { r ->
            try {
                Candle(
                    openTime = (r[0] as String).toLong(),
                    open     = (r[1] as String).toDouble(),
                    high     = (r[2] as String).toDouble(),
                    low      = (r[3] as String).toDouble(),
                    close    = (r[4] as String).toDouble(),
                    volume   = (r[5] as String).toDouble()
                )
            } catch (e: Exception) { null }
        }.sortedBy { it.openTime }
    }

    fun placeOrder(
        symbol: String,
        side: String,
        size: String,
        reduceOnly: Boolean = false
    ): Boolean {
        return when (tradingMode) {
            TradingMode.PAPER -> true
            TradingMode.DEMO,
            TradingMode.LIVE  -> sendOrder(symbol, side, size, reduceOnly)
            else              -> true
        }
    }

    private fun sendOrder(
        symbol: String,
        side: String,
        size: String,
        reduceOnly: Boolean
    ): Boolean {
        val path = "/api/v2/mix/order/place-order"
        val payload = mapOf(
            "symbol"      to symbol,
            "productType" to "USDT-FUTURES",
            "marginMode"  to "crossed",
            "marginCoin"  to "USDT",
            "size"        to size,
            "side"        to side,
            "orderType"   to "market",
            "reduceOnly"  to reduceOnly.toString(),
            "tradeSide"   to if (reduceOnly) "close" else "open"
        )
        val bodyStr = gson.toJson(payload)
        val ts      = System.currentTimeMillis().toString()
        val sign    = sign(ts, "POST", path, bodyStr)

        val req = Request.Builder()
            .url("$BASE$path")
            .addHeader("ACCESS-KEY",        apiKey)
            .addHeader("ACCESS-SIGN",       sign)
            .addHeader("ACCESS-TIMESTAMP",  ts)
            .addHeader("ACCESS-PASSPHRASE", passphrase)
            .addHeader("locale",            "en-US")
            .apply {
                if (tradingMode == TradingMode.DEMO) addHeader("paptrading", "1")
            }
            .post(bodyStr.toRequestBody(JSON_MT))
            .build()

        val resp = client.newCall(req).execute().body?.string() ?: return false
        val map  = gson.fromJson<Map<String, Any>>(
            resp, object : TypeToken<Map<String, Any>>() {}.type)
        return map["code"] == "00000"
    }

    private fun sign(timestamp: String, method: String, path: String, body: String): String {
        val message = "$timestamp$method$path$body"
        val mac     = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(apiSecret.toByteArray(), "HmacSHA256"))
        return Base64.encodeToString(mac.doFinal(message.toByteArray()), Base64.NO_WRAP)
    }
}

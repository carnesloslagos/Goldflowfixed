package com.goldflow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.abs

data class MarketState(
    val connected: Boolean = false,
    val price: Double = 0.0,
    val bestBid: Double = 0.0,
    val bestAsk: Double = 0.0,
    val bidVolume: Double = 0.0,
    val askVolume: Double = 0.0,
    val bidPct: Int = 50,
    val delta: Double = 0.0,
    val cvd: Double = 0.0,
    val cvdHistory: List<Double> = listOf(0.0),
    val signal: String = "ESPERAR",
    val reading: String = "Esperando datos..."
)

class GoldFlowViewModel : ViewModel() {
    private val _state = MutableStateFlow(MarketState())
    val state = _state.asStateFlow()

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var socket: WebSocket? = null
    private var cvd = 0.0
    private var currentDelta = 0.0

    fun toggle() {
        if (_state.value.connected) disconnect() else connect()
    }

    fun connect() {
        disconnect()
        // USDⓈ-M Futures public market stream.
        val url = "wss://fstream.binance.com/stream?streams=xauusdt@aggTrade/xauusdt@depth20@100ms"
        socket = client.newWebSocket(
            Request.Builder().url(url).build(),
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    _state.value = _state.value.copy(
                        connected = true,
                        reading = "Conectado. Procesando trades y libro..."
                    )
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val root = JSONObject(text)
                        val data = root.optJSONObject("data") ?: root
                        when (data.optString("e")) {
                            "aggTrade" -> onTrade(data)
                            "depthUpdate" -> onDepth(data)
                        }
                    } catch (_: Exception) {
                        // Ignore malformed/non-market messages.
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    _state.value = _state.value.copy(
                        connected = false,
                        reading = "Desconectado: ${t.message ?: "error de red"}"
                    )
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    _state.value = _state.value.copy(connected = false)
                }
            }
        )
    }

    private fun onTrade(data: JSONObject) {
        val price = data.optString("p").toDoubleOrNull() ?: return
        val qty = data.optString("q").toDoubleOrNull() ?: return
        val buyerIsMaker = data.optBoolean("m", false)

        // Binance aggTrade: m=true means buyer is maker, so the taker was selling.
        val signed = if (buyerIsMaker) -qty else qty
        currentDelta += signed
        cvd += signed

        val old = _state.value.cvdHistory
        val next = (old + cvd).takeLast(180)

        val signal = when {
            signed > 0 && cvd > 0 -> "LONG"
            signed < 0 && cvd < 0 -> "SHORT"
            else -> "ESPERAR"
        }

        val reading = when {
            signed > 0 -> "Compras agresivas: el taker está levantando el ask."
            signed < 0 -> "Ventas agresivas: el taker está golpeando el bid."
            else -> "Flujo mixto."
        }

        _state.value = _state.value.copy(
            price = price,
            delta = currentDelta,
            cvd = cvd,
            cvdHistory = next,
            signal = signal,
            reading = reading
        )
        currentDelta = 0.0
    }

    private fun onDepth(data: JSONObject) {
        val bids = data.optJSONArray("b") ?: return
        val asks = data.optJSONArray("a") ?: return
        var bidVol = 0.0
        var askVol = 0.0
        var bestBid = 0.0
        var bestAsk = 0.0

        for (i in 0 until bids.length()) {
            val row = bids.optJSONArray(i) ?: continue
            val p = row.optString(0).toDoubleOrNull() ?: continue
            val q = row.optString(1).toDoubleOrNull() ?: continue
            if (i == 0) bestBid = p
            bidVol += q
        }
        for (i in 0 until asks.length()) {
            val row = asks.optJSONArray(i) ?: continue
            val p = row.optString(0).toDoubleOrNull() ?: continue
            val q = row.optString(1).toDoubleOrNull() ?: continue
            if (i == 0) bestAsk = p
            askVol += q
        }

        val total = bidVol + askVol
        val bidPct = if (total > 0) ((bidVol / total) * 100).toInt() else 50

        _state.value = _state.value.copy(
            bestBid = bestBid,
            bestAsk = bestAsk,
            bidVolume = bidVol,
            askVolume = askVol,
            bidPct = bidPct
        )
    }

    fun disconnect() {
        socket?.close(1000, "user")
        socket = null
        _state.value = _state.value.copy(connected = false)
    }

    override fun onCleared() {
        disconnect()
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }
}

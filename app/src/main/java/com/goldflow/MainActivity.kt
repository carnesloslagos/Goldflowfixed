package com.goldflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GoldFlowApp() }
    }
}

@Composable
fun GoldFlowApp(vm: GoldFlowViewModel = viewModel()) {
    val state by vm.state.collectAsState()

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF0B0F14),
            surface = Color(0xFF111821),
            primary = Color(0xFF7DB7FF)
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0B0F14))
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("GOLD FLOW", style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold)
                        Text("Binance XAUUSDT • Order Flow", color = Color.LightGray)
                    }
                    StatusPill(state.connected)
                }

                Spacer(Modifier.height(12.dp))

                PriceCard(state)

                Spacer(Modifier.height(10.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard("DELTA", fmt(state.delta), state.delta >= 0, Modifier.weight(1f))
                    MetricCard("CVD", fmt(state.cvd), state.cvd >= 0, Modifier.weight(1f))
                    MetricCard("BOOK", "${state.bidPct}% B", state.bidPct >= 50, Modifier.weight(1f))
                }

                Spacer(Modifier.height(10.dp))

                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF111821))) {
                    Column(Modifier.padding(12.dp)) {
                        Text("CVD EN TIEMPO REAL", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        CvdChart(state.cvdHistory)
                        Text(
                            "La pendiente importa más que el número absoluto. ↑ compradores agresivos; ↓ vendedores agresivos.",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF111821))) {
                    Column(Modifier.padding(12.dp)) {
                        Text("LECTURA", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            state.reading,
                            color = when (state.signal) {
                                "LONG" -> Color(0xFF69D391)
                                "SHORT" -> Color(0xFFFF6B6B)
                                else -> Color(0xFFFFD166)
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Regla base: ZONA → AGRESIÓN → REACCIÓN. Esta V1 no ejecuta órdenes.",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF111821))) {
                    Column(Modifier.padding(12.dp)) {
                        Text("TOP LIQUIDEZ", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Bid: ${fmt2(state.bestBid)}", color = Color(0xFF69D391))
                        Text("Ask: ${fmt2(state.bestAsk)}", color = Color(0xFFFF6B6B))
                        Text("Bid/Ask volumen: ${fmt(state.bidVolume)} / ${fmt(state.askVolume)}")
                    }
                }

                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = { vm.toggle() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.connected) "DESCONECTAR" else "CONECTAR A BINANCE")
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "V0.1 • Datos públicos de mercado. No es asesoría financiera.",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun StatusPill(connected: Boolean) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (connected) Color(0xFF173D2A) else Color(0xFF3A2020)
    ) {
        Text(
            if (connected) "● LIVE" else "● OFFLINE",
            color = if (connected) Color(0xFF69D391) else Color(0xFFFF6B6B),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
fun PriceCard(s: MarketState) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF111821))) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("XAUUSDT", color = Color.Gray)
                Text(fmt2(s.price), style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("SEÑAL", color = Color.Gray)
                Text(
                    s.signal,
                    fontWeight = FontWeight.Bold,
                    color = when(s.signal) {
                        "LONG" -> Color(0xFF69D391)
                        "SHORT" -> Color(0xFFFF6B6B)
                        else -> Color(0xFFFFD166)
                    }
                )
            }
        }
    }
}

@Composable
fun MetricCard(title: String, value: String, positive: Boolean, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color(0xFF111821))) {
        Column(Modifier.padding(10.dp)) {
            Text(title, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            Text(value, fontWeight = FontWeight.Bold,
                color = if (positive) Color(0xFF69D391) else Color(0xFFFF6B6B))
        }
    }
}

@Composable
fun CvdChart(values: List<Double>) {
    Canvas(Modifier.fillMaxWidth().height(150.dp)) {
        if (values.size < 2) return@Canvas
        val min = values.minOrNull() ?: 0.0
        val max = values.maxOrNull() ?: 1.0
        val range = (max - min).takeIf { it > 0.0 } ?: 1.0
        val step = size.width / (values.size - 1)
        for (i in 1 until values.size) {
            val x1 = (i - 1) * step
            val x2 = i * step
            val y1 = size.height - ((values[i - 1] - min) / range * size.height).toFloat()
            val y2 = size.height - ((values[i] - min) / range * size.height).toFloat()
            drawLine(
                Color(0xFF7DB7FF),
                Offset(x1, y1),
                Offset(x2, y2),
                strokeWidth = 3f
            )
        }
    }
}

fun fmt(v: Double): String = if (kotlin.math.abs(v) >= 1000) {
    String.format("%,.0f", v)
} else String.format("%,.1f", v)

fun fmt2(v: Double): String = String.format("%.2f", v)

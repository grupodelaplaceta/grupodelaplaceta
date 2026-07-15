package org.laplaceta.placetajunior.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.laplaceta.placetajunior.network.HttpHelper
import org.laplaceta.placetajunior.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonederoScreen(
    dip: String,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var data by remember { mutableStateOf<JSONObject?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(dip) {
        try {
            val result = HttpHelper.get("https://grupodelaplaceta.vercel.app/api/junior/monedero?dip=$dip")
            result.fold(
                onSuccess = { json -> data = JSONObject(json) },
                onFailure = { e -> error = e.message }
            )
        } catch (e: Exception) {
            error = e.message
        } finally {
            isLoading = false
        }
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize().background(PJWhite), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PJPurple)
        }
        return
    }

    // Header
    Column(Modifier.fillMaxSize().background(PJGray50)) {
        Row(
            Modifier.fillMaxWidth().background(PJPurple).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver", tint = PJWhite) }
            Spacer(Modifier.width(8.dp))
            Text("💰 Monedero Junior", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PJWhite)
        }

        if (error != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚠️", fontSize = 48.sp)
                    Text("Error: $error", color = PJRed, fontSize = 14.sp)
                }
            }
            return
        }

        val d = data ?: return

        LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // ── Cuenta Bancaria Card ──────────────────────────────
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PJPurple)
                ) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        // Account type badge
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(PJWhite.copy(alpha = 0.15f))
                                .padding(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "👶 Cuenta Child",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PJWhite
                            )
                        }
                        Spacer(Modifier.height(12.dp))

                        // Saldo
                        Text(
                            "${d.optInt("saldo_actual", 0)} Pz",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = PJYellow
                        )
                        Text("Saldo disponible", color = PJWhite.copy(alpha = 0.6f), fontSize = 12.sp)

                        Spacer(Modifier.height(16.dp))

                        // IBAN real del banco
                        val ibanReal = d.optJSONObject("cuenta_bancaria")?.optString("iban") ?: ""
                        if (ibanReal.isNotEmpty()) {
                            Card(
                                Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = PJWhite.copy(alpha = 0.1f))
                            ) {
                                Column(Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🏦", fontSize = 16.sp)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "IBAN Banco de La Placeta",
                                            fontSize = 11.sp,
                                            color = PJWhite.copy(alpha = 0.6f)
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = ibanReal,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PJWhite,
                                        fontFamily = OutfitRegular,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Titular y Cotitular
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = PJWhite.copy(alpha = 0.08f))
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("👤", fontSize = 14.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text("Titular", fontSize = 10.sp, color = PJWhite.copy(alpha = 0.5f))
                                        val nombreMenor = d.optString("nombre_menor", "").ifEmpty { "Menor" }
                                        Text(nombreMenor, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PJWhite)
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("👤", fontSize = 14.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text("Cotitular (Tutor legal)", fontSize = 10.sp, color = PJWhite.copy(alpha = 0.5f))
                                        val tutorNombre = d.optString("tutor_nombre", "").ifEmpty { "Tutor" }
                                        Text(tutorNombre, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PJWhite)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📥 Ingresos", color = PJWhite.copy(alpha = 0.6f), fontSize = 10.sp)
                                Text("+${d.optInt("ingresos_totales", 0)} Pz", color = PJGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📤 Hoy", color = PJWhite.copy(alpha = 0.6f), fontSize = 10.sp)
                                Text("-${d.optInt("gasto_hoy", 0)} Pz", color = PJRed, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📊 Semana", color = PJWhite.copy(alpha = 0.6f), fontSize = 10.sp)
                                Text("-${d.optInt("gasto_semana", 0)} Pz", color = PJOrange, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ── RBU — Renta Básica Universal ─────────────────────
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PJGreenLight)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎁", fontSize = 28.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "RBU — Renta Básica Universal",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = PJGreen
                                )
                                Text(
                                    "5 Pz / día",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    color = PJGreen
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Es un regalo en Placetas que hace la Fundación Banco de La Placeta. " +
                                   "Tienen Placetas que han juntado de muchas personas y entidades que " +
                                   "voluntariamente han querido que se den a gente como tú. " +
                                   "¡Cada día recibes 5 Pz solo por ser parte de La Placeta!",
                            fontSize = 12.sp,
                            color = PJGray700,
                            lineHeight = 17.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🏛️", fontSize = 12.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Fundación Banco de La Placeta — RBU Infantil",
                                fontSize = 10.sp,
                                color = PJGray400
                            )
                        }
                    }
                }
            }

            // ── Límites card ─────────────────────────────────────
            val limites = d.optJSONObject("limites")
            if (limites != null) {
                item {
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PJWhite)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("🛡️ Límites", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PJGray900)
                            Spacer(Modifier.height(12.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                LimitItem("Gasto diario", "${limites.optInt("gasto_diario", 10)} Pz", PJBlue)
                                LimitItem("Gasto semanal", "${limites.optInt("gasto_semanal", 50)} Pz", PJPurple)
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                LimitItem("Aprobación tutor", "${limites.optInt("limite_aprobacion_tutor", 1000)} Pz", PJOrange)
                                LimitItem("Tiempo uso", "${limites.optInt("tiempo_uso", 60)} min", PJGreen)
                            }
                        }
                    }
                }
            }

            // ── RBU acumulado + Disponible hoy ────────────────────
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(
                        Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PJWhite)
                    ) {
                        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🎁 RBU recibido", fontSize = 11.sp, color = PJGray500)
                            Text(
                                "+${d.optInt("ingresos_totales", 0)} Pz",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = PJGreen
                            )
                        }
                    }
                    Card(
                        Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PJWhite)
                    ) {
                        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Semana disponible", fontSize = 11.sp, color = PJGray500)
                            Text(
                                "${d.optInt("saldo_disponible_semana", d.optInt("saldo_actual", 0))} Pz",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = PJBlue
                            )
                        }
                    }
                }
            }

            // ── Historial ────────────────────────────────────────
            val historial = d.optJSONArray("historial")
            if (historial != null && historial.length() > 0) {
                item {
                    Text("📋 Movimientos", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PJGray900, modifier = Modifier.padding(top = 8.dp))
                }
                val txList = mutableListOf<JSONObject>()
                for (i in 0 until historial.length()) txList.add(historial.getJSONObject(i))
                items(txList) { tx ->
                    val tipo = tx.optString("tipo", "otro")
                    val esIngreso = tipo == "rbu" || tipo == "ganar" || tipo == "ingreso"
                    val icono = when (tipo) {
                        "rbu" -> "🎁"
                        "ganar" -> "🏆"
                        "gastar" -> "🛒"
                        "transferencia" -> "💸"
                        else -> "📌"
                    }
                    val color = if (esIngreso) PJGreen else PJRed
                    val signo = if (esIngreso) "+" else "-"

                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = PJWhite)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                                Text(icono, fontSize = 18.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(tx.optString("concepto", "Movimiento"), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = PJGray900)
                                Text(tx.optString("creado_en", "").take(10), fontSize = 11.sp, color = PJGray400)
                            }
                            Text(
                                "$signo${tx.optInt("cantidad", 0)} Pz",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = color
                            )
                        }
                    }
                }
            } else {
                item {
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PJWhite)
                    ) {
                        Column(Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📭", fontSize = 40.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("Sin movimientos aún", fontSize = 14.sp, color = PJGray500)
                            Text("¡Empieza a usar tu monedero!", fontSize = 12.sp, color = PJGray400)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun LimitItem(label: String, value: String, color: Color) {
    Column(Modifier.padding(4.dp)) {
        Text(label, fontSize = 11.sp, color = PJGray500)
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

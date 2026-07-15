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
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgresoScreen(
    dip: String,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var data by remember { mutableStateOf<JSONObject?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var expandedMateria by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(dip) {
        try {
            val result = HttpHelper.get("https://grupodelaplaceta.vercel.app/api/junior/academy/cuestionarios?dip=$dip")
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

    Column(Modifier.fillMaxSize().background(PJGray50)) {
        Row(
            Modifier.fillMaxWidth().background(PJPurple).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver", tint = PJWhite) }
            Spacer(Modifier.width(8.dp))
            Text("📊 Mi Progreso", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PJWhite)
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
        val progreso = d.optJSONObject("progreso")
        val nivelActual = d.optInt("nivel_actual", 1)
        val puntuacionTotal = progreso?.optInt("puntuacion_total", 0) ?: 0
        val completados = progreso?.optJSONObject("completados")

        LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // ── Stats top ─────────────────────────────────────────
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PJPurple)
                ) {
                    Row(Modifier.padding(24.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatItem("🏆", "Nivel", "$nivelActual", PJYellow)
                        StatItem("⭐", "XP Total", "$puntuacionTotal", PJGreen)
                        StatItem("🪙", "Saldo", "${d.optInt("placetas_saldo", 0)} Pz", PJOrange)
                    }
                }
            }

            // ── Progress bar niveles ──────────────────────────────
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PJWhite)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("🎯 Progreso de niveles", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PJGray900)
                        Spacer(Modifier.height(12.dp))
                        Text("${nivelActual}/10 niveles", fontSize = 13.sp, color = PJGray500)
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { nivelActual.toFloat() / 10f },
                            modifier = Modifier.fillMaxWidth().height(10.dp),
                            color = PJPurple,
                            trackColor = PJPurpleLight,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (nivelActual >= 10) "🎉 ¡Nivel máximo alcanzado!" else "Faltan ${10 - nivelActual} niveles para completar",
                            fontSize = 12.sp,
                            color = PJGray500
                        )
                    }
                }
            }

            // ── Por materia ───────────────────────────────────────
            val materias = arrayOf("matematicas", "calculo_mental", "lengua", "medio", "geografia")
            val nombresMateria = mapOf(
                "matematicas" to "🧮 Matemáticas",
                "calculo_mental" to "⚡ Cálculo Mental",
                "lengua" to "📖 Lengua",
                "medio" to "🌍 Medio",
                "geografia" to "🌍 Geografía"
            )
            val coloresMateria = mapOf(
                "matematicas" to PJBlue,
                "calculo_mental" to PJOrange,
                "lengua" to PJGreen,
                "medio" to PJRed,
                "geografia" to PJPurple
            )

            item {
                Text("📚 Por materia", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PJGray900, modifier = Modifier.padding(top = 4.dp))
            }

            materias.forEach { materia ->
                val materiaCompletada = completados?.optJSONObject(materia)
                val nivelesCompletados = materiaCompletada?.length() ?: 0
                val color = coloresMateria[materia] ?: PJPurple

                item {
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PJWhite)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(36.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                        Text(nombresMateria[materia]?.take(2) ?: "?", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(nombresMateria[materia] ?: materia, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = PJGray900)
                                        Text("$nivelesCompletados niveles completados", fontSize = 12.sp, color = PJGray500)
                                    }
                                }
                                Text("$nivelesCompletados/$nivelActual", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
                            }
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { if (nivelActual > 0) nivelesCompletados.toFloat() / nivelActual.toFloat() else 0f },
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = color,
                                trackColor = color.copy(alpha = 0.12f),
                            )

                            // Show completed level details
                            if (materiaCompletada != null && materiaCompletada.length() > 0) {
                                Spacer(Modifier.height(8.dp))
                                val keys = materiaCompletada.keys().asSequence().toList().sortedBy { it.toIntOrNull() ?: 0 }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    keys.takeLast(5).forEach { nivelKey ->
                                        val intentos = materiaCompletada.optJSONArray(nivelKey)
                                        val ultimo = if (intentos != null && intentos.length() > 0) intentos.getJSONObject(intentos.length() - 1) else null
                                        val pct = ultimo?.optInt("porcentaje", 0) ?: 0
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (pct >= 80) PJGreen.copy(alpha = 0.15f) else if (pct >= 60) PJOrange.copy(alpha = 0.15f) else PJRed.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                "Nv.$nivelKey: $pct%",
                                                Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (pct >= 80) PJGreen else if (pct >= 60) PJOrange else PJRed
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Historial reciente ────────────────────────────────
            val historial = d.optJSONObject("progreso")?.optJSONObject("historial")
            if (historial != null && historial.length() > 0) {
                item {
                    Text("🕐 Historial reciente", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PJGray900, modifier = Modifier.padding(top = 4.dp))
                }
                val keys = historial.keys().asSequence().toList().sortedDescending().take(10)
                items(keys) { key ->
                    val entry = historial.optJSONObject(key)
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = PJWhite)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(36.dp).clip(CircleShape).background(PJGreen.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                                Text("✅", fontSize = 16.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(entry?.optString("materia", "Ejercicio") ?: "Ejercicio", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = PJGray900)
                                Text(key.take(10), fontSize = 11.sp, color = PJGray400)
                            }
                            Text(
                                "+${entry?.optInt("placetas", 0) ?: 0} Pz",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PJGreen
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun StatItem(emoji: String, label: String, value: String, color: Color = PJWhite) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 24.sp)
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 11.sp, color = color.copy(alpha = 0.6f))
    }
}

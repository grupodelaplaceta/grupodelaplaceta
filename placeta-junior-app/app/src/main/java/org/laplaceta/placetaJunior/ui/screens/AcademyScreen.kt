package org.laplaceta.placetajunior.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.laplaceta.placetajunior.network.HttpHelper
import org.laplaceta.placetajunior.ui.theme.*
import java.net.URL

@Composable
fun AcademyScreen(
    dip: String,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var academyData by remember { mutableStateOf<JSONObject?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    var selectedSubject by remember { mutableStateOf<String?>(null) }
    var selectedLevel by remember { mutableStateOf<Int?>(null) }
    var quizQuestions by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var quizInProgress by remember { mutableStateOf(false) }
    var quizResults by remember { mutableStateOf<JSONObject?>(null) }

    // Payment confirmation dialog
    var showPaymentDialog by remember { mutableStateOf(false) }
    var paymentData by remember { mutableStateOf<JSONObject?>(null) }
    var saldoActual by remember { mutableIntStateOf(academyData?.optInt("placetas_saldo", 0) ?: 0) }

    LaunchedEffect(dip) {
        try {
            val result = HttpHelper.get("https://grupodelaplaceta.vercel.app/api/junior/academy/cuestionarios?dip=$dip")
            result.fold(
                onSuccess = { json -> academyData = JSONObject(json) },
                onFailure = { e -> error = e.message }
            )
        } catch (e: Exception) {
            error = e.message
        } finally {
            isLoading = false
        }
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize().background(PJPurpleBg), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = PJPurple)
                Spacer(Modifier.height(12.dp))
                Text("Cargando Academia...", color = PJGray500, fontSize = 14.sp)
            }
        }
        return
    }

    if (quizInProgress && quizQuestions.isNotEmpty()) {
        QuizView(
            questions = quizQuestions,
            subject = selectedSubject ?: "",
            level = selectedLevel ?: 1,
            onFinish = { results ->
                quizResults = results
                quizInProgress = false
            }
        )
        return
    }

    if (quizResults != null && selectedSubject != null) {
        QuizResultView(
            results = quizResults!!,
            subject = selectedSubject!!,
            level = selectedLevel ?: 1,
            onContinue = {
                quizResults = null
                selectedSubject = null
                selectedLevel = null
            }
        )
        return
    }

    Column(Modifier.fillMaxSize().background(PJPurpleBg)) {
        // ── Payment confirmation dialog ─────────────────────────────
        if (showPaymentDialog && paymentData != null) {
            AlertDialog(
                onDismissRequest = { showPaymentDialog = false },
                shape = RoundedCornerShape(20.dp),
                title = { Text("💳 Confirmar pago", fontWeight = FontWeight.Bold, color = PJPurple) },
                text = {
                    Column {
                        Text(paymentData!!.optString("concepto", "Pago"), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = PJBlack)
                        Spacer(Modifier.height(8.dp))
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = PJOrangeLight)) {
                            Column(Modifier.padding(16.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Importe:", fontSize = 14.sp, color = PJGray600)
                                    Text("-${paymentData!!.optInt("cantidad", 0)} Pz", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PJRed)
                                }
                                Spacer(Modifier.height(4.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Tu saldo:", fontSize = 14.sp, color = PJGray600)
                                    Text("${saldoActual} Pz", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = PJBlack)
                                }
                                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Quedarán:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = PJBlack)
                                    Text("${saldoActual - paymentData!!.optInt("cantidad", 0)} Pz", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PJGreen)
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("El pago se realiza desde tu cuenta del Banco de La Placeta a Capitalia. Esta operación queda registrada.", fontSize = 11.sp, color = PJGray400)
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        scope.launch {
                            try {
                                val body = JSONObject().apply {
                                    put("dip", dip)
                                    put("cantidad", paymentData!!.optInt("cantidad"))
                                    put("concepto", paymentData!!.optString("concepto"))
                                    put("nivel", paymentData!!.optInt("nivel"))
                                }
                                val result = HttpHelper.post(
                                    "https://grupodelaplaceta.vercel.app/api/junior/academy/confirmar-pago",
                                    body
                                )
                                result.fold(
                                    onSuccess = { json ->
                                        val resp = JSONObject(json)
                                        if (resp.optBoolean("success")) {
                                            saldoActual = resp.optInt("nuevo_saldo")
                                        }
                                    },
                                    onFailure = { e -> error = e.message }
                                )
                            } catch (_: Exception) {}
                            showPaymentDialog = false
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = PJGreen)) {
                        Text("Confirmar pago", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPaymentDialog = false }) { Text("Cancelar", color = PJGray500) }
                }
            )
        }

        // Header
        Row(
            Modifier.fillMaxWidth().background(PJPurple).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver", tint = PJWhite) }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("🎓 Academia GDLP", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PJWhite)
                Text("Aprende y gana Placeta", fontSize = 12.sp, color = PJWhite.copy(alpha = 0.7f))
            }
            Surface(shape = RoundedCornerShape(16.dp), color = PJOrange) {
                Text(
                    "Nv. ${academyData?.optInt("nivel_actual", 1) ?: 1}",
                    Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PJWhite
                )
            }
        }

        if (academyData == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: ${error ?: "Desconocido"}", color = PJRed)
            }
            return
        }

        val data = academyData!!
        val subjects = arrayOf("matematicas", "calculo_mental", "lengua", "medio")
        val names = mapOf(
            "matematicas" to "🧮 Matemáticas",
            "calculo_mental" to "⚡ Cálculo Mental",
            "lengua" to "📖 Lengua",
            "medio" to "🌍 Medio"
        )
        val icons: Map<String, ImageVector> = mapOf(
            "matematicas" to Icons.Default.Calculate,
            "calculo_mental" to Icons.Default.Speed,
            "lengua" to Icons.Default.MenuBook,
            "medio" to Icons.Default.Public
        )
        val colors: Map<String, Color> = mapOf(
            "matematicas" to PJBlue,
            "calculo_mental" to PJOrange,
            "lengua" to PJGreen,
            "medio" to PJRed
        )

        LazyColumn(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                // Stats card
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = PJWhite)) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatItem("🏆", "Nivel", "${data.optInt("nivel_actual", 1)}")
                        StatItem("🪙", "Saldo", "${saldoActual} Pz")
                        StatItem("⭐", "XP", "${data.optJSONObject("progreso")?.optInt("puntuacion_total", 0) ?: 0}")
                    }
                    // RBU button
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    val r = HttpHelper.get("https://grupodelaplaceta.vercel.app/api/junior/academy/rbu?dip=$dip")
                                    r.getOrNull()?.let { json ->
                                        val resp = JSONObject(json)
                                        if (resp.optBoolean("success")) {
                                            saldoActual = resp.optInt("nuevo_saldo")
                                        }
                                    }
                                } catch (_: Exception) {}
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PJYellow)
                    ) {
                        Text("🎁 Reclamar RBU diaria", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = PJBlack)
                    }
                }
            }

            // Subjects
            subjects.forEach { subject ->
                val subjectData = data.optJSONObject("cuestionarios")?.optJSONObject(subject)
                if (subjectData != null) {
                    item {
                        SubjectCard(
                            name = names[subject] ?: subject,
                            icon = icons[subject] ?: Icons.Default.School,
                            color = colors[subject] ?: PJPurple,
                            subjectData = subjectData,
                            nivelActual = data.optInt("nivel_actual", 1),
                            onClick = { level ->
                                selectedSubject = subject
                                selectedLevel = level
                                // Load quiz questions for this subject + level
                                val levels = subjectData.optJSONObject("niveles")
                                val questions = levels?.optJSONArray("$level")
                                if (questions != null) {
                                    val list = mutableListOf<JSONObject>()
                                    for (i in 0 until questions.length()) {
                                        list.add(questions.getJSONObject(i))
                                    }
                                    quizQuestions = list
                                    quizInProgress = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectCard(
    name: String, icon: ImageVector, color: Color,
    subjectData: JSONObject, nivelActual: Int,
    onClick: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        Modifier.fillMaxWidth().clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PJWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = PJBlack, modifier = Modifier.weight(1f))
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = PJGray400)
            }

            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(top = 12.dp)) {
                    val niveles = subjectData.optJSONObject("niveles")
                    if (niveles != null) {
                        val maxLevel = nivelActual.coerceAtMost(10)
                        for (n in 1..maxLevel) {
                            val key = "$n"
                            if (niveles.has(key)) {
                                Row(
                                    Modifier.fillMaxWidth().clickable { onClick(n) }.padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(Modifier.size(28.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
                                        Text("$n", color = PJWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Text("Nivel $n", fontSize = 14.sp, color = PJGray700, modifier = Modifier.weight(1f))
                                    Text("🪙 +${niveles.optJSONArray(key)?.optJSONObject(0)?.optInt("placetas_recompensa", 5) ?: 5} Pz", fontSize = 12.sp, color = PJOrange, fontWeight = FontWeight.SemiBold)
                                    Icon(Icons.Default.PlayArrow, null, tint = color, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizView(
    questions: List<JSONObject>,
    subject: String,
    level: Int,
    onFinish: (JSONObject) -> Unit
) {
    var currentIdx by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var answers by remember { mutableStateOf(mutableListOf<JSONObject>()) }
    var showFeedback by remember { mutableStateOf(false) }

    if (currentIdx >= questions.size) {
        LaunchedEffect(Unit) {
            val result = JSONObject().apply {
                put("correctas", answers.count { it.optBoolean("correcta") })
                put("total", questions.size)
                put("xp", answers.sumOf { it.optInt("placetas_ganadas", 0) })
                put("porcentaje", if (questions.isNotEmpty()) (answers.count { it.optBoolean("correcta") } * 100) / questions.size else 0)
            }
            onFinish(result)
        }
        return
    }

    val q = questions[currentIdx]
    val options = q.optJSONArray("opciones")

    Column(Modifier.fillMaxSize().background(PJPurpleBg)) {
        // Progress bar
        LinearProgressIndicator(
            progress = { currentIdx.toFloat() / questions.size },
            Modifier.fillMaxWidth().height(4.dp),
            color = PJPurple,
            trackColor = PJPurpleLight
        )

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp)) {
            Text("Pregunta ${currentIdx + 1}/${questions.size}", fontSize = 13.sp, color = PJGray500)
            Spacer(Modifier.height(8.dp))
            Text(q.optString("pregunta", ""), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = PJBlack)

            // Imagen si existe
            val imagenUrl = q.optString("imagen", null)
            val fuente = q.optString("fuente", null)
            if (!imagenUrl.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                    AsyncImage(
                        model = imagenUrl,
                        contentDescription = "Imagen de la pregunta",
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        contentScale = ContentScale.FillWidth
                    )
                    if (!fuente.isNullOrBlank()) {
                        Text(
                            "Fuente: $fuente",
                            fontSize = 9.sp,
                            color = PJGray400,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            if (options != null) {
                for (i in 0 until options.length()) {
                    val isSelected = selectedOption == i
                    val isCorrect = showFeedback && i == q.optInt("respuesta_correcta", q.optInt("correcta", -1))
                    val bgColor = when {
                        showFeedback && isCorrect -> PJGreen.copy(alpha = 0.15f)
                        showFeedback && isSelected && !isCorrect -> PJRedLight
                        isSelected -> PJPurpleLight
                        else -> PJGray100
                    }

                    Card(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(enabled = !showFeedback) { selectedOption = i },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = bgColor)
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${('A' + i)}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (isSelected) PJPurple else PJGray500)
                            Spacer(Modifier.width(12.dp))
                            Text(options.optString(i, ""), fontSize = 15.sp, color = PJBlack, modifier = Modifier.weight(1f))
                            if (showFeedback && isCorrect) Icon(Icons.Default.Check, null, tint = PJGreen, modifier = Modifier.size(20.dp))
                            if (showFeedback && isSelected && !isCorrect) Icon(Icons.Default.Close, null, tint = PJRed, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        // Bottom button
        if (!showFeedback && selectedOption != null) {
            Button(
                onClick = {
                    showFeedback = true
                    val correct = selectedOption == (q.optInt("respuesta_correcta", q.optInt("correcta", -1)))
                    val reward = if (correct) q.optInt("placetas_recompensa", 5) else 0
                    answers.add(JSONObject().apply {
                        put("idx", currentIdx)
                        put("opcion", selectedOption)
                        put("correcta", correct)
                        put("placetas_ganadas", reward)
                    })
                },
                Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PJPurple)
            ) {
                Text("Responder", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        if (showFeedback) {
            Button(
                onClick = {
                    currentIdx++
                    selectedOption = null
                    showFeedback = false
                },
                Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PJOrange)
            ) {
                Text(if (currentIdx < questions.size - 1) "Siguiente" else "Ver resultados", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun QuizResultView(
    results: JSONObject,
    subject: String,
    level: Int,
    onContinue: () -> Unit
) {
    val correctas = results.optInt("correctas", 0)
    val total = results.optInt("total", 0)
    val xp = results.optInt("xp", 0)
    val porcentaje = results.optInt("porcentaje", 0)
    val aprobado = porcentaje >= 60

    Column(Modifier.fillMaxSize().background(PJPurpleBg), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(60.dp))
        Text(if (aprobado) "🎉" else "📚", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            if (aprobado) "¡Nivel completado!" else "Sigue practicando",
            fontWeight = FontWeight.Bold, fontSize = 24.sp, color = PJBlack, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text("$correctas/$total correctas ($porcentaje%)", fontSize = 16.sp, color = PJGray500)

        Spacer(Modifier.height(32.dp))

        Card(Modifier.fillMaxWidth().padding(horizontal = 32.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = PJWhite)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Recompensa", fontSize = 13.sp, color = PJGray500)
                Spacer(Modifier.height(4.dp))
                Text("🪙 +$xp Placeta", fontWeight = FontWeight.Bold, fontSize = 28.sp, color = PJOrange)
                if (aprobado) {
                    Spacer(Modifier.height(8.dp))
                    Text("⭐ +${(xp * 2)} XP", fontSize = 16.sp, color = PJBlue)
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onContinue,
            Modifier.fillMaxWidth().padding(horizontal = 32.dp).height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (aprobado) PJGreen else PJPurple)
        ) {
            Text(if (aprobado) "Continuar aprendiendo" else "Intentar de nuevo", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun StatItem(emoji: String, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 24.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PJBlack)
        Text(label, fontSize = 11.sp, color = PJGray500)
    }
}

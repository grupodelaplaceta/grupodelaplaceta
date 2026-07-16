package org.laplaceta.placetaid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.laplaceta.placetaid.data.api.ApiClient
import org.laplaceta.placetaid.data.models.JuniorInfo
import org.laplaceta.placetaid.data.models.PlacetaIDInfo
import org.laplaceta.placetaid.ui.components.DipDigitalCard
import org.laplaceta.placetaid.ui.components.PlacetaIDCard
import org.laplaceta.placetaid.ui.theme.*

@Composable
fun PlacetaIDsScreen(
    placetaids: List<PlacetaIDInfo>,
    menores: List<JuniorInfo>,
    selectedDip: String?,
    onSelect: (String) -> Unit,
    onRemove: (String) -> Unit,
    onAdd: () -> Unit
) {
    var showDipDigital by remember { mutableStateOf<PlacetaIDInfo?>(null) }
    var showParentalControls by remember { mutableStateOf<JuniorInfo?>(null) }
    var showLegalSigning by remember { mutableStateOf<JuniorInfo?>(null) }
    var showGeneralSigning by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main content
        val hasContent = placetaids.isNotEmpty() || menores.isNotEmpty()

        if (!hasContent) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "🔐", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "No hay PlacetaIDs guardados", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = PlacetaidTextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Añade tu primer PlacetaID\npara empezar a usar la app", textAlign = TextAlign.Center, fontSize = 14.sp, color = PlacetaidTextMuted, lineHeight = 20.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = PlacetaidPrimary), shape = MaterialTheme.shapes.medium) {
                        Text("+ Añadir PlacetaID", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Mis Identidades", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PlacetaidTextPrimary)
                        TextButton(onClick = { showGeneralSigning = true }, colors = ButtonDefaults.textButtonColors(contentColor = PlacetaidAccent)) {
                            Text("✍️ Firmar doc.", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                }

                // Regular identities
                items(placetaids, key = { "pid-${it.dip}" }) { info ->
                    PlacetaIDCard(
                        placetaID = info,
                        isSelected = info.dip == selectedDip,
                        onClick = { showDipDigital = info },
                        onRemove = { onRemove(info.dip) }
                    )
                }

                // Linked minors section
                if (menores.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "👶 Menores vinculados",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = PlacetaidTextMuted,
                            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                        )
                    }

                    items(menores, key = { "jr-${it.dip}" }) { junior ->
                        JuniorIdentityCard(
                            junior = junior,
                            onClick = { showParentalControls = junior }
                        )
                    }
                }
            }
        }

        // DIP Digital Card overlay (for regular identities)
        if (showDipDigital != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { showDipDigital = null },
                contentAlignment = Alignment.Center
            ) {
                DipDigitalCard(
                    identidad = showDipDigital!!,
                    onDismiss = { showDipDigital = null },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                )
            }
        }

        // Parental Controls overlay (for minors)
        if (showParentalControls != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { showParentalControls = null },
                contentAlignment = Alignment.Center
            ) {
                ParentalControlsCard(
                    junior = showParentalControls!!,
                    onDismiss = { showParentalControls = null },
                    onFirmarDocumentos = {
                        val junior = showParentalControls!!
                        showParentalControls = null
                        showLegalSigning = junior
                    },
                    tutorDip = selectedDip ?: "",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                )
            }
        }

        // Legal Docs Signing full-screen overlay (for minors)
        if (showLegalSigning != null) {
            val junior = showLegalSigning!!
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                LegalDocsSigningScreen(
                    juniorId = junior.id?.toIntOrNull() ?: 0,
                    juniorDip = junior.dip,
                    tutorDip = selectedDip ?: junior.tutorDip ?: "",
                    tutorNombre = "Tutor",
                    modoGeneral = false,
                    onAllSigned = {
                        showLegalSigning = null
                    },
                    onDismiss = {
                        showLegalSigning = null
                    }
                )
            }
        }

        // General signing full-screen overlay (any identity)
        if (showGeneralSigning) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                LegalDocsSigningScreen(
                    juniorId = 0,
                    juniorDip = selectedDip ?: "",
                    tutorDip = selectedDip ?: "",
                    tutorNombre = "Firmante",
                    modoGeneral = true,
                    onAllSigned = {
                        showGeneralSigning = false
                    },
                    onDismiss = {
                        showGeneralSigning = false
                    }
                )
            }
        }
    }
}

// ── Junior Identity Card ────────────────────────────────────────────────

@Composable
private fun JuniorIdentityCard(
    junior: JuniorInfo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar - orange for minors
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(PlacetaidAccent, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = junior.alias.ifBlank { junior.nombre ?: "Menor" },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = PlacetaidTextPrimary
                )
                Text(
                    text = junior.dip,
                    fontSize = 13.sp,
                    color = PlacetaidTextMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
                if (junior.nombre != null) {
                    Text(
                        text = listOfNotNull(junior.nombre, junior.apellidos).joinToString(" "),
                        fontSize = 12.sp,
                        color = PlacetaidTextMuted.copy(alpha = 0.7f)
                    )
                }
            }

            // Badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = PlacetaidAccent.copy(alpha = 0.12f)
            ) {
                Text(
                    text = "Jr",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PlacetaidAccent,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// ── Parental Controls Card ──────────────────────────────────────────────

@Composable
private fun ParentalControlsCard(
    junior: JuniorInfo,
    onDismiss: () -> Unit,
    onFirmarDocumentos: () -> Unit,
    tutorDip: String,
    modifier: Modifier = Modifier
) {
    var gastoDiario by remember { mutableStateOf("10") }
    var gastoSemanal by remember { mutableStateOf("50") }
    var aprobacionTutor by remember { mutableStateOf("1000") }
    var tiempoUso by remember { mutableStateOf("60") }
    var guardado by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(PlacetaidAccent, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Favorite, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Controles Parentales",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = PlacetaidTextPrimary
                        )
                        Text(
                            text = junior.alias.ifBlank { "Menor" },
                            fontSize = 13.sp,
                            color = PlacetaidTextMuted
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Cerrar", tint = PlacetaidTextMuted)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = PlacetaidSurfaceVariant)

            // Junior info
            Column(modifier = Modifier.fillMaxWidth()) {
                InfoRow("👤 Identidad", junior.dip)
                if (junior.nombre != null) InfoRow("📛 Nombre", listOfNotNull(junior.nombre, junior.apellidos).joinToString(" "))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Editable limits
            Text("💰 Límites de gasto", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = PlacetaidTextPrimary, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(gastoDiario, { if (it.all { c -> c.isDigit() }) gastoDiario = it }, label = { Text("Gasto diario (Pz)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(gastoSemanal, { if (it.all { c -> c.isDigit() }) gastoSemanal = it }, label = { Text("Gasto semanal (Pz)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(aprobacionTutor, { if (it.all { c -> c.isDigit() }) aprobacionTutor = it }, label = { Text("Requiere aprobación desde (Pz)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(tiempoUso, { if (it.all { c -> c.isDigit() }) tiempoUso = it }, label = { Text("Tiempo de uso diario (min)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())

            if (guardado) {
                Spacer(Modifier.height(8.dp))
                Text("✅ Límites actualizados", color = PlacetaidSuccess, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(16.dp))

            // Save button
            Button(
                onClick = {
                    scope.launch {
                        val body = JSONObject().apply {
                            put("dip_tutor", tutorDip)
                            put("dip_menor", junior.dip)
                            put("limite_gasto_diario", gastoDiario.toIntOrNull() ?: 10)
                            put("limite_gasto_semanal", gastoSemanal.toIntOrNull() ?: 50)
                            put("limite_aprobacion_tutor", aprobacionTutor.toIntOrNull() ?: 1000)
                            put("tiempo_uso_diario", tiempoUso.toIntOrNull() ?: 60)
                        }
                        withContext(Dispatchers.IO) {
                            val url = java.net.URL("${ApiClient.getAdminUrl()}/api/junior/control-parental")
                            val conn = url.openConnection() as java.net.HttpURLConnection
                            conn.requestMethod = "POST"; conn.doOutput = true
                            conn.setRequestProperty("Content-Type", "application/json")
                            conn.outputStream.write(body.toString().toByteArray())
                            conn.inputStream.bufferedReader().readText()
                        }
                        guardado = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PlacetaidSuccess)
            ) {
                Text("💾 Guardar límites", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Spacer(Modifier.height(8.dp))

            // Firmar documentos
            OutlinedButton(
                onClick = onFirmarDocumentos,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PlacetaidPrimary)
            ) { Text("✍️ Firmar documentos legales", fontWeight = FontWeight.SemiBold) }

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cerrar") }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = PlacetaidTextMuted)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = PlacetaidTextPrimary)
    }
}

@Composable
private fun LimitRow(label: String, amount: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = "• $label", fontSize = 13.sp, color = PlacetaidTextMuted)
        Text(text = amount, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PlacetaidAccent)
    }
}

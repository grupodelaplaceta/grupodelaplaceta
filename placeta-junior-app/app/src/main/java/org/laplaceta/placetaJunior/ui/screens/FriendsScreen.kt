package org.laplaceta.placetajunior.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.laplaceta.placetajunior.ui.components.DipDigitalJuniorCard
import org.laplaceta.placetajunior.ui.components.GdlpShape
import org.laplaceta.placetajunior.ui.components.QRCode
import org.laplaceta.placetajunior.ui.theme.*

data class AmigoJunior(
    val id: String,
    val nombre: String,
    val dip: String,
    val avatarColor: Color,
    val placetas: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    dip: String,
    saldo: Int,
    onBack: () -> Unit
) {
    var showAddFriend by remember { mutableStateOf(false) }
    var showTransfer by remember { mutableStateOf<AmigoJunior?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val amigos = remember {
        mutableStateListOf(
            AmigoJunior("1", "Lucía García", "JUNIOR-A1B2", PJRed),
            AmigoJunior("2", "Martín López", "JUNIOR-C3D4", PJBlue),
            AmigoJunior("3", "Sofía Pérez", "JUNIOR-E5F6", PJGreen)
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(PJWhite)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(PJPurple).statusBarsPadding().padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = PJWhite) }
            Spacer(Modifier.width(8.dp))
            Text("Amistades Junior", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PJWhite, modifier = Modifier.weight(1f))
            Text("$saldo Pz", fontSize = 14.sp, color = PJYellow, fontWeight = FontWeight.Bold)
        }

        // Tabs
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            listOf("Amigos", "Transferencias").forEachIndexed { idx, label ->
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (selectedTab == idx) PJPurple else PJGray100).clickable { selectedTab = idx }.padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = if (selectedTab == idx) PJWhite else PJGray600, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                if (idx == 0) Spacer(Modifier.width(8.dp))
            }
        }

        if (selectedTab == 0) {
            // Friends list
            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp)) {
                item {
                    Button(
                        onClick = { showAddFriend = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PJPurple)
                    ) {
                        Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Añadir amigo", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(16.dp))
                }

                items(amigos) { amigo ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable { showTransfer = amigo },
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(4.dp),
                        colors = CardDefaults.cardColors(PJWhite)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            // Avatar with forma shape
                            Box(modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(amigo.avatarColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                Text(amigo.nombre.take(1), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = amigo.avatarColor)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(amigo.nombre, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PJBlack)
                                Text(amigo.dip, fontSize = 12.sp, color = PJGray400)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = PJGray300, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // Floating saldo card
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(PJPurpleLight)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = PJPurple, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Las comisiones de transferencias las paga Capitalia. Sin coste para ti.", fontSize = 12.sp, color = PJPurple)
                }
            }
        } else {
            // Transfer history
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SwapHoriz, null, tint = PJGray300, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Sin movimientos", fontSize = 16.sp, color = PJGray400)
                    Text("Tus transferencias aparecerán aquí", fontSize = 13.sp, color = PJGray300)
                }
            }
        }
    }

    // Add friend dialog — QR + DIP
    if (showAddFriend) {
        var dipAmigo by remember { mutableStateOf("") }
        var showMyQr by remember { mutableStateOf(false) }
        var enviado by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        if (showMyQr) {
            // ── Show my DIP QR for the other person to scan ──────
            AlertDialog(
                onDismissRequest = { showMyQr = false },
                title = { Text("Tu código de amistad", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("Que tu amigo escanee este código", fontSize = 13.sp, color = PJGray500)
                        Spacer(Modifier.height(12.dp))
                        QRCode(data = dip, modifier = Modifier.size(160.dp), size = 512)
                        Spacer(Modifier.height(8.dp))
                        Text(dip, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PJPurple, letterSpacing = 2.sp)
                    }
                },
                confirmButton = {
                    Button({ showMyQr = false }, colors = ButtonDefaults.buttonColors(PJPurple), shape = RoundedCornerShape(12.dp)) {
                        Text("Listo", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton({ showMyQr = false }) { Text("Cerrar") }
                }
            )
        } else if (enviado) {
            AlertDialog(
                onDismissRequest = { showAddFriend = false; enviado = false },
                title = { Text("✅ Solicitud enviada", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                text = {
                    Text("Tu tutor recibirá una notificación en PlacetaID Móvil para aprobar esta amistad.", fontSize = 13.sp, color = PJGray500, textAlign = TextAlign.Center)
                },
                confirmButton = {
                    Button({ showAddFriend = false; enviado = false }, colors = ButtonDefaults.buttonColors(PJGreen), shape = RoundedCornerShape(12.dp)) {
                        Text("Entendido", fontWeight = FontWeight.Bold)
                    }
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { showAddFriend = false },
                title = { Text("Añadir amigo", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Escanea el QR de tu amigo o pide que escanee el tuyo", fontSize = 13.sp, color = PJGray500)
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                { showMyQr = true },
                                Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PJPurple)
                            ) {
                                Icon(Icons.Default.QrCode, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Mi QR", fontSize = 13.sp)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = PJGray200)
                        Spacer(Modifier.height(12.dp))
                        Text("O introduce el DIP manualmente:", fontSize = 12.sp, color = PJGray400)
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(dipAmigo, { dipAmigo = it.uppercase().take(9) }, label = { Text("DIP del amigo") }, singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                    }
                },
                confirmButton = {
                    Button({
                        if (dipAmigo.isNotBlank()) {
                            scope.launch {
                                val body = org.json.JSONObject().apply {
                                    put("dip", dip)
                                    put("dip_amigo", dipAmigo)
                                }
                                val r = org.laplaceta.placetajunior.network.HttpHelper.post(
                                    "https://grupodelaplaceta.vercel.app/api/junior/amigos/solicitar",
                                    body
                                )
                                if (r.isSuccess) {
                                    enviado = true
                                }
                            }
                        }
                    }, enabled = dipAmigo.isNotBlank(), colors = ButtonDefaults.buttonColors(PJPurple), shape = RoundedCornerShape(12.dp)) {
                        Text("Enviar solicitud", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton({ showAddFriend = false }) { Text("Cancelar") }
                }
            )
        }
    }

    // Transfer dialog — con aprobación del tutor si es > 50 Pz
    showTransfer?.let { amigo ->
        var cantidad by remember { mutableStateOf("") }
        var concepto by remember { mutableStateOf("") }
        var enviadoTutor by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        val cantidadInt = cantidad.toIntOrNull() ?: 0

        if (enviadoTutor) {
            AlertDialog(
                onDismissRequest = { showTransfer = null; enviadoTutor = false },
                title = { Text("⏳ Pendiente de aprobación", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                text = { Text("Como la cantidad es alta (${cantidadInt} Pz), tu tutor debe aprobarla desde PlacetaID Móvil.", fontSize = 13.sp, color = PJGray500, textAlign = TextAlign.Center) },
                confirmButton = {
                    Button({ showTransfer = null; enviadoTutor = false }, colors = ButtonDefaults.buttonColors(PJPurple), shape = RoundedCornerShape(12.dp)) {
                        Text("Entendido", fontWeight = FontWeight.Bold)
                    }
                }
            )
            return@let
        }

        AlertDialog(
            onDismissRequest = { showTransfer = null },
            title = { Text("Enviar a ${amigo.nombre}", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Saldo disponible: $saldo Pz", fontSize = 13.sp, color = PJGray500)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(cantidad, { if (it.all { c -> c.isDigit() }) cantidad = it }, label = { Text("Cantidad (Pz)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(concepto, { concepto = it }, label = { Text("Concepto (opcional)") }, singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text("Comisión: 0 Pz (la paga Capitalia 💰)", fontSize = 11.sp, color = PJGreen)
                    if (cantidadInt > 50) {
                        Spacer(Modifier.height(8.dp))
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(PJOrange.copy(alpha = 0.08f))) {
                            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("⚠️", fontSize = 14.sp)
                                Spacer(Modifier.width(6.dp))
                                Text("Por ser más de 50 Pz, tu tutor deberá aprobar esta transferencia.", fontSize = 11.sp, color = PJOrange, lineHeight = 14.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button({
                    if (cantidadInt > 0 && cantidadInt <= saldo) {
                        if (cantidadInt > 50) {
                            // Enviar a tutor para aprobación
                            scope.launch {
                                val body = org.json.JSONObject().apply {
                                    put("dip", dip)
                                    put("dip_amigo", amigo.dip)
                                    put("cantidad", cantidadInt)
                                    put("concepto", concepto)
                                }
                                org.laplaceta.placetajunior.network.HttpHelper.post(
                                    "https://grupodelaplaceta.vercel.app/api/junior/amigos/transferir",
                                    body
                                )
                                enviadoTutor = true
                            }
                        } else {
                            showTransfer = null
                        }
                    }
                }, enabled = cantidadInt in 1..saldo, colors = ButtonDefaults.buttonColors(PJGreen), shape = RoundedCornerShape(12.dp)) {
                    Text("Enviar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton({ showTransfer = null }) { Text("Cancelar") }
            }
        )
    }
}

package org.laplaceta.placetajunior.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.laplaceta.placetajunior.network.ApiClient
import org.laplaceta.placetajunior.ui.components.GdlpShape
import org.laplaceta.placetajunior.ui.theme.*

data class LegalDoc(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val summary: String,
    val color: Color,
    var signed: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalDocsScreen(
    tutorDip: String,
    tutorNombre: String,
    minorNombre: String,
    minorDip: String,
    onBack: () -> Unit,
    onAllSigned: () -> Unit
) {
    val scope = rememberCoroutineScope()

    val docs = remember {
        mutableStateListOf(
            LegalDoc("PJ-TYC-001", "Términos y Condiciones", Icons.Default.Description,
                "Reglas de uso de Placeta Junior: responsabilidades del tutor, límites de uso, privacidad del menor y condiciones del servicio.",
                PJPurple),
            LegalDoc("PJ-PRV-001", "Política de Privacidad", Icons.Default.Lock,
                "Protección de datos del menor: qué información recogemos, cómo la usamos, derechos ARCO y medidas de seguridad aplicadas.",
                PJBlue),
            LegalDoc("PJ-CON-001", "Consentimiento Tutor", Icons.Default.Approval,
                "Autorización legal para que el menor use Placeta Junior: confirmación de tutela, responsabilidad parental y consentimiento expreso.",
                PJGreen)
        )
    }

    var allSigned by remember { mutableStateOf(false) }
    var signingDoc by remember { mutableStateOf<String?>(null) }
    var showPreview by remember { mutableStateOf<String?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { showContent = true }

    val allSignedCheck = docs.all { it.signed }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PJWhite)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PJPurple)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, enabled = signingDoc == null) {
                Icon(Icons.Default.ArrowBack, null, tint = PJWhite)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Documentos legales",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = PJWhite,
                modifier = Modifier.weight(1f)
            )
            // Progress indicator
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(PJWhite.copy(alpha = 0.2f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${docs.count { it.signed }}/${docs.size}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PJWhite
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            // Formas decorativas
            Box(modifier = Modifier.fillMaxWidth().height(0.dp))

            AnimatedVisibility(visible = showContent, enter = fadeIn() + slideInVertically()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    GdlpShape(
                        modifier = Modifier.size(64.dp),
                        color = PJPurple
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Firma los documentos",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = PJBlack,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "El tutor legal debe leer y firmar estos documentos desde PlacetaID Móvil",
                        fontSize = 13.sp,
                        color = PJGray500,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Documentos
            docs.forEachIndexed { index, doc ->
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn() + slideInVertically { it / 3 }
                ) {
                    DocumentCard(
                        doc = doc,
                        isSigning = signingDoc == doc.id,
                        onPreview = { showPreview = doc.id },
                        onSign = {
                            signingDoc = doc.id
                            errorMsg = null
                            scope.launch {
                                try {
                                    val json = withContext(Dispatchers.IO) {
                                        ApiClient.crearSolicitudPlacetaID(
                                            dip = tutorDip,
                                            servicio = "Firma: ${doc.title}",
                                            url = "placeta-junior://auth?doc=${doc.id}"
                                        )
                                    }
                                    if (json != null && json.optBoolean("ok", false)) {
                                        // Poll for signature
                                        val requestId = json.optString("requestId")
                                        var approved = false
                                        while (!approved) {
                                            delay(2000)
                                            val result = withContext(Dispatchers.IO) {
                                                ApiClient.pollAutorizacion(requestId)
                                            }
                                            if (result.aprobado) {
                                                docs[index] = doc.copy(signed = true)
                                                approved = true
                                                if (docs.all { it.signed }) {
                                                    allSigned = true
                                                    delay(500)
                                                    onAllSigned()
                                                }
                                                break
                                            }
                                            if (result.status == "denied" || result.status == "expired") {
                                                errorMsg = "Firma denegada o expirada para: ${doc.title}"
                                                break
                                            }
                                        }
                                    } else {
                                        errorMsg = "Error al conectar con PlacetaID"
                                    }
                                } catch (e: Exception) {
                                    errorMsg = "Error: ${e.message}"
                                }
                                signingDoc = null
                            }
                        }
                    )
                }

                if (index < docs.size - 1) {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Error
            if (errorMsg != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PJRedLight)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, null, tint = PJRed, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = errorMsg ?: "", color = PJRed, fontSize = 13.sp)
                    }
                }
            }

            // Info del menor
            if (allSignedCheck) {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PJGreenLight)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = PJGreen, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Todos los documentos firmados", fontWeight = FontWeight.Bold, color = PJGreen, fontSize = 15.sp)
                            Text("$minorNombre ($minorDip) ya puede usar Placeta Junior", fontSize = 12.sp, color = PJGreen.copy(alpha = 0.8f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DocumentCard(
    doc: LegalDoc,
    isSigning: Boolean,
    onPreview: () -> Unit,
    onSign: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (doc.signed) 2.dp else 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (doc.signed) PJGray50 else PJWhite
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Icon in shape
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(doc.color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (doc.signed) {
                        Icon(Icons.Default.CheckCircle, null, tint = PJGreen, modifier = Modifier.size(28.dp))
                    } else {
                        Icon(doc.icon, null, tint = doc.color, modifier = Modifier.size(28.dp))
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = doc.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PJBlack
                        )
                        if (doc.signed) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "✓ Firmado",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PJGreen
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = doc.id,
                        fontSize = 11.sp,
                        color = PJGray400
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = doc.summary,
                fontSize = 13.sp,
                color = PJGray600,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (!doc.signed) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onPreview,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = doc.color)
                    ) {
                        Icon(Icons.Default.Visibility, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Leer", fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = onSign,
                        enabled = !isSigning,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = doc.color)
                    ) {
                        if (isSigning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = PJWhite,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Fingerprint, null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (isSigning) "Firmando..." else "Firmar con PlacetaID",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

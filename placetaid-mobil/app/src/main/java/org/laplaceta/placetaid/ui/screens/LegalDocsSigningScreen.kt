package org.laplaceta.placetaid.ui.screens

import androidx.compose.foundation.background
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
import org.laplaceta.placetaid.data.api.ApiClient
import org.laplaceta.placetaid.ui.components.SignaturePad
import org.laplaceta.placetaid.ui.theme.*
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class DocumentoPendiente(
    val codigo: String,
    val nombre: String,
    val orden: Int,
    val firmado: Boolean,
    val titulo: String = "",
    val contenido: String = ""
)

data class FirmaState(
    val juniorId: Int,
    val juniorDip: String,
    val juniorNombre: String,
    val tutorDip: String,
    val tutorNombre: String,
    val documentos: List<DocumentoPendiente>,
    val todosFirmados: Boolean
)

@Composable
fun LegalDocsSigningScreen(
    juniorId: Int,
    juniorDip: String,
    tutorDip: String,
    tutorNombre: String,
    modoGeneral: Boolean = false,
    onAllSigned: () -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var firmaState by remember { mutableStateOf<FirmaState?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var selectedDoc by remember { mutableStateOf<DocumentoPendiente?>(null) }
    var showSignaturePad by remember { mutableStateOf(false) }
    var docContent by remember { mutableStateOf<JSONObject?>(null) }
    var signingDoc by remember { mutableStateOf<DocumentoPendiente?>(null) }
    var docContentRaw by remember { mutableStateOf<String>("") }

    // Load pending documents
    LaunchedEffect(juniorId, modoGeneral) {
        try {
            isLoading = true
            val url = if (modoGeneral) {
                "${ApiClient.getAdminUrl()}/api/documentos/pendientes?dip=${tutorDip}"
            } else {
                "${ApiClient.getAdminUrl()}/api/junior/documentos-pendientes/$juniorId"
            }
            val json = withContext(Dispatchers.IO) { URL(url).readText() }
            if (modoGeneral) {
                // Parse general documents display
                val arr = JSONArray(json)
                val docs = mutableListOf<DocumentoPendiente>()
                for (i in 0 until arr.length()) {
                    val d = arr.getJSONObject(i)
                    docs.add(DocumentoPendiente(
                        codigo = d.optString("codigo", ""),
                        nombre = d.optString("nombre", d.optString("codigo", "Documento")),
                        orden = i,
                        firmado = d.optInt("firmado", 0) == 1 || d.optString("estado", "") == "firmado"
                    ))
                }
                firmaState = FirmaState(
                    juniorId = 0,
                    juniorDip = juniorDip,
                    juniorNombre = tutorNombre,
                    tutorDip = tutorDip,
                    tutorNombre = tutorNombre,
                    documentos = docs,
                    todosFirmados = docs.all { it.firmado }
                )
            } else {
                val obj = JSONObject(json)
                val junior = obj.getJSONObject("junior")
                val docsArr = obj.getJSONArray("documentos")
                val docs = mutableListOf<DocumentoPendiente>()
                for (i in 0 until docsArr.length()) {
                    val d = docsArr.getJSONObject(i)
                    docs.add(DocumentoPendiente(
                        codigo = d.getString("codigo"),
                        nombre = d.getString("nombre"),
                        orden = d.getInt("orden"),
                        firmado = d.getBoolean("firmado")
                    ))
                }
                firmaState = FirmaState(
                    juniorId = junior.getInt("id"),
                    juniorDip = junior.getString("dip"),
                    juniorNombre = "${junior.getString("nombre")} ${junior.getString("apellidos")}",
                    tutorDip = tutorDip,
                    tutorNombre = tutorNombre,
                    documentos = docs,
                    todosFirmados = obj.getBoolean("todos_firmados")
                )
            }
            isLoading = false
        } catch (e: Exception) {
            error = e.message ?: "Error al cargar documentos"
            isLoading = false
        }
    }

    // Check if all signed
    LaunchedEffect(firmaState) {
        if (firmaState?.todosFirmados == true) {
            onAllSigned()
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PlacetaidPrimary)
        }
        return
    }

    if (error != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("⚠️", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Error: $error", color = PlacetaidError, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = PlacetaidPrimary)) {
                    Text("Volver")
                }
            }
        }
        return
    }

    val state = firmaState ?: return

    // Show document content + signature pad
    if (showSignaturePad && signingDoc != null && docContent != null) {
        DocumentSigningView(
            documento = signingDoc!!,
            contenido = docContent!!,
            state = state,
            onFirmaCompletada = { firmaBase64 ->
                scope.launch {
                    try {
                        val body = JSONObject().apply {
                            if (modoGeneral) {
                                put("codigo_modelo", signingDoc!!.codigo)
                                put("firma_base64", firmaBase64)
                                put("firmante_dip", state.tutorDip)
                                put("firmante_nombre", state.tutorNombre)
                            } else {
                                put("junior_id", state.juniorId)
                                put("codigo_documento", signingDoc!!.codigo)
                                put("firma_base64", firmaBase64)
                                put("firmante_dip", state.tutorDip)
                                put("firmante_nombre", state.tutorNombre)
                            }
                        }

                        val endpoint = if (modoGeneral) {
                            "${ApiClient.getAdminUrl()}/api/firma/firmar-manuscrito"
                        } else {
                            "${ApiClient.getAdminUrl()}/api/junior/firmar-documento"
                        }
                        val result = withContext(Dispatchers.IO) {
                            val url = URL(endpoint)
                            val conn = url.openConnection() as HttpURLConnection
                            conn.requestMethod = "POST"
                            conn.doOutput = true
                            conn.connectTimeout = 15000
                            conn.readTimeout = 15000
                            conn.setRequestProperty("Content-Type", "application/json")
                            val writer = OutputStreamWriter(conn.outputStream)
                            writer.write(body.toString())
                            writer.flush()
                            writer.close()
                            val code = conn.responseCode
                            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                            stream.bufferedReader().readText()
                        }
                        val resp = JSONObject(result)

                        if (resp.optBoolean("todos_firmados", false)) {
                            // All done!
                            onAllSigned()
                        } else {
                            // Update local state
                            val updatedDocs = state.documentos.map {
                                if (it.codigo == signingDoc!!.codigo) it.copy(firmado = true) else it
                            }
                            firmaState = state.copy(documentos = updatedDocs)
                            showSignaturePad = false
                            signingDoc = null
                            docContent = null
                        }
                    } catch (e: Exception) {
                        error = "Error al enviar firma: ${e.message}"
                        showSignaturePad = false
                    }
                }
            },
            onBack = {
                showSignaturePad = false
                signingDoc = null
                docContent = null
            }
        )
        return
    }

    // Main list of documents
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlacetaidBackground)
    ) {
        // Header
        Surface(
            color = PlacetaidPrimary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Cerrar", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        if (modoGeneral) "Firma de Documentos" else "Firma de Documentos Legales",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    Text(
                        if (modoGeneral) "Firmante: ${state.juniorNombre}" else "Menor: ${state.juniorNombre}",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = PlacetaidInfoSoft.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📋 Documentos requeridos", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = PlacetaidTextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (modoGeneral) "Firme los documentos disponibles. La firma manuscrita tiene validez legal conforme al Reglamento eIDAS."
                        else "Como tutor legal, debe firmar los siguientes documentos para activar la cuenta de ${state.juniorNombre}. La firma manuscrita tiene validez legal conforme al Reglamento eIDAS.",
                        fontSize = 12.sp,
                        color = PlacetaidTextMuted,
                        lineHeight = 16.sp
                    )
                }
            }

            // Document list
            state.documentos.forEach { doc ->
                DocumentCard(
                    documento = doc,
                    isCompleted = doc.firmado,
                    onFirmar = {
                        // Load document content and show signature pad
                        scope.launch {
                            try {
                                val contentUrl = if (modoGeneral) {
                                    URL("${ApiClient.getAdminUrl()}/api/junior/documento-contenido/${doc.codigo}")
                                } else {
                                    URL("${ApiClient.getAdminUrl()}/api/junior/documento-contenido/${doc.codigo}")
                                }
                                val content = withContext(Dispatchers.IO) { contentUrl.readText() }
                                docContent = JSONObject(content)
                                signingDoc = doc
                                showSignaturePad = true
                            } catch (e: Exception) {
                                error = "Error al cargar documento: ${e.message}"
                            }
                        }
                    }
                )
            }

            if (state.documentos.all { it.firmado }) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PlacetaidSuccessSoft)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = PlacetaidSuccess, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("¡Todos los documentos firmados!", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PlacetaidSuccess)
                            Text("La cuenta de ${state.juniorNombre} ha sido activada.", fontSize = 13.sp, color = PlacetaidTextMuted)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onAllSigned,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PlacetaidSuccess)
                ) {
                    Text("Continuar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }

        // Progress bar
        val firmados = state.documentos.count { it.firmado }
        val total = state.documentos.size
        Surface(
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Progreso", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PlacetaidTextPrimary)
                    Text("$firmados/$total firmados", fontSize = 13.sp, color = PlacetaidTextMuted)
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { if (total > 0) firmados.toFloat() / total else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = PlacetaidSuccess,
                    trackColor = PlacetaidSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DocumentCard(
    documento: DocumentoPendiente,
    isCompleted: Boolean,
    onFirmar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) PlacetaidSuccessSoft.copy(alpha = 0.3f) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCompleted) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (isCompleted) PlacetaidSuccess.copy(alpha = 0.15f) else PlacetaidWarning.copy(alpha = 0.15f),
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Edit,
                    null,
                    tint = if (isCompleted) PlacetaidSuccess else PlacetaidWarning,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    documento.nombre,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = PlacetaidTextPrimary
                )
                Text(
                    if (isCompleted) "✓ Firmado" else "⚠ Pendiente de firma",
                    fontSize = 12.sp,
                    color = if (isCompleted) PlacetaidSuccess else PlacetaidWarning
                )
            }

            if (!isCompleted) {
                Button(
                    onClick = onFirmar,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PlacetaidPrimary),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Firmar", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DocumentSigningView(
    documento: DocumentoPendiente,
    contenido: JSONObject,
    state: FirmaState,
    onFirmaCompletada: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlacetaidBackground)
    ) {
        // Header
        Surface(color = PlacetaidPrimary, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White)
                }
                Text(
                    "Firmar: ${documento.nombre}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Document content
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        contenido.optString("titulo", documento.nombre),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = PlacetaidTextPrimary
                    )
                    Text(
                        "Versión ${contenido.optString("version", "1.0")} · ${contenido.optString("fecha", "")}",
                        fontSize = 11.sp,
                        color = PlacetaidTextMuted,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = PlacetaidBorder)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        contenido.optString("contenido", "Contenido no disponible"),
                        fontSize = 12.sp,
                        color = PlacetaidTextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }

            // Signature pad
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "✍️ Firma manuscrita",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = PlacetaidTextPrimary
                    )
                    Text(
                        "Como tutor legal: ${state.tutorNombre}",
                        fontSize = 12.sp,
                        color = PlacetaidTextMuted,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SignaturePad(
                        onSignatureReady = { firmaBase64 ->
                            onFirmaCompletada(firmaBase64)
                        }
                    )
                }
            }

            // Legal note
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = PlacetaidInfoSoft.copy(alpha = 0.2f))
            ) {
                Row(modifier = Modifier.padding(12.dp)) {
                    Icon(Icons.Default.Info, null, tint = PlacetaidPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Esta firma manuscrita digitalizada tiene validez legal conforme al Reglamento eIDAS (UE 910/2014) y la Ley 6/2020. Al firmar, usted acepta jurídicamente este documento.",
                        fontSize = 11.sp,
                        color = PlacetaidTextMuted,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

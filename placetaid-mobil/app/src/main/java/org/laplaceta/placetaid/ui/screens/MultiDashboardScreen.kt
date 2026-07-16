package org.laplaceta.placetaid.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.laplaceta.placetaid.data.api.ApiClient
import org.laplaceta.placetaid.data.models.*
import org.laplaceta.placetaid.ui.theme.*
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * Pantalla multi-identidad: muestra autorizaciones, documentos y votaciones
 * de TODAS las identidades vinculadas al dispositivo simultáneamente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiDashboardScreen(
    placetaIds: List<PlacetaIDInfo>,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var auths by remember { mutableStateOf<List<MultiAuthRequestResponse>>(emptyList()) }
    var docs by remember { mutableStateOf<List<MultiDocumentoResponse>>(emptyList()) }
    var votos by remember { mutableStateOf<List<MultiVotacionResponse>>(emptyList()) }
    var notifs by remember { mutableStateOf<List<NotificacionResponse>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var previewDocId by remember { mutableStateOf<String?>(null) }
    var previewDocTitle by remember { mutableStateOf("") }

    // Obtener todos los DIPs almacenados
    val dips = placetaIds.map { it.dip }.filter { it.length >= 8 }

    // Cargar datos multi-identidad
    fun cargarDatos() {
        scope.launch {
            loading = true; error = null
            try {
                val api = ApiClient.getApi()
                val body = mapOf("dips" to dips)
                val authResp = withContext(Dispatchers.IO) { api.getMultiPending(body) }
                val docResp = withContext(Dispatchers.IO) { api.getMultiDocumentos(body) }
                val votResp = withContext(Dispatchers.IO) { api.getMultiVotaciones(body) }
                val notResp = withContext(Dispatchers.IO) { api.getMultiNotificaciones(body) }

                if (authResp.isSuccessful) auths = authResp.body() ?: emptyList()
                if (docResp.isSuccessful) docs = docResp.body() ?: emptyList()
                if (votResp.isSuccessful) votos = votResp.body() ?: emptyList()
                if (notResp.isSuccessful) notifs = notResp.body() ?: emptyList()
            } catch (e: Exception) { error = e.message }
            loading = false
        }
    }

    LaunchedEffect(dips) { if (dips.isNotEmpty()) cargarDatos() }

    // ── Vista de previsualización de documento ──
    if (previewDocId != null) {
        DocumentPreviewScreen(
            docId = previewDocId!!,
            titulo = previewDocTitle,
            dips = dips,
            onFirmar = {
                scope.launch {
                    try {
                        val api = ApiClient.getApi()
                        api.firmarDocumento(previewDocId!!, mapOf("dip" to dips.first()))
                    } catch (_: Exception) {}
                    previewDocId = null
                    cargarDatos()
                }
            },
            onRechazar = {
                scope.launch {
                    try {
                        val api = ApiClient.getApi()
                        api.rechazarDocumento(previewDocId!!, mapOf("dip" to dips.first(), "motivo" to "Rechazado por el usuario"))
                    } catch (_: Exception) {}
                    previewDocId = null
                    cargarDatos()
                }
            },
            onBack = { previewDocId = null }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel Multi-Identidad", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PlacetaidPrimary),
                actions = {
                    IconButton(onClick = { cargarDatos() }) {
                        Icon(Icons.Default.Refresh, "Actualizar", tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PlacetaidPrimary)
            }
            return@Scaffold
        }

        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // ── KPIs ──
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    KpiBox("🔑 ${auths.size}", "Pendientes", PlacetaidPrimary)
                    KpiBox("📄 ${docs.size}", "Documentos", Color(0xFF2563EB))
                    KpiBox("🗳️ ${votos.size}", "Votaciones", Color(0xFF7C3AED))
                    KpiBox("🔔 ${notifs.size}", "Avisos", Color(0xFFD97706))
                }
            }

            // ── Autorizaciones pendientes ──
            if (auths.isNotEmpty()) {
                item {
                    Text("🔑 Autorizaciones Pendientes", fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp))
                }
                items(auths) { a ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).background(PlacetaidPrimary, RoundedCornerShape(4.dp)))
                                Spacer(Modifier.width(6.dp))
                                Text(a.servicio ?: "Autorización", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(Modifier.height(4.dp))
                            val idIdentidad = a.identidad ?: "-"
                            val idCodigo = a.codigo ?: "-"
                            Text("🔹 Identidad: $idIdentidad", fontSize = 11.sp, color = PlacetaidPrimary)
                            Text("🔹 Código: $idCodigo", fontSize = 11.sp, color = Color.Gray)
                            Text("🔹 Estado: ${a.estado ?: "pending"}", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }

            // ── Documentos pendientes ──
            if (docs.isNotEmpty()) {
                item {
                    Text("📄 Documentos Pendientes de Firma", fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                }
                items(docs) { d ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            previewDocId = d.id; previewDocTitle = d.titulo ?: ""
                        },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📄", fontSize = 16.sp)
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(d.titulo ?: "Documento", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    val entDoc = d.entidad ?: ""
                                    val tipDoc = d.tipo ?: "documento"
                                    Text("$entDoc · $tipDoc", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            val idDoc = d.identidadNombre ?: d.identidad ?: "-"
                            val csvDoc = d.csv ?: "-"
                            Text("👤 Identidad: $idDoc", fontSize = 11.sp, color = PlacetaidPrimary)
                            Text("🔖 CSV: $csvDoc", fontSize = 10.sp, color = Color.Gray)
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { previewDocId = d.id; previewDocTitle = d.titulo ?: "" },
                                    colors = ButtonDefaults.buttonColors(containerColor = PlacetaidPrimary),
                                    modifier = Modifier.height(32.dp)
                                ) { Text("👁️ Ver", fontSize = 11.sp) }
                            }
                        }
                    }
                }
            }

            // ── Votaciones activas ──
            if (votos.isNotEmpty()) {
                item {
                    Text("🗳️ Votaciones Activas", fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                }
                items(votos) { v ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🗳️", fontSize = 16.sp)
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(v.titulo ?: "Votación", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    val grp = v.grupo ?: "-"
                                    Text("Grupo: $grp", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            val idVot = v.identidadNombre ?: v.identidad ?: "-"
                            Text("👤 Identidad: $idVot", fontSize = 11.sp, color = PlacetaidPrimary)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                Text("✅ ${v.aFavor ?: 0}", fontSize = 13.sp, color = Color(0xFF22a06b))
                                Text("❌ ${v.enContra ?: 0}", fontSize = 13.sp, color = Color(0xFFd03131))
                                Text("⬜ ${v.abstenciones ?: 0}", fontSize = 13.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }

            // ── Sin datos ──
            if (auths.isEmpty() && docs.isEmpty() && votos.isEmpty() && notifs.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("✅", fontSize = 48.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("Todo al día", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("No hay autorizaciones, documentos ni votaciones pendientes.", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiBox(valor: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(valor, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 10.sp, color = Color.Gray)
    }
}

/**
 * Pantalla de previsualización de documento antes de firmar.
 * Permite ver, firmar o rechazar. Una vez firmado, descarga el PDF.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentPreviewScreen(
    docId: String,
    titulo: String,
    dips: List<String>,
    onFirmar: () -> Unit,
    onRechazar: () -> Unit,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var contenido by remember { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(true) }
    var firmado by remember { mutableStateOf(false) }
    var pdfLocalPath by remember { mutableStateOf<String?>(null) }

    // Verificar si ya está descargado localmente
    fun checkLocalPdf(): String? {
        val file = File(ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "${docId}.pdf")
        return if (file.exists()) file.absolutePath else null
    }

    fun descargarPdf() {
        scope.launch {
            try {
                val url = "${ApiClient.getAdminUrl()}/api/documentos/${docId}/pdf"
                val input = withContext(Dispatchers.IO) { URL(url).openStream() }
                val file = File(ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "${docId}.pdf")
                FileOutputStream(file).use { output -> input.copyTo(output) }
                pdfLocalPath = file.absolutePath
            } catch (_: Exception) {}
        }
    }

    fun abrirPdf(path: String) {
        val uri = Uri.fromFile(File(path))
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(intent)
    }

    LaunchedEffect(docId) {
        cargando = true
        pdfLocalPath = checkLocalPdf()
        try {
            val api = ApiClient.getApi()
            val resp = withContext(Dispatchers.IO) {
                api.getDocumentoContenido(docId, mapOf("dips" to dips))
            }
            if (resp.isSuccessful) {
                val body = resp.body()
                contenido = when (val c = body?.contenido) {
                    is String -> c
                    else -> null
                }
            }
        } catch (_: Exception) {}
        cargando = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titulo, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PlacetaidPrimary)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (cargando) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PlacetaidPrimary)
                }
                return@Scaffold
            }

            // Previsualización del documento
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("📄 $titulo", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(contenido ?: "Documento PDF — previsualización no disponible",
                        fontSize = 13.sp, color = Color.DarkGray)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Acciones
            if (!firmado) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onFirmar(); firmado = true; descargarPdf() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22a06b)),
                        modifier = Modifier.weight(1f)
                    ) { Text("✍️ Firmar", fontWeight = FontWeight.Bold) }

                    OutlinedButton(
                        onClick = onRechazar,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFd03131)),
                        modifier = Modifier.weight(1f)
                    ) { Text("❌ Rechazar", fontWeight = FontWeight.Bold) }
                }
            } else {
                // Ya firmado — descargar o abrir PDF
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("✅ Documento Firmado", fontWeight = FontWeight.Bold, color = Color(0xFF22a06b))
                        Spacer(Modifier.height(8.dp))

                        if (pdfLocalPath != null) {
                            Button(
                                onClick = { abrirPdf(pdfLocalPath!!) },
                                colors = ButtonDefaults.buttonColors(containerColor = PlacetaidPrimary),
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("📂 Abrir PDF (descargado)", fontWeight = FontWeight.Bold) }
                        } else {
                            Button(
                                onClick = { descargarPdf() },
                                colors = ButtonDefaults.buttonColors(containerColor = PlacetaidPrimary),
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("⬇️ Descargar PDF", fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
    }
}

package org.laplaceta.placetaid.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.laplaceta.placetaid.data.api.ApiClient
import org.laplaceta.placetaid.data.models.MultiDocumentoResponse
import org.laplaceta.placetaid.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun DocumentsScreen(
    documentos: List<MultiDocumentoResponse>,
    dips: List<String>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onNavigateToMulti: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var signingDocId by remember { mutableStateOf<String?>(null) }
    var signingState by remember { mutableStateOf<String?>(null) } // "signing" | "signed" | "error"

    Box(modifier = Modifier.fillMaxSize()) {
        if (documentos.isEmpty() && !isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "📄", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Sin documentos pendientes",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = PlacetaidTextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Los documentos que requieran tu firma\nelectrónica aparecerán aquí",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        color = PlacetaidTextMuted,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = onNavigateToMulti) {
                        Text("📋 Ver multi-identidad", color = PlacetaidPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp, 16.dp, 16.dp, 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Documentos Pendientes",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = PlacetaidTextPrimary
                        )
                        Text(
                            text = "Firma electrónica con PlacetaID",
                            fontSize = 12.sp,
                            color = PlacetaidTextMuted
                        )
                    }
                    if (documentos.isNotEmpty()) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = PlacetaidInfoSoft
                        ) {
                            Text(
                                text = "${documentos.size}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = PlacetaidPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PlacetaidPrimary)
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp) // space for bottom nav
                ) {
                    items(documentos, key = { "${it.identidad}-${it.id}" }) { doc ->
                        DocumentCard(
                            documento = doc,
                            isSigning = signingDocId == doc.id && signingState == "signing",
                            isSigned = signingDocId == doc.id && signingState == "signed",
                            onSign = {
                                scope.launch {
                                    signingDocId = doc.id
                                    signingState = "signing"
                                    try {
                                        val api = ApiClient.getApi()
                                        val dip = dips.firstOrNull() ?: doc.identidad ?: ""
                                        val resp = api.firmarDocumento(doc.id ?: "", mapOf("dip" to dip))
                                        if (resp.isSuccessful) {
                                            signingState = "signed"
                                            kotlinx.coroutines.delay(1500)
                                            onRefresh()
                                        } else {
                                            signingState = "error"
                                        }
                                    } catch (e: Exception) {
                                        signingState = "error"
                                    }
                                    signingDocId = null
                                    signingState = null
                                }
                            },
                            onView = {
                                scope.launch {
                                    try {
                                        val api = ApiClient.getApi()
                                        val dip = dips.firstOrNull() ?: doc.identidad ?: ""
                                        val resp = api.getDocumentoContenido(doc.id ?: "", mapOf("dips" to dips))
                                        // Just refresh for now - content preview can be added later
                                    } catch (_: Exception) {}
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
fun DocumentCard(
    documento: MultiDocumentoResponse,
    isSigning: Boolean,
    isSigned: Boolean,
    onSign: () -> Unit,
    onView: () -> Unit
) {
    val bgColor = when {
        isSigned -> Color(0xFFE8F5E9)
        isSigning -> Color(0xFFFFF8E1)
        else -> Color.White
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSigning) 4.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row: icon + title + badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Document type icon
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PlacetaidInfoSoft,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "📄", fontSize = 22.sp)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = documento.titulo ?: "Documento",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = PlacetaidTextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val ent = documento.entidad ?: ""
                        val tipo = documento.tipo ?: ""
                        Text(
                            text = if (ent.isNotEmpty()) "$ent · $tipo" else (tipo.ifEmpty { "Documento" }),
                            fontSize = 11.sp,
                            color = PlacetaidTextMuted
                        )
                    }
                }
                // Status badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        isSigned -> PlacetaidSuccessSoft
                        isSigning -> PlacetaidWarningSoft
                        else -> PlacetaidInfoSoft
                    }
                ) {
                    Text(
                        text = when {
                            isSigned -> "✅ Firmado"
                            isSigning -> "⏳ Firmando..."
                            else -> "Pendiente"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            isSigned -> PlacetaidSuccess
                            isSigning -> PlacetaidWarning
                            else -> PlacetaidPrimary
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Identity info
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(6.dp), color = PlacetaidInfoSoft) {
                    Text(
                        text = "🔑 ${documento.identidad ?: documento.identidadNombre ?: "—"}",
                        fontSize = 11.sp,
                        color = PlacetaidPrimary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                if (documento.csv != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "CSV: ${documento.csv}", fontSize = 9.sp, color = PlacetaidTextMuted)
                }
            }

            // Actions
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSigned) {
                    Text(
                        text = "✅ Firmado electrónicamente",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = PlacetaidSuccess
                    )
                } else {
                    OutlinedButton(
                        onClick = onView,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PlacetaidTextMuted),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("👁️ Ver", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onSign,
                        enabled = !isSigning,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PlacetaidPrimary,
                            disabledContainerColor = PlacetaidInfoSoft
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        if (isSigning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = if (isSigning) "Firmando..." else "✍️ Firmar",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

package org.laplaceta.placetaid.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.laplaceta.placetaid.data.api.ApiClient
import org.laplaceta.placetaid.data.models.AccessHistory
import org.laplaceta.placetaid.data.models.PlacetaIDInfo
import org.laplaceta.placetaid.ui.components.HistoryItem
import org.laplaceta.placetaid.ui.theme.*
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    placetaids: List<PlacetaIDInfo>,
    selectedDip: String?,
    history: List<AccessHistory>,
    onSelectDip: (String) -> Unit
) {
    var currentTab by remember { mutableStateOf("accesos") }
    val ctx = LocalContext.current

    var firmas by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var juniorData by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(selectedDip, currentTab) {
        if (selectedDip == null) return@LaunchedEffect
        if (currentTab == "firmas" && firmas.isEmpty()) {
            isLoading = true
            try {
                val json = withContext(Dispatchers.IO) {
                    URL("${ApiClient.getAdminUrl()}/api/documentos/pendientes?dip=${selectedDip}").readText()
                }
                val arr = JSONArray(json)
                val list = mutableListOf<JSONObject>()
                for (i in 0 until arr.length()) {
                    val d = arr.getJSONObject(i)
                    list.add(d)
                }
                firmas = list
            } catch (_: Exception) { firmas = emptyList() }
            isLoading = false
        }
        if (currentTab == "junior" && juniorData.isEmpty()) {
            isLoading = true
            try {
                val json = withContext(Dispatchers.IO) {
                    URL("${ApiClient.getAdminUrl()}/api/junior/logs/$selectedDip").readText()
                }
                val arr = JSONArray(json)
                val list = mutableListOf<JSONObject>()
                for (i in 0 until arr.length()) {
                    val log = arr.getJSONObject(i)
                    val det = log.optString("detalle", "")
                    if (det.contains(selectedDip ?: "")) list.add(log)
                }
                juniorData = list
            } catch (_: Exception) { juniorData = emptyList() }
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (placetaids.size > 1) {
            var expanded by remember { mutableStateOf(false) }
            val sel = placetaids.find { it.dip == selectedDip }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth().padding(16.dp, 16.dp, 16.dp, 4.dp)
            ) {
                OutlinedTextField(value = sel?.dip ?: "Selecciona PlacetaID", onValueChange = {}, readOnly = true,
                    label = { Text("Identidad") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PlacetaidPrimary, focusedLabelColor = PlacetaidPrimary))
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    placetaids.forEach { info ->
                        DropdownMenuItem(text = { Column { Text("${info.nombre} ${info.apellidos}", fontWeight = FontWeight.Medium, fontSize = 14.sp); Text(info.dip, fontSize = 12.sp, color = PlacetaidTextMuted) } },
                            onClick = { onSelectDip(info.dip); expanded = false })
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("accesos" to "📱 Accesos", "firmas" to "✍️ Firmas", "junior" to "👶 Junior").forEach { (tab, label) ->
                FilterChip(selected = currentTab == tab, onClick = { currentTab = tab },
                    label = { Text(label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PlacetaidPrimary, selectedLabelColor = Color.White))
            }
        }

        when (currentTab) {
            "accesos" -> {
                if (history.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📋", fontSize = 48.sp); Spacer(Modifier.height(16.dp))
                            Text("Sin historial", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = PlacetaidTextPrimary)
                            Text("Los accesos aparecerán aquí", fontSize = 14.sp, color = PlacetaidTextMuted)
                        }
                    }
                } else {
                    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                        item { Text("Historial de Acceso", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PlacetaidTextPrimary, modifier = Modifier.padding(vertical = 12.dp)) }
                        itemsIndexed(history) { index, entry -> HistoryItem(entry = entry, isLast = index == history.lastIndex) }
                    }
                }
            }
            "firmas" -> {
                if (isLoading) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PlacetaidPrimary) } }
                else if (firmas.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("✍️", fontSize = 48.sp); Spacer(Modifier.height(12.dp))
                            Text("Sin documentos firmados", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = PlacetaidTextPrimary)
                            Text("Los documentos que firmes\naparecerán aquí con su PDF", textAlign = TextAlign.Center, fontSize = 13.sp, color = PlacetaidTextMuted)
                        }
                    }
                } else {
                    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                        item { Text("📜 Documentos firmados", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PlacetaidTextPrimary, modifier = Modifier.padding(vertical = 8.dp)) }
                        itemsIndexed(firmas) { _, doc ->
                            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.List, null, tint = PlacetaidPrimary, modifier = Modifier.size(24.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(doc.optString("titulo_documento", "Documento"), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = PlacetaidTextPrimary)
                                        Text(doc.optString("codigo_modelo", ""), fontSize = 11.sp, color = PlacetaidTextMuted)
                                        Text(doc.optString("creado_en", "").take(10), fontSize = 10.sp, color = PlacetaidTextMuted)
                                    }
                                    TextButton(onClick = {
                                        val url = "${ApiClient.getAdminUrl()}/api/junior/documento-verificable/${doc.optInt("id")}"
                                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    }) { Text("PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PlacetaidPrimary) }
                                }
                            }
                        }
                    }
                }
            }
            "junior" -> {
                if (isLoading) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PlacetaidAccent) } }
                else if (juniorData.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("👶", fontSize = 48.sp); Spacer(Modifier.height(12.dp))
                            Text("Sin actividad Junior", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = PlacetaidTextPrimary)
                            Text("Sesiones y autorizaciones\nde Placeta Junior", textAlign = TextAlign.Center, fontSize = 13.sp, color = PlacetaidTextMuted)
                        }
                    }
                } else {
                    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                        item { Text("👶 Placeta Junior", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PlacetaidTextPrimary, modifier = Modifier.padding(vertical = 8.dp)) }
                        itemsIndexed(juniorData) { _, s ->
                            val a = s.optString("accion", "")
                            val em = when { a.contains("login") -> "🔑"; a.contains("autorizacion") || a.contains("aprobado") -> "✅"; a.contains("firma") -> "✍️"; a.contains("activada") -> "🎉"; else -> "📋" }
                            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = PlacetaidAccent.copy(alpha = 0.06f))) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                                    Text(em, fontSize = 20.sp); Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(a.replace("_", " "), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = PlacetaidTextPrimary)
                                        Text(s.optString("detalle", ""), fontSize = 11.sp, color = PlacetaidTextMuted, lineHeight = 14.sp)
                                        Text(s.optString("creado_en", "").take(16), fontSize = 10.sp, color = PlacetaidTextMuted.copy(alpha = 0.6f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

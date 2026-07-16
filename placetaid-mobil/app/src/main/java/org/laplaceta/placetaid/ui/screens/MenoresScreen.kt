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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.laplaceta.placetaid.data.models.JuniorInfo
import org.laplaceta.placetaid.data.api.ApiClient
import org.laplaceta.placetaid.data.models.PlacetaIDInfo
import org.laplaceta.placetaid.ui.theme.*
import java.net.URL

@Composable
fun MenoresScreen(
    miDip: String?,
    miNombre: String?
) {
    val scope = rememberCoroutineScope()
    var menores by remember { mutableStateOf<List<JuniorInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Load minors
    LaunchedEffect(miDip) {
        if (miDip.isNullOrBlank()) {
            isLoading = false
            return@LaunchedEffect
        }
        try {
            isLoading = true
            error = null
            val result = withContext(Dispatchers.IO) {
                val url = URL("${ApiClient.getAdminUrl()}/api/junior/menores/$miDip")
                url.readText()
            }
            val arr = JSONArray(result)
            val list = mutableListOf<JuniorInfo>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(JuniorInfo(
                    id = obj.optString("id", null),
                    dip = obj.optString("dip", ""),
                    alias = obj.optString("alias", ""),
                    tutorDip = obj.optString("tutor_dip", null),
                    cuentaBanco = obj.optString("cuenta_banco", null),
                    nombre = obj.optString("nombre", null),
                    apellidos = obj.optString("apellidos", null),
                    creadoEn = obj.optString("creado_en", null)
                ))
            }
            menores = list
        } catch (e: Exception) {
            error = e.message ?: "Error desconocido"
        } finally {
            isLoading = false
        }
    }

    if (miDip.isNullOrBlank()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "🔐", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Sin identidad seleccionada", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = PlacetaidTextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Añade un PlacetaID para ver\nlos menores vinculados a tu identidad", textAlign = TextAlign.Center, fontSize = 14.sp, color = PlacetaidTextMuted, lineHeight = 20.sp)
            }
        }
        return
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
                Text(text = "⚠️", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Error al cargar menores", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = PlacetaidTextPrimary)
                Text(text = error ?: "", fontSize = 13.sp, color = PlacetaidTextMuted, modifier = Modifier.padding(horizontal = 32.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    scope.launch { isLoading = true; error = null; /* trigger reload */ }
                }, colors = ButtonDefaults.buttonColors(containerColor = PlacetaidPrimary)) {
                    Text("Reintentar")
                }
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = "Mis Menores", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PlacetaidTextPrimary)
                Text(text = "Vinculados a $miNombre", fontSize = 13.sp, color = PlacetaidTextMuted)
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = PlacetaidInfoSoft
            ) {
                Text(
                    text = "${menores.size} vinculado${if (menores.size != 1) "s" else ""}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PlacetaidPrimary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }

        if (menores.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "👶", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "No tienes menores vinculados", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = PlacetaidTextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Usa la app Placeta Junior para\nregistrar y vincular menores a tu identidad", textAlign = TextAlign.Center, fontSize = 13.sp, color = PlacetaidTextMuted, lineHeight = 18.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(menores, key = { it.dip }) { junior ->
                    JuniorCard(junior)
                }
            }
        }
    }
}

@Composable
fun JuniorCard(junior: JuniorInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Junior alias + DIP
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = PlacetaidInfoSoft,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = "👶", fontSize = 20.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = junior.alias, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PlacetaidTextPrimary)
                        if (junior.nombre != null) {
                            val fullName = listOfNotNull(junior.nombre, junior.apellidos).joinToString(" ")
                            Text(text = fullName, fontSize = 13.sp, color = PlacetaidTextMuted)
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFE3F2FD)
                ) {
                    Text(
                        text = junior.dip,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1565C0),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bank account info
            if (junior.cuentaBanco != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🏦", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Cuenta: ${junior.cuentaBanco}", fontSize = 13.sp, color = PlacetaidTextMuted)
                }
            }

            if (junior.creadoEn != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "📅", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Desde: ${junior.creadoEn.take(10)}", fontSize = 13.sp, color = PlacetaidTextMuted)
                }
            }
        }
    }
}

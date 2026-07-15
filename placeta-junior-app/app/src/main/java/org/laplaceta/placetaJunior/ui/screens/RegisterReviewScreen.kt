package org.laplaceta.placetajunior.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.laplaceta.placetajunior.ui.theme.*

@Composable
fun RegisterReviewScreen(
    tutorData: TutorData,
    minorData: MinorData,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
    isLoading: Boolean,
    error: String?
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PJOrangeBg)
    ) {
        // ── Top bar ────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PJOrange)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, enabled = !isLoading) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = PJWhite)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Confirmar registro",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = PJWhite,
                modifier = Modifier.weight(1f)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            Text(
                text = "Tutor legal",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = PJGray500,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PJWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    DataRow(icon = Icons.Default.Person, label = "Nombre", value = "${tutorData.nombre} ${tutorData.apellidos}")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = PJGray100)
                    DataRow(icon = Icons.Default.Badge, label = "DNI", value = tutorData.dni)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = PJGray100)
                    DataRow(icon = Icons.Default.Email, label = "Email", value = tutorData.email)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = PJGray100)
                    DataRow(icon = Icons.Default.Cake, label = "Fecha nacimiento", value = tutorData.fechaNacimiento)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Menor",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = PJGray500,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PJWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    DataRow(icon = Icons.Default.Face, label = "Nombre", value = "${minorData.nombre} ${minorData.apellidos}")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = PJGray100)
                    DataRow(icon = Icons.Default.Cake, label = "Fecha nacimiento", value = minorData.fechaNacimiento)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = PJYellowLight)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.Lightbulb, null, tint = PJYellow, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Tras confirmar, el tutor recibirá una solicitud en PlacetaID Móvil para firmar el alta y activar la cuenta del menor.",
                        fontSize = 13.sp,
                        color = PJGray700
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PJRedLight)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, null, tint = PJRed, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = error, color = PJRed, fontSize = 13.sp)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                onClick = onConfirm,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PJPurple),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = PJWhite,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Registrando...", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirmar y enviar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DataRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = PJPurple, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = label, fontSize = 12.sp, color = PJGray400)
            Text(text = value, fontSize = 15.sp, color = PJBlack, fontWeight = FontWeight.Medium)
        }
    }
}

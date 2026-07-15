package org.laplaceta.placetajunior.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.laplaceta.placetajunior.ui.theme.*

/**
 * DIP Digital Junior — Carnet de identidad del ecosistema para menores.
 * Fondo blanco, formas GDLP de colores, QR real, tipografía HandlyCasual.
 */
@Composable
fun DipDigitalJuniorCard(
    dip: String,
    nombre: String = "",
    edad: Int = 0,
    nivel: Int = 1,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PJWhite)
                .padding(24.dp)
        ) {
            // ── Formas decorativas GDLP ─────────────────────────
            GdlpShape(
                modifier = Modifier.size(50.dp).align(Alignment.TopEnd),
                color = PJOrange.copy(alpha = 0.15f)
            )
            GdlpShape(
                modifier = Modifier.size(40.dp).align(Alignment.BottomStart),
                color = PJBlue.copy(alpha = 0.12f)
            )
            GdlpShape(
                modifier = Modifier.size(30.dp).align(Alignment.TopStart).offset(x = 10.dp, y = 20.dp),
                color = PJGreen.copy(alpha = 0.1f)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = "DIP DIGITAL JUNIOR",
                    fontFamily = HandlyCasual,
                    color = PJPurple.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    letterSpacing = 3.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(20.dp))

                // QR Code real con el DIP
                QRCode(
                    data = dip,
                    modifier = Modifier.size(120.dp),
                    size = 512
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Escanea para verificar identidad",
                    color = PJGray400,
                    fontSize = 9.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Full name
                if (nombre.isNotEmpty()) {
                    Text(
                        text = nombre,
                        fontFamily = HandlyCasual,
                        color = PJPurple,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // DIP
                Text(
                    text = dip,
                    color = PJGray900,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Info row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    InfoChip("Edad", "${if (edad > 0) "$edad años" else "—"}", PJOrange)
                    InfoChip("Nivel", "$nivel", PJGreen)
                    InfoChip("Estado", "Activo", PJBlue)
                }

                Spacer(modifier = Modifier.height(18.dp))

                HorizontalDivider(color = PJGray200)

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Emisor", color = PJGray400, fontSize = 9.sp, letterSpacing = 1.sp)
                        Text("Grupo de La Placeta", color = PJGray600, fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Ecosistema", color = PJGray400, fontSize = 9.sp, letterSpacing = 1.sp)
                        Text("Placeta Junior", color = PJPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = PJPurple),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CERRAR", fontFamily = HandlyCasual, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                }
            }
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = PJGray400, fontSize = 9.sp, letterSpacing = 1.sp)
        Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

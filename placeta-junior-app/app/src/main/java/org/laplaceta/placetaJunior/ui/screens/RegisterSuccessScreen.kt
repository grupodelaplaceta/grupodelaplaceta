package org.laplaceta.placetajunior.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlinx.coroutines.delay
import org.laplaceta.placetajunior.ui.theme.*

@Composable
fun RegisterSuccessScreen(
    dip: String,
    placetaidCodigo: String?,
    onIrAlLogin: () -> Unit
) {
    var showContent by remember { mutableStateOf(false) }
    var showCode by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showContent = true
        delay(800)
        showCode = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PJPurple),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn() + scaleIn()
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = PJWhite,
                    modifier = Modifier.size(100.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn() + slideInVertically()
            ) {
                Text(
                    text = "¡Registro completado!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = PJWhite,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn() + slideInVertically()
            ) {
                Text(
                    text = "El tutor debe firmar el alta desde PlacetaID Móvil",
                    fontSize = 14.sp,
                    color = PJWhite.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // DIP del menor
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn() + slideInVertically()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PJWhite.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "DIP del menor",
                            fontSize = 12.sp,
                            color = PJWhite.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dip,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = PJWhite,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Código PlacetaID
            AnimatedVisibility(
                visible = showCode,
                enter = fadeIn() + scaleIn()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PJWhite)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.PhonelinkSetup,
                            contentDescription = null,
                            tint = PJOrange,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Código de verificación",
                            fontSize = 13.sp,
                            color = PJGray500
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = placetaidCodigo ?: "---",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = PJOrange,
                            letterSpacing = 8.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Introduce este código en PlacetaID Móvil\no abre la app para autorizar",
                            fontSize = 12.sp,
                            color = PJGray400,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            AnimatedVisibility(
                visible = showCode,
                enter = fadeIn()
            ) {
                OutlinedButton(
                    onClick = onIrAlLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PJWhite)
                ) {
                    Text("Ir a iniciar sesión", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

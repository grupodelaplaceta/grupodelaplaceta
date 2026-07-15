package org.laplaceta.placetajunior.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.laplaceta.placetajunior.R
import org.laplaceta.placetajunior.ui.components.GdlpShape
import org.laplaceta.placetajunior.ui.theme.*

@Composable
fun HomeScreen(
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    onTutorLogin: () -> Unit
) {
    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showContent = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PJGray50)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Header ─────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(PJPurple)
        ) {
            GdlpShape(
                modifier = Modifier.size(120.dp).align(Alignment.CenterEnd).padding(8.dp),
                color = PJBlue
            )
            GdlpShape(
                modifier = Modifier.size(80.dp).align(Alignment.TopStart).padding(8.dp),
                color = PJOrange
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn() + scaleIn()
                ) {
                    Image(
                        painter = painterResource(R.drawable.sobreblanco),
                        contentDescription = "Logo",
                        colorFilter = ColorFilter.tint(PJWhite),
                        modifier = Modifier.size(80.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn() + slideInVertically { it / 2 }
                ) {
                    // PLACETA en Outfit, JUNIOR en HandlyCasual
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontFamily = OutfitBold, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = PJWhite)) {
                                append("PLACETA ")
                            }
                            withStyle(SpanStyle(fontFamily = HandlyCasual, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = PJWhite)) {
                                append("JUNIOR")
                            }
                        },
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn() + slideInVertically { it / 2 }
                ) {
                    Text(
                        text = "🌟 Aprende, juega y crece",
                        fontSize = 14.sp,
                        color = PJWhite.copy(alpha = 0.9f)
                    )
                }
            }
        }

        // ── Parental Info Card ─────────────────────────────────────
        Spacer(modifier = Modifier.height(24.dp))
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PJRedLight),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(PJRed.copy(alpha = 0.2f))
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("👨‍👩‍👧", fontSize = 28.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "🛡️ Control Parental",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = PJRed
                    )
                    Text(
                        "Cada acceso requiere autorización del tutor vía PlacetaID Móvil. Límites de gasto y tiempo configurados por tu tutor.",
                        fontSize = 11.sp,
                        color = PJGray600,
                        lineHeight = 15.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // ── Opciones principales ───────────────────────────────────
        Spacer(modifier = Modifier.height(32.dp))

        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn() + slideInVertically { it / 4 }
        ) {
            OptionCard(
                icon = Icons.Default.Login,
                title = "Ya tengo cuenta",
                subtitle = "Inicia sesión con el DIP del menor",
                cardColor = PJPurple,
                onClick = onLogin
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn() + slideInVertically { it / 4 }
        ) {
            OptionCard(
                icon = Icons.Default.PersonAdd,
                title = "Registrar nuevo menor",
                subtitle = "El tutor legal da de alta a un menor de 16 años",
                cardColor = PJBlue,
                onClick = onRegister
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn() + slideInVertically { it / 4 }
        ) {
            OptionCard(
                icon = Icons.Default.VerifiedUser,
                title = "¿Eres tutor?",
                subtitle = "Gestiona menores desde tu PlacetaID Móvil",
                cardColor = PJGreen,
                onClick = onTutorLogin
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── Footer ─────────────────────────────────────────────────
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn()
        ) {
            Text(
                text = "Grupo de La Placeta © 2026",
                fontSize = 12.sp,
                color = PJGray400,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun OptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    cardColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = PJWhite)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(cardColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PJWhite,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PJGray900
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = PJGray500
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = PJGray400,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

package org.laplaceta.placetajunior.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.clickable
import org.laplaceta.placetajunior.R
import org.laplaceta.placetajunior.ui.components.DipDigitalJuniorCard
import org.laplaceta.placetajunior.ui.components.GdlpShape
import org.laplaceta.placetajunior.ui.theme.*

@Composable
fun DashboardScreen(
    dip: String,
    saldo: Int,
    nombre: String = "",
    onLogout: () -> Unit,
    onAcademia: () -> Unit = {},
    onMonedero: () -> Unit = {},
    onProgreso: () -> Unit = {},
    onAmigos: () -> Unit = {}
) {
    // IBAN se obtiene del API, no se genera en cliente
    val scrollState = rememberScrollState()
    var showContent by remember { mutableStateOf(false) }
    var showDipDigital by remember { mutableStateOf(false) }

    // Geography greetings
    val saludos = listOf(
        "👋 ¡Hola, %s! ¿Sabías que La Placeta tiene su propia geografía?" to "Mapa de La Placeta — GDLP Cartografía",
        "🌟 ¡Hey %s! La Placeta se divide en hexágonos, como un tablero gigante." to "Sistema Hexagonal — Normativa GDLP Art. 4",
        "🌍 %s, ¿conoces los distritos de La Placeta? Hay zonas verdes, comerciales y residenciales." to "Plan Urbanístico — GDLP",
        "🚀 ¡Bienvenido %s! En La Placeta cada hexágono es una comunidad única." to "GDLP — Guía del Ciudadano"
    )
    val (saludoRaw, fuente) = remember { saludos.random() }
    val saludo = saludoRaw.format(nombre.ifEmpty { "Amigo" })

    LaunchedEffect(Unit) {
        showContent = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PJGray50)
    ) {
        // ── Header ─────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PJPurple)
                .padding(24.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PLACETA  JUNIOR",
                            fontSize = 11.sp,
                            fontFamily = HandlyCasual,
                            color = PJWhite.copy(alpha = 0.7f),
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = dip,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PJWhite
                        )
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, "Cerrar sesión", tint = PJWhite)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Parental Control Bar ───────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PJRedLight),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(PJRed.copy(alpha = 0.2f))
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🛡️", fontSize = 20.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Supervisado por tu tutor",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = PJRed
                            )
                            Text(
                                "Límite diario: ${saldo.coerceAtMost(50)} Pz/día",
                                fontSize = 11.sp,
                                color = PJGray600
                            )
                        }
                        Text(
                            "🔒",
                            fontSize = 16.sp,
                            color = PJRed.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── DIP Digital Card ───────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { showDipDigital = true },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PJWhite.copy(alpha = 0.12f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(R.drawable.sobreblanco),
                                contentDescription = "Logo",
                                colorFilter = ColorFilter.tint(PJYellow),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "DIP DIGITAL",
                                fontSize = 13.sp,
                                fontFamily = HandlyCasual,
                                color = PJWhite.copy(alpha = 0.8f),
                                letterSpacing = 2.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = dip,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = PJWhite,
                            letterSpacing = 2.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── IBAN Card ───────────────────────────────────────
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn() + slideInVertically()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = PJYellowLight)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(PJYellow.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AccountBalance,
                                    null,
                                    tint = PJYellow,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Tus Placetas",
                                    fontSize = 13.sp,
                                    color = PJGray600
                                )
                                Text(
                                    text = "$saldo Pz",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PJGray900
                                )
                                Text(
                                    text = "Cuenta Child · Banco de La Placeta",
                                    fontSize = 11.sp,
                                    color = PJGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Acciones rápidas ──────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            Text(
                text = "Acciones",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PJGray900,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn() + slideInVertically()
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    ActionButton(
                        icon = Icons.Default.School,
                        label = "Academia",
                        color = PJPurple,
                        bgColor = PJPurpleBg,
                        modifier = Modifier.weight(1f),
                        onClick = onAcademia
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    ActionButton(
                        icon = Icons.Default.AccountBalanceWallet,
                        label = "Monedero",
                        color = PJGreen,
                        bgColor = PJGreenBg,
                        modifier = Modifier.weight(1f),
                        onClick = onMonedero
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn() + slideInVertically()
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    ActionButton(
                        icon = Icons.Default.Leaderboard,
                        label = "Progreso",
                        color = PJBlue,
                        bgColor = PJBlueBg,
                        modifier = Modifier.weight(1f),
                        onClick = onProgreso
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    ActionButton(
                        icon = Icons.Default.People,
                        label = "Amigos",
                        color = PJOrange,
                        bgColor = PJOrangeBg,
                        modifier = Modifier.weight(1f),
                        onClick = onAmigos
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Curiosidad geográfica ──────────────────────────────
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn() + slideInVertically()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PJWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🗺️", fontSize = 28.sp)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "Geografía de La Placeta",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = PJGray900
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = saludo,
                            fontSize = 14.sp,
                            color = PJGray700,
                            lineHeight = 20.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📖", fontSize = 14.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = fuente,
                                fontSize = 11.sp,
                                color = PJGray400,
                                fontFamily = OutfitRegular
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // ── DIP Digital Modal ──────────────────────────────────────────
    if (showDipDigital) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PJBlack.copy(alpha = 0.6f))
                .clickable { showDipDigital = false },
            contentAlignment = Alignment.Center
        ) {
            DipDigitalJuniorCard(
                dip = dip,
                nombre = "",
                edad = 0,
                nivel = 1,
                onDismiss = { showDipDigital = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            )
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    bgColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PJWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                GdlpShape(
                    modifier = Modifier.fillMaxSize(),
                    color = bgColor
                )
                Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = PJGray700,
                textAlign = TextAlign.Center
            )
        }
    }
}

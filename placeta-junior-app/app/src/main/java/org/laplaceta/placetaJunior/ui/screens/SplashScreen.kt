package org.laplaceta.placetajunior.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.laplaceta.placetajunior.R
import org.laplaceta.placetajunior.ui.components.GdlpShape
import org.laplaceta.placetajunior.ui.theme.*

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "splash")

    val logoScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoscale"
    )

    val logoAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoalpha"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Colored forma shapes animation
    val shapeAlpha by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shapealpha"
    )

    var showText by remember { mutableStateOf(false) }
    var showSubtext by remember { mutableStateOf(false) }

    val formaColors = listOf(PJRed, PJOrange, PJYellow, PJGreen, PJBlue, PJPurple)

    LaunchedEffect(Unit) {
        delay(200)
        showText = true
        delay(400)
        showSubtext = true
        delay(2500)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PJWhite),
        contentAlignment = Alignment.Center
    ) {
        // ── Formas geométricas decorativas animadas ───────────────
        // Forma central grande
        // Formas con colores planos, sin transparencia, sin colisionar
        GdlpShape(
            modifier = Modifier.size(140.dp).align(Alignment.TopStart).padding(8.dp),
            color = PJRed
        )
        GdlpShape(
            modifier = Modifier.size(100.dp).align(Alignment.TopEnd).padding(8.dp),
            color = PJOrange
        )
        GdlpShape(
            modifier = Modifier.size(120.dp).align(Alignment.BottomStart).padding(8.dp),
            color = PJBlue
        )
        GdlpShape(
            modifier = Modifier.size(80.dp).align(Alignment.BottomEnd).padding(8.dp),
            color = PJGreen
        )

        // ── Contenido principal ───────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo Placeta Junior
            Image(
                painter = painterResource(id = R.drawable.sobreblanco),
                contentDescription = "Placeta Junior",
                modifier = Modifier
                    .scale(logoScale)
                    .alpha(logoAlpha)
                    .width(180.dp)
                    .height(180.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (showText) {
                Text(
                    text = "PLACETA JUNIOR",
                    fontSize = 36.sp,
                    fontFamily = HandlyCasual,
                    fontWeight = FontWeight.Bold,
                    color = PJPurple,
                    textAlign = TextAlign.Center,
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (showSubtext) {
                Text(
                    text = "La app para los peques de La Placeta",
                    fontSize = 14.sp,
                    fontFamily = OutfitRegular,
                    color = PJGray500,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

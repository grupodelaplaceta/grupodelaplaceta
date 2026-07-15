package org.laplaceta.placetajunior.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.laplaceta.placetajunior.ui.theme.*

enum class LoginStep {
    DIP, PLACETAID_AUTH, DONE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onLoginSuccess: (dip: String, nombre: String, apellidos: String, saldo: Int) -> Unit,
    onGoToDashboard: (dip: String) -> Unit
) {
    var step by remember { mutableStateOf(LoginStep.DIP) }
    var dip by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var requestId by remember { mutableStateOf<String?>(null) }
    var codigoPlacetaID by remember { mutableStateOf<String?>(null) }
    var dipMenor by remember { mutableStateOf<String?>(null) }
    var nombreMenor by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PJWhite)
    ) {
        // ── Top bar ────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PJPurple)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (step != LoginStep.PLACETAID_AUTH) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = PJWhite)
                }
            } else {
                Spacer(modifier = Modifier.width(56.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = when (step) {
                    LoginStep.DIP -> "Iniciar sesión"
                    LoginStep.PLACETAID_AUTH -> "Autorización del tutor"
                    LoginStep.DONE -> "¡Bienvenido!"
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = PJWhite,
                modifier = Modifier.weight(1f)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            when (step) {
                LoginStep.DIP -> DipInputStep(
                    dip = dip,
                    onDipChange = { dip = it.uppercase(); error = null },
                    error = error,
                    isLoading = isLoading,
                    onContinue = {
                        if (dip.isBlank()) {
                            error = "Introduce el DIP del menor"
                            return@DipInputStep
                        }
                        scope.launch {
                            isLoading = true
                            error = null
                            val response = org.laplaceta.placetajunior.network.ApiClient.login(dip)
                            isLoading = false
                            if (response.error != null) {
                                error = response.error
                            } else if (response.requiere_autorizacion_tutor) {
                                requestId = response.requestId
                                codigoPlacetaID = response.codigo
                                dipMenor = response.dip_menor ?: dip
                                nombreMenor = response.nombre_menor
                                step = LoginStep.PLACETAID_AUTH
                            } else if (response.success) {
                                onLoginSuccess(dip, response.junior?.nombre ?: "", response.junior?.apellidos ?: "", response.junior?.placetas_saldo ?: 0)
                            } else {
                                error = "Error al iniciar sesión"
                            }
                        }
                    }
                )

                LoginStep.PLACETAID_AUTH -> PlacetaidAuthStep(
                    codigo = codigoPlacetaID,
                    dipMenor = dipMenor,
                    nombreMenor = nombreMenor,
                    requestId = requestId,
                    error = error,
                    onVerified = {
                        onGoToDashboard(dip)
                    }
                )

                LoginStep.DONE -> DoneStep(
                    dip = dip,
                    onGoToDashboard = { onGoToDashboard(dip) }
                )
            }
        }
    }
}

@Composable
private fun DipInputStep(
    dip: String,
    onDipChange: (String) -> Unit,
    error: String?,
    isLoading: Boolean,
    onContinue: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // ── Parental Warning ───────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = PJRedLight),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(PJRed.copy(alpha = 0.2f))
            )
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🛡️", fontSize = 24.sp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Control Parental Activo", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PJRed)
                    Text("El tutor autoriza cada acceso desde su PlacetaID Móvil", fontSize = 11.sp, color = PJGray600)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Icon(
            Icons.Default.AccountCircle,
            contentDescription = null,
            tint = PJPurple,
            modifier = Modifier.size(80.dp)
        )

        Text(
            text = "Introduce el DIP del menor",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = PJGray900,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "El DIP se generó al registrar al menor.\nPregunta al tutor si no lo recuerdas.",
            fontSize = 13.sp,
            color = PJGray500,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = dip,
            onValueChange = onDipChange,
            label = { Text("DIP del menor") },
            placeholder = { Text("Ej: JUNIOR-ABCD") },
            leadingIcon = { Icon(Icons.Default.Badge, null, tint = PJOrange) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
        )

        if (error != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = error, color = PJRed, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onContinue,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PJOrange,
                disabledContainerColor = PJOrange.copy(alpha = 0.3f)
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = PJWhite, strokeWidth = 2.dp)
            } else {
                Text("Continuar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PlacetaidAuthStep(
    codigo: String?,
    dipMenor: String?,
    nombreMenor: String?,
    requestId: String?,
    error: String?,
    onVerified: () -> Unit
) {
    var estado by remember { mutableStateOf("Esperando autorización...") }
    var polling by remember { mutableStateOf(true) }

    LaunchedEffect(requestId) {
        if (requestId == null) return@LaunchedEffect
        while (polling) {
            val result = org.laplaceta.placetajunior.network.ApiClient.pollAutorizacion(requestId)
            if (result.aprobado) {
                estado = "¡Autorizado!"
                polling = false
                delay(1000)
                onVerified()
            } else if (result.status == "denied") {
                estado = "Solicitud denegada"
                polling = false
            } else if (result.status == "expired") {
                estado = "La solicitud ha expirado. Intenta de nuevo."
                polling = false
            }
            delay(2000)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Parental Warning ───────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = PJRedLight),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(PJRed.copy(alpha = 0.2f))
            )
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("👨‍👩‍👧", fontSize = 24.sp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Esperando autorización", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PJRed)
                    Text("El tutor recibió una notificación en su PlacetaID Móvil", fontSize = 11.sp, color = PJGray600)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Icon(
            Icons.Default.PhonelinkSetup,
            contentDescription = null,
            tint = PJOrange,
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Autorización del tutor",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = PJGray900,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (nombreMenor != null) "El tutor debe autorizar el acceso de $nombreMenor" else "El tutor debe autorizar el acceso",
            fontSize = 13.sp,
            color = PJGray500,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Código grande
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = PJWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Código",
                    fontSize = 12.sp,
                    color = PJGray400
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = codigo ?: "---",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = PJOrange,
                    letterSpacing = 12.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Introduce este código en PlacetaID Móvil",
                    fontSize = 13.sp,
                    color = PJGray500,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (polling) {
            CircularProgressIndicator(color = PJOrange, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(12.dp))
        }

        Text(
            text = estado,
            fontSize = 14.sp,
            color = if (estado.contains("Autorizado")) PJGreen else PJGray600,
            textAlign = TextAlign.Center
        )

        if (error != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = error, color = PJRed, fontSize = 13.sp)
        }
    }
}

@Composable
private fun DoneStep(
    dip: String,
    onGoToDashboard: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = PJGreen,
            modifier = Modifier.size(100.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "¡Bienvenido!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = PJGray900
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Sesión iniciada para $dip",
            fontSize = 14.sp,
            color = PJGray500
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onGoToDashboard,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PJOrange),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
        ) {
            Text("Ir al Dashboard", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

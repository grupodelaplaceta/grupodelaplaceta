package org.laplaceta.placetajunior.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.laplaceta.placetajunior.network.ApiClient
import org.laplaceta.placetajunior.ui.theme.*

data class TutorData(
    val nombre: String = "",
    val apellidos: String = "",
    val dni: String = "",
    val email: String = "",
    val fechaNacimiento: String = "",
    val aceptaTerminos: Boolean = false,
    val yaTienePlacetaID: Boolean = false,
    val placetaidRequestId: String? = null,
    val placetaidCodigo: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterTutorScreen(
    initialData: TutorData = TutorData(),
    onBack: () -> Unit,
    onContinue: (TutorData) -> Unit
) {
    var step by remember { mutableStateOf(
        if (initialData.yaTienePlacetaID) 2 else 0
    ) }
    var tienePlacetaID by remember { mutableStateOf(initialData.yaTienePlacetaID) }

    // Form fields
    var nombre by remember { mutableStateOf(initialData.nombre) }
    var apellidos by remember { mutableStateOf(initialData.apellidos) }
    var dni by remember { mutableStateOf(initialData.dni) }
    var email by remember { mutableStateOf(initialData.email) }
    var fechaNacimiento by remember { mutableStateOf(initialData.fechaNacimiento) }
    var aceptaTerminos by remember { mutableStateOf(initialData.aceptaTerminos) }

    // PlacetaID fields
    var dipTutor by remember { mutableStateOf(initialData.dni) }
    var placetaidRequestId by remember { mutableStateOf(initialData.placetaidRequestId) }
    var placetaidCodigo by remember { mutableStateOf(initialData.placetaidCodigo) }

    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val scrollState = rememberScrollState()

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
            if (step > 0) {
                IconButton(onClick = {
                    when (step) {
                        1 -> { step = 0; tienePlacetaID = false }
                        2 -> if (tienePlacetaID) step = 1 else step = 0
                        else -> onBack()
                    }
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = PJWhite)
                }
            } else {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = PJWhite)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (tienePlacetaID) "Registro con PlacetaID" else "Registro - Tutor Legal",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = PJWhite,
                modifier = Modifier.weight(1f)
            )
        }

        when (step) {
            0 -> ChoiceStep(
                onTienePlacetaID = {
                    tienePlacetaID = true
                    step = 1
                },
                onNuevoTutor = {
                    tienePlacetaID = false
                    step = 1
                }
            )

            1 -> if (tienePlacetaID) {
                PlacetaIDLoginStep(
                    dipTutor = dipTutor,
                    onDipChange = { dipTutor = it.uppercase(); error = null },
                    error = error,
                    isLoading = isLoading,
                    onBack = { step = 0 },
                    onVerified = { requestId, codigo ->
                        placetaidRequestId = requestId
                        placetaidCodigo = codigo
                        // Fetch tutor data from CRM
                        scope.launch {
                            var tutorNombre = ""
                            var tutorApellidos = ""
                            var tutorEmail = ""
                            try {
                                val info = ApiClient.getTutorInfo(dipTutor)
                                if (info != null) {
                                    val fullName = info.optString("nombre", "")
                                    val parts = fullName.split(" ", limit = 2)
                                    tutorNombre = parts.getOrElse(0) { "" }
                                    tutorApellidos = parts.getOrElse(1) { "" }
                                    tutorEmail = info.optString("email", "")
                                }
                            } catch (_: Exception) {}
                            onContinue(TutorData(
                                nombre = tutorNombre,
                                apellidos = tutorApellidos,
                                dni = dipTutor,
                                email = tutorEmail,
                                yaTienePlacetaID = true,
                                placetaidRequestId = requestId,
                                placetaidCodigo = codigo,
                                aceptaTerminos = true
                            ))
                        }
                    }
                )
            } else {
                FullTutorForm(
                    nombre = nombre, apellidos = apellidos, dni = dni,
                    email = email, fechaNacimiento = fechaNacimiento,
                    aceptaTerminos = aceptaTerminos,
                    onNombreChange = { nombre = it; error = null },
                    onApellidosChange = { apellidos = it; error = null },
                    onDniChange = { dni = it.uppercase(); error = null },
                    onEmailChange = { email = it; error = null },
                    onFechaNacimientoChange = { if (it.length <= 10) { fechaNacimiento = it; error = null } },
                    onTerminosChange = { aceptaTerminos = it; error = null },
                    error = error,
                    onBack = { step = 0 },
                    onContinue = {
                        if (nombre.isBlank() || apellidos.isBlank() || dni.isBlank() || email.isBlank() || fechaNacimiento.isBlank()) {
                            error = "Todos los campos son obligatorios"
                            return@FullTutorForm
                        }
                        if (dni.length < 8) { error = "DNI no válido"; return@FullTutorForm }
                        if (!email.contains("@")) { error = "Email no válido"; return@FullTutorForm }
                        if (!aceptaTerminos) { error = "Debes aceptar los términos"; return@FullTutorForm }
                        error = null
                        onContinue(TutorData(nombre, apellidos, dni, email, fechaNacimiento, aceptaTerminos = true))
                    }
                )
            }
        }
    }
}

// ── Step 0: ¿El tutor ya tiene PlacetaID? ──────────────────────────────

@Composable
private fun ChoiceStep(
    onTienePlacetaID: () -> Unit,
    onNuevoTutor: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.VerifiedUser,
            contentDescription = null,
            tint = PJPurple,
            modifier = Modifier.size(72.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "¿El tutor legal ya tiene PlacetaID?",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = PJBlack,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Selecciona cómo quieres registrar al tutor",
            fontSize = 14.sp,
            color = PJGray500,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Opción: Sí, ya tiene PlacetaID
        ChoiceCard(
            icon = Icons.Default.QrCodeScanner,
            title = "Sí, ya tiene PlacetaID",
            subtitle = "Solo necesita su DIP y aprobar desde PlacetaID Móvil",
            cardColor = PJPurple,
            onClick = onTienePlacetaID
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Opción: No, registrar nuevo
        ChoiceCard(
            icon = Icons.Default.PersonAdd,
            title = "No, es nuevo",
            subtitle = "Rellenar formulario completo con sus datos",
            cardColor = PJOrange,
            onClick = onNuevoTutor
        )
    }
}

@Composable
private fun ChoiceCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    cardColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
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
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = PJWhite, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PJBlack)
                Spacer(modifier = Modifier.height(4.dp))
                Text(subtitle, fontSize = 13.sp, color = PJGray500)
            }
            Icon(Icons.Default.ChevronRight, null, tint = PJGray400)
        }
    }
}

// ── Step 1: PlacetaID login (tutor ya registrado) ──────────────────────

@Composable
private fun PlacetaIDLoginStep(
    dipTutor: String,
    onDipChange: (String) -> Unit,
    error: String?,
    isLoading: Boolean,
    onBack: () -> Unit,
    onVerified: (requestId: String, codigo: String) -> Unit
) {
    var verifying by remember { mutableStateOf(false) }
    var placetaRequestId by remember { mutableStateOf<String?>(null) }
    var placetaCodigo by remember { mutableStateOf<String?>(null) }
    var localError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Icon(
            Icons.Default.QrCodeScanner,
            contentDescription = null,
            tint = PJPurple,
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Introduce el DIP del tutor",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = PJBlack,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "El tutor recibirá una solicitud en PlacetaID Móvil para aprobar el registro del menor",
            fontSize = 13.sp,
            color = PJGray500,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = dipTutor,
            onValueChange = { onDipChange(it); localError = null },
            label = { Text("DIP del tutor") },
            placeholder = { Text("Ej: 12345678A") },
            leadingIcon = { Icon(Icons.Default.Badge, null, tint = PJPurple) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            enabled = !verifying
        )

        val displayError = localError ?: error
        if (displayError != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = PJRedLight)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Error, null, tint = PJRed, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = displayError, color = PJRed, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (verifying && placetaRequestId != null) {
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
                    Icon(Icons.Default.PhonelinkSetup, null, tint = PJPurple, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Código de verificación", fontSize = 13.sp, color = PJGray500)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = placetaCodigo ?: "---",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = PJPurple,
                        letterSpacing = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Introduce este código en PlacetaID Móvil", fontSize = 12.sp, color = PJGray400)
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator(color = PJPurple, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Esperando autorización...", fontSize = 13.sp, color = PJGray500)
                }
            }

            LaunchedEffect(placetaRequestId) {
                val rId = placetaRequestId ?: return@LaunchedEffect
                while (true) {
                    val result = withContext(Dispatchers.IO) {
                        ApiClient.pollAutorizacion(rId)
                    }
                    if (result.aprobado) {
                        onVerified(rId, placetaCodigo ?: "")
                        break
                    }
                    if (result.status == "denied" || result.status == "expired") {
                        localError = "Solicitud ${result.status}"
                        verifying = false
                        break
                    }
                    delay(2000)
                }
            }
        } else {
            Button(
                onClick = {
                    if (dipTutor.isBlank()) {
                        localError = "Introduce el DIP del tutor"
                        return@Button
                    }
                    localError = null
                    verifying = true
                    coroutineScope.launch {
                        try {
                            val json = withContext(Dispatchers.IO) {
                                ApiClient.crearSolicitudPlacetaID(
                                    dip = dipTutor,
                                    servicio = "Placeta Junior - Registro",
                                    url = "placeta-junior://auth"
                                )
                            }
                            if (json != null && json.optBoolean("ok", false)) {
                                placetaRequestId = json.optString("requestId")
                                placetaCodigo = json.optString("codigo")
                            } else {
                                val errMsg = json?.optString("error") ?: "Error al conectar con PlacetaID"
                                localError = errMsg
                                verifying = false
                            }
                        } catch (e: Exception) {
                            localError = "Error de conexión: ${e.message}"
                            verifying = false
                        }
                    }
                },
                enabled = dipTutor.isNotBlank() && !verifying,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PJPurple),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                if (verifying) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = PJWhite, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Verificando...", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.QrCode, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Verificar con PlacetaID", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onBack) {
                Text("Volver", color = PJPurple)
            }
        }
    }
}

// ── Step 1: Formulario completo (nuevo tutor) ──────────────────────────

@Composable
private fun FullTutorForm(
    nombre: String, apellidos: String, dni: String,
    email: String, fechaNacimiento: String, aceptaTerminos: Boolean,
    onNombreChange: (String) -> Unit,
    onApellidosChange: (String) -> Unit,
    onDniChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onFechaNacimientoChange: (String) -> Unit,
    onTerminosChange: (Boolean) -> Unit,
    error: String?,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val scrollState = rememberScrollState()

    // ── Step indicator ─────────────────────────────────────────────
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        listOf("Tutor", "Menor", "Revisar").forEachIndexed { idx, label ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (idx + 1 <= 1) PJPurple else PJGray200,
                            shape = RoundedCornerShape(18.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${idx + 1}",
                        color = if (idx + 1 <= 1) PJWhite else PJGray500,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = if (idx + 1 <= 1) PJPurple else PJGray400,
                    fontWeight = if (idx + 1 <= 1) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = "Datos del tutor legal",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = PJBlack
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "El tutor legal será el responsable del menor en Placeta Junior",
            fontSize = 13.sp,
            color = PJGray500
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = nombre,
            onValueChange = onNombreChange,
            label = { Text("Nombre del tutor") },
            placeholder = { Text("Ej: María") },
            leadingIcon = { Icon(Icons.Default.Person, null, tint = PJPurple) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = apellidos,
            onValueChange = onApellidosChange,
            label = { Text("Apellidos del tutor") },
            placeholder = { Text("Ej: García López") },
            leadingIcon = { Icon(Icons.Default.Person, null, tint = PJPurple) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = dni,
            onValueChange = onDniChange,
            label = { Text("DNI del tutor") },
            placeholder = { Text("Ej: 12345678A") },
            leadingIcon = { Icon(Icons.Default.Badge, null, tint = PJPurple) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Correo electrónico") },
            placeholder = { Text("Ej: maria@email.com") },
            leadingIcon = { Icon(Icons.Default.Email, null, tint = PJPurple) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = fechaNacimiento,
            onValueChange = onFechaNacimientoChange,
            label = { Text("Fecha de nacimiento del tutor") },
            placeholder = { Text("Ej: 1985-06-15") },
            leadingIcon = { Icon(Icons.Default.Cake, null, tint = PJPurple) },
            supportingText = { Text("Formato: AAAA-MM-DD") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = PJPurpleLight)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = aceptaTerminos,
                        onCheckedChange = onTerminosChange,
                        colors = CheckboxDefaults.colors(checkedColor = PJPurple, uncheckedColor = PJGray400)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Acepto los términos y condiciones, la política de privacidad y el consentimiento de tratamiento de datos del menor",
                        fontSize = 13.sp,
                        color = PJGray600,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (error != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = PJRedLight)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Error, null, tint = PJRed, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = error, color = PJRed, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PJPurple),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
        ) {
            Text("Continuar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForward, null)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

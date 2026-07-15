package org.laplaceta.placetajunior.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.laplaceta.placetajunior.ui.theme.*
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter

data class MinorData(
    val nombre: String = "",
    val apellidos: String = "",
    val fechaNacimiento: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterMinorScreen(
    initialData: MinorData = MinorData(),
    onBack: () -> Unit,
    onContinue: (MinorData) -> Unit
) {
    var nombre by remember { mutableStateOf(initialData.nombre) }
    var apellidos by remember { mutableStateOf(initialData.apellidos) }
    var fechaNacimiento by remember { mutableStateOf(initialData.fechaNacimiento) }
    var error by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    fun calcularEdad(fechaStr: String): Int? {
        return try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val nacimiento = LocalDate.parse(fechaStr, formatter)
            val hoy = LocalDate.of(2026, 7, 12)
            Period.between(nacimiento, hoy).years
        } catch (e: Exception) {
            null
        }
    }

    fun validar(): Boolean {
        if (nombre.isBlank() || apellidos.isBlank() || fechaNacimiento.isBlank()) {
            error = "Todos los campos son obligatorios"
            return false
        }
        val edad = calcularEdad(fechaNacimiento)
        if (edad == null) {
            error = "Fecha de nacimiento no válida. Usa formato AAAA-MM-DD"
            return false
        }
        if (edad >= 16) {
            error = "El menor debe tener menos de 16 años. Edad calculada: $edad años"
            return false
        }
        if (edad < 3) {
            error = "La edad mínima para Placeta Junior es 3 años"
            return false
        }
        error = null
        return true
    }

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
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = PJWhite)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Registro - Datos del Menor",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = PJWhite,
                modifier = Modifier.weight(1f)
            )
        }

        // ── Step indicator ─────────────────────────────────────────
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
                                if (idx + 1 <= 2) PJPurple else PJGray200,
                                shape = RoundedCornerShape(18.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${idx + 1}",
                            color = if (idx + 1 <= 2) PJWhite else PJGray500,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        color = if (idx + 1 <= 2) PJPurple else PJGray400,
                        fontWeight = if (idx + 1 <= 2) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        // ── Form ───────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PJBlueLight)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, null, tint = PJBlue, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Placeta Junior es para menores de 3 a 15 años.",
                        fontSize = 13.sp,
                        color = PJBlue,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Datos del menor",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = PJBlack
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Introduce los datos del niño o niña que vas a registrar",
                fontSize = 13.sp,
                color = PJGray500
            )

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Face,
                    contentDescription = null,
                    tint = PJPurple.copy(alpha = 0.3f),
                    modifier = Modifier.size(80.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it; error = null },
                label = { Text("Nombre del menor") },
                placeholder = { Text("Ej: Ana") },
                leadingIcon = { Icon(Icons.Default.Face, null, tint = PJPurple) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = apellidos,
                onValueChange = { apellidos = it; error = null },
                label = { Text("Apellidos del menor") },
                placeholder = { Text("Ej: Martínez Pérez") },
                leadingIcon = { Icon(Icons.Default.Face, null, tint = PJPurple) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = fechaNacimiento,
                onValueChange = { if (it.length <= 10) { fechaNacimiento = it; error = null } },
                label = { Text("Fecha de nacimiento del menor") },
                placeholder = { Text("Ej: 2015-03-20") },
                leadingIcon = { Icon(Icons.Default.Cake, null, tint = PJPurple) },
                supportingText = {
                    val edad = calcularEdad(fechaNacimiento)
                    if (edad != null) {
                        Text("Edad: $edad años", color = if (edad < 16) PJGreen else PJRed)
                    } else {
                        Text("Formato: AAAA-MM-DD")
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PJRedLight)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, null, tint = PJRed, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = error ?: "", color = PJRed, fontSize = 13.sp)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (validar()) {
                        onContinue(MinorData(nombre, apellidos, fechaNacimiento))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PJPurple),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Text("Revisar y confirmar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, null)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

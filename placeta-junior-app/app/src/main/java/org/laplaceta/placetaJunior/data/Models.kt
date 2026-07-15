package org.laplaceta.placetajunior.data

data class RegisterRequest(
    val nombre: String,
    val apellidos: String,
    val fecha_nacimiento: String,
    val nombre_tutor: String = "",
    val apellidos_tutor: String = "",
    val dni_tutor: String,
    val email: String = "",
    val fecha_nacimiento_tutor: String = "",
    val tutor_ya_existe: Boolean = false
)

data class RegisterResponse(
    val success: Boolean,
    val message: String?,
    val redirect: String?,
    val dip: String?,
    val necesita_firma_tutor: Boolean,
    val junior_id: Int?,
    val tutor_dip: String?,
    val placetaid_codigo: String?,
    val placetaid_requestId: String?,
    val tutor_nombre: String?,
    val tutor_email: String?
)

data class LoginRequest(
    val dip: String
)

data class LoginResponse(
    val success: Boolean,
    val redirect: String?,
    val requiere_autorizacion_tutor: Boolean,
    val requestId: String?,
    val codigo: String?,
    val mensaje: String?,
    val dip_menor: String?,
    val nombre_menor: String?,
    val junior: JuniorSession?,
    val error: String?
)

data class JuniorSession(
    val id: Int?,
    val solicitante_id: Int?,
    val dip: String?,
    val nombre: String?,
    val apellidos: String?,
    val alias: String?,
    val edad: Int?,
    val modalidad: String?,
    val placetas_saldo: Int?,
    val nivel_academia: Int?,
    val estado: String?
)

data class PollResponse(
    val success: Boolean,
    val aprobado: Boolean,
    val status: String?,
    val redirect: String?,
    val error: String?
)

data class MonederoResponse(
    val saldo_actual: Int,
    val ingresos_totales: Int,
    val gasto_hoy: Int,
    val gasto_semana: Int,
    val limites: ParentalLimits?,
    val saldo_disponible_hoy: Int?,
    val saldo_disponible_semana: Int?,
    val historial: List<Transaction>?,
    val nivel_academia: Int,
    val cuenta_bancaria: BankAccount?
)

data class ParentalLimits(
    val gasto_diario: Int,
    val gasto_semanal: Int,
    val limite_aprobacion_tutor: Int,
    val tiempo_uso: Int,
    val requiere_aprobacion: Boolean,
    val categorias_bloqueadas: List<String>
)

data class Transaction(
    val id: Int?,
    val junior_id: Int?,
    val tipo: String?,
    val concepto: String?,
    val cantidad: Int?,
    val saldo_resultante: Int?,
    val creado_en: String?
)

data class BankAccount(
    val id: String?,
    val tipo: String?,
    val iban: String?,
    val sendLimitPz: Int?,
    val titular: String = "",
    val cotitular: String = "",
    val tutorDip: String = "",
    val tutorNombre: String = ""
)

data class AcademyProgress(
    val nivel_actual: Int,
    val niveles: List<NivelInfo>
)

data class NivelInfo(
    val nivel: Int,
    val nombre: String,
    val completado: Boolean,
    val puntuacion: Int?,
    val desbloqueado: Boolean,
    val costo: Int
)

data class MinorInfo(
    val junior: JuniorData,
    val limites_parentales: ParentalLimits?
)

data class JuniorData(
    val id: Int?,
    val dip: String?,
    val nombre: String?,
    val apellidos: String?,
    val edad: Int?,
    val estado: String?,
    val placetas_saldo: Int?,
    val nivel_academia: Int?,
    val tutor_dip: String?,
    val tutor_nombre: String?
)

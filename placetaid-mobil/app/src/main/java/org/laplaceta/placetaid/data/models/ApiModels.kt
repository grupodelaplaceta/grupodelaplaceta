package org.laplaceta.placetaid.data.models

import com.google.gson.annotations.SerializedName

// ── API Request models ────────────────────────────────────────────────────────

data class RegisterDeviceRequest(
    @SerializedName("dip") val dip: String,
    @SerializedName("password") val password: String,
    @SerializedName("deviceToken") val deviceToken: String,
    @SerializedName("deviceName") val deviceName: String
)

data class CreateAuthRequest(
    @SerializedName("dip") val dip: String,
    @SerializedName("servicio") val servicio: String,
    @SerializedName("servicioUrl") val servicioUrl: String?,
    @SerializedName("plataforma") val plataforma: String
)

data class AuthorizeRequest(
    @SerializedName("requestId") val requestId: String,
    @SerializedName("dip") val dip: String,
    @SerializedName("authorized") val authorized: Boolean
)

// ── API Response models ───────────────────────────────────────────────────────

data class ApiResponse<T>(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("data") val data: T?,
    @SerializedName("error") val error: String?
)

data class RegisterDeviceResponse(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("mensaje") val mensaje: String?,
    @SerializedName("error") val error: String?
)

data class CreateAuthResponse(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("codigo") val codigo: String?,
    @SerializedName("requestId") val requestId: String?,
    @SerializedName("error") val error: String?
)

data class PendingRequestsResponse(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("requests") val requests: List<AuthRequestDto>?,
    @SerializedName("error") val error: String?
)

data class AuthRequestDto(
    @SerializedName("_id") val id: String,
    @SerializedName("dip") val dip: String,
    @SerializedName("codigo") val codigo: String,
    @SerializedName("servicio") val servicio: String,
    @SerializedName("servicioUrl") val servicioUrl: String?,
    @SerializedName("plataforma") val plataforma: String,
    @SerializedName("creadoEn") val creadoEn: String?,
    @SerializedName("estado") val estado: String
)

data class HistoryResponse(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("logs") val logs: List<LogDto>?,
    @SerializedName("error") val error: String?
)

data class LogDto(
    @SerializedName("_id") val id: String,
    @SerializedName("dip") val dip: String,
    @SerializedName("servicio") val servicio: String,
    @SerializedName("evento") val evento: String,
    @SerializedName("ip") val ip: String?,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("fase") val fase: String?
)

data class StatusResponse(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("activo") val activo: Boolean?,
    @SerializedName("bloqueado") val bloqueado: Boolean?,
    @SerializedName("registro") val registro: RegistroDto?,
    @SerializedName("error") val error: String?
)

data class RegistroDto(
    @SerializedName("dip") val dip: String,
    @SerializedName("placeid") val placeid: String?,
    @SerializedName("nombre") val nombre: String?,
    @SerializedName("apellidos") val apellidos: String?,
    @SerializedName("nombreCompleto") val nombreCompleto: String?,
    @SerializedName("rol") val rol: String?,
    @SerializedName("edad") val edad: Int?,
    @SerializedName("activo") val activo: Boolean?,
    @SerializedName("banned") val banned: Boolean?,
    @SerializedName("bloqueado") val bloqueado: Boolean?
)

// ── Votaciones ───────────────────────────────────────────────────────────────

data class VotacionResponse(
    @SerializedName("id") val id: String,
    @SerializedName("titulo") val titulo: String,
    @SerializedName("grupo") val grupo: String?,
    @SerializedName("quorum") val quorum: Int?,
    @SerializedName("aFavor") val aFavor: Int?,
    @SerializedName("enContra") val enContra: Int?,
    @SerializedName("abstenciones") val abstenciones: Int?,
    @SerializedName("estado") val estado: String?,
    @SerializedName("resultado") val resultado: String?
)

// ── Documentos ────────────────────────────────────────────────────────────────

data class DocumentoResponse(
    @SerializedName("id") val id: String,
    @SerializedName("titulo") val titulo: String,
    @SerializedName("tipo") val tipo: String?,
    @SerializedName("entidad") val entidad: String?,
    @SerializedName("csv") val csv: String?,
    @SerializedName("estado") val estado: String?,
    @SerializedName("destinatarios") val destinatarios: List<DestinatarioDto>?,
    @SerializedName("creadoEn") val creadoEn: String?
)

data class DestinatarioDto(
    @SerializedName("dip") val dip: String,
    @SerializedName("nombre") val nombre: String?,
    @SerializedName("firmado") val firmado: Boolean?,
    @SerializedName("rechazado") val rechazado: Boolean?,
    @SerializedName("fechaFirma") val fechaFirma: String?
)

data class FirmaResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("estado") val estado: String?,
    @SerializedName("firmado") val firmado: Boolean?,
    @SerializedName("error") val error: String?
)

// ── Notificaciones ────────────────────────────────────────────────────────────

data class NotificacionResponse(
    @SerializedName("tipo") val tipo: String?,
    @SerializedName("dip") val dip: String?,
    @SerializedName("titulo") val titulo: String?,
    @SerializedName("cuerpo") val cuerpo: String?,
    @SerializedName("votacionId") val votacionId: String?,
    @SerializedName("documentoId") val documentoId: String?,
    @SerializedName("leido") val leido: Boolean?,
    @SerializedName("creadoEn") val creadoEn: String?
)

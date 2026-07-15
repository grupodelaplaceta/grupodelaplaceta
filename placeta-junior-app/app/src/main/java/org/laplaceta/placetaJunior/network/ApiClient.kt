package org.laplaceta.placetajunior.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.laplaceta.placetajunior.BuildConfig
import org.laplaceta.placetajunior.data.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {

    private val BASE_URL get() = BuildConfig.CRM_BASE_URL
    private val PLACETAID_URL get() = BuildConfig.PLACETAID_URL
    private const val TIMEOUT = 15000

    private suspend fun httpPost(path: String, body: JSONObject): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL$path")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = TIMEOUT
            conn.readTimeout = TIMEOUT
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")

            val writer = OutputStreamWriter(conn.outputStream)
            writer.write(body.toString())
            writer.flush()
            writer.close()

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val reader = BufferedReader(InputStreamReader(stream))
            val response = reader.readText()
            reader.close()
            conn.disconnect()

            JSONObject(response)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun httpGet(path: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL$path")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = TIMEOUT
            conn.readTimeout = TIMEOUT
            conn.setRequestProperty("Accept", "application/json")

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val reader = BufferedReader(InputStreamReader(stream))
            val response = reader.readText()
            reader.close()
            conn.disconnect()

            JSONObject(response)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ── Registro ──────────────────────────────────────────────────

    suspend fun register(request: RegisterRequest): RegisterResponse {
        val body = JSONObject().apply {
            put("nombre", request.nombre)
            put("apellidos", request.apellidos)
            put("fecha_nacimiento", request.fecha_nacimiento)
            put("nombre_tutor", request.nombre_tutor)
            put("apellidos_tutor", request.apellidos_tutor)
            put("dni_tutor", request.dni_tutor)
            put("email", request.email)
            put("fecha_nacimiento_tutor", request.fecha_nacimiento_tutor)
            put("tutor_ya_existe", request.tutor_ya_existe)
        }

        val json = httpPost("/api/junior/register", body)
        return RegisterResponse(
            success = json?.optBoolean("success", false) ?: false,
            message = json?.optString("message"),
            redirect = json?.optString("redirect"),
            dip = json?.optString("dip"),
            necesita_firma_tutor = json?.optBoolean("necesita_firma_tutor", false) ?: false,
            junior_id = json?.optInt("junior_id"),
            tutor_dip = json?.optString("tutor_dip"),
            placetaid_codigo = json?.optString("placetaid_codigo"),
            placetaid_requestId = json?.optString("placetaid_requestId"),
            tutor_nombre = json?.optString("tutor_nombre"),
            tutor_email = json?.optString("tutor_email")
        )
    }

    // ── Login ─────────────────────────────────────────────────────

    suspend fun login(dip: String): LoginResponse {
        val body = JSONObject().apply {
            put("dip", dip)
        }
        val json = httpPost("/api/junior/login", body)
        if (json == null) {
            return LoginResponse(
                success = false, error = "Error de conexión con el servidor", redirect = null,
                requiere_autorizacion_tutor = false, requestId = null, codigo = null,
                mensaje = null, dip_menor = null, nombre_menor = null, junior = null
            )
        }

        val error = json.optString("error")
        if (error.isNotEmpty()) {
            return LoginResponse(
                success = false, error = error, redirect = null,
                requiere_autorizacion_tutor = false, requestId = null, codigo = null,
                mensaje = null, dip_menor = null, nombre_menor = null, junior = null
            )
        }

        val reqAuth = json.optBoolean("requiere_autorizacion_tutor", false)

        val junior = if (json.has("junior")) {
            val j = json.getJSONObject("junior")
            JuniorSession(
                id = j.optInt("id"), solicitante_id = j.optInt("solicitante_id"),
                dip = j.optString("dip"), nombre = j.optString("nombre"),
                apellidos = j.optString("apellidos"), alias = j.optString("alias"),
                edad = if (j.has("edad") && !j.isNull("edad")) j.optInt("edad") else null,
                modalidad = j.optString("modalidad"),
                placetas_saldo = if (j.has("placetas_saldo") && !j.isNull("placetas_saldo")) j.optInt("placetas_saldo") else null,
                nivel_academia = if (j.has("nivel_academia") && !j.isNull("nivel_academia")) j.optInt("nivel_academia") else null,
                estado = j.optString("estado")
            )
        } else null

        return LoginResponse(
            success = json.optBoolean("success", false),
            redirect = json.optString("redirect"),
            requiere_autorizacion_tutor = reqAuth,
            requestId = json.optString("requestId"),
            codigo = json.optString("codigo"),
            mensaje = json.optString("mensaje"),
            dip_menor = json.optString("dip_menor"),
            nombre_menor = json.optString("nombre_menor"),
            junior = junior,
            error = null
        )
    }

    // ── Poll autorización PlacetaID (directo a id.laplaceta.org) ──

    suspend fun pollAutorizacion(requestId: String): PollResponse = withContext(Dispatchers.IO) {
        val json = httpPlacetaidGetSync("/api/mobil/poll/$requestId")
        if (json == null) {
            return@withContext PollResponse(
                success = false, aprobado = false,
                status = "error", redirect = null, error = "Error de conexión con PlacetaID"
            )
        }
        val autorizado = json.optBoolean("autorizado", false) ||
                         json.optString("estado") == "authorized" ||
                         json.optBoolean("ok", false) && json.optString("estado") == "authorized"
        PollResponse(
            success = true,
            aprobado = autorizado,
            status = json.optString("estado", "pending"),
            redirect = null,
            error = null
        )
    }

    // ── PlacetaID direct HTTP helpers ─────────────────────────────

    private fun httpPlacetaidGetSync(path: String): JSONObject? {
        return try {
            val url = URL("$PLACETAID_URL$path")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = TIMEOUT
            conn.readTimeout = TIMEOUT
            conn.setRequestProperty("Accept", "application/json")
            val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
            val reader = java.io.BufferedReader(java.io.InputStreamReader(stream))
            val response = reader.readText()
            reader.close()
            conn.disconnect()
            JSONObject(response)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ── Monedero ──────────────────────────────────────────────────

    suspend fun getMonedero(dip: String): MonederoResponse? {
        val json = httpGet("/api/junior/monedero?dip=$dip")
        if (json == null) return null

        return MonederoResponse(
            saldo_actual = json.optInt("saldo_actual", 0),
            ingresos_totales = json.optInt("ingresos_totales", 0),
            gasto_hoy = json.optInt("gasto_hoy", 0),
            gasto_semana = json.optInt("gasto_semana", 0),
            limites = null,
            saldo_disponible_hoy = json.optInt("saldo_disponible_hoy"),
            saldo_disponible_semana = json.optInt("saldo_disponible_semana"),
            historial = null,
            nivel_academia = json.optInt("nivel_academia", 1),
            cuenta_bancaria = null
        )
    }

    // ── Perfil Junior ─────────────────────────────────────────────

    suspend fun getPerfil(dip: String): MinorInfo? {
        val json = httpGet("/api/junior/perfil?dip=$dip")
        if (json == null) return null

        val juniorObj = json.optJSONObject("junior")
        val junior = if (juniorObj != null) {
            JuniorData(
                id = juniorObj.optInt("id"), dip = juniorObj.optString("dip"),
                nombre = juniorObj.optString("nombre"), apellidos = juniorObj.optString("apellidos"),
                edad = if (juniorObj.has("edad") && !juniorObj.isNull("edad")) juniorObj.optInt("edad") else null,
                estado = juniorObj.optString("estado"),
                placetas_saldo = if (juniorObj.has("placetas_saldo") && !juniorObj.isNull("placetas_saldo")) juniorObj.optInt("placetas_saldo") else null,
                nivel_academia = if (juniorObj.has("nivel_academia") && !juniorObj.isNull("nivel_academia")) juniorObj.optInt("nivel_academia") else null,
                tutor_dip = juniorObj.optString("tutor_dip"), tutor_nombre = juniorObj.optString("tutor_nombre")
            )
        } else null

        return if (junior != null) MinorInfo(junior = junior, limites_parentales = null) else null
    }

    // ── Tutor Info — Obtener datos del tutor por DIP ─────────────

    suspend fun getTutorInfo(dip: String): JSONObject? {
        return httpGet("/api/junior/tutor-info/$dip")
    }

    // ── PlacetaID: crear solicitud autenticación ──────────────────

    suspend fun crearSolicitudPlacetaID(dip: String, servicio: String, url: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("dip", dip.replace("-", ""))
                put("servicio", servicio)
                put("servicioUrl", url)
                put("plataforma", "android")
            }
            val conn = URL("$PLACETAID_URL/api/mobil/request").openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.setRequestProperty("Content-Type", "application/json")
            val writer = java.io.OutputStreamWriter(conn.outputStream)
            writer.write(body.toString())
            writer.flush()
            writer.close()
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val reader = java.io.BufferedReader(java.io.InputStreamReader(stream))
            val response = reader.readText()
            reader.close()
            conn.disconnect()
            JSONObject(response)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ── PlacetaID: verificar estado solicitud ─────────────────────

    fun verificarPlacetaID(requestId: String): JSONObject? {
        return try {
            val url = URL("$PLACETAID_URL/api/mobil/poll/$requestId")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
            val reader = java.io.BufferedReader(java.io.InputStreamReader(stream))
            val response = reader.readText()
            reader.close()
            conn.disconnect()
            JSONObject(response)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

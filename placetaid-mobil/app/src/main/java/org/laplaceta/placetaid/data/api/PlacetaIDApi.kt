package org.laplaceta.placetaid.data.api

import org.laplaceta.placetaid.data.models.*
import retrofit2.Response
import retrofit2.http.*

interface PlacetaIDApi {

    // ── Device Registration ──────────────────────────────────────────────

    @POST("mobil/register")
    suspend fun registerDevice(
        @Body request: RegisterDeviceRequest
    ): Response<RegisterDeviceResponse>

    @POST("mobil/unregister")
    suspend fun unregisterDevice(
        @Body body: Map<String, String>
    ): Response<RegisterDeviceResponse>

    // ── Authentication Requests ──────────────────────────────────────────

    @POST("mobil/request")
    suspend fun createAuthRequest(
        @Body request: CreateAuthRequest
    ): Response<CreateAuthResponse>

    @GET("mobil/pending")
    suspend fun getPendingRequests(
        @Query("dip") dip: String
    ): Response<PendingRequestsResponse>

    @POST("mobil/authorize")
    suspend fun authorizeRequest(
        @Body request: AuthorizeRequest
    ): Response<RegisterDeviceResponse>

    // ── PlacetaID Status ─────────────────────────────────────────────────

    @GET("mobil/status/{dip}")
    suspend fun getStatus(
        @Path("dip") dip: String
    ): Response<StatusResponse>

    // ── Access History ───────────────────────────────────────────────────

    @GET("mobil/history/{dip}")
    suspend fun getHistory(
        @Path("dip") dip: String
    ): Response<HistoryResponse>

    // ── Votaciones ──────────────────────────────────────────────────────

    @GET("mobil/votaciones/{dip}")
    suspend fun getVotaciones(
        @Path("dip") dip: String
    ): Response<List<VotacionResponse>>

    // ── Documentos ───────────────────────────────────────────────────────

    @GET("mobil/documentos/{dip}")
    suspend fun getDocumentosPendientes(
        @Path("dip") dip: String
    ): Response<List<DocumentoResponse>>

    @POST("mobil/documentos/{id}/firmar")
    suspend fun firmarDocumento(
        @Path("id") id: String,
        @Body body: Map<String, String>
    ): Response<FirmaResponse>

    @POST("mobil/documentos/{id}/rechazar")
    suspend fun rechazarDocumento(
        @Path("id") id: String,
        @Body body: Map<String, String>
    ): Response<FirmaResponse>

    // ── Notificaciones ──────────────────────────────────────────────────

    @GET("mobil/notificaciones/{dip}")
    suspend fun getNotificaciones(
        @Path("dip") dip: String
    ): Response<List<NotificacionResponse>>

    @POST("mobil/notificaciones/leer")
    suspend fun marcarNotificacionLeida(
        @Body body: Map<String, String>
    ): Response<Map<String, Any>>

    // ── Session verification ─────────────────────────────────────────────

    @GET("auth/session")
    suspend fun verifySession(
        @Header("Authorization") token: String
    ): Response<StatusResponse>
}

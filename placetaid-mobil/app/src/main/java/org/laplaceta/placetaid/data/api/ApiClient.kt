package org.laplaceta.placetaid.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    // Servidor PlacetaID (autenticación, votaciones, documentos)
    private var baseUrl: String = "https://id.laplaceta.org/api/"
    // Servidor Admin-Placeta (junior, paneles)
    private var adminUrl: String = "https://admin-placeta.vercel.app/"
    // Servidor Backend-Banco
    private var bancoUrl: String = "https://api.banco.laplaceta.org/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    private var _retrofit: Retrofit = createRetrofit(baseUrl)
    private var _api: PlacetaIDApi = _retrofit.create(PlacetaIDApi::class.java)

    private fun createRetrofit(url: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(url.trimEnd('/') + "/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun getApi(): PlacetaIDApi {
        val currentBase = _retrofit.baseUrl().toString()
        if (currentBase != baseUrl.trimEnd('/') + "/") {
            _retrofit = createRetrofit(baseUrl)
            _api = _retrofit.create(PlacetaIDApi::class.java)
        }
        return _api
    }

    fun getAdminUrl(): String = adminUrl.trimEnd('/')
    fun getBancoUrl(): String = bancoUrl.trimEnd('/')

    fun setBaseUrl(url: String) {
        baseUrl = url.trimEnd('/') + "/"
        _retrofit = createRetrofit(baseUrl)
        _api = _retrofit.create(PlacetaIDApi::class.java)
    }

    fun setAdminUrl(url: String) { adminUrl = url.trimEnd('/') }
    fun setBancoUrl(url: String) { bancoUrl = url.trimEnd('/') }
}

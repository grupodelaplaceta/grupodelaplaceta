package org.laplaceta.placetajunior.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Persistencia local de sesión del menor.
 * Guarda DIP, nombre y saldo para evitar re-login cada vez.
 */
object SessionManager {

    private const val PREFS = "placeta_junior_session"
    private const val KEY_DIP = "dip"
    private const val KEY_NOMBRE = "nombre"
    private const val KEY_APELLIDOS = "apellidos"
    private const val KEY_SALDO = "saldo"
    private const val KEY_LOGGED_IN = "logged_in"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    fun saveSession(dip: String, nombre: String, apellidos: String, saldo: Int) {
        prefs?.edit()?.apply {
            putString(KEY_DIP, dip)
            putString(KEY_NOMBRE, nombre)
            putString(KEY_APELLIDOS, apellidos)
            putInt(KEY_SALDO, saldo)
            putBoolean(KEY_LOGGED_IN, true)
            apply()
        }
    }

    fun getDip(): String = prefs?.getString(KEY_DIP, "") ?: ""
    fun getNombre(): String = prefs?.getString(KEY_NOMBRE, "") ?: ""
    fun getApellidos(): String = prefs?.getString(KEY_APELLIDOS, "") ?: ""
    fun getSaldo(): Int = prefs?.getInt(KEY_SALDO, 0) ?: 0

    fun updateSaldo(nuevo: Int) {
        prefs?.edit()?.putInt(KEY_SALDO, nuevo)?.apply()
    }

    fun isLoggedIn(): Boolean = prefs?.getBoolean(KEY_LOGGED_IN, false) ?: false

    fun logout() {
        prefs?.edit()?.clear()?.apply()
    }

    data class SessionData(
        val dip: String,
        val nombre: String,
        val apellidos: String,
        val saldo: Int
    )

    fun getSession(): SessionData? {
        if (!isLoggedIn()) return null
        return SessionData(getDip(), getNombre(), getApellidos(), getSaldo())
    }
}

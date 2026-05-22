package com.jluqgon214.alphabot2.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Punto central de preferencias locales (ligero y moderno con DataStore).
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "alphabot_prefs")

/**
 * Gestor de preferencias para login.
 * - Guarda si el usuario activó "Recordarme"
 * - Guarda el último correo usado
 *
 * Nota: aquí NO se guarda la contraseña por seguridad.
 */
class DataStoreManager(private val context: Context) {
    companion object {
        private val KEY_REMEMBER_ME = booleanPreferencesKey("remember_me")
        private val KEY_REMEMBER_EMAIL = stringPreferencesKey("remember_email")
    }

    // Devuelve el correo recordado (o vacío si no hay nada guardado).
    val rememberedEmailFlow: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[KEY_REMEMBER_EMAIL] ?: "" }

    // Devuelve si "Recordarme" está activo.
    val rememberMeFlow: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[KEY_REMEMBER_ME] ?: false }

    // Guarda o limpia preferencias según la opción elegida en login.
    suspend fun saveRememberMe(email: String?, remember: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_REMEMBER_ME] = remember
            if (remember) {
                prefs[KEY_REMEMBER_EMAIL] = email ?: ""
            } else {
                prefs.remove(KEY_REMEMBER_EMAIL)
            }
        }
    }
}


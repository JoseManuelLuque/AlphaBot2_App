package com.jluqgon214.alphabot2.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel para gestionar la selección de idioma.
 * Por ahora es monolengua en estado local (solo UI);
 * más adelante se persistirá en DataStore.
 */
class LanguageViewModel : ViewModel() {

    // Valores posibles: "es" (español) o "en" (inglés)
    private val _selectedLanguage = MutableStateFlow("es")
    val selectedLanguage: StateFlow<String> = _selectedLanguage

    /**
     * Actualiza el idioma seleccionado en memoria.
     *
     * @param language Código de idioma (`es` o `en`).
     */
    fun setLanguage(language: String) {
        _selectedLanguage.value = language
        // TODO: Guardar en SharedPreferences o DataStore
    }
}


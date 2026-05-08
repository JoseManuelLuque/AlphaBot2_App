package com.jluqgon214.alphabot2.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel para gestionar la selección de idioma.
 * Por ahora es moneta (solo UI), se implementará el guardado en preferencias después.
 */
class LanguageViewModel : ViewModel() {

    // Valores posibles: "es" (español) o "en" (inglés)
    private val _selectedLanguage = MutableStateFlow("es")
    val selectedLanguage: StateFlow<String> = _selectedLanguage

    fun setLanguage(language: String) {
        _selectedLanguage.value = language
        // TODO: Guardar en SharedPreferences o DataStore
    }
}


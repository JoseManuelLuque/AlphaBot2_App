package com.jluqgon214.alphabot2

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Prueba instrumentada que se ejecuta en un dispositivo Android.
 *
 * Consulta la [documentación de testing](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Contexto de la app bajo prueba.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.jluqgon214.alphabot2", appContext.packageName)
    }
}
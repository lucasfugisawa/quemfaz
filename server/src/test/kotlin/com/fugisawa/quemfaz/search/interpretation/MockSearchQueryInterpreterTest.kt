package com.fugisawa.quemfaz.search.interpretation

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MockSearchQueryInterpreterTest {

    private val interpreter = MockSearchQueryInterpreter()

    @Test
    fun `should interpret cleaning service`() {
        val result = interpreter.interpret("Preciso de uma faxina amanhã", "Batatais")
        
        assertEquals("preciso de uma faxina amanhã", result.normalizedQuery)
        assertTrue(result.serviceIds.contains("clean-house"))
        assertEquals("Batatais", result.cityName)
    }

    @Test
    fun `should interpret electrician and city`() {
        val result = interpreter.interpret("Eletricista em Franca", "Batatais")
        
        assertTrue(result.serviceIds.contains("repair-electrician"))
        assertEquals("Franca", result.cityName)
    }

    @Test
    fun `should interpret land cleaning and neighborhood`() {
        val result = interpreter.interpret("Limpar lote no Jardim Bandeirantes", "Batatais")
        
        assertTrue(result.serviceIds.contains("clean-land"))
        assertTrue(result.neighborhoods.contains("Jardim Bandeirantes"))
    }

    @Test
    fun `should interpret bartender for events`() {
        val result = interpreter.interpret("Barman para festa", "Batatais")
        
        assertTrue(result.serviceIds.contains("event-bartender"))
    }
}

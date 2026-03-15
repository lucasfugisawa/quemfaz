package com.fugisawa.quemfaz.profile.interpretation

import com.fugisawa.quemfaz.contract.profile.InputMode
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MockProfessionalInputInterpreterTest {
    private val interpreter = MockProfessionalInputInterpreter()

    @Test
    fun `should interpret painting service and Batatais city`() {
        val input = "Faço pintura residencial em Batatais"
        val response = interpreter.interpret(input, InputMode.TEXT)

        assertTrue(response.interpretedServices.any { it.serviceId == "paint-residential" })
        assertEquals("Batatais", response.cityName)
        assertTrue(response.missingFields.isEmpty())
    }

    @Test
    fun `should interpret land cleaning service`() {
        val input = "Limpeza de terreno no Centro e Jardim Bandeirantes de Batatais"
        val response = interpreter.interpret(input, InputMode.TEXT)

        assertTrue(response.interpretedServices.any { it.serviceId == "clean-land" })
        assertEquals("Batatais", response.cityName)
        assertTrue(response.missingFields.isEmpty())
    }

    @Test
    fun `should detect missing services`() {
        val input = "Trabalho em Franca"
        val response = interpreter.interpret(input, InputMode.TEXT)

        assertTrue(response.interpretedServices.isEmpty())
        assertEquals("Franca", response.cityName)
        assertTrue(response.missingFields.contains("services"))
    }
}

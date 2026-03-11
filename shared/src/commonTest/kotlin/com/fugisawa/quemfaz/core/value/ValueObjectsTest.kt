package com.fugisawa.quemfaz.core.value

import com.fugisawa.quemfaz.core.validation.ValidationResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PersonNameTest {
    @Test
    fun `create valid name should succeed`() {
        val result = PersonName.create("John Doe")
        assertTrue(result is ValidationResult.Valid)
        assertEquals("John Doe", (result as ValidationResult.Valid).value.value)
    }

    @Test
    fun `create name with leading and trailing spaces should trim`() {
        val result = PersonName.create("  John Doe  ")
        assertTrue(result is ValidationResult.Valid)
        assertEquals("John Doe", (result as ValidationResult.Valid).value.value)
    }

    @Test
    fun `create empty name should fail`() {
        val result = PersonName.create("")
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `create name with only spaces should fail`() {
        val result = PersonName.create("   ")
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `create name shorter than minimum length should fail`() {
        val result = PersonName.create("A")
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `create name with numbers should fail`() {
        val result = PersonName.create("John123")
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `create name with hyphen should succeed`() {
        val result = PersonName.create("Mary-Jane")
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `create name with apostrophe should succeed`() {
        val result = PersonName.create("O'Brien")
        assertTrue(result is ValidationResult.Valid)
    }
}

class PhoneNumberTest {
    @Test
    fun `create valid Brazilian phone should succeed`() {
        val result = PhoneNumber.create("11987654321")
        assertTrue(result is ValidationResult.Valid)
        assertEquals("11987654321", (result as ValidationResult.Valid).value.value)
    }

    @Test
    fun `create phone with E164 format should succeed`() {
        val result = PhoneNumber.create("+5511987654321")
        assertTrue(result is ValidationResult.Valid)
        assertEquals("+5511987654321", (result as ValidationResult.Valid).value.value)
    }

    @Test
    fun `create phone with formatting characters should clean them`() {
        val result = PhoneNumber.create("(11) 98765-4321")
        assertTrue(result is ValidationResult.Valid)
        assertEquals("11987654321", (result as ValidationResult.Valid).value.value)
    }

    @Test
    fun `create empty phone should fail`() {
        val result = PhoneNumber.create("")
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `create phone with only spaces should fail`() {
        val result = PhoneNumber.create("   ")
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `create phone too short should fail`() {
        val result = PhoneNumber.create("123456789")
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `create phone too long should fail`() {
        val result = PhoneNumber.create("12345678901234567")
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `create phone with letters should fail`() {
        val result = PhoneNumber.create("11987abc321")
        assertTrue(result is ValidationResult.Invalid)
    }
}

class WhatsAppPhoneTest {
    @Test
    fun `create valid WhatsApp phone should succeed`() {
        val result = WhatsAppPhone.create("11987654321")
        assertTrue(result is ValidationResult.Valid)
        assertEquals("11987654321", (result as ValidationResult.Valid).value.value)
    }

    @Test
    fun `convert WhatsApp phone to regular phone should work`() {
        val whatsApp = WhatsAppPhone.unsafe("11987654321")
        val phone = whatsApp.toPhoneNumber()
        assertEquals("11987654321", phone.value)
    }
}

class PhotoUrlTest {
    @Test
    fun `create valid HTTP URL should succeed`() {
        val result = PhotoUrl.create("http://example.com/photo.jpg")
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `create valid HTTPS URL should succeed`() {
        val result = PhotoUrl.create("https://example.com/photo.jpg")
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `create empty URL should fail`() {
        val result = PhotoUrl.create("")
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `create non-HTTP URL should fail`() {
        val result = PhotoUrl.create("ftp://example.com/photo.jpg")
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `create malformed URL should fail`() {
        val result = PhotoUrl.create("not a url")
        assertTrue(result is ValidationResult.Invalid)
    }
}

class NeighborhoodNameTest {
    @Test
    fun `create valid neighborhood should succeed`() {
        val result = NeighborhoodName.create("Vila Mariana")
        assertTrue(result is ValidationResult.Valid)
        assertEquals("Vila Mariana", (result as ValidationResult.Valid).value.value)
    }

    @Test
    fun `create neighborhood with trimming should work`() {
        val result = NeighborhoodName.create("  Pinheiros  ")
        assertTrue(result is ValidationResult.Valid)
        assertEquals("Pinheiros", (result as ValidationResult.Valid).value.value)
    }

    @Test
    fun `create empty neighborhood should fail`() {
        val result = NeighborhoodName.create("")
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `create neighborhood too short should fail`() {
        val result = NeighborhoodName.create("A")
        assertTrue(result is ValidationResult.Invalid)
    }
}

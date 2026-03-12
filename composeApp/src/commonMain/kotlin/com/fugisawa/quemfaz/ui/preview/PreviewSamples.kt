package com.fugisawa.quemfaz.ui.preview

import com.fugisawa.quemfaz.contract.auth.UserProfileResponse
import com.fugisawa.quemfaz.contract.profile.CreateProfessionalProfileDraftResponse
import com.fugisawa.quemfaz.contract.profile.InterpretedServiceDto
import com.fugisawa.quemfaz.contract.profile.ProfessionalProfileResponse
import com.fugisawa.quemfaz.contract.search.SearchProfessionalsResponse

object PreviewSamples {

    val sampleServices = listOf(
        InterpretedServiceDto("plumber", "Plumber", "HIGH"),
        InterpretedServiceDto("electrician", "Electrician", "HIGH"),
        InterpretedServiceDto("painter", "Residential Painter", "MEDIUM"),
    )

    val sampleServicesLong = listOf(
        InterpretedServiceDto("plumber", "Plumber", "HIGH"),
        InterpretedServiceDto("electrician", "Electrician", "HIGH"),
        InterpretedServiceDto("painter", "Residential & Commercial Painter", "MEDIUM"),
        InterpretedServiceDto("tiler", "Floor & Wall Tiling Specialist", "MEDIUM"),
        InterpretedServiceDto("handyman", "General Handyman Services", "LOW"),
    )

    val sampleProfile = ProfessionalProfileResponse(
        id = "prof-1",
        name = "Carlos Silva",
        photoUrl = null,
        description = "Experienced plumber with 10+ years in residential and commercial projects. Available on weekends.",
        cityName = "São Paulo",
        neighborhoods = listOf("Vila Mariana", "Moema"),
        services = sampleServices,
        profileComplete = true,
        activeRecently = true,
        whatsAppPhone = "+55 11 99999-1234",
        contactPhone = "+55 11 99999-1234",
    )

    val sampleProfileLongText = sampleProfile.copy(
        id = "prof-long",
        name = "Maria Aparecida dos Santos Ferreira de Oliveira",
        description = "I am a highly experienced professional offering a wide range of home improvement services including plumbing, electrical work, painting, tiling, and general repairs. I have been working in this field for over 15 years and take pride in delivering quality work at competitive prices. I serve the entire metropolitan area and am available on weekends and holidays.",
        neighborhoods = listOf("Vila Mariana", "Moema", "Itaim Bibi", "Pinheiros", "Jardim Paulista"),
        services = sampleServicesLong,
    )

    val sampleProfileMinimal = ProfessionalProfileResponse(
        id = "prof-min",
        name = null,
        photoUrl = null,
        description = "",
        cityName = "Batatais",
        neighborhoods = emptyList(),
        services = emptyList(),
        profileComplete = false,
        activeRecently = false,
        whatsAppPhone = null,
        contactPhone = "+55 16 99999-0000",
    )

    val sampleProfile2 = ProfessionalProfileResponse(
        id = "prof-2",
        name = "Ana Beatriz",
        photoUrl = null,
        description = "Professional house cleaner. Reliable and punctual.",
        cityName = "Ribeirão Preto",
        neighborhoods = listOf("Centro", "Jardim Sumaré"),
        services = listOf(InterpretedServiceDto("cleaner", "House Cleaning", "HIGH")),
        profileComplete = true,
        activeRecently = false,
        whatsAppPhone = "+55 16 98888-5678",
        contactPhone = "+55 16 98888-5678",
    )

    val sampleSearchResponse = SearchProfessionalsResponse(
        normalizedQuery = "plumber",
        interpretedServices = sampleServices.take(1),
        results = listOf(sampleProfile, sampleProfile2, sampleProfileMinimal),
    )

    val sampleSearchResponseEmpty = SearchProfessionalsResponse(
        normalizedQuery = "underwater basket weaving",
        interpretedServices = emptyList(),
        results = emptyList(),
    )

    val sampleDraftResponse = CreateProfessionalProfileDraftResponse(
        normalizedDescription = "Experienced residential painter with 10 years of experience. Also does small wall repairs.",
        interpretedServices = sampleServices,
        cityName = "Batatais",
        neighborhoods = listOf("Centro", "Vila Nova"),
        missingFields = emptyList(),
        followUpQuestions = emptyList(),
        freeTextAliases = listOf("pintor", "painter"),
    )

    val sampleUser = UserProfileResponse(
        id = "user-1",
        phoneNumber = "+55 11 99999-1234",
        name = "Lucas Fugisawa",
        photoUrl = null,
        cityName = "São Paulo",
        status = "ACTIVE",
    )

    val sampleUserMinimal = UserProfileResponse(
        id = "user-2",
        phoneNumber = "+55 16 99999-0000",
        name = null,
        photoUrl = null,
        cityName = null,
        status = "ACTIVE",
    )

    val sampleCities = listOf(
        "São Paulo", "Ribeirão Preto", "Batatais", "Campinas",
        "Franca", "Barretos", "Araraquara", "São Carlos",
    )
}

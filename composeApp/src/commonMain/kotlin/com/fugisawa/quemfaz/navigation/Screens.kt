package com.fugisawa.quemfaz.navigation

sealed class Screen(val route: String) {
    object AuthPhone : Screen("auth/phone")
    object AuthOtp : Screen("auth/otp/{phone}") {
        fun createRoute(phone: String) = "auth/otp/$phone"
    }
    object ProfileCompletion : Screen("auth/profile-completion")
    
    object Home : Screen("home")
    object CitySelection : Screen("city-selection")
    object SearchResults : Screen("search-results?query={query}") {
        fun createRoute(query: String) = "search-results?query=$query"
    }
    object ProfessionalProfile : Screen("profile/{id}") {
        fun createRoute(id: String) = "profile/$id"
    }
    
    object Favorites : Screen("favorites")
    
    object OnboardingStart : Screen("onboarding/start")
    object OnboardingDraft : Screen("onboarding/draft")
    object MyProfile : Screen("profile/my")
    object EditProfessionalProfile : Screen("profile/professional/edit")
    
    object CategoryBrowsing : Screen("categories")
    object Blocked : Screen("blocked")
}

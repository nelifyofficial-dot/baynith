package com.example.util

import com.example.data.model.Movie

data class Country(
    val code: String,
    val name: String,
    val flagEmoji: String,
    val isEastAfrica: Boolean = false
)

object RegionService {

    const val DEFAULT_COUNTRY_CODE = "TZ"

    val EAST_AFRICA_CODES = setOf(
        "TZ", // Tanzania
        "KE", // Kenya
        "UG", // Uganda
        "RW", // Rwanda
        "BI", // Burundi
        "SS"  // South Sudan
    )

    private val COUNTRIES_LIST = listOf(
        // East Africa Group (First-class support)
        Country("TZ", "Tanzania", "🇹🇿", isEastAfrica = true),
        Country("KE", "Kenya", "🇰🇪", isEastAfrica = true),
        Country("UG", "Uganda", "🇺🇬", isEastAfrica = true),
        Country("RW", "Rwanda", "🇷🇼", isEastAfrica = true),
        Country("BI", "Burundi", "🇧🇮", isEastAfrica = true),
        Country("SS", "South Sudan", "🇸🇸", isEastAfrica = true),

        // Rest of Africa
        Country("CD", "DR Congo", "🇨🇩"),
        Country("NG", "Nigeria", "🇳🇬"),
        Country("ZA", "South Africa", "🇿🇦"),
        Country("GH", "Ghana", "🇬🇭"),
        Country("ET", "Ethiopia", "🇪🇹"),
        Country("ZM", "Zambia", "🇿🇲"),
        Country("ZW", "Zimbabwe", "🇿🇼"),
        Country("MW", "Malawi", "🇲🇼"),
        Country("MZ", "Mozambique", "🇲🇿"),
        Country("EG", "Egypt", "🇪🇬"),
        Country("MA", "Morocco", "🇲🇦"),

        // Americas
        Country("US", "United States", "🇺🇸"),
        Country("CA", "Canada", "🇨🇦"),
        Country("BR", "Brazil", "🇧🇷"),
        Country("MX", "Mexico", "🇲🇽"),

        // Europe
        Country("GB", "United Kingdom", "🇬🇧"),
        Country("DE", "Germany", "🇩🇪"),
        Country("FR", "France", "🇫🇷"),
        Country("IT", "Italy", "🇮🇹"),
        Country("ES", "Spain", "🇪🇸"),
        Country("NL", "Netherlands", "🇳🇱"),
        Country("SE", "Sweden", "🇸🇪"),
        Country("NO", "Norway", "🇳🇴"),

        // Asia & Middle East & Oceania
        Country("AE", "United Arab Emirates", "🇦🇪"),
        Country("SA", "Saudi Arabia", "🇸🇦"),
        Country("IN", "India", "🇮🇳"),
        Country("CN", "China", "🇨🇳"),
        Country("JP", "Japan", "🇯🇵"),
        Country("AU", "Australia", "🇦🇺"),
        Country("NZ", "New Zealand", "🇳🇿")
    )

    fun isEastAfrica(countryCode: String?): Boolean {
        if (countryCode.isNullOrBlank()) return false
        return EAST_AFRICA_CODES.contains(countryCode.trim().uppercase())
    }

    fun getAllCountries(): List<Country> = COUNTRIES_LIST

    fun getCountry(code: String?): Country {
        if (code.isNullOrBlank()) {
            return COUNTRIES_LIST.first { it.code == DEFAULT_COUNTRY_CODE }
        }
        val upper = code.trim().uppercase()
        return COUNTRIES_LIST.firstOrNull { it.code == upper }
            ?: Country(upper, upper, "🌐", isEastAfrica(upper))
    }

    fun getCountryName(countryCode: String?): String {
        return getCountry(countryCode).name
    }

    fun searchCountries(query: String): List<Country> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return COUNTRIES_LIST
        return COUNTRIES_LIST.filter {
            it.name.contains(trimmed, ignoreCase = true) ||
                    it.code.contains(trimmed, ignoreCase = true)
        }
    }

    /**
     * Checks whether content is available in the user's country.
     * Respects backward compatibility for older documents.
     */
    fun isContentAvailable(movie: Movie, userCountryCode: String?): Boolean {
        val userCode = userCountryCode?.trim()?.uppercase() ?: DEFAULT_COUNTRY_CODE

        // 1. Explicit availability setting
        val availability = movie.regionAvailability?.trim()?.uppercase()
        when (availability) {
            "WORLDWIDE" -> return true
            "EAST_AFRICA" -> return isEastAfrica(userCode)
            "SELECTED_COUNTRIES" -> {
                val allowed = movie.availableCountries?.map { it.trim().uppercase() }
                return if (!allowed.isNullOrEmpty()) allowed.contains(userCode) else false
            }
        }

        // 2. Available countries list check
        val allowedList = movie.availableCountries?.map { it.trim().uppercase() }
        if (!allowedList.isNullOrEmpty()) {
            return allowedList.contains(userCode)
        }

        // 3. Backward-compatibility: if Swahili content is flagged with East Africa restrictions or default behavior
        // Unrestricted content is available worldwide
        return true
    }

    /**
     * Helper to get user-facing explanation if version is restricted.
     */
    fun getRestrictionExplanation(movie: Movie, userCountryCode: String?): String {
        val availability = movie.regionAvailability?.trim()?.uppercase()
        val countryName = getCountryName(userCountryCode)
        return when (availability) {
            "EAST_AFRICA" -> "This Swahili version is currently available exclusively in East Africa (Tanzania, Kenya, Uganda, Rwanda, Burundi, South Sudan)."
            "SELECTED_COUNTRIES" -> "This title is not currently licensed for streaming in $countryName."
            else -> "This version is not available in your region."
        }
    }
}

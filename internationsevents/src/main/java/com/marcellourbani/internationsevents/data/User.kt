package com.marcellourbani.internationsevents.data

data class User (
    val countryOfOriginCode: Country,
    val countryOfResidencyCode: Country,
    val isPremium: Boolean,
    val id: Long,
    val imagePath: String,
    val localcommunityName: String,
    val roles: List<String>,
    val role: String,
    val origin: Origin,
    val residency: Origin,
    val gender: String,
    val firstName: String,
    val lastName: String,
    val membership: Long,
    val isActive: Boolean,
    val localcommunityId: Long,
    val workplacePosition: String,
    val workplaceCompany: String,
    val motto: String? = null,
    val languages: List<String>,
    val interests: List<String>,
    val registeredOnInLocalTimezone: String?=null,
    val registeredOn: String,
    val expatType: String,
    val _links: UserLinks,
    val _embedded: Embedded
)

data class Country (
    val name: String,
    val iocCode: String
)

data class Embedded (
    val permissions: Permissions,
    val localcommunity: Localcommunity,
    val currentLocalcommunity: Localcommunity,
    val privacySettings: PrivacySettings
)

data class Localcommunity (
    val coordinates: Coordinates,
    val id: Long,
    val name: String,
    val shortName: String,
    val urlName: String,
    val countryCode: String,
    val countryFlagPath: String,
    val countryName: String,
    val countryUrlName: String,
    val timeZone: String,
    val lockdownState: String,
    val statistics: Statistics,
    val _links: InLinkWrapper
)

data class Coordinates (
    val latitude: Double,
    val longitude: Double
)

data class InLinkWrapper (
    val self: InLink
)

data class InLink (
    val href: String
)

data class Statistics (
    val ambassadors: Long
)

data class Permissions (
    val permissions: List<Permission>,
    val subjectId: String
)

data class Permission (
    val name: String,
    val status: String,
    val reason: String
)

data class PrivacySettings (
    val displayAds: Boolean
)

data class UserLinks (
    val self: InLink,
    val localcommunity: InLink,
    val currentLocalcommunity: InLink
)

data class Origin (
    val placeId: Any? = null,
    val city: String?=null,
    val country: Country,
    val coordinates: Coordinates
)

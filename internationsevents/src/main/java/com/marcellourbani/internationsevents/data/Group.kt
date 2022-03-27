package com.marcellourbani.internationsevents.data

data class GroupResponse (
    val total: Long,
    val limit: Long,
    val offset: Long,
    val _links: InLinkWrapper,
    val _embedded: GroupRequestEmbedded
)

data class GroupRequestEmbedded (
    val self: List<Group>
)

data class Group (
    val activityGroupId: Long,
    val name: String,
    val description: String,
    val subtitle: String,
    val category: String,
    val imageUrl: String,
    val memberCount: Long,
    val nationalityCount: Long,
    val galleryId: Long,
    val consulIds: List<Long>,
    val categoryId: Long,
    val externalUrl: String,
    val isMember: Boolean,
    val isGuestMember: Any? = null,
    val isVisibleExternally: Boolean,
    val spareConsulInvitationsCount: Any? = null,
    val _links: InLinkWrapper,
    val _embedded: GroupEmbedded
)

data class GroupEmbedded (
    val permissions: Permissions,
    val consuls: List<Consul>
)

data class Consul (
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
    val gender: Gender,
    val firstName: String,
    val lastName: String,
    val membership: Long,
    val isActive: Boolean,
    val localcommunityId: Long,
    val workplacePosition: String? = null,
    val workplaceCompany: String? = null,
    val motto: String? = null,
    val languages: List<String>,
    val interests: List<String>,
    val registeredOn: String,
    val expatType: String,
    val _links: ConsulLinks,
    val _embedded: ConsulEmbedded
)

data class ConsulEmbedded (
    val permissions: Permissions
)

data class ConsulLinks (
    val self: InLink,
    val localcommunity: InLink
)







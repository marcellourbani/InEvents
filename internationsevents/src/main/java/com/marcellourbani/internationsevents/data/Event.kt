package com.marcellourbani.internationsevents.data

data class EventResponse (
    val total: Long,
    val limit: Long,
    val offset: Long,
    val _links: InLinkWrapper,
    val _embedded: EventResponseEmbedded
)
data class EventResponseEmbedded (
    val self: List<Event>
)

data class Event (
    val guestlistId: Long,
    val id: Long,
    val type: Type,
    val title: String,
    val surtitle: String,
    val description: String,
    val stickerTitle: String,
    val imageUrl: String,
    val startDateTime: String,
    val startsOn: String,
    val startsOnUtc: String,
    val endsOn: String,
    val endsOnUtc: String,
    val attendeeCount: Long,
    val nationalityCount: Long,
    val isNewcomerOnly: Boolean,
    val activityGroupId: Long? = null,
    val activityGroupCategoryId: Long? = null,
    val videoConferenceType: String?=null,
    val isPublished: Boolean,
    val externalUrl: String,
    val invitationType: String? = null,
    val userGalleryId: Long,
    val promotionGalleryId: Long,
    val activityContactPhone: Any? = null,
    val attendingContactsIds: List<String>,
    val totalAttendingContacts: Long,
    val meetingPoint: String? = null,
    val format: String,
    val isActivityHostedByConsul: Boolean? = null,
    val ticketShopIframeCode: Any? = null,
    val localcommunityId: Long,
    val _links: EventLinks,
    val _embedded: EventEmbedded
)

data class EventEmbedded (
    val permissions: Permissions,
    val guestlist: Guestlist,
    val venue: Venue,
    val hosts: Hosts,
    val latestInvitationSender: AttendingContactElement? = null,
    val userGallery: Gallery,
    val promotionGallery: Gallery,
    val localcommunity: Localcommunity,
    val attendingContact: AttendingContactElement? = null,
    val rsvp: Rsvp? = null,
)

data class Rsvp (
    val guestlistId: Long,
    val inviteeId: Long,
    val id: Long,
    val status: String,
    val isNewcomer: Boolean,
    val _links: InLinkWrapper,
    val _embedded: AttendingContactEmbedded
)
data class AttendingContactElement (
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
    val expatType: String?=null,
    val _links: AttendingContactLinks,
    val _embedded: AttendingContactEmbedded
)


data class AttendingContactEmbedded (
    val permissions: Permissions
)

enum class Gender {
    f,
    m
}

data class AttendingContactLinks (
    val self: InLink,
    val localcommunity: InLink
)


data class Guestlist (
    val id: Long,
    val attendeeCount: Long,
    val guestlistStatistics: GuestlistStatistics,
    val guestlistRestrictions: GuestlistRestrictions,
    val isOpen: Boolean,
    val pricing: Pricing? = null,
    val activityPricing: List<ActivityPricing>? = null,
    val closedOn: Any? = null,
    val ticketingEd25519PublicKeyId: Long? = null,
    val _links: GuestlistLinks,
    val _embedded: GuestlistEmbedded
)

data class ActivityPricing (
    val name: String,
    val price: BasicMemberFee
)

data class BasicMemberFee (
    val text: String,
    val amount: String,
    val currencyName: String,
    val currencySymbol: String
)
data class GuestlistEmbedded (
    val permissions: Permissions,
    val ticketingEd25519PublicKey: TicketingEd25519PublicKey? = null
)

data class TicketingEd25519PublicKey (
    val id: Long,
    val publicKey: String,
    val createdOn: String,
    val _links: InLinkWrapper
)


data class GuestlistRestrictions (
    val attendanceLimit: Long? = null,
    val waitingListEnabled: Boolean,
    val newcomerOnly: Boolean,
    val expatsOnly: Boolean,
    val albatrossOnly: Boolean,
    val womenOnly: Boolean
)

data class GuestlistStatistics (
    val albatrossAttendeesCount: Long,
    val premiumAttendeesWithAffirmativeRsvpCount: Long,
    val basicAttendeesCount: Long,
    val basicAttendeesWithAffirmativeRsvpCount: Long,
    val otherAttendeesCount: Long,
    val attendeesWithoutAffirmativeRsvpCount: Long,
    val totalAttendeesCount: Long
)

data class GuestlistLinks (
    val self: InLink,
    val acceptedRsvps: InLink,
    val ticketingEd25519PublicKey: InLink? = null
)

data class Pricing (
    val benefits: String,
    val premiumMemberFee: BasicMemberFee,
    val basicMemberFee: BasicMemberFee,
    val nonMemberFee: BasicMemberFee
)

data class Hosts (
    val total: Long,
    val limit: Any? = null,
    val offset: Any? = null,
    val _embedded: HostsEmbedded
)

data class HostsEmbedded (
    val self: List<AttendingContactElement>
)

data class Gallery (
    val id: Long,
    val entityType: Type,
    val entityId: Long,
    val galleryType: GalleryType,
    val title: String,
    val photoCount: Long,
    val _links: PromotionGalleryLinks,
    val _embedded: AttendingContactEmbedded
)

enum class Type {
    activity,
    event
}

enum class GalleryType {
    promotion,
    user
}

data class PromotionGalleryLinks (
    val self: InLink,
    val photos: InLink,
    val privileges: InLink
)

data class Venue (
    val localcommunityId: Any? = null,
    val id: Any? = null,
    val name: String,
    val address: String? = null,
    val website: String? = null,
    val city: String?=null,
    val coordinates: Coordinates
)

data class EventLinks (
    val self: InLink,
    val guestlist: InLink,
    val latestInvitationSender: InLink? = null,
    val userGallery: InLink,
    val promotionGallery: InLink,
    val localcommunity: InLink,
    val attendingContact: InLink? = null
)


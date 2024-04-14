package tech.checkstory.payment

import kotlinx.serialization.Serializable

@Serializable
data class StoreGooglePlayTokenPayload(val token: String)

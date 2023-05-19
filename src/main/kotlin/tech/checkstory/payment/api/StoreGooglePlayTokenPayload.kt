package tech.checkstory.payment.api

import kotlinx.serialization.Serializable

@Serializable
data class StoreGooglePlayTokenPayload(val token: String)
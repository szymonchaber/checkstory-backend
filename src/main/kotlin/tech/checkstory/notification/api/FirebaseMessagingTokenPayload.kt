package tech.checkstory.notification.api

import kotlinx.serialization.Serializable

@Serializable
data class FirebaseMessagingTokenPayload(val token: String)

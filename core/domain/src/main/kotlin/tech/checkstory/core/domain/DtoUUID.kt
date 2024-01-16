package tech.checkstory.core.domain

import kotlinx.serialization.Serializable
import java.util.*

typealias DtoUUID = @Serializable(UUIDSerializer::class) UUID

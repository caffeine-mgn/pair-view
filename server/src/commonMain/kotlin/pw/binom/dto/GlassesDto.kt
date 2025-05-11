package pw.binom.dto

import kotlinx.serialization.Serializable

@Serializable
data class GlassesDto(
    val id: String,
    val name: String,
)
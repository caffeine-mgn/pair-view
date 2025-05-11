package pw.binom.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateViewDto(
    val padding: Int,
    val align: Int,
)
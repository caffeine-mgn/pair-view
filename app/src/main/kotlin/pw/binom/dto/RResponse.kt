package pw.binom.dto

import kotlinx.serialization.Serializable

@Serializable
sealed interface RResponse {
    @Serializable
    data object OK : RResponse
}
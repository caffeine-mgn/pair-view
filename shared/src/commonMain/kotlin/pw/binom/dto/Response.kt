package pw.binom.dto

import kotlinx.serialization.Serializable

@Serializable
data class Response<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null,
) {
    companion object {
        fun success() = Response<Unit>(success = true, data = null, error = null)
        fun <T> success(value: T) = Response(success = true, data = value, error = null)
        fun <T> fail(message: String) = Response<T>(success = false, data = null, error = message)
    }
}
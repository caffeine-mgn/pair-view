package pw.binom

import kotlinx.serialization.KSerializer

data class Method<REQUEST, RESPONSE>(
    val request: KSerializer<REQUEST>,
    val response: KSerializer<RESPONSE>,
) {
    val name
        get() = request.descriptor.serialName
}
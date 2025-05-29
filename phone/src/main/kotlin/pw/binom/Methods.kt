package pw.binom

import kotlinx.serialization.Serializable

object Methods {
    val getServiceStatus = Method(
        request = GetServiceState.serializer(),
        response = ServiceState.serializer(),
    )

    val sendServiceState = Method(
        request = ServiceState.serializer(),
        response = OK.serializer(),
    )

    @Serializable
    object GetServiceState

    @Serializable
    object OK

    @Serializable
    object ServiceStateChanged

    @Serializable
    data class ServiceState(val running: Boolean, val connected: Boolean)
}
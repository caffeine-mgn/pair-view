package pw.binom.glasses

import pw.binom.Method

object Methods {
    val openFile = Method(
        request = RRequest.OpenVideoFile.serializer(),
        response = RResponse.OK.serializer(),
    )
    val play = Method(
        request = RRequest.Play.serializer(),
        response = RResponse.OK.serializer(),
    )

    val pause = Method(
        request = RRequest.Pause.serializer(),
        response = RResponse.OK.serializer(),
    )

    val seek = Method(
        request = RRequest.Seek.serializer(),
        response = RResponse.OK.serializer(),
    )

    val seekDelta = Method(
        request = RRequest.SeekDelta.serializer(),
        response = RResponse.OK.serializer(),
    )

    val getState = Method(
        request = RRequest.GetState.serializer(),
        response = RResponse.State.serializer(),
    )
}
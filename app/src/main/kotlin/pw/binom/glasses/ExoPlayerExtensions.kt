package pw.binom.glasses

import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer

fun SimpleExoPlayer.onError(func: (PlaybackException) -> Unit) {
    addListener(
        object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                func(error)
            }
        }
    )
}
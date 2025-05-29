package pw.binom.glasses

import com.rayneo.arsdk.android.ui.activity.BaseEventActivity
import pw.binom.logger.Logger

abstract class AbstractVideoActivity: BaseEventActivity() {
    protected val logger by Logger.ofThisOrGlobal
}
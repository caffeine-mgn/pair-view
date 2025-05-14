package pw.binom

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import pw.binom.strong.BeanLifeCycle
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

fun BeanLifeCycle.process(
    context: CoroutineContext? = null,
    func: suspend CoroutineScope.() -> Unit,
) {
    var job: Job? = null
    afterInit {
        job = GlobalScope.launch(context ?: coroutineContext) {
            func()
        }
    }
    preDestroy {
        job?.cancelAndJoin()
    }
}

package pw.binom

import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalAtomicApi::class)
class ContextVariable<SELF, T>(private val constructor: (SELF) -> T) : ReadOnlyProperty<Any?, T> {
    private var value: T? = null
    private var destructor: ((T) -> Unit)? = null
    private val created = AtomicBoolean(false)
    internal fun create(context: SELF) {
        if (!created.compareAndSet(false, true)) {
            throw IllegalStateException("Variable already initialized")
        }
        try {
            value = constructor(context)
        } catch (e: Throwable) {
            created.store(false)
            throw e
        }
    }

    internal fun destroy() {
        if (!created.compareAndSet(true, false)) {
            return
        }
        val destructor = destructor ?: return
        val value = value as T
        destructor.invoke(value)
    }

    override fun getValue(
        thisRef: Any?,
        property: KProperty<*>,
    ): T {
        if (!created.load()) {
            throw IllegalStateException("Variable not initialized")
        }
        return value as T
    }

    fun destroyWith(destructor: (T) -> Unit): ContextVariable<SELF, T> {
        this.destructor = destructor
        return this
    }
}
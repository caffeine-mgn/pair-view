package pw.binom

//@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "CANNOT_OVERRIDE_INVISIBLE_MEMBER")
class ContextVariableHolder<SELF>(val self: SELF) {

    private val variables = ArrayList<ContextVariable<SELF, Any?>>()

    fun create() {
        variables.forEach {
            it.create(self)
        }
    }

    fun destroy() {
        variables.forEach {
            it.destroy()
        }
    }

    fun <T> define(constructor: (SELF) -> T): ContextVariable<SELF, T> {
        val variable = ContextVariable<SELF, T>(constructor = constructor)
        variables += variable as ContextVariable<SELF, Any?>
        return variable
    }
}
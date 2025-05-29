package pw.binom

import android.app.Service

abstract class BService : Service() {

    private val contextVariableHolder = ContextVariableHolder(this)

    override fun onCreate() {
        super.onCreate()
        contextVariableHolder.create()
    }

    override fun onDestroy() {
        contextVariableHolder.destroy()
        super.onDestroy()
    }

    protected fun <T> define(constructor: (BService) -> T) =
        contextVariableHolder.define(constructor)
}
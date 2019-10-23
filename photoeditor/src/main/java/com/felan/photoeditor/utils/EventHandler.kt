package com.felan.photoeditor.utils

open class EventHandler<TEventArgs> {
    private val handlers: ArrayList<(TEventArgs) -> Unit> = ArrayList()

    operator fun plusAssign(handler: EventHandler<TEventArgs>) =
        addHandler { e -> handler(e) }


    operator fun plusAssign(handler: (TEventArgs) -> Unit) =
        addHandler(handler)


    protected open fun addHandler(handler: (TEventArgs) -> Unit) {
        handlers.add(handler)
    }

    open operator fun invoke(e: TEventArgs) {
        handlers.forEach { it.invoke(e) }
    }

    fun clearHandlers() = handlers.clear()
}

class ReplayEventHandler<TEventArgs> : EventHandler<TEventArgs>() {
    private var lastArgs: TEventArgs? = null

    override operator fun invoke(e: TEventArgs) {
        super.invoke(e)
        lastArgs = e
    }

    override fun addHandler(handler: (TEventArgs) -> Unit) {
        super.addHandler(handler)
        lastArgs?.let { handler(it) }
    }
}
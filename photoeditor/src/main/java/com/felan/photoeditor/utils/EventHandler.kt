package com.felan.photoeditor.utils

class EventHandler<TEventArgs> {
    private val handlers: ArrayList<(TEventArgs) -> Unit> = ArrayList()

    operator fun plusAssign(handler: EventHandler<TEventArgs>) {
        handlers.add { e -> handler(e) }
    }

    operator fun plusAssign(handler: (TEventArgs) -> Unit) {
        handlers.add(handler)
    }

    operator fun invoke(e: TEventArgs) {
        handlers.forEach { it.invoke(e) }
    }

    fun clearHandlers() = handlers.clear()
}
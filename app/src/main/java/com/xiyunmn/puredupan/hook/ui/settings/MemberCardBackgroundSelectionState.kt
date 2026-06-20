package com.xiyunmn.puredupan.hook.ui.settings

internal object MemberCardBackgroundSelectionState {
    @Volatile private var listener: ((String) -> Unit)? = null

    fun setListener(newListener: (String) -> Unit) {
        listener = newListener
    }

    fun clearIfSame(expectedListener: (String) -> Unit) {
        if (listener === expectedListener) {
            listener = null
        }
    }

    fun notifySelected(uriString: String) {
        listener?.invoke(uriString)
    }
}

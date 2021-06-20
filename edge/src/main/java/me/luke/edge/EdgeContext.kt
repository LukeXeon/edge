package me.luke.edge

import android.content.Context
import android.content.ContextWrapper
import android.os.Parcelable


class EdgeContext(base: Context) : ContextWrapper(base.applicationContext) {

    val permission: String
        get() = "${packageName}.USE_EDGE_LIVE_DATA"

    @JvmOverloads
    fun <T : Parcelable?> createEdgeLiveData(
        dataId: Int,
        exported: Boolean = false
    ): EdgeLiveData<T> {
        return EdgeLiveData(this, dataId, exported)
    }
}
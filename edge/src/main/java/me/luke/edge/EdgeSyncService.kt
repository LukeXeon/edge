package me.luke.edge

import android.app.Service
import android.content.Intent
import android.os.*
import android.util.Log
import android.util.SparseArray
import androidx.annotation.RestrictTo
import java.util.*


@RestrictTo(RestrictTo.Scope.LIBRARY)
internal open class EdgeSyncService : Service() {
    private val callbacks = SparseArray<RemoteCallbackList<IEdgeLiveDataCallback>>()

    override fun onBind(intent: Intent): IBinder {
        return object : IEdgeSyncService.Stub() {

            override fun setLiveDataCallback(
                dataId: Int,
                value: ModifiedData,
                callback: IEdgeLiveDataCallback
            ) {
                val callbackList = synchronized(callbacks) {
                    var list = callbacks.get(dataId)
                    if (list == null) {
                        list = RemoteCallbackList()
                        callbacks.put(dataId, list)
                    }
                    return@synchronized list
                }
                notifyDataChanged(callbackList, value)
                callbackList.register(callback)
            }

            private fun notifyDataChanged(
                callbackList: RemoteCallbackList<IEdgeLiveDataCallback>,
                value: ModifiedData
            ) {
                synchronized(callbackList) {
                    val count = callbackList.beginBroadcast()
                    for (i in 0 until count) {
                        val callback = callbackList.getBroadcastItem(i)
                        try {
                            callback.onReceive(value)
                        } catch (e: RemoteException) {
                            Log.w(TAG, e)
                        }
                    }
                    callbackList.finishBroadcast()
                }
            }

            override fun notifyDataChanged(
                dataId: Int,
                value: ModifiedData
            ) {
                val callbackList = synchronized(callbacks) { callbacks[dataId] } ?: return
                notifyDataChanged(callbackList, value)
            }
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        synchronized(callbacks) {
            callbacks.clear()
        }
        return super.onUnbind(intent)
    }

    companion object {
        private const val TAG = "EdgeSyncService"
    }

}
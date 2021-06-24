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
            ): ParcelUuid {
                val callbackList = synchronized(callbacks) {
                    var list = callbacks.get(dataId)
                    if (list == null) {
                        list = RemoteCallbackList()
                        callbacks.put(dataId, list)
                    }
                    return@synchronized list
                }
                notifyDataChanged(callbackList, null, value)
                val uuid = UUID.randomUUID()
                callbackList.register(callback, uuid)
                return ParcelUuid(uuid)
            }

            private fun notifyDataChanged(
                callbackList: RemoteCallbackList<IEdgeLiveDataCallback>,
                ignoreId: ParcelUuid?,
                value: ModifiedData
            ) {
                val ignoreUuid = ignoreId?.uuid
                synchronized(callbackList) {
                    val count = callbackList.beginBroadcast()
                    for (i in 0 until count) {
                        val callback = callbackList.getBroadcastItem(i)
                        val id = callbackList.getBroadcastCookie(i)
                        if (id != ignoreUuid) {
                            try {
                                callback.onReceive(value)
                            } catch (e: RemoteException) {
                                Log.w(TAG, e)
                            }
                        }
                    }
                    callbackList.finishBroadcast()
                }
            }

            override fun notifyDataChanged(
                dataId: Int,
                ignoreId: ParcelUuid,
                value: ModifiedData
            ) {
                val callbackList = synchronized(callbacks) { callbacks[dataId] } ?: return
                notifyDataChanged(callbackList, ignoreId, value)
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
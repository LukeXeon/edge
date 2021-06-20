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
    private val callbacks = SparseArray<RemoteCallbackList<IEdgeSyncCallback>>()
    private val stub = object : IEdgeSyncService.Stub() {

        override fun setCallback(
            dataId: Int,
            value: VersionedParcelable,
            callback: IEdgeSyncCallback
        ): ParcelUuid {
            val pid = Binder.getCallingPid()
            val callbackList = synchronized(callbacks) {
                var list = callbacks.get(dataId)
                if (list == null) {
                    list = RemoteCallbackList()
                    callbacks.put(dataId, list)
                }
                return@synchronized list
            }
            synchronized(callbackList) {
                val count = callbackList.beginBroadcast()
                for (i in 0 until count) {
                    val cb = callbackList.getBroadcastItem(i)
                    try {
                        cb.onReceive(
                            PendingParcelable(value.version, value.data, true, pid)
                        )
                    } catch (e: RemoteException) {
                        Log.w(logTag, e)
                    }
                }
                callbackList.finishBroadcast()
            }
            val uuid = UUID.randomUUID()
            callbackList.register(callback, uuid)
            return ParcelUuid(uuid)
        }

        override fun notifyDataChanged(
            dataId: Int,
            instanceId: ParcelUuid,
            value: VersionedParcelable
        ) {
            val pid = Binder.getCallingPid()
            val ignoreId = instanceId.uuid
            val callbackList = synchronized(callbacks) { callbacks[dataId] } ?: return
            synchronized(callbackList) {
                val count = callbackList.beginBroadcast()
                for (i in 0 until count) {
                    val callback = callbackList.getBroadcastItem(i)
                    val callbackId = callbackList.getBroadcastCookie(i) as? UUID
                    if (callbackId != ignoreId) {
                        try {
                            callback.onReceive(
                                PendingParcelable(value.version, value.data, false, pid)
                            )
                        } catch (e: RemoteException) {
                            Log.w(logTag, e)
                        }
                    }
                }
                callbackList.finishBroadcast()
            }
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return stub
    }

    private val logTag by lazy { javaClass.simpleName }

}
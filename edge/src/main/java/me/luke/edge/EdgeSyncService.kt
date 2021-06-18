package me.luke.edge

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.RemoteCallbackList
import android.os.RemoteException
import android.util.Log


class EdgeSyncService : Service() {
    private val lock = Any()
    private val callbacks = HashMap<String, RemoteCallbackList<IEdgeSyncClient>>()
    private val stub = object : IEdgeSyncService.Stub() {
        override fun onClientConnected(
            dataId: String,
            instanceId: String,
            value: EdgeValue,
            client: IEdgeSyncClient
        ) {
            val callbackList =
                synchronized(lock) { callbacks.getOrPut(dataId) { RemoteCallbackList() } }
            synchronized(callbackList) {
                val count = callbackList.beginBroadcast()
                for (i in 0 until count) {
                    val callback = callbackList.getBroadcastItem(i)
                    try {
                        callback.onNewClientConnected(value)
                    } catch (e: RemoteException) {
                        Log.w(TAG, e)
                    }
                }
                callbackList.finishBroadcast()
                callbackList.register(client, instanceId)
            }
        }

        override fun notifyDataChanged(
            dataId: String,
            instanceId: String,
            value: EdgeValue
        ) {
            val callbackList = synchronized(lock) { callbacks[dataId] } ?: return
            synchronized(callbackList) {
                val count = callbackList.beginBroadcast()
                for (i in 0 until count) {
                    val callback = callbackList.getBroadcastItem(i)
                    val callbackId = callbackList.getBroadcastCookie(i) as? String
                    if (callbackId != instanceId) {
                        try {
                            callback.onRemoteChanged(value)
                        } catch (e: RemoteException) {
                            Log.w(TAG, e)
                        }
                    }
                }
                callbackList.finishBroadcast()
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return stub
    }

    companion object {
        private const val TAG = "EdgeLiveDataSyncService"
    }
}
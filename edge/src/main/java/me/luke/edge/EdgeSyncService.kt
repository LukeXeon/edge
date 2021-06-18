package me.luke.edge

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.RemoteCallbackList
import android.os.RemoteException
import android.util.Log


class EdgeSyncService : Service() {
    private val callbacks = HashMap<String, RemoteCallbackList<IEdgeSyncCallback>>()
    private val stub = object : IEdgeSyncService.Stub() {

        override fun setCallback(
                dataId: String,
                instanceId: String,
                client: IEdgeSyncCallback
        ) {
            val callbackList = synchronized(callbacks) {
                callbacks.getOrPut(dataId) { RemoteCallbackList() }
            }
            callbackList.register(client, instanceId)
        }

        override fun notifyDataChanged(
                dataId: String,
                instanceId: String,
                value: EdgeValue
        ) {
            val callbackList = synchronized(callbacks) { callbacks[dataId] } ?: return
            synchronized(callbackList) {
                val count = callbackList.beginBroadcast()
                for (i in 0 until count) {
                    val callback = callbackList.getBroadcastItem(i)
                    val callbackId = callbackList.getBroadcastCookie(i) as? String
                    if (callbackId != instanceId) {
                        try {
                            callback.onReceive(value, false)
                        } catch (e: RemoteException) {
                            Log.w(TAG, e)
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

    companion object {
        private const val TAG = "EdgeLiveDataSyncService"
    }
}
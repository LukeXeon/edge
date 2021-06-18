package me.luke.edge

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.RemoteCallbackList
import android.os.RemoteException
import android.util.Log


class EdgeLiveDataSyncService : Service() {
    private val lock = Any()
    private val groups = HashMap<String, ClientGroup>()
    private val stub = object : IEdgeLiveDataSyncService.Stub() {
        override fun onClientConnected(
            dataId: String,
            instanceId: String,
            value: EdgeLiveDataPendingValue,
            client: IEdgeLiveDataSyncClient
        ) {
            synchronized(lock) {
                val group = groups[dataId]
                if (group != null) {
                    if (value.timestamp > (group.value?.timestamp ?: 0)) {
                        if (group.registeredCallbackCount > 0) {
                            group.notifyDataChanged(value, instanceId)
                        }
                    } else {
                        RemoteCallbackList<IEdgeLiveDataSyncClient>().apply {
                            register(client)
                            beginBroadcast()
                            getBroadcastItem(0).onRemoteChanged(group.value)
                            finishBroadcast()
                            unregister(client)
                        }
                    }
                    group.register(client, instanceId)
                } else {
                    groups[dataId] = ClientGroup(client, instanceId, value)
                }
            }
        }

        override fun notifyDataChanged(
            dataId: String,
            instanceId: String,
            value: EdgeLiveDataPendingValue
        ) {
            (synchronized(lock) { groups[dataId] } ?: return).notifyDataChanged(value, instanceId)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return stub
    }

    private class ClientGroup(
        client: IEdgeLiveDataSyncClient,
        instanceId: String,
        value: EdgeLiveDataPendingValue
    ) : RemoteCallbackList<IEdgeLiveDataSyncClient>() {

        private val lock = Any()

        var value: EdgeLiveDataPendingValue? = value
            private set

        init {
            register(client, instanceId)
        }

        override fun onCallbackDied(
            callback: IEdgeLiveDataSyncClient,
            cookie: Any?
        ) {
            synchronized(lock) {
                if (registeredCallbackCount == 0) {
                    value = null
                }
            }
        }

        fun notifyDataChanged(
            newValue: EdgeLiveDataPendingValue,
            ignoreId: String
        ) {
            synchronized(lock) {
                value = newValue
                val count = beginBroadcast()
                for (i in 0 until count) {
                    val callback = getBroadcastItem(i)
                    val callbackId = getBroadcastCookie(i) as? String
                    if (callbackId != ignoreId) {
                        try {
                            callback.onRemoteChanged(newValue)
                        } catch (e: RemoteException) {
                            Log.w(TAG, e)
                        }
                    }
                }
                finishBroadcast()
            }
        }
    }

    companion object {
        private const val TAG = "EdgeLiveDataSyncService"
    }
}
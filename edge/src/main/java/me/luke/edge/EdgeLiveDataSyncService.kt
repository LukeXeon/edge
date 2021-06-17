package me.luke.edge

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.RemoteCallbackList
import android.os.RemoteException
import android.util.SparseArray
import androidx.core.util.contains


class EdgeLiveDataSyncService : Service() {
    private val lock = Any()
    private val instances = HashMap<String, IEdgeLiveDataCallback>()
    private val observers = SparseArray<RemoteCallbackList<IEdgeLiveDataCallback>>()

    override fun onBind(intent: Intent): IBinder? {
        val id = intent.getIntExtra(EdgeLiveData.ID_KEY, 0)
        if (id == 0) {
            return null
        }
        val instanceId = intent.getStringExtra(EdgeLiveData.INSTANCE_ID_KEY)!!
        return object : IEdgeLiveDataService.Stub() {
            override fun setValueRemote(value: ParcelableTransporter) {
                synchronized(lock) {
                    val list = observers.get(id) ?: return
                    val current = instances[instanceId]
                    val count = list.beginBroadcast()
                    for (i in 0 until count) {
                        val callback = list.getBroadcastItem(i)
                        if (callback != current) {
                            try {
                                callback.onRemoteChanged(value)
                            } catch (e: RemoteException) {
                            }
                        }
                    }
                    list.finishBroadcast()
                }
            }

            override fun registerCallback(callback: IEdgeLiveDataCallback) {
                synchronized(lock) {
                    instances[instanceId] = callback
                    val list = if (observers.contains(id)) {
                        observers.get(id)
                    } else {
                        RemoteCallbackList<IEdgeLiveDataCallback>().apply {
                            observers.put(id, this)
                        }
                    }
                    list.register(callback)
                }
            }
        }
    }

    override fun onUnbind(intent: Intent): Boolean {
        val id = intent.getIntExtra(EdgeLiveData.ID_KEY, 0)
        if (id != 0) {
            val instanceId = intent.getStringExtra(EdgeLiveData.INSTANCE_ID_KEY)!!
            synchronized(lock) {
                val list = observers.get(id)
                val callback = instances[instanceId]
                if (list != null && callback != null) {
                    list.unregister(callback)
                }
            }
        }
        return false
    }
}
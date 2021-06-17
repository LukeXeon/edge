package me.luke.edge

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.RemoteCallbackList
import android.os.RemoteException
import android.util.Log
import android.util.SparseArray


class EdgeLiveDataService : Service() {
    private val lock = Any()
    private val instances = HashMap<String, IEdgeLiveDataCallback>()
    private val groups = SparseArray<Group>()

    private class Group(
            callback: IEdgeLiveDataCallback,
            value: PendingParcelable
    ) {
        val callbacks = RemoteCallbackList<IEdgeLiveDataCallback>().apply {
            register(callback)
        }
        var value: PendingParcelable? = value
    }

    companion object {
        private const val TAG = "EdgeLiveDataSyncService"
    }

    override fun onBind(intent: Intent): IBinder? {
        val id = intent.getIntExtra(EdgeLiveData.ID_KEY, 0)
        if (id == 0) {
            return null
        }
        val instanceId = intent.getStringExtra(EdgeLiveData.INSTANCE_ID_KEY)!!
        return object : IEdgeLiveDataService.Stub() {
            override fun setValue(value: PendingParcelable) {
                synchronized(lock) {
                    val group = groups.get(id) ?: return
                    group.value = value
                    val callbacks = group.callbacks
                    val current = instances[instanceId]
                    val count = callbacks.beginBroadcast()
                    for (i in 0 until count) {
                        val callback = callbacks.getBroadcastItem(i)
                        if (callback != current) {
                            try {
                                callback.onRemoteChanged(value)
                            } catch (e: RemoteException) {
                                Log.w(TAG, e)
                            }
                        }
                    }
                    callbacks.finishBroadcast()
                }
            }

            override fun syncValue(
                    value: PendingParcelable,
                    callback: IEdgeLiveDataCallback
            ) {
                synchronized(lock) {
                    if (groups.indexOfKey(id) > 0) {
                        val group = groups.get(id)
                        if (value.timestamp > (group.value?.timestamp ?: 0)) {
                            if (group.callbacks.registeredCallbackCount > 0) {
                                setValue(value)
                            }
                        } else {
                            RemoteCallbackList<IEdgeLiveDataCallback>().apply {
                                register(callback)
                                beginBroadcast()
                                callback.onRemoteChanged(group.value)
                                finishBroadcast()
                                unregister(callback)
                            }
                        }
                        group.callbacks.register(callback)
                    } else {
                        groups.put(id, Group(callback, value))
                    }
                    instances[instanceId] = callback
                }
            }
        }
    }

    override fun onUnbind(intent: Intent): Boolean {
        val id = intent.getIntExtra(EdgeLiveData.ID_KEY, 0)
        if (id != 0) {
            val instanceId = intent.getStringExtra(EdgeLiveData.INSTANCE_ID_KEY)!!
            synchronized(lock) {
                val callbacks = groups.get(id)?.callbacks
                val callback = instances[instanceId]
                if (callbacks != null) {
                    if (callback != null) {
                        callbacks.unregister(callback)
                    }
                    if (callbacks.registeredCallbackCount == 0) {
                        groups.remove(id)
                    }
                }
            }
        }
        return false
    }
}
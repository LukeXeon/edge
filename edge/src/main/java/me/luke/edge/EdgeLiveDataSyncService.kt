package me.luke.edge

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Parcelable
import android.os.RemoteCallbackList
import android.os.RemoteException
import android.util.Log
import android.util.SparseArray
import androidx.core.util.contains


class EdgeLiveDataSyncService : Service() {
    private val lock = Any()
    private val instances = HashMap<String, IEdgeLiveDataCallback>()
    private val observers = SparseArray<Observer>()

    private class Observer {
        val callbacks = RemoteCallbackList<IEdgeLiveDataCallback>()
        var value: ParcelableTransporter? = null
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
            override fun setValueRemote(value: ParcelableTransporter) {
                synchronized(lock) {
                    val observer = observers.get(id) ?: return
                    observer.value = value
                    val callbacks = observer.callbacks
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

            override fun registerCallback(callback: IEdgeLiveDataCallback) {
                synchronized(lock) {
                    instances[instanceId] = callback
                    val list = if (observers.contains(id)) {
                        observers.get(id).also {
                            try {
                                callback.onRemoteChanged(it.value ?: ParcelableTransporter.EMPTY)
                            } catch (e: RemoteException) {
                                Log.w(TAG, e)
                            }
                        }
                    } else {
                        Observer().apply {
                            observers.put(id, this)
                        }
                    }
                    list.callbacks.register(callback)
                }
            }
        }
    }

    override fun onUnbind(intent: Intent): Boolean {
        val id = intent.getIntExtra(EdgeLiveData.ID_KEY, 0)
        if (id != 0) {
            val instanceId = intent.getStringExtra(EdgeLiveData.INSTANCE_ID_KEY)!!
            synchronized(lock) {
                val callbacks = observers.get(id)?.callbacks
                val callback = instances[instanceId]
                if (callbacks != null) {
                    if (callback != null) {
                        callbacks.unregister(callback)
                    }
                    if (callbacks.registeredCallbackCount == 0) {
                        observers.remove(id)
                    }
                }
            }
        }
        return false
    }
}
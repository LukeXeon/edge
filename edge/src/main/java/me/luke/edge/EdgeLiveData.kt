package me.luke.edge

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import androidx.annotation.IdRes
import androidx.annotation.MainThread
import androidx.lifecycle.MutableLiveData
import java.lang.ref.WeakReference

class EdgeLiveData<T : Parcelable?>(
    context: Context,
    @IdRes
    private val dataId: Int
) : MutableLiveData<T>() {
    private val dataLock = Any()
    private val handleReceiveRunnable = HandleReceiveRunnable(this)
    private val stub by lazy {
        object : IEdgeLiveDataCallback.Stub() {
            override fun onReceive(value: ModifiedData) {
                var postTask: Boolean
                synchronized(dataLock) {
                    postTask = pendingData == Unit
                    pendingData = value
                }
                if (postTask) {
                    MAIN_HANDLER.post(handleReceiveRunnable)
                }
            }
        }
    }
    private var pendingData: Any? = Unit
    private var instanceId: ParcelUuid? = null
    private var service: IEdgeSyncService? = null
        set(newValue) {
            field = try {
                instanceId = newValue?.setLiveDataCallback(
                    dataId,
                    ModifiedData(lastUpdate, value),
                    stub
                )
                newValue
            } catch (e: RemoteException) {
                Log.w(TAG, e)
                instanceId = null
                null
            }
        }

    private var lastUpdate: Long = 0

    init {
        Connection(context, this)
    }

    @MainThread
    override fun setValue(value: T) {
        lastUpdate = SystemClock.elapsedRealtimeNanos()
        val service = service
        if (service != null) {
            try {
                service.notifyDataChanged(dataId, instanceId, ModifiedData(lastUpdate, value))
            } catch (e: RemoteException) {
                Log.w(TAG, e)
            }
        }
        super.setValue(value)
    }

    private fun handleRemoteChanged() {
        var newValue: Any?
        synchronized(dataLock) {
            newValue = pendingData
            pendingData = Unit
        }
        val value = newValue as? ModifiedData ?: return
        val version = value.version
        val data = value.data
        if (lastUpdate <= version) {
            @Suppress("UNCHECKED_CAST")
            super.setValue(data as T)
            this.lastUpdate = version
        } else {
            val service = service ?: return
            try {
                service.notifyDataChanged(dataId, instanceId, ModifiedData(lastUpdate, value))
            } catch (e: RemoteException) {
                Log.w(TAG, e)
            }
        }
    }

    private class HandleReceiveRunnable(instance: EdgeLiveData<*>) : Runnable {

        private val reference = WeakReference(instance)

        override fun run() {
            reference.get()?.handleRemoteChanged()
        }
    }

    private class Connection(
        context: Context,
        instance: EdgeLiveData<*>
    ) : ServiceConnection {

        private val reference = WeakReference(instance)

        private val appContext = context.applicationContext

        init {
            connect()
        }

        private fun connect() {
            appContext.bindService(
                Intent(
                    appContext,
                    EdgeSyncService::class.java
                ),
                this,
                Context.BIND_AUTO_CREATE
            )
        }

        private fun disconnect() {
            appContext.unbindService(this)
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val instance = reference.get()
            if (instance == null) {
                disconnect()
            } else {
                instance.service = IEdgeSyncService.Stub.asInterface(service)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            val instance = reference.get()
            if (instance != null) {
                instance.service = null
                connect()
            }
        }

    }

    companion object {
        private val MAIN_HANDLER = Handler(Looper.getMainLooper())
        private const val TAG = "EdgeLiveData"
    }

}
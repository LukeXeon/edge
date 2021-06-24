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
                val version = value.version
                synchronized(dataLock) {
                    postTask = pendingData == Unit
                    pendingData = value
                }
                if (postTask) {
                    val message = Message.obtain(handler, handleReceiveRunnable)
                    message.obj = version
                    handler.sendMessage(message)
                }
            }
        }
    }
    private val handler = Handler(Looper.getMainLooper())
    private var pendingData: Any? = Unit
    private var service: IEdgeSyncService? = null
        set(newValue) {
            field = try {
                newValue?.setLiveDataCallback(
                    dataId,
                    ModifiedData(lastUpdate, value),
                    stub
                )
                newValue
            } catch (e: RemoteException) {
                Log.w(TAG, e)
                null
            }
        }
    private var lastUpdate: Long = 0

    init {
        Connection(context, this)
    }

    @MainThread
    override fun setValue(value: T) {
        val version = SystemClock.elapsedRealtimeNanos()
        val service = service
        var sended = false
        if (service != null) {
            try {
                service.notifyDataChanged(dataId, ModifiedData(version, value))
                sended = true
            } catch (e: RemoteException) {
                Log.w(TAG, e)
            }
        }
        if (sended) {
            handleRemoteChanged()
            handler.removeCallbacks(handleReceiveRunnable, version)
        } else {
            super.setValue(value)
            lastUpdate = version
        }
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
        if (lastUpdate < version) {
            @Suppress("UNCHECKED_CAST")
            super.setValue(data as T)
            lastUpdate = version
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

        private const val TAG = "EdgeLiveData"
    }

}
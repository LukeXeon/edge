package me.luke.edge

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import androidx.annotation.*
import androidx.lifecycle.MutableLiveData
import java.lang.ref.WeakReference
import java.util.*

class EdgeLiveData<T : Parcelable?>(
    context: Context,
    @IdRes
    private val dataId: Int
) : MutableLiveData<T>() {
    private val dataLock = Any()
    private val handleReceiveRunnable = Runnable {
        handleRemoteChanged()
    }
    private val handleReceiveFromNewRunnable = Runnable {
        if (!handleRemoteChanged()) {
            notifyRemoteDataChanged()
        }
    }
    private val instanceId by lazy { ParcelUuid(UUID.randomUUID()) }
    private val stub by lazy {
        object : IEdgeSyncCallback.Stub() {
            override fun onReceive(
                fromNew: Boolean,
                value: VersionedParcelable
            ) {
                if (setPendingData(value)) {
                    MAIN_HANDLER.post(
                        if (fromNew)
                            handleReceiveFromNewRunnable
                        else
                            handleReceiveRunnable
                    )
                }
            }
        }
    }
    private var pendingData: Any? = PENDING_NO_SET
    internal var service: IEdgeSyncService? = null
        set(newValue) {
            field = newValue
            newValue?.setCallback(
                dataId,
                instanceId,
                VersionedParcelable(lastUpdate, value),
                stub
            )
        }
    private var lastUpdate: Long = 0

    init {
        Connection(context, this)
    }

    @MainThread
    override fun setValue(value: T) {
        super.setValue(value)
        lastUpdate = SystemClock.uptimeMillis()
        notifyRemoteDataChanged()
    }

    private fun setPendingData(value: VersionedParcelable): Boolean {
        var postTask: Boolean
        synchronized(dataLock) {
            postTask = pendingData == PENDING_NO_SET
            pendingData = value
        }
        return postTask
    }

    private fun handleRemoteChanged(): Boolean {
        var newValue: Any?
        synchronized(dataLock) {
            newValue = pendingData
            pendingData = PENDING_NO_SET
        }
        val value = newValue as? VersionedParcelable ?: return false
        return if (lastUpdate < value.version) {
            @Suppress("UNCHECKED_CAST")
            super.setValue(value.data as T)
            lastUpdate = value.version
            true
        } else {
            false
        }
    }

    private fun notifyRemoteDataChanged() {
        val service = service ?: return
        try {
            service.notifyDataChanged(
                dataId,
                instanceId,
                VersionedParcelable(lastUpdate, value)
            )
        } catch (e: RemoteException) {
            Log.w(TAG, e)
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
                Intent(appContext, EdgeSyncService::class.java),
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
        private val MAIN_HANDLER by lazy { Handler(Looper.getMainLooper()) }
        private val PENDING_NO_SET = Any()
        private const val TAG = "EdgeLiveData"
    }

}
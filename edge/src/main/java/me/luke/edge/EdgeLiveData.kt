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

class EdgeLiveData<T : Parcelable?>
@JvmOverloads
constructor(
    context: Context,
    @IdRes
    private val dataId: Int,
    exported: Boolean = false
) : MutableLiveData<T>() {
    private val dataLock = Any()
    private val handleReceiveRunnable = Runnable {
        if (!handleRemoteChanged()) {
            notifyRemoteDataChanged()
        }
    }
    private val instanceId by lazy { ParcelUuid(UUID.randomUUID()) }
    private val stub by lazy {
        object : IEdgeSyncCallback.Stub() {
            override fun onReceive(value: PendingParcelable) {
                var postTask: Boolean
                synchronized(dataLock) {
                    postTask = pendingData == PENDING_NO_SET
                    pendingData = value
                }
                if (postTask) {
                    MAIN_HANDLER.post(handleReceiveRunnable)
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
        Connection(context, this, exported)
    }

    @MainThread
    override fun setValue(value: T) {
        super.setValue(value)
        lastUpdate = SystemClock.elapsedRealtimeNanos()
        notifyRemoteDataChanged()
    }

    private fun handleRemoteChanged(): Boolean {
        var newValue: Any?
        synchronized(dataLock) {
            newValue = pendingData
            pendingData = PENDING_NO_SET
        }
        val value = newValue as? PendingParcelable ?: return false
        val pid = value.pid
        val version = value.version
        val data = value.data
        val fromNew = value.fromNew
        return if (lastUpdate < version || (lastUpdate == version && pid < Process.myPid())) {
            @Suppress("UNCHECKED_CAST")
            super.setValue(data as T)
            lastUpdate = version
            fromNew
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
        instance: EdgeLiveData<*>,
        private val exported: Boolean
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
                    if (exported)
                        EdgeSyncServiceEx::class.java
                    else
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
        @JvmStatic
        fun getPackagePermissionName(context: Context): String {
            return "${context.packageName}.USE_EDGE_LIVE_DATA"
        }

        private val MAIN_HANDLER by lazy { Handler(Looper.getMainLooper()) }
        private val PENDING_NO_SET = Any()
        private const val TAG = "EdgeLiveData"
    }

}
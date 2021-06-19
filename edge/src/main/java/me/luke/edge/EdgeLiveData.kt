package me.luke.edge

import android.annotation.TargetApi
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import androidx.annotation.*
import androidx.lifecycle.MutableLiveData
import java.util.*

class EdgeLiveData<T : Parcelable?>(
    context: Context,
    @IdRes
    private val dataId: Int
) : MutableLiveData<T>(), ServiceConnection {
    private val appContext = context.applicationContext
    private val dataLock = Any()
    private val handleReceiveRunnable = Runnable {
        handleRemoteChanged()
    }
    private val handleReceiveFromNewRunnable = Runnable {
        if (!handleRemoteChanged()) {
            notifyRemoteDataChanged()
        }
    }
    private val instanceId by lazy { UUID.randomUUID() }
    private val stub by lazy {
        object : IEdgeSyncCallback.Stub() {
            override fun onReceive(value: VersionedParcelable, fromNew: Boolean) {
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
    private var service: IEdgeSyncService? = null
    private var lastUpdate: Long = 0

    override fun onActive() {
        if (service == null) {
            appContext.bindService(
                Intent(appContext, EdgeSyncService::class.java),
                this,
                Context.BIND_AUTO_CREATE
            )
        }
    }

    override fun onInactive() {
        if (service != null) {
            appContext.unbindService(this)
        }
    }

    @MainThread
    override fun setValue(value: T) {
        super.setValue(value)
        lastUpdate = SystemClock.uptimeMillis()
        notifyRemoteDataChanged()
    }

    @TargetApi(Int.MAX_VALUE)
    @RequiresApi(Int.MAX_VALUE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Deprecated(
        "Part of the ServiceConnection interface.  Do not call.",
        level = DeprecationLevel.HIDDEN
    )
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        try {
            this.service = IEdgeSyncService.Stub
                .asInterface(service)
                .apply {
                    onClientConnected(
                        dataId,
                        ParcelUuid(instanceId),
                        VersionedParcelable(lastUpdate, value),
                        stub
                    )
                }
        } catch (e: RemoteException) {
            Log.w(TAG, e)
        }
    }

    @TargetApi(Int.MAX_VALUE)
    @RequiresApi(Int.MAX_VALUE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Deprecated(
        "Part of the ServiceConnection interface.  Do not call.",
        level = DeprecationLevel.HIDDEN
    )
    override fun onServiceDisconnected(name: ComponentName?) {
        service = null
        if (hasActiveObservers()) {
            onActive()
        }
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
            service.notifyDataChanged(dataId, ParcelUuid(instanceId), VersionedParcelable(lastUpdate, value))
        } catch (e: RemoteException) {
            Log.w(TAG, e)
        }
    }

    companion object {
        private val MAIN_HANDLER by lazy { Handler(Looper.getMainLooper()) }
        private val PENDING_NO_SET = Any()
        private const val TAG = "EdgeLiveData"
    }

}
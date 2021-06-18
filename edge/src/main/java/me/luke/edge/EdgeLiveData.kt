package me.luke.edge

import android.annotation.TargetApi
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.lifecycle.LiveData
import java.util.*

class EdgeLiveData<T : Parcelable?>(
        context: Context,
        private val dataId: String
) : LiveData<T>(), ServiceConnection {
    private val instanceId = UUID.randomUUID().toString()
    private val appContext = context.applicationContext
    private val dataLock = Any()
    private val handleDataChangedRunnable = Runnable {
        handleRemoteChanged()
    }
    private val handleNewClientConnectedRunnable = Runnable {
        if (!handleRemoteChanged()) {
            notifyRemoteDataChanged()
        }
    }
    private val stub = object : IEdgeSyncClient.Stub() {

        override fun onDataChanged(value: EdgeValue) {
            if (setPendingData(value)) {
                MAIN_HANDLER.post(handleDataChangedRunnable)
            }
        }

        override fun onNewClientConnected(value: EdgeValue) {
            if (setPendingData(value)) {
                MAIN_HANDLER.post(handleNewClientConnectedRunnable)
            }
        }
    }
    private var pendingData: Any? = null
    private var service: IEdgeSyncService? = null
    private var lastUpdate: Long = 0

    init {
        if (dataId.isBlank()) {
            throw IllegalArgumentException("dataId must not be empty")
        }
    }

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

    @AnyThread
    public override fun postValue(value: T) {
        super.postValue(value)
    }

    @MainThread
    public override fun setValue(value: T) {
        super.setValue(value)
        lastUpdate = SystemClock.uptimeMillis()
        notifyRemoteDataChanged()
    }

    @TargetApi(Int.MAX_VALUE)
    @RequiresApi(Int.MAX_VALUE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Deprecated("Part of the ServiceConnection interface.  Do not call.", level = DeprecationLevel.HIDDEN)
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        try {
            this.service = IEdgeSyncService.Stub
                    .asInterface(service)
                    .apply {
                        attachToService(
                                dataId,
                                instanceId,
                                EdgeValue(lastUpdate, value),
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
    @Deprecated("Part of the ServiceConnection interface.  Do not call.", level = DeprecationLevel.HIDDEN)
    override fun onServiceDisconnected(name: ComponentName?) {
        service = null
        if (hasActiveObservers()) {
            onActive()
        }
    }

    private fun setPendingData(value: EdgeValue): Boolean {
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
        val value = newValue as? EdgeValue ?: return false
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
                    EdgeValue(lastUpdate, value)
            )
        } catch (e: RemoteException) {
            Log.w(TAG, e)
        }
    }

    companion object {
        private val MAIN_HANDLER = Handler(Looper.getMainLooper())
        private val PENDING_NO_SET = Any()
        private const val TAG = "EdgeLiveData"
    }

}
package me.luke.edge

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import java.util.*


class EdgeLiveData<T : Parcelable?>(
    context: Context,
    private val dataId: String
) : LiveData<T>() {
    private val instanceId = UUID.randomUUID().toString()
    private val appContext = context.applicationContext
    private val dataLock = Any()
    private val handleRemoteChangedRunnable = Runnable {
        var newValue: Any?
        synchronized(dataLock) {
            newValue = pendingData
            pendingData = PENDING_NO_SET
        }
        val value = newValue as? EdgeLiveDataPendingValue ?: return@Runnable
        if (lastUpdate < value.timestamp) {
            @Suppress("UNCHECKED_CAST")
            super.setValue(value.data as T)
            lastUpdate = value.timestamp
        }
    }
    private val stub = object : IEdgeLiveDataSyncClient.Stub() {
        override fun onRemoteChanged(value: EdgeLiveDataPendingValue) {
            var postTask: Boolean
            synchronized(dataLock) {
                postTask = pendingData == PENDING_NO_SET
                pendingData = value
            }
            if (!postTask) {
                return
            }
            MAIN_HANDLER.post(handleRemoteChangedRunnable)
        }
    }
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            try {
                this@EdgeLiveData.service = IEdgeLiveDataSyncService.Stub
                    .asInterface(service)
                    .also {
                        it.onClientConnected(
                            dataId,
                            instanceId,
                            EdgeLiveDataPendingValue(lastUpdate, value),
                            stub
                        )
                    }
            } catch (e: RemoteException) {
                Log.w(TAG, e)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            if (hasActiveObservers()) {
                onActive()
            }
        }
    }
    private var pendingData: Any? = null
    private var service: IEdgeLiveDataSyncService? = null
    private var lastUpdate: Long = 0

    init {
        if (dataId.isBlank()) {
            throw IllegalArgumentException("dataId must not be empty")
        }
    }

    override fun onActive() {
        if (service == null) {
            appContext.bindService(
                Intent(appContext, EdgeLiveDataSyncService::class.java),
                connection,
                Context.BIND_AUTO_CREATE
            )
        }
    }

    override fun onInactive() {
        if (service != null) {
            appContext.unbindService(connection)
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
        try {
            (service ?: return).notifyDataChanged(
                dataId,
                instanceId,
                EdgeLiveDataPendingValue(lastUpdate, value)
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
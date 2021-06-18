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
        handleRemoteChanged()
    }
    private val handleNewClientConnectedRunnable = Runnable {
        if (!handleRemoteChanged()) {
            notifyDataChanged()
        }
    }
    private val stub = object : IEdgeSyncClient.Stub() {

        private fun setPendingData(value: EdgeValue): Boolean {
            var postTask: Boolean
            synchronized(dataLock) {
                postTask = pendingData == PENDING_NO_SET
                pendingData = value
            }
            return postTask
        }

        override fun onRemoteChanged(value: EdgeValue) {
            if (setPendingData(value)) {
                MAIN_HANDLER.post(handleRemoteChangedRunnable)
            }
        }

        override fun onNewClientConnected(value: EdgeValue) {
            if (setPendingData(value)) {
                MAIN_HANDLER.post(handleNewClientConnectedRunnable)
            }
        }
    }
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            try {
                this@EdgeLiveData.service = IEdgeSyncService.Stub
                    .asInterface(service)
                    .apply {
                        onClientConnected(
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

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            if (hasActiveObservers()) {
                onActive()
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
        notifyDataChanged()
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

    private fun notifyDataChanged() {
        val s = service ?: return
        try {
            s.notifyDataChanged(
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
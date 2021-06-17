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
        private val id: Int
) : LiveData<T>() {
    private val instanceId = UUID.randomUUID().toString()
    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private val connection = object : IEdgeLiveDataCallback.Stub(), ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            try {
                remote = IEdgeLiveDataService.Stub.asInterface(service).also {
                    it.syncValue(ParcelableTransporter(lastUpdate, value), this)
                }
            } catch (e: RemoteException) {
                Log.w(TAG, e)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            remote = null
            if (hasActiveObservers()) {
                onActive()
            }
        }

        override fun onRemoteChanged(value: ParcelableTransporter) {
            handler.post { handleRemoteChanged(value) }
        }
    }
    private var remote: IEdgeLiveDataService? = null
    private var lastUpdate: Long = 0

    init {
        if (id == 0) {
            throw IllegalArgumentException("id must not be 0")
        }
    }

    override fun onActive() {
        if (remote == null) {
            appContext.bindService(
                    Intent(appContext, EdgeLiveDataSyncService::class.java).apply {
                        putExtra(INSTANCE_ID_KEY, instanceId)
                        putExtra(ID_KEY, id)
                    },
                    connection,
                    Context.BIND_AUTO_CREATE
            )
        }
    }

    override fun onInactive() {
        if (remote != null) {
            appContext.unbindService(connection)
        }
    }

    @AnyThread
    public override fun postValue(value: T) {
        super.postValue(value)
    }

    @MainThread
    private fun handleRemoteChanged(value: ParcelableTransporter) {
        if (lastUpdate < value.timestamp) {
            @Suppress("UNCHECKED_CAST")
            super.setValue(value.parcelable as T)
            lastUpdate = value.timestamp
        }
    }

    @MainThread
    public override fun setValue(value: T) {
        super.setValue(value)
        lastUpdate = SystemClock.uptimeMillis()
        try {
            (remote ?: return).setValue(
                    ParcelableTransporter(lastUpdate, value)
            )
        } catch (e: RemoteException) {
            Log.w(TAG, e)
        }
    }

    companion object {
        internal const val ID_KEY = "id"
        internal const val INSTANCE_ID_KEY = "instanceId"
        private const val TAG = "EdgeLiveData"
    }
}
package me.luke.edge

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.Parcelable
import android.os.RemoteException
import android.util.Log
import androidx.lifecycle.LiveData
import java.util.*


class EdgeLiveData<T : Parcelable>(
    context: Context,
    private val id: Int
) : LiveData<T>() {
    private val instanceId = UUID.randomUUID().toString()
    private val appContext = context.applicationContext
    private val connection = object : IEdgeLiveDataCallback.Stub(), ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            try {
                remote = IEdgeLiveDataService.Stub.asInterface(service).also {
                    it.registerCallback(this)
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
            @Suppress("UNCHECKED_CAST")
            postValue(value.parcelable as T)
        }
    }
    private var remote: IEdgeLiveDataService? = null

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

    public override fun postValue(value: T) {
        super.postValue(value)
    }

    public override fun setValue(value: T) {
        super.setValue(value)
        try {
            (remote ?: return).setValueRemote(ParcelableTransporter(value))
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
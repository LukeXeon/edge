package me.luke.edge

import android.content.Context
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import android.view.inspector.WindowInspector

internal class VersionedParcelable(
    val version: Long,
    val data: Parcelable?
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readParcelable(getClassLoader()),
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(version)
        parcel.writeParcelable(data, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<VersionedParcelable> {

        private const val TAG = "VersionedParcelable"

        private val application by lazy {
            try {
                @Suppress("PrivateApi")
                val activityThreadClass = Class.forName("android.app.ActivityThread")
                val current = activityThreadClass
                    .getDeclaredField("sCurrentActivityThread")
                    .apply { isAccessible = true }.get(null)
                activityThreadClass.getDeclaredField("mInitialApplication")
                    .apply { isAccessible = true }
                    .get(current) as Context
            } catch (e: Throwable) {
                Log.e(TAG, "get application", e)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    WindowInspector.getGlobalWindowViews()
                        .firstOrNull()
                        ?.context
                        ?.applicationContext
                } else {
                    null
                }
            }
        }

        private fun getClassLoader(): ClassLoader? {
            return application?.classLoader
                ?: VersionedParcelable::class.java.classLoader
                ?: ClassLoader.getSystemClassLoader()
        }

        override fun createFromParcel(parcel: Parcel): VersionedParcelable {
            return VersionedParcelable(parcel)
        }

        override fun newArray(size: Int): Array<VersionedParcelable?> {
            return arrayOfNulls(size)
        }
    }
}
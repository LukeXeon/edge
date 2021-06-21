package me.luke.edge

import android.content.Context
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import android.view.inspector.WindowInspector
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal open class ModifiedData(
    val version: Long,
    val data: Parcelable?
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readParcelable(application?.classLoader),
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(version)
        parcel.writeParcelable(data, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ModifiedData> {

        private const val TAG = "ModifiedData"

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

        override fun createFromParcel(parcel: Parcel): ModifiedData {
            return ModifiedData(parcel)
        }

        override fun newArray(size: Int): Array<ModifiedData?> {
            return arrayOfNulls(size)
        }
    }
}
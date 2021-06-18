package me.luke.edge

import android.os.Parcel
import android.os.Parcelable

class EdgeLiveDataPendingValue(
    val timestamp: Long,
    val data: Parcelable?
) : Parcelable {
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(timestamp)
        parcel.writeParcelable(data, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<EdgeLiveDataPendingValue> {

        override fun createFromParcel(parcel: Parcel): EdgeLiveDataPendingValue {
            return EdgeLiveDataPendingValue(
                parcel.readLong(),
                parcel.readParcelable(EdgeLiveDataPendingValue::class.java.classLoader)
            )
        }

        override fun newArray(size: Int): Array<EdgeLiveDataPendingValue?> {
            return arrayOfNulls(size)
        }
    }
}
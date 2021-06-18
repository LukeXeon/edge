package me.luke.edge

import android.os.Parcel
import android.os.Parcelable

class EdgeValue(
    val version: Long,
    val data: Parcelable?
) : Parcelable {
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(version)
        parcel.writeParcelable(data, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<EdgeValue> {

        override fun createFromParcel(parcel: Parcel): EdgeValue {
            return EdgeValue(
                parcel.readLong(),
                parcel.readParcelable(EdgeValue::class.java.classLoader)
            )
        }

        override fun newArray(size: Int): Array<EdgeValue?> {
            return arrayOfNulls(size)
        }
    }
}
package me.luke.edge

import android.os.Parcel
import android.os.Parcelable

class EdgeValue(
        val version: Long,
        val data: Parcelable?
) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readLong(),
            parcel.readParcelable(EdgeValue::class.java.classLoader)) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(version)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<EdgeValue> {
        override fun createFromParcel(parcel: Parcel): EdgeValue {
            return EdgeValue(parcel)
        }

        override fun newArray(size: Int): Array<EdgeValue?> {
            return arrayOfNulls(size)
        }
    }
}
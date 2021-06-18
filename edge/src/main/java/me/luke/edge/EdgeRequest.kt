package me.luke.edge

import android.os.Parcel
import android.os.Parcelable


class EdgeRequest(
        val dataId: String,
        val instanceId: String,
        val value: EdgeValue
) : Parcelable {

    constructor(parcel: Parcel) : this(
            parcel.readString()!!,
            parcel.readString()!!,
            parcel.readParcelable(EdgeValue::class.java.classLoader)!!)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(dataId)
        parcel.writeString(instanceId)
        parcel.writeParcelable(value, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<EdgeRequest> {
        override fun createFromParcel(parcel: Parcel): EdgeRequest {
            return EdgeRequest(parcel)
        }

        override fun newArray(size: Int): Array<EdgeRequest?> {
            return arrayOfNulls(size)
        }
    }
}
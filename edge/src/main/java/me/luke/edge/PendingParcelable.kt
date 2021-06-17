package me.luke.edge

import android.os.Parcel
import android.os.Parcelable

class PendingParcelable(
        val timestamp: Long,
        val parcelable: Parcelable?
) : Parcelable {
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(timestamp)
        parcel.writeParcelable(parcelable, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<PendingParcelable> {

        override fun createFromParcel(parcel: Parcel): PendingParcelable {
            return PendingParcelable(
                    parcel.readLong(),
                    parcel.readParcelable(PendingParcelable::class.java.classLoader)
            )
        }

        override fun newArray(size: Int): Array<PendingParcelable?> {
            return arrayOfNulls(size)
        }
    }
}
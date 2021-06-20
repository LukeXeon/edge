package me.luke.edge

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class PendingParcelable : VersionedParcelable {
    val fromNew: Boolean
    val pid: Int

    constructor(
        version: Long,
        data: Parcelable?,
        fromNew: Boolean,
        pid: Int
    ) : super(version, data) {
        this.fromNew = fromNew
        this.pid = pid
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeInt(if (fromNew) 1 else 0)
        parcel.writeInt(pid)
    }

    constructor(parcel: Parcel) : super(parcel) {
        this.fromNew = parcel.readInt() != 0
        this.pid = parcel.readInt()
    }

    companion object CREATOR : Parcelable.Creator<PendingParcelable> {

        override fun createFromParcel(source: Parcel): PendingParcelable {
            return PendingParcelable(source)
        }

        override fun newArray(size: Int): Array<PendingParcelable?> {
            return arrayOfNulls(size)
        }
    }
}
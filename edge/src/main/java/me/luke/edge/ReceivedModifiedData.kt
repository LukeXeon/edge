package me.luke.edge

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class ReceivedModifiedData : ModifiedData {
    val isFromNew: Boolean
    val pid: Int

    constructor(
        version: Long,
        data: Parcelable?,
        isFromNew: Boolean,
        pid: Int
    ) : super(version, data) {
        this.isFromNew = isFromNew
        this.pid = pid
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeInt(if (isFromNew) 1 else 0)
        parcel.writeInt(pid)
    }

    constructor(parcel: Parcel) : super(parcel) {
        this.isFromNew = parcel.readInt() != 0
        this.pid = parcel.readInt()
    }

    companion object CREATOR : Parcelable.Creator<ReceivedModifiedData> {

        override fun createFromParcel(source: Parcel): ReceivedModifiedData {
            return ReceivedModifiedData(source)
        }

        override fun newArray(size: Int): Array<ReceivedModifiedData?> {
            return arrayOfNulls(size)
        }
    }
}
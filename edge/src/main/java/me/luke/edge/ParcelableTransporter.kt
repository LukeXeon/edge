package me.luke.edge

import android.os.Parcel
import android.os.Parcelable

class ParcelableTransporter(
        val timestamp: Long,
        val parcelable: Parcelable?
) : Parcelable {
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(timestamp)
        val className = parcelable?.javaClass?.name
        parcel.writeString(className)
        parcel.writeParcelable(parcelable, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ParcelableTransporter> {

        private val CREATORS by lazy { HashMap<String, Parcelable.Creator<Parcelable>>() }

        override fun createFromParcel(parcel: Parcel): ParcelableTransporter {
            val timestamp = parcel.readLong()
            val clazzName = parcel.readString()
            if (clazzName.isNullOrEmpty()) {
                return ParcelableTransporter(timestamp, null)
            }
            val creator = synchronized(CREATORS) {
                CREATORS.getOrPut(clazzName) {
                    val clazz = Class.forName(clazzName)
                    @Suppress("UNCHECKED_CAST")
                    clazz.getDeclaredField("CREATOR")
                            .apply { isAccessible = true }
                            .get(null) as Parcelable.Creator<Parcelable>
                }
            }
            return ParcelableTransporter(timestamp, creator.createFromParcel(parcel))
        }

        override fun newArray(size: Int): Array<ParcelableTransporter?> {
            return arrayOfNulls(size)
        }
    }
}
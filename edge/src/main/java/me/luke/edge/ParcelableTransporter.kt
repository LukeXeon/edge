package me.luke.edge

import android.os.Parcel
import android.os.Parcelable

class ParcelableTransporter(
        val parcelable: Parcelable?
) : Parcelable {
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        val className = parcelable?.javaClass?.name
        parcel.writeString(className)
        parcel.writeParcelable(parcelable, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ParcelableTransporter> {

        val EMPTY = ParcelableTransporter(null)

        private val CREATORS by lazy { HashMap<String, Parcelable.Creator<Parcelable>>() }

        override fun createFromParcel(parcel: Parcel): ParcelableTransporter {
            val clazzName = parcel.readString()
            if (clazzName.isNullOrEmpty()) {
                return ParcelableTransporter(null)
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
            return ParcelableTransporter(creator.createFromParcel(parcel))
        }

        override fun newArray(size: Int): Array<ParcelableTransporter?> {
            return arrayOfNulls(size)
        }
    }
}
package com.andreydymko.testtask.serpapi

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

/**
 * Data class made specifically to hold data of *serpapi.com* google image search results.
 * @param thumbnailImg Link for thumbnail of the actual image. Usually located
 * on *[serpapi.com]* domain.
 * @param sourceWebsite Short URL for the source page *(eg. [en.wikipedia.org])*.
 * @param title Short description of page content, title of source
 * website *(eg. Coffee - Wikipedia)*.
 * @param sourcePage Link to the source website page where image is actually located
 * *(eg. [https://en.wikipedia.org/wiki/Coffee])*.
 * @param originalImg Link to the image in full size
 * *(eg. [https://upload.wikimedia.org/wikipedia/commons/4/45/A_small_cup_of_coffee.JPG])*.
 */
data class ImageDescription(
    @SerializedName("thumbnail") val thumbnailImg: Uri,
    @SerializedName("source") val sourceWebsite: Uri,
    @SerializedName("title") val title: String,
    @SerializedName("link") val sourcePage: Uri,
    @SerializedName("original") val originalImg: Uri
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readParcelable<Uri>(Uri::class.java.classLoader)!!,
        parcel.readParcelable<Uri>(Uri::class.java.classLoader)!!,
        parcel.readString()!!,
        parcel.readParcelable<Uri>(Uri::class.java.classLoader)!!,
        parcel.readParcelable<Uri>(Uri::class.java.classLoader)!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.apply {
            writeParcelable(thumbnailImg, flags)
            writeParcelable(sourceWebsite, flags)
            writeString(title)
            writeParcelable(sourcePage, flags)
            writeParcelable(originalImg, flags)
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun toString(): String {
        return "ImageDescription(thumbnailImg=$thumbnailImg, sourceWebsite=$sourceWebsite, " +
                "title='$title', sourcePage=$sourcePage, originalImg=$originalImg)"
    }

    companion object CREATOR : Parcelable.Creator<ImageDescription> {
        override fun createFromParcel(parcel: Parcel): ImageDescription {
            return ImageDescription(parcel)
        }

        override fun newArray(size: Int): Array<ImageDescription?> {
            return arrayOfNulls(size)
        }
    }
}


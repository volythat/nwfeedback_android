package com.newway.nwphotospicker.model

import android.net.Uri
import androidx.core.net.toFile
import java.io.File

class NWPhotoDirectory(
    var id: Long = 0,
    var bucketId: String? = null,
    private var coverPath: Uri? = null,
    var name: String? = null,
    var dateAdded: Long = 0,
    val medias: MutableList<NWMedia> = mutableListOf()
) {
    fun getCoverPath(): Uri? {
        return when {
            medias.size > 0 -> medias[0].path
            coverPath != null -> coverPath
            else -> null
        };
    }

    fun setCoverPath(coverPath: Uri?) {
        this.coverPath = coverPath
    }

    fun addPhoto(imageId: Long, fileName: String, path: Uri, mediaType: Int) {
        medias.add(NWMedia(imageId, fileName, path, mediaType))
    }

    override fun equals(other: Any?): Boolean {
        return this.bucketId == (other as? NWPhotoDirectory)?.bucketId
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (bucketId?.hashCode() ?: 0)
        result = 31 * result + (coverPath?.hashCode() ?: 0)
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + dateAdded.hashCode()
        result = 31 * result + medias.hashCode()
        return result
    }
}
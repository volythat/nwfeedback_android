package com.newway.nwphotospicker.viewmodel

import android.app.Application
import android.content.ContentUris
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.provider.BaseColumns
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.newway.nwphotospicker.extension.registerObserver
import com.newway.nwphotospicker.model.NWMedia
import com.newway.nwphotospicker.model.NWPhotoDirectory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NWPhotosPickerViewModel(application: Application) : NWBaseViewModel(application) {

    var medias : MutableLiveData<List<NWMedia>> = MutableLiveData(listOf())
    var folders : MutableLiveData<List<NWPhotoDirectory>> = MutableLiveData(listOf())
    var isDataChanged : MutableLiveData<Boolean> = MutableLiveData(false)

    private var contentObserver: ContentObserver? = null

    private fun registerContentObserver() {
        if (contentObserver == null) {

            contentObserver = getApplication<Application>().contentResolver.registerObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            ) {
                isDataChanged.value = true
            }
        }
    }
    fun getSelectedMedias():List<Uri>{
        val list = medias.value?.filter { it.isSelected } ?: listOf()
        return list.map { it.path }
    }

    fun getMedia(bucketId: String? = null) {
        launchDataLoad {
            val medias = mutableListOf<NWMedia>()

            queryImages(bucketId).map { dir ->
                medias.addAll(dir.medias)
            }
            medias.sortWith { a, b -> (b.id - a.id).toInt() }
            this.medias.postValue(medias)

            registerContentObserver()
        }
    }

    fun getPhotoDirs(bucketId: String? = null) {
        launchDataLoad {
            val dirs = queryImages(bucketId)
            val photoDirectory = NWPhotoDirectory()
            photoDirectory.bucketId = null

            if (dirs.isNotEmpty() && dirs[0].medias.size > 0) {
                photoDirectory.dateAdded = dirs[0].dateAdded
                photoDirectory.setCoverPath(dirs[0].medias[0].path)
            }

            for (i in dirs.indices) {
                photoDirectory.medias.addAll(dirs[i].medias)
            }

            dirs.add(0, photoDirectory)
            folders.postValue(dirs)
            registerContentObserver()
        }
    }

    @WorkerThread
    suspend fun queryImages(bucketId: String?): MutableList<NWPhotoDirectory> {
        var data = mutableListOf<NWPhotoDirectory>()
        withContext(Dispatchers.IO) {

            val projection = null
            val uri = MediaStore.Files.getContentUri("external")
            val sortOrder = MediaStore.Images.Media._ID + " DESC"
            val selectionArgs = mutableListOf<String>()

            var selection = (MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                    + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE)

            if (bucketId != null)
                selection += " AND " + MediaStore.Images.Media.BUCKET_ID + "='" + bucketId + "'"

            selection += " AND " + MediaStore.Images.Media.BUCKET_DISPLAY_NAME + "!='" + "Raw" + "'"

            val cursor = getApplication<Application>().contentResolver.query(uri, projection, selection, selectionArgs.toTypedArray(), sortOrder)

            if (cursor != null) {
                data = getPhotoDirectories(cursor)
                cursor.close()
            }
        }
        return data
    }

    @WorkerThread
    private fun getPhotoDirectories(data: Cursor): MutableList<NWPhotoDirectory> {
        val directories = mutableListOf<NWPhotoDirectory>()

        while (data.moveToNext()) {

            val imageId = data.getLong(data.getColumnIndexOrThrow(BaseColumns._ID))
            val bucketId = data.getString(data.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.BUCKET_ID))
            val name = data.getString(data.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME))
            val fileName = data.getString(data.getColumnIndexOrThrow(MediaStore.MediaColumns.TITLE))
            val mediaType = data.getInt(data.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE))
            val widthImage = data.getInt(data.getColumnIndexOrThrow(MediaStore.Files.FileColumns.WIDTH))
            val heightImage = data.getInt(data.getColumnIndexOrThrow(MediaStore.Files.FileColumns.HEIGHT))

            val photoDirectory = NWPhotoDirectory()
            photoDirectory.id = imageId
            photoDirectory.bucketId = bucketId
            photoDirectory.name = name

            val contentUri = ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                imageId
            )
            if (widthImage >= 512 && heightImage >= 512) {
                if (!directories.contains(photoDirectory)) {
                    photoDirectory.addPhoto(imageId, fileName, contentUri, mediaType)
                    photoDirectory.dateAdded =
                        data.getLong(data.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED))
                    directories.add(photoDirectory)
                } else {
                    directories[directories.indexOf(photoDirectory)]
                        .addPhoto(imageId, fileName, contentUri, mediaType)
                }
            }else{
                Log.e("Image Size small","width = $widthImage -- height = $heightImage")
            }
        }

        return directories
    }

    override fun onCleared() {
        contentObserver?.let {
            getApplication<Application>().contentResolver.unregisterContentObserver(it)
        }
    }
}
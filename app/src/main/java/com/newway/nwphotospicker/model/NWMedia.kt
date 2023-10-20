package com.newway.nwphotospicker.model

import android.net.Uri
import java.io.Serializable

enum class FileType(value:Int) {
    IMAGE(0),VIDEO(1)
}

class NWMedia(var id: Long = 0,
              var name: String,
              var path: Uri,
              var mediaType: Int = 0,
              var isSelected : Boolean = false) : Serializable
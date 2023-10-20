package com.newway.nwfeedback

import android.net.Uri
import android.util.Log
import com.zendesk.service.ErrorResponse
import com.zendesk.service.ZendeskCallback
import zendesk.support.Support
import zendesk.support.UploadResponse
import java.io.File
import android.webkit.MimeTypeMap


class SMAttackment(val image : Uri) {
    var response : String? = null

    fun upload(callback: () -> Unit){
        val mime = getMimeType(image.path)
        image.path?.let { path ->
            val provider = Support.INSTANCE.provider()?.uploadProvider()

            provider?.uploadAttachment(path.lastComponent(), File(path),mime,object :
                ZendeskCallback<UploadResponse>() {
                override fun onSuccess(p0: UploadResponse?) {
                    Log.e("AT","success = ${p0.toString()}")
                    response = p0?.token
                    callback()
                }

                override fun onError(p0: ErrorResponse?) {
                    Log.e("AT","error = ${p0?.reason}")
                    callback()
                }

            })
        }
    }
    fun delete(callback:()->Unit){
        if (response != null){
            val provider = Support.INSTANCE.provider()?.uploadProvider()
            provider?.deleteAttachment(response!!,object : ZendeskCallback<Void>(){
                override fun onSuccess(p0: Void?) {
                    callback()
                }
                override fun onError(p0: ErrorResponse?) {
                    callback()
                }

            })
        }else{
            callback()
        }
    }

    private fun getMimeType(url: String?): String {
        var type = "image/png"
        val extension = MimeTypeMap.getFileExtensionFromUrl(url)
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension).toString()
        }
        return type
    }
    fun String.lastComponent() : String {
        val fullName = this.substringAfterLast("/")
        val full = fullName.substringBeforeLast("?")
        val fileName = full.substringBeforeLast(".")
        val extension = full.substringAfterLast(".")
        return "$fileName.$extension"
    }
}
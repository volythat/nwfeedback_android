package com.newway.nwfeedback
import android.app.Activity
import android.net.Uri
import android.text.TextUtils
import android.util.Patterns
import android.widget.Toast

fun String.isAnEmailAddress() : Boolean = (!TextUtils.isEmpty(this) && Patterns.EMAIL_ADDRESS.matcher(this).matches())
class SMFeedbackModel(
    var email: String = "",
    var content: String = "",
    var projectId : String = "",
    var attachments : ArrayList<SMAttackment> = arrayListOf(),
    var moreInfo : Map<String,String>? = null
) {

    fun isValid(activity: Activity): Boolean {
        if (!email.isAnEmailAddress()) {
            Toast.makeText(activity,"Please use a valid email address.",Toast.LENGTH_LONG).show()
            return false
        }
        if (content.isBlank()) {
            Toast.makeText(activity,"The content is not empty!",Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }
    fun addImages(images:ArrayList<Uri>){
        attachments.clear()
        images.forEach {
            attachments.add(SMAttackment(it))
        }
    }
    fun getTokenAttachments(): ArrayList<String>{
        val rs = ArrayList<String>()
        attachments.forEach {
            if (!it.response.isNullOrBlank()){
                rs.add(it.response!!)
            }
        }
        return rs
    }
    private fun haveUploadedAllAttachments(): Boolean {
        val rs = attachments.filter { it.response.isNullOrBlank() }
        return rs.isEmpty()
    }
    fun uploadMultiAttachments(callback:(Boolean)->Unit){
        if (attachments.isEmpty()){
            callback(true)
        }else{
            val dispatchGroup = DispatchGroup()

            attachments.forEach {
                dispatchGroup.enter()
                it.upload {
                    dispatchGroup.leave()
                }
            }

            dispatchGroup.notify {
                callback(haveUploadedAllAttachments())
            }
        }
    }
}
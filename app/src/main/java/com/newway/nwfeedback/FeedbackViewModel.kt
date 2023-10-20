package com.newway.nwfeedback

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Build.MODEL
import android.os.Environment
import android.os.Environment.MEDIA_MOUNTED
import android.os.StatFs
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.zendesk.service.ErrorResponse
import com.zendesk.service.ZendeskCallback
import zendesk.core.AnonymousIdentity
import zendesk.core.Zendesk
import zendesk.support.CreateRequest
import zendesk.support.Request
import zendesk.support.Support
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class FeedbackViewModel : ViewModel()  {
    var enableSendButton : MutableLiveData<Boolean> = MutableLiveData(false)

    fun validate(email: String,content: String) {
        enableSendButton.value = email.isAnEmailAddress() && email.isNotEmpty() && content.isNotEmpty()
    }
    fun clearCache(cache:File){
        cache.deleteRecursively()
    }
    fun savePhotoFeedback(bitmap: Bitmap, folder: File):File{
        //save vào folder cache
        val current = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())
        val file = File(folder,"${current.toInt()}.png")
        if (file.exists()){
            file.delete()
        }
        val resize = getResizedBitmap(bitmap, 1024)
        Log.e("Feedback","resize image : width = ${resize?.width} - height= ${resize?.height}")
        resize?.saveToPng(file)
        return file
    }
    private fun getResizedBitmap(image: Bitmap, maxSize: Int): Bitmap? {
        var width = image.width
        var height = image.height
        val bitmapRatio = width.toFloat() / height.toFloat()
        if (bitmapRatio > 1) {
            width = maxSize
            height = (width / bitmapRatio).toInt()
        } else {
            height = maxSize
            width = (height * bitmapRatio).toInt()
        }
        return Bitmap.createScaledBitmap(image, width, height, true)
    }
    fun Bitmap.saveToPng(file: File): Boolean{
        try {
            FileOutputStream(file).use { out ->
                this.compress(
                    Bitmap.CompressFormat.PNG,
                    100,
                    out
                ) // bmp is your Bitmap instance
            }
            return true
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }
    // Zendesk
    fun createIdentify(email:String){
        val identity = AnonymousIdentity.Builder()
            .withEmailIdentifier(email)
            .build()
        Zendesk.INSTANCE.setIdentity(identity)
    }
    fun sendFeedback(model : SMFeedbackModel, context:Context, resolution:String, callback: (Boolean)->Unit){
        var formatedContent = createContent(model.content,context,resolution)
        model.moreInfo?.let {
            formatedContent += addMoreInfo(it)
        }

        val provider = Support.INSTANCE.provider()?.requestProvider()
        val date = currentDateString("ddMMyy-HHmmss")
        val request = CreateRequest()
        request.subject = "[Android] Ticket number #$date"
        request.description = formatedContent
        request.tags = arrayListOf(model.projectId)
        request.attachments = model.getTokenAttachments()
        provider?.createRequest(request, object : ZendeskCallback<Request>() {

            override fun onError(errorResponse: ErrorResponse) {
                // Handle the error
                callback(false)
            }

            override fun onSuccess(p0: Request?) {
                callback(true)
            }
        })
    }
    private fun currentDateString(fm:String = "dd-MM-yyyy"): String {
        val sdf = SimpleDateFormat(fm, Locale.ENGLISH)
        return sdf.format(Date())
    }


    private fun createContent(content: String, context:Context, resolution:String) : String{
        val name = context.applicationInfo.loadLabel(context.packageManager).toString()

        val osVersion = Build.VERSION.RELEASE
        val deviceName = getDeviceName()
        val freeInternalSpace = getAvailableInternalMemorySize()
        val totalInternalSpace = getTotalInternalMemorySize()
        val freeExternalSpace = getAvailableExternalMemorySize()
        val totalExternalSpace = getTotalExternalMemorySize()
        val time = currentDateString("yyyy-MM-dd HH:mm:ss")
        val timezone = getTimeZoneGmt()
        val language = Locale.getDefault().displayLanguage
        val carrier = (context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager).networkOperatorName

        var ct = "$content\n"
        ct += addLine()
        ct += addItem("App Name",name)
        ct += addItem("Bundle ID",context.packageName)
        getPackageInfo(context)?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ct += addItem("Version Code", it.longVersionCode.toString())
            }else{
                ct += addItem("Version Code", it.versionCode.toString())
            }
            ct += addItem("Version Name",it.versionName)
        }
        ct += addItem("OS Version",osVersion)
        ct += addItem("Device Name", deviceName.ifBlank { "Unknown" })
        ct += addItem("Free space internal Space", freeInternalSpace.ifBlank { "Unknown" })
        ct += addItem("Total space internal Space", totalInternalSpace.ifBlank { "Unknown" })
        ct += addItem("Free space external Space", freeExternalSpace.ifBlank { "Unknown" })
        ct += addItem("Total space external Space", totalExternalSpace.ifBlank { "Unknown" })
        ct += addItem("System time",time)
        ct += addItem("System zone",timezone)
        ct += addItem("Screen Resolution",resolution)
        ct += addItem("Language",language)
        ct += addItem("Mobile Carrier",carrier)

        return ct
    }

    private fun addItem(key: String, value: String) : String {
        return "- $key : $value\n"
    }

    private fun getPackageInfo(context:Context):PackageInfo?{
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            }else{
                context.packageManager.getPackageInfo(context.packageName,0)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            null
        }
    }

    private fun addLine() : String {
        return "***********Log Info**********\n"
    }

    private fun addMoreInfo(info:Map<String,String>):String{
        val str = info.entries.map {
            return addItem(it.key,it.value)
        }
        return "***********More Info**********\n$str"
    }


    //info
    private fun getTimeZoneGmt():String {
        val calendar = Calendar.getInstance(
            TimeZone.getTimeZone("GMT"),
            Locale.getDefault()
        )
        val currentLocalTime = calendar.time
        val date = SimpleDateFormat("z", Locale.getDefault())
        return date.format(currentLocalTime)
    }

    private fun getDeviceName(): String {
        val manufacturer = Build.MANUFACTURER
        val model = MODEL
        return if (model.lowercase(Locale.getDefault()).startsWith(manufacturer.lowercase(Locale.getDefault()))) {
            capitalize(model)
        } else {
            capitalize(manufacturer) + " " + model
        }
    }
    private fun externalMemoryAvailable(): Boolean {
        return Environment.getExternalStorageState() ==
                MEDIA_MOUNTED
    }

    private fun getAvailableInternalMemorySize(): String {
        val path: File = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val blockSize = stat.blockSizeLong
        val availableBlocks = stat.availableBlocksLong
        return formatSize(availableBlocks * blockSize)
    }

    private fun getTotalInternalMemorySize(): String {
        val path: File = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        return formatSize(totalBlocks * blockSize)
    }

    private fun getAvailableExternalMemorySize(): String {
        return if (externalMemoryAvailable()) {
            val path: File = Environment.getExternalStorageDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val availableBlocks = stat.availableBlocksLong
            formatSize(availableBlocks * blockSize)
        } else {
            "Unknown"
        }
    }

    private fun getTotalExternalMemorySize(): String {
        return if (externalMemoryAvailable()) {
            val path: File = Environment.getExternalStorageDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            formatSize(totalBlocks * blockSize)
        } else {
            "Unknown"
        }
    }

    private fun formatSize(size: Long): String {
        var size = size
        var suffix: String? = null
        if (size >= 1024) {
            suffix = "KB"
            size /= 1024
            if (size >= 1024) {
                suffix = "MB"
                size /= 1024
            }
        }
        val resultBuffer = StringBuilder(size.toString())
        var commaOffset = resultBuffer.length - 3
        while (commaOffset > 0) {
            resultBuffer.insert(commaOffset, ',')
            commaOffset -= 3
        }
        if (suffix != null) resultBuffer.append(suffix)
        return resultBuffer.toString()
    }


    private fun capitalize(s: String): String {
        if (s.isEmpty()) {
            return ""
        }
        val first = s[0]
        return if (Character.isUpperCase(first)) {
            s
        } else {
            Character.toUpperCase(first).toString() + s.substring(1)
        }
    }
}

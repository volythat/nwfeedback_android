package com.newway.nwfeedback.nwphotospicker.extension

import android.animation.ObjectAnimator
import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions

fun View.rotateImage(startAngle:Float) {
    ObjectAnimator.ofFloat(this, View.ROTATION, startAngle, startAngle + 180f).apply {
        duration = 300
        interpolator = LinearInterpolator()
        start()
    }
}

// slide the view from below itself to the current position
fun View.slideUp() {
    visibility = View.VISIBLE
    val animate = TranslateAnimation(
        0f,  // fromXDelta
        0f,  // toXDelta
        height.toFloat(),  // fromYDelta
        0f
    ) // toYDelta
    animate.duration = 300
    animate.fillAfter = true
    startAnimation(animate)
}


// slide the view from its current position to below itself
fun View.slideDown() {
    val animate = TranslateAnimation(
        0f,  // fromXDelta
        0f,  // toXDelta
        0f,  // fromYDelta
        height.toFloat()
    ) // toYDelta
    animate.duration = 300
    animate.fillAfter = true
    startAnimation(animate)
}


fun ContentResolver.registerObserver(
    uri: Uri,
    observer: (selfChange: Boolean) -> Unit
): ContentObserver {
    val contentObserver = object : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean) {
            observer(selfChange)
        }
    }
    registerContentObserver(uri, true, contentObserver)
    return contentObserver
}

fun View.singleClick(debounceTime: Long = 600L, action: () -> Unit) {
    this.setOnClickListener(object : View.OnClickListener {
        private var lastClickTime: Long = 0

        override fun onClick(v: View) {
            if (SystemClock.elapsedRealtime() - lastClickTime < debounceTime) return
            else action()

            lastClickTime = SystemClock.elapsedRealtime()
        }
    })
}

fun loadImage(image: ImageView, url: Uri?) {
    url?.let {
        Glide.with(image)
            .load(it)
            .override(300, 300)
            .centerCrop()
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(image)
    }
}
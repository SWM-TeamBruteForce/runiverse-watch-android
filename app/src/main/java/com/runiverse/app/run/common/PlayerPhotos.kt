package com.runiverse.app.run.common

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.runiverse.app.phone.PlayerPhoto

private const val TAG = "PlayerPhotos"

/** 사람 번호마다 그릴 사진이다. 못 푼 사진은 빼서 그 사람은 첫 글자로 그린다. */
fun decodePlayerPhotos(photos: List<PlayerPhoto>): Map<String, ImageBitmap> =
    photos.mapNotNull { photo ->
        val image = decodeJpegOrNull(photo.jpegBase64)
        if (image == null) Log.w(TAG, "사진을 못 풀어 첫 글자로 그린다 — ${photo.userId}")
        image?.let { photo.userId to it }
    }.toMap()

private fun decodeJpegOrNull(jpegBase64: String): ImageBitmap? {
    val bytes = try {
        Base64.decode(jpegBase64, Base64.DEFAULT)
    } catch (_: IllegalArgumentException) {
        return null
    }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
}

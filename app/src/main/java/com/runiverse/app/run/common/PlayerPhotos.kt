package com.runiverse.app.run.common

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.runiverse.app.phone.PlayerPhoto

private const val TAG = "PlayerPhotos"

// 폰은 64로 줄여 보낸다 — 원본이 잘못 와도 풀다가 앱이 죽지 않게 넉넉히 막는다
private const val MAX_PHOTO_SIDE = 256

/** 사람 번호마다 그릴 사진이다. 못 푼 사진은 빼서 그 사람은 첫 글자로 그린다. */
fun decodePlayerPhotos(photos: List<PlayerPhoto>): Map<String, ImageBitmap> =
    photos.mapNotNull { photo ->
        val image = decodeJpegOrNull(photo.jpegBase64)
        if (image == null) Log.w(TAG, "파티원 사진 풀기 실패 — ${photo.userId}")
        image?.let { photo.userId to it }
    }.toMap()

private fun decodeJpegOrNull(jpegBase64: String): ImageBitmap? {
    val bytes = try {
        Base64.decode(jpegBase64, Base64.DEFAULT)
    } catch (_: IllegalArgumentException) {
        return null
    }
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth > MAX_PHOTO_SIDE || bounds.outHeight > MAX_PHOTO_SIDE) return null
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
}

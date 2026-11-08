package com.runiverse.app.run.common

import android.graphics.Bitmap
import android.util.Base64
import com.runiverse.app.phone.PlayerPhoto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream

// 기본 그림 흉내는 그림이 아닌 값도 풀어 줘서 실제 그림 처리로 돌린다
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(RobolectricTestRunner::class)
class PlayerPhotosTest {

    @Test
    fun `JPEG 사진은 그 사람 번호로 풀린다`() {
        // given
        val photos = listOf(PlayerPhoto(userId = "u2", jpegBase64 = jpegBase64(width = 4, height = 3)))

        // when
        val images = decodePlayerPhotos(photos)

        // then
        assertEquals(setOf("u2"), images.keys)
        assertEquals(4, images.getValue("u2").width)
        assertEquals(3, images.getValue("u2").height)
    }

    @Test
    fun `못 푼 사진은 빼고 나머지만 남긴다`() {
        // given
        val photos = listOf(
            PlayerPhoto(userId = "u2", jpegBase64 = "A"),
            PlayerPhoto(userId = "u3", jpegBase64 = Base64.encodeToString("사진 아님".toByteArray(), Base64.NO_WRAP)),
            PlayerPhoto(userId = "u4", jpegBase64 = jpegBase64(width = 4, height = 4)),
        )

        // when
        val images = decodePlayerPhotos(photos)

        // then
        assertEquals(setOf("u4"), images.keys)
    }

    @Test
    fun `한 변이 256보다 큰 사진은 풀지 않고 뺀다`() {
        // given
        val photos = listOf(
            PlayerPhoto(userId = "u2", jpegBase64 = jpegBase64(width = 257, height = 10)),
            PlayerPhoto(userId = "u3", jpegBase64 = jpegBase64(width = 256, height = 256)),
        )

        // when
        val images = decodePlayerPhotos(photos)

        // then
        assertEquals(setOf("u3"), images.keys)
    }

    @Test
    fun `사진이 없으면 빈 결과다`() {
        // when & then
        assertTrue(decodePlayerPhotos(emptyList()).isEmpty())
    }

    private fun jpegBase64(width: Int, height: Int): String {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val jpeg = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }.toByteArray()
        return Base64.encodeToString(jpeg, Base64.NO_WRAP)
    }
}

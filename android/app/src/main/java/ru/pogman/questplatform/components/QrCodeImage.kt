package ru.pogman.questplatform.components

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import ru.pogman.questplatform.R

@Composable
fun QrCodeImage(token: String) {
    val bitmap =
        remember(token) {
            runCatching {
                val matrix =
                    MultiFormatWriter()
                        .encode(
                            token,
                            BarcodeFormat.QR_CODE,
                            512,
                            512,
                            mapOf(EncodeHintType.CHARACTER_SET to "UTF-8"),
                        )
                val pixels =
                    IntArray(matrix.width * matrix.height) { index ->
                        if (matrix[index % matrix.width, index / matrix.width]) Color.BLACK
                        else Color.WHITE
                    }
                Bitmap.createBitmap(pixels, matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
                    .asImageBitmap()
            }
                .getOrNull()
        }
    if (bitmap != null)
        Image(
            bitmap,
            stringResource(R.string.qr_code_description),
            Modifier.fillMaxWidth()
                .aspectRatio(1f)
                .background(androidx.compose.ui.graphics.Color.White)
                .padding(8.dp),
        )
    else Text(stringResource(R.string.qr_generation_error))
}

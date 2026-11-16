package ru.pogman.questplatform.data

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

fun questQrModules(token: String): List<List<Boolean>> {
    val matrix =
        QRCodeWriter()
            .encode(
                token,
                BarcodeFormat.QR_CODE,
                0,
                0,
                mapOf(
                    EncodeHintType.CHARACTER_SET to "UTF-8",
                    EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
                    EncodeHintType.MARGIN to 4,
                ),
            )
    return List(matrix.height) { row -> List(matrix.width) { col -> matrix[col, row] } }
}

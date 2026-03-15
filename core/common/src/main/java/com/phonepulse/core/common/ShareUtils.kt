package com.phonepulse.core.common

import android.content.Context
import android.content.Intent

object ShareUtils {
    fun shareCertificate(
        context: Context,
        certId: String,
        deviceModel: String,
        score: Int,
        grade: String
    ) {
        val text = """
            PhonePulse - Certificate

            Device: $deviceModel
            Overall score: $score/100 (Grade $grade)

            Certificate: $certId

            Scan the QR in PhonePulse to verify details.
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "PhonePulse: Certificate $certId")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share certificate"))
    }
}

package ru.pogman.questplatform.data

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.pogman.questplatform.domain.QuestException
import ru.pogman.questplatform.domain.QuestFailure

class NfcTagStore {
    suspend fun read(tag: Tag): String =
        withContext(Dispatchers.IO) {
            val ndef = Ndef.get(tag) ?: throw QuestException(QuestFailure.NFC_NO_TEXT)
            try {
                ndef.connect()
                val message = ndef.ndefMessage ?: throw QuestException(QuestFailure.NFC_NO_TEXT)
                message.records.firstNotNullOfOrNull { record ->
                    if (
                        record.tnf == NdefRecord.TNF_WELL_KNOWN &&
                            record.type.contentEquals(NdefRecord.RTD_TEXT)
                    ) {
                        decodeNdefText(record.payload)?.takeIf(String::isNotBlank)
                    } else record.toUri()?.toString()
                } ?: throw QuestException(QuestFailure.NFC_NO_TEXT)
            } catch (_: IOException) {
                throw QuestException(QuestFailure.NFC_IO)
            } catch (_: android.nfc.FormatException) {
                throw QuestException(QuestFailure.NFC_IO)
            } finally {
                runCatching { ndef.close() }
            }
        }

    suspend fun write(tag: Tag, token: String) =
        withContext(Dispatchers.IO) {
            val message = NdefMessage(arrayOf(NdefRecord.createTextRecord("en", token)))
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                try {
                    ndef.connect()
                    if (!ndef.isWritable) throw QuestException(QuestFailure.NFC_READ_ONLY)
                    if (ndef.maxSize < message.toByteArray().size)
                        throw QuestException(QuestFailure.NFC_TOO_SMALL)
                    ndef.writeNdefMessage(message)
                } catch (_: IOException) {
                    throw QuestException(QuestFailure.NFC_IO)
                } catch (_: android.nfc.FormatException) {
                    throw QuestException(QuestFailure.NFC_IO)
                } finally {
                    runCatching { ndef.close() }
                }
            } else {
                val formatable =
                    NdefFormatable.get(tag) ?: throw QuestException(QuestFailure.NFC_NO_TEXT)
                try {
                    formatable.connect()
                    formatable.format(message)
                } catch (_: IOException) {
                    throw QuestException(QuestFailure.NFC_IO)
                } catch (_: android.nfc.FormatException) {
                    throw QuestException(QuestFailure.NFC_IO)
                } finally {
                    runCatching { formatable.close() }
                }
            }
        }
}

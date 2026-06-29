package com.example.util

import android.content.Context
import android.net.Uri
import java.io.InputStream
import java.util.zip.ZipInputStream
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

object DocxParser {
    fun extractText(context: Context, uri: Uri): String {
        var inputStream: InputStream? = null
        var zipInputStream: ZipInputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(uri) ?: return "Hujjatni ochib bo'lmadi"
            zipInputStream = ZipInputStream(inputStream)
            var entry = zipInputStream.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    return parseDocumentXml(zipInputStream)
                }
                entry = zipInputStream.nextEntry
            }
        } catch (e: Exception) {
            return "Xatolik yuz berdi: ${e.localizedMessage}"
        } finally {
            try {
                zipInputStream?.close()
                inputStream?.close()
            } catch (ex: Exception) {}
        }
        return "Word hujjati kontenti topilmadi yoki fayl formati xato"
    }

    private fun parseDocumentXml(inputStream: InputStream): String {
        val result = StringBuilder()
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(inputStream, "UTF-8")

        var eventType = parser.eventType
        var insideParagraph = false
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "p") {
                        insideParagraph = true
                    } else if (parser.name == "t" && insideParagraph) {
                        val text = parser.nextText()
                        result.append(text)
                    } else if (parser.name == "br") {
                        result.append("\n")
                    } else if (parser.name == "cr") {
                        result.append("\n")
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "p") {
                        result.append("\n\n")
                        insideParagraph = false
                    }
                }
            }
            eventType = parser.next()
        }
        return result.toString().trim()
    }
}

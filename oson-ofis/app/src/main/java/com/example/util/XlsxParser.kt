package com.example.util

import android.content.Context
import android.net.Uri
import java.io.InputStream
import java.util.zip.ZipInputStream
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

object XlsxParser {
    fun parseExcel(context: Context, uri: Uri): List<List<String>> {
        var inputStream: InputStream? = null
        var zipInputStream: ZipInputStream? = null
        val sharedStrings = mutableListOf<String>()
        var sheetBytes: ByteArray? = null

        try {
            inputStream = context.contentResolver.openInputStream(uri) ?: return emptyList()
            zipInputStream = ZipInputStream(inputStream)
            var entry = zipInputStream.nextEntry
            while (entry != null) {
                if (entry.name == "xl/sharedStrings.xml") {
                    sharedStrings.addAll(parseSharedStrings(zipInputStream))
                } else if (entry.name == "xl/worksheets/sheet1.xml") {
                    sheetBytes = zipInputStream.readBytes()
                }
                entry = zipInputStream.nextEntry
            }
            zipInputStream.close()
            inputStream.close()

            if (sheetBytes != null) {
                return parseSheetXml(sheetBytes.inputStream(), sharedStrings)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                zipInputStream?.close()
                inputStream?.close()
            } catch (ex: Exception) { }
        }
        return emptyList()
    }

    private fun parseSharedStrings(inputStream: InputStream): List<String> {
        val strings = mutableListOf<String>()
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(inputStream, "UTF-8")

        var eventType = parser.eventType
        var insideT = false
        val currentString = StringBuilder()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "t") {
                        insideT = true
                        currentString.setLength(0)
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideT) {
                        currentString.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "t") {
                        strings.add(currentString.toString())
                        insideT = false
                    }
                }
            }
            eventType = parser.next()
        }
        return strings
    }

    private fun parseSheetXml(inputStream: InputStream, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(inputStream, "UTF-8")

        var eventType = parser.eventType
        val currentCells = mutableMapOf<Int, String>()
        var cellColIndex = 0
        var cellType: String? = null
        var insideV = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "row") {
                        currentCells.clear()
                    } else if (parser.name == "c") {
                        val cellRef = parser.getAttributeValue(null, "r") ?: ""
                        cellColIndex = getColumnIndexFromRef(cellRef)
                        cellType = parser.getAttributeValue(null, "t")
                    } else if (parser.name == "v") {
                        insideV = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideV) {
                        val rawValue = parser.text ?: ""
                        val displayValue = if (cellType == "s") {
                            val index = rawValue.toIntOrNull() ?: -1
                            if (index in sharedStrings.indices) sharedStrings[index] else rawValue
                        } else {
                            rawValue
                        }
                        currentCells[cellColIndex] = displayValue
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "v") {
                        insideV = false
                    } else if (parser.name == "row") {
                        if (currentCells.isNotEmpty()) {
                            val maxIndex = currentCells.keys.maxOrNull() ?: 0
                            val rowList = ArrayList<String>()
                            for (i in 0..maxIndex) {
                                rowList.add(currentCells[i] ?: "")
                            }
                            rows.add(rowList)
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return rows
    }

    private fun getColumnIndexFromRef(ref: String): Int {
        var colStr = ""
        for (char in ref) {
            if (char.isLetter()) {
                colStr += char
            } else {
                break
            }
        }
        var index = 0
        for (char in colStr) {
            index = index * 26 + (char.uppercaseChar() - 'A' + 1)
        }
        return index - 1
    }
}

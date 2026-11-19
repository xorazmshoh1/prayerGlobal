package uz.xumoyun.tools.quran

import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/** Tanzil fayli formati xatosi (yo'q atribut, noto'g'ri qator, izchil bo'lmagan ma'lumot). */
class QuranFormatException(message: String) : IllegalArgumentException(message)

/** quran-uthmani.txt dagi bitta qator: `sura|oyat|matn`. [text] — baytma-bayt Tanzil matni. */
data class AyahLine(val sura: Int, val aya: Int, val text: String)

enum class Revelation { MECCAN, MEDINAN }

/** Sura metama'lumoti (quran-data.xml `sura` elementi). */
data class SuraMeta(
    val index: Int,
    val ayaCount: Int,
    val nameArabic: String,
    val nameTranslit: String,
    val revelation: Revelation,
)

/** Pozitsiya: sura va oyat raqami. Tartiblash — sura, keyin oyat. */
data class Position(val sura: Int, val aya: Int) : Comparable<Position> {
    override fun compareTo(other: Position): Int =
        if (sura != other.sura) sura.compareTo(other.sura) else aya.compareTo(other.aya)
}

/** Juz yoki sahifa boshlanishi: [index] (1-based) va boshlanish pozitsiyasi. */
data class Marker(val index: Int, val start: Position)

data class QuranData(
    val suras: List<SuraMeta>,
    val juzStarts: List<Marker>,
    val pageStarts: List<Marker>,
)

/**
 * Tanzil fayllarini o'qiydi. Matnni HECH QACHON o'zgartirmaydi: faqat satr oxiridagi CR (`\r`)
 * va fayl boshidagi BOM olib tashlanadi — bular Tanzil matnining qismi emas.
 * Bo'sh qatorlar va `#` bilan boshlangan izoh qatorlari o'tkazib yuboriladi.
 */
object QuranSource {

    fun parseUthmani(bytes: ByteArray): List<AyahLine> {
        val text = decodeStrict(bytes)
        val result = ArrayList<AyahLine>(6300)
        var lineNo = 0
        for (rawLine in text.split('\n')) {
            lineNo++
            val line = if (rawLine.endsWith('\r')) rawLine.dropLast(1) else rawLine
            if (line.isEmpty() || line.startsWith('#')) continue
            val parts = line.split('|', limit = 3)
            if (parts.size != 3) throw QuranFormatException("uthmani: $lineNo-qator 'sura|oyat|matn' formatida emas")
            val sura = parts[0].toIntOrNull() ?: throw QuranFormatException("uthmani: $lineNo-qator, sura raqami noto'g'ri: '${parts[0]}'")
            val aya = parts[1].toIntOrNull() ?: throw QuranFormatException("uthmani: $lineNo-qator, oyat raqami noto'g'ri: '${parts[1]}'")
            val body = parts[2]
            if (body.isEmpty()) throw QuranFormatException("uthmani: $lineNo-qator, matn bo'sh")
            result.add(AyahLine(sura, aya, body))
        }
        if (result.isEmpty()) throw QuranFormatException("uthmani: birorta oyat topilmadi")
        return result
    }

    fun parseData(xml: ByteArray): QuranData {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true)
        }
        val doc = factory.newDocumentBuilder().parse(ByteArrayInputStream(xml))
        val suras = doc.getElementsByTagName("sura").toElements().map { el ->
            SuraMeta(
                index = el.intAttr("index", "sura"),
                ayaCount = el.intAttr("ayas", "sura"),
                nameArabic = el.strAttr("name", "sura"),
                nameTranslit = el.strAttr("tname", "sura"),
                revelation = when (val t = el.strAttr("type", "sura")) {
                    "Meccan" -> Revelation.MECCAN
                    "Medinan" -> Revelation.MEDINAN
                    else -> throw QuranFormatException("quran-data: sura type noma'lum: '$t'")
                },
            )
        }.sortedBy { it.index }
        return QuranData(
            suras = suras,
            juzStarts = markers(doc.getElementsByTagName("juz"), "juz"),
            pageStarts = markers(doc.getElementsByTagName("page"), "page"),
        )
    }

    private fun markers(nodes: org.w3c.dom.NodeList, tag: String): List<Marker> =
        nodes.toElements().map { el ->
            Marker(
                index = el.intAttr("index", tag),
                start = Position(el.intAttr("sura", tag), el.intAttr("aya", tag)),
            )
        }.sortedBy { it.index }

    private fun org.w3c.dom.NodeList.toElements(): List<Element> =
        (0 until length).mapNotNull { item(it) as? Element }

    private fun Element.strAttr(name: String, tag: String): String =
        if (hasAttribute(name)) getAttribute(name) else throw QuranFormatException("quran-data: <$tag> da '$name' atributi yo'q")

    private fun Element.intAttr(name: String, tag: String): Int =
        strAttr(name, tag).toIntOrNull() ?: throw QuranFormatException("quran-data: <$tag $name> butun son emas")

    /** Baytlarni qat'iy UTF-8 sifatida dekodlaydi; noto'g'ri bayt bo'lsa xato beradi. */
    private fun decodeStrict(bytes: ByteArray): String {
        val buf = ByteBuffer.wrap(bytes)
        val decoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        val chars = try {
            decoder.decode(buf)
        } catch (e: java.nio.charset.CharacterCodingException) {
            throw QuranFormatException("uthmani: fayl UTF-8 emas (${e.javaClass.simpleName})")
        }
        val s = chars.toString()
        return if (s.startsWith('﻿')) s.substring(1) else s
    }
}

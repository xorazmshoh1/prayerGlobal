package uz.xumoyun.tools.quran

import java.io.File
import java.sql.Connection
import java.sql.DriverManager

/** Yaratilgan bazaning statistikasi (hisobot va testlar uchun). */
data class BuildStats(val surahCount: Int, val ayahCount: Int, val juzCount: Int, val pageCount: Int)

/**
 * quran.db yaratadi (TZ 7-bo'lim sxemasi). Jadvallar Room entity'lari bilan bir xil bo'lishi shart,
 * chunki `createFromAsset` ochilganda Room sxemani tekshiradi.
 *
 * room_master_table: [roomIdentityHash] berilsa yoziladi (Room `schemas/.../N.json` dagi `identityHash`).
 * Berilmasa Room ochishda xato beradi — bu holat chiqish oldidan ogohlantiriladi.
 */
object QuranDbBuilder {

    const val ROOM_DB_VERSION_DEFAULT = 1

    fun build(
        ayahs: List<AyahLine>,
        data: QuranData,
        out: File,
        roomIdentityHash: String? = null,
        dbVersion: Int = ROOM_DB_VERSION_DEFAULT,
    ): BuildStats {
        val plan = validateAndPlan(ayahs, data)
        out.parentFile?.mkdirs()
        if (out.exists() && !out.delete()) throw IllegalStateException("Eski faylni o'chirib bo'lmadi: ${out.path}")

        DriverManager.getConnection("jdbc:sqlite:${out.absolutePath}").use { conn ->
            conn.autoCommit = false
            createSchema(conn)
            insertSurahs(conn, plan.surahs)
            insertAyahs(conn, plan.ayahs)
            createIndexes(conn)
            if (roomIdentityHash != null) {
                conn.createStatement().use { st ->
                    st.executeUpdate("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
                    st.executeUpdate("DELETE FROM room_master_table")
                }
                conn.prepareStatement("INSERT INTO room_master_table (id, identity_hash) VALUES (42, ?)").use {
                    it.setString(1, roomIdentityHash)
                    it.executeUpdate()
                }
            }
            // PRAGMA user_version tranzaksiya ichida yoziladi (commit'dan oldin), aks holda saqlanmaydi
            conn.createStatement().use { it.executeUpdate("PRAGMA user_version = $dbVersion") }
            conn.commit()
        }
        return BuildStats(plan.surahs.size, plan.ayahs.size, data.juzStarts.size, data.pageStarts.size)
    }

    internal data class PlannedAyah(
        val globalId: Int, val surahId: Int, val numberInSurah: Int,
        val juz: Int, val page: Int, val text: String,
    )

    internal data class PlannedSurah(
        val id: Int, val nameArabic: String, val nameTranslit: String,
        val ayahCount: Int, val revelation: Revelation, val startAyahGlobal: Int,
    )

    internal class Plan(val surahs: List<PlannedSurah>, val ayahs: List<PlannedAyah>)

    /**
     * Tekshiruvlar: suralar 1..N ketma-ket va takrorsiz; har sura oyatlari 1..ayas ketma-ket;
     * oyatlar soni quran-data dagi yig'indiga teng; juz/sahifa boshlanishi topiladigan bo'lishi.
     */
    internal fun validateAndPlan(ayahs: List<AyahLine>, data: QuranData): Plan {
        if (data.suras.isEmpty()) throw QuranFormatException("quran-data: suralar yo'q")
        val suras = data.suras.sortedBy { it.index }
        suras.forEachIndexed { i, s ->
            if (s.index != i + 1) {
                throw QuranFormatException("quran-data: sura indekslari 1..N ketma-ket emas (kutilgan ${i + 1}, topilgan ${s.index})")
            }
            if (s.ayaCount <= 0) throw QuranFormatException("quran-data: sura ${s.index} oyatlar soni 0 yoki manfiy")
        }
        val totalExpected = suras.sumOf { it.ayaCount }
        if (ayahs.size != totalExpected) {
            throw QuranFormatException("Oyatlar soni mos emas: uthmani=${ayahs.size}, quran-data yig'indisi=$totalExpected")
        }
        val juzMarkers = data.juzStarts.sortedBy { it.index }
        val pageMarkers = data.pageStarts.sortedBy { it.index }
        if (juzMarkers.isEmpty()) throw QuranFormatException("quran-data: juz ma'lumoti yo'q")
        if (pageMarkers.isEmpty()) throw QuranFormatException("quran-data: sahifa ma'lumoti yo'q")

        val surahPlans = ArrayList<PlannedSurah>(suras.size)
        var global = 0
        for (s in suras) {
            surahPlans.add(PlannedSurah(s.index, s.nameArabic, s.nameTranslit, s.ayaCount, s.revelation, global + 1))
            global += s.ayaCount
        }

        val ayahPlans = ArrayList<PlannedAyah>(ayahs.size)
        var curSura = 0
        var curAya = 0
        ayahs.forEachIndexed { idx, line ->
            val meta = suras.getOrNull(line.sura - 1)
                ?: throw QuranFormatException("uthmani: ${idx + 1}-qator, sura ${line.sura} quran-data da yo'q")
            val expectedAya: Int
            if (line.sura == curSura) {
                expectedAya = curAya + 1
            } else {
                val previousComplete = curSura == 0 || curAya == suras[curSura - 1].ayaCount
                if (line.sura != curSura + 1 || !previousComplete) {
                    throw QuranFormatException("uthmani: ${idx + 1}-qator ${line.sura}:${line.aya} kutilmagan joyda (oldingi ${curSura}:${curAya})")
                }
                expectedAya = 1
            }
            if (line.aya != expectedAya) {
                throw QuranFormatException("uthmani: ${line.sura}:${line.aya} o'rniga ${line.sura}:$expectedAya kutilgan")
            }
            if (line.aya > meta.ayaCount) throw QuranFormatException("uthmani: sura ${line.sura} da ortiqcha oyat ${line.aya}")
            val pos = Position(line.sura, line.aya)
            ayahPlans.add(
                PlannedAyah(
                    globalId = idx + 1,
                    surahId = line.sura,
                    numberInSurah = line.aya,
                    juz = markerFor(juzMarkers, pos, "juz"),
                    page = markerFor(pageMarkers, pos, "sahifa"),
                    text = line.text,
                ),
            )
            curSura = line.sura
            curAya = line.aya
        }
        return Plan(surahPlans, ayahPlans)
    }

    private fun markerFor(markers: List<Marker>, pos: Position, what: String): Int {
        var found: Marker? = null
        for (m in markers) if (m.start <= pos) found = m
        return found?.index ?: throw QuranFormatException("${pos.sura}:${pos.aya} uchun $what topilmadi")
    }

    private fun createSchema(conn: Connection) {
        conn.createStatement().use { st ->
            st.executeUpdate(
                "CREATE TABLE surah (" +
                    "id INTEGER NOT NULL PRIMARY KEY, " +
                    "nameArabic TEXT NOT NULL, " +
                    "nameTranslit TEXT NOT NULL, " +
                    "ayahCount INTEGER NOT NULL, " +
                    "revelation TEXT NOT NULL, " +
                    "startAyahGlobal INTEGER NOT NULL)",
            )
            st.executeUpdate(
                "CREATE TABLE ayah (" +
                    "globalId INTEGER NOT NULL PRIMARY KEY, " +
                    "surahId INTEGER NOT NULL, " +
                    "numberInSurah INTEGER NOT NULL, " +
                    "juz INTEGER NOT NULL, " +
                    "page INTEGER NOT NULL, " +
                    "textUthmani TEXT NOT NULL)",
            )
        }
    }

    private fun insertSurahs(conn: Connection, surahs: List<PlannedSurah>) {
        conn.prepareStatement(
            "INSERT INTO surah (id, nameArabic, nameTranslit, ayahCount, revelation, startAyahGlobal) VALUES (?, ?, ?, ?, ?, ?)",
        ).use { ps ->
            for (s in surahs) {
                ps.setInt(1, s.id)
                ps.setString(2, s.nameArabic)
                ps.setString(3, s.nameTranslit)
                ps.setInt(4, s.ayahCount)
                ps.setString(5, s.revelation.name)
                ps.setInt(6, s.startAyahGlobal)
                ps.addBatch()
            }
            ps.executeBatch()
        }
    }

    private fun insertAyahs(conn: Connection, ayahs: List<PlannedAyah>) {
        conn.prepareStatement(
            "INSERT INTO ayah (globalId, surahId, numberInSurah, juz, page, textUthmani) VALUES (?, ?, ?, ?, ?, ?)",
        ).use { ps ->
            for (a in ayahs) {
                ps.setInt(1, a.globalId)
                ps.setInt(2, a.surahId)
                ps.setInt(3, a.numberInSurah)
                ps.setInt(4, a.juz)
                ps.setInt(5, a.page)
                ps.setString(6, a.text)
                ps.addBatch()
            }
            ps.executeBatch()
        }
    }

    private fun createIndexes(conn: Connection) {
        conn.createStatement().use { st ->
            st.executeUpdate("CREATE INDEX index_ayah_surahId ON ayah(surahId)")
            st.executeUpdate("CREATE INDEX index_ayah_juz ON ayah(juz)")
            st.executeUpdate("CREATE INDEX index_ayah_page ON ayah(page)")
        }
    }
}

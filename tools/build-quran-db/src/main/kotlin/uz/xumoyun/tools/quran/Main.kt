package uz.xumoyun.tools.quran

import java.io.File
import kotlin.system.exitProcess

private const val USAGE = """Foydalanish:
  gradle run --args="--uthmani <quran-uthmani.txt> --data <quran-data.xml> --out <quran.db> [--room-schema <N.json> | --room-hash <hex>] [--db-version 1]"

  --uthmani  Tanzil matn fayli (sura|oyat|matn)
  --data     Tanzil metama'lumot fayli (quran-data.xml)
  --out      Chiqish fayli (odatda feature-hifz/src/main/assets/quran.db)
  --room-schema  Room eksport qilgan sxema JSON (identityHash o'qiladi)
  --room-hash    Room identityHash qiymatini qo'lda berish
"""

private val identityHashRegex = Regex("\"identityHash\"\\s*:\\s*\"([0-9a-fA-F]+)\"")

fun main(args: Array<String>) {
    // Konsol kodlashi (POSIX locale'da) o'zbekcha va arabcha matnni buzmasligi uchun
    System.setOut(java.io.PrintStream(java.io.FileOutputStream(java.io.FileDescriptor.out), true, "UTF-8"))
    System.setErr(java.io.PrintStream(java.io.FileOutputStream(java.io.FileDescriptor.err), true, "UTF-8"))
    if (args.isEmpty() || args.any { it == "--help" || it == "-h" }) {
        println(USAGE)
        exitProcess(if (args.isEmpty()) 2 else 0)
    }
    try {
        run(parseArgs(args))
    } catch (e: QuranFormatException) {
        System.err.println("Xato (Tanzil ma'lumoti): ${e.message}")
        exitProcess(1)
    } catch (e: IllegalArgumentException) {
        System.err.println("Xato: ${e.message}\n\n$USAGE")
        exitProcess(2)
    }
}

internal data class Options(
    val uthmani: File,
    val data: File,
    val out: File,
    val roomHash: String?,
    val dbVersion: Int,
)

internal fun parseArgs(args: Array<String>): Options {
    val map = HashMap<String, String>()
    var i = 0
    while (i < args.size) {
        val key = args[i]
        require(key.startsWith("--")) { "noma'lum argument: $key" }
        require(i + 1 < args.size) { "$key uchun qiymat kerak" }
        map[key] = args[i + 1]
        i += 2
    }
    val known = setOf("--uthmani", "--data", "--out", "--room-schema", "--room-hash", "--db-version")
    map.keys.firstOrNull { it !in known }?.let { throw IllegalArgumentException("noma'lum opsiya: $it") }
    val uthmani = File(map["--uthmani"] ?: throw IllegalArgumentException("--uthmani berilmagan"))
    val data = File(map["--data"] ?: throw IllegalArgumentException("--data berilmagan"))
    val out = File(map["--out"] ?: throw IllegalArgumentException("--out berilmagan"))
    require(!(map.containsKey("--room-schema") && map.containsKey("--room-hash"))) {
        "--room-schema va --room-hash birga berilmaydi"
    }
    val hash = when {
        map.containsKey("--room-hash") -> map.getValue("--room-hash")
        map.containsKey("--room-schema") -> readIdentityHash(File(map.getValue("--room-schema")))
        else -> null
    }
    val version = map["--db-version"]?.toIntOrNull() ?: QuranDbBuilder.ROOM_DB_VERSION_DEFAULT
    return Options(uthmani, data, out, hash, version)
}

internal fun readIdentityHash(schema: File): String {
    require(schema.isFile) { "Room sxema fayli topilmadi: ${schema.path}" }
    val match = identityHashRegex.find(schema.readText())
        ?: throw IllegalArgumentException("${schema.path} da identityHash topilmadi")
    return match.groupValues[1]
}

private fun run(o: Options) {
    requireFile(o.uthmani, "quran-uthmani.txt")
    requireFile(o.data, "quran-data.xml")
    val ayahs = QuranSource.parseUthmani(o.uthmani.readBytes())
    val data = QuranSource.parseData(o.data.readBytes())
    if (o.roomHash == null) {
        System.err.println(
            "OGOHLANTIRISH: --room-schema/--room-hash berilmadi. room_master_table yozilmaydi, " +
                "Room quran.db ni ochishda xato beradi. Room sxemasini (feature-hifz/schemas) yarating va qayta ishga tushiring.",
        )
    }
    val stats = QuranDbBuilder.build(ayahs, data, o.out, o.roomHash, o.dbVersion)
    println(
        "quran.db yaratildi: ${o.out.absolutePath} — ${stats.surahCount} sura, ${stats.ayahCount} oyat, " +
            "${stats.juzCount} juz, ${stats.pageCount} sahifa",
    )
}

private fun requireFile(f: File, label: String) {
    if (!f.isFile) {
        System.err.println("Xato: $label fayli topilmadi: ${f.path}\nTanzil'dan yuklab oling: https://tanzil.net/download")
        exitProcess(1)
    }
}

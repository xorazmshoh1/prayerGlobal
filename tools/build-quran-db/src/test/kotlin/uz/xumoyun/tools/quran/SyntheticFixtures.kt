package uz.xumoyun.tools.quran

/**
 * SINTETIK format fixture'lari. Bu YO'Q Qur'on matni: har bir oyat matni "test-S-O" kabi belgilar.
 * Maqsad — parser, validatsiya va sxemani tekshirish; haqiqiy matn testlari RealTanzilTest da.
 *
 * Tuzilma: sura 1 — 3 oyat, sura 2 — 2 oyat, sura 3 — 1 oyat (jami 6).
 * Juz: 1 (1:1 dan), 2 (2:1 dan). Sahifa: 1 (1:1 dan), 2 (2:1 dan), 3 (3:1 dan).
 */
object SyntheticFixtures {

    const val UTHMANI = "# SINTETIK test fayli — haqiqiy Qur'on emas\n" +
        "1|1|test-1-1\n" +
        "1|2|test-1-2\n" +
        "1|3|test-1-3\n" +
        "2|1|test-2-1\r\n" +
        "2|2|test-2-2\r\n" +
        "3|1|test-3-1\n"

    const val DATA = """<?xml version="1.0" encoding="utf-8"?>
<quran-data>
  <suras>
    <sura index="1" ayas="3" start="0" name="sura-ism-1" tname="Test-One" ename="T1" type="Meccan" order="5" rukus="1"/>
    <sura index="2" ayas="2" start="3" name="sura-ism-2" tname="Test-Two" ename="T2" type="Medinan" order="87" rukus="1"/>
    <sura index="3" ayas="1" start="5" name="sura-ism-3" tname="Test-Three" ename="T3" type="Meccan" order="7" rukus="1"/>
  </suras>
  <juzs>
    <juz index="1" sura="1" aya="1"/>
    <juz index="2" sura="2" aya="1"/>
  </juzs>
  <pages>
    <page index="1" sura="1" aya="1"/>
    <page index="2" sura="2" aya="1"/>
    <page index="3" sura="3" aya="1"/>
  </pages>
</quran-data>
"""

    fun uthmaniBytes(): ByteArray = ("﻿" + UTHMANI).toByteArray(Charsets.UTF_8)
    fun dataBytes(): ByteArray = DATA.toByteArray(Charsets.UTF_8)
}

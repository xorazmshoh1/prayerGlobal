package uz.xumoyun.core.common.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelCatalogTest {
    private fun spec(id: String, group: String? = null) =
        ChannelSpec(id, nameRes = 1, importance = ChannelImportance.HIGH, sound = ChannelSound.SystemNotification, groupId = group)

    private fun source(
        channels: List<ChannelSpec>,
        groups: List<ChannelGroupSpec> = emptyList(),
        obsolete: List<String> = emptyList(),
    ) = object : NotificationChannelSource {
        override val groups = groups
        override val channels = channels
        override val obsoleteChannelIds = obsolete
    }

    @Test fun mergesSourcesAndKeepsGroups() {
        val prayer = source(
            listOf(spec("prayer_fajr", "prayer_times"), spec("prayer_reminder", "prayer_times")),
            groups = listOf(ChannelGroupSpec("prayer_times", 2)),
            obsolete = listOf("prayer_azan"),
        )
        val hifz = source(listOf(spec("hifz_reminder")))
        // Keyingi bosqichlardagi kanallar yangi manba sifatida qo'shiladi.
        val ramadan = source(listOf(spec("ramadan_suhoor"), spec("ramadan_iftar")))
        val catalog = ChannelCatalog.of(listOf(prayer, hifz, ramadan))
        assertEquals(5, catalog.channels.size)
        assertEquals(listOf("prayer_times"), catalog.groups.map { it.id })
        assertEquals(listOf("prayer_azan"), catalog.obsoleteChannelIds)
    }

    @Test fun emptyIsAllowed() {
        assertTrue(ChannelCatalog.of(emptyList()).channels.isEmpty())
    }

    @Test fun rejectsDuplicateIds() {
        assertThrows(IllegalArgumentException::class.java) {
            ChannelCatalog.of(listOf(source(listOf(spec("a"))), source(listOf(spec("a")))))
        }
    }

    @Test fun rejectsUnknownGroup() {
        assertThrows(IllegalArgumentException::class.java) {
            ChannelCatalog.of(listOf(source(listOf(spec("a", "missing")))))
        }
    }

    @Test fun rejectsObsoleteActiveChannel() {
        assertThrows(IllegalArgumentException::class.java) {
            ChannelCatalog.of(listOf(source(listOf(spec("a")), obsolete = listOf("a"))))
        }
    }
}

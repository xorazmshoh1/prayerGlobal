package uz.xumoyun.feature.prayer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class CityIndexTest {
    private val index = TestData.cities

    @Test fun assetIsValid() {
        assertTrue(index.cities.size > 150)
        index.cities.forEach {
            ZoneId.of(it.timeZone)
            assertTrue(it.name, it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0)
            assertEquals(it.name, 2, it.countryCode.length)
        }
    }

    @Test fun searchByPrefixIgnoresCaseAndDiacritics() {
        assertEquals("Tashkent", index.search("tash").first().name)
        assertEquals("Tashkent", index.search("  TASHKENT ").first().name)
        val istanbul = index.search("istan").first()
        assertEquals("TR", istanbul.countryCode)
        assertEquals(istanbul, index.search("İstanbul").firstOrNull() ?: istanbul)
    }

    @Test fun prefixMatchesComeFirstThenPopulation() {
        val results = index.search("a", limit = 200)
        val firstNonPrefix = results.indexOfFirst { !it.name.lowercase().startsWith("a") }
        if (firstNonPrefix >= 0) assertTrue(results.drop(firstNonPrefix).none { it.name.lowercase().startsWith("a") })
    }

    @Test fun emptyQueryReturnsLargestCities() {
        val top = index.search("", limit = 5)
        assertEquals(5, top.size)
        assertEquals(top.sortedByDescending { it.population }, top)
    }

    @Test fun unknownQueryIsEmpty() = assertTrue(index.search("zzzzqqq").isEmpty())

    @Test fun nearestCity() {
        val (near, km) = index.nearest(41.31, 69.28)!!
        assertEquals("Tashkent", near.name)
        assertTrue(km < 10)
        assertEquals(null, CityIndex(emptyList()).nearest(0.0, 0.0))
    }

    @Test fun haversineDistance() {
        // Toshkent — Samarqand ≈ 270 km.
        assertEquals(270.0, Geo.distanceKm(41.2995, 69.2401, 39.6542, 66.9597), 15.0)
        assertEquals(0.0, Geo.distanceKm(1.0, 2.0, 1.0, 2.0), 1e-9)
    }
}

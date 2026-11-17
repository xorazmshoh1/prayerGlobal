package uz.xumoyun.core.ads

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class AdPoliciesTest {
    @Test fun interstitialRespectsMinInterval() {
        val p = InterstitialPolicy()
        assertTrue(p.canShow(0))
        p.recordShown(0)
        assertFalse(p.canShow(2 * 60 * 1000L))
        assertTrue(p.canShow(3 * 60 * 1000L))
    }

    @Test fun interstitialMaxThreePerSession() {
        val p = InterstitialPolicy()
        var t = 0L
        repeat(3) {
            assertTrue(p.canShow(t))
            p.recordShown(t)
            t += 10 * 60 * 1000L
        }
        assertFalse(p.canShow(t))
        p.resetSession()
        assertTrue(p.canShow(t))
    }

    @Test fun appOpenOncePerDay() {
        val now = Instant.parse("2026-11-17T10:00:00Z")
        assertTrue(AppOpenPolicy.canShow(null, now, ZoneOffset.UTC))
        assertFalse(AppOpenPolicy.canShow(LocalDate.parse("2026-11-17"), now, ZoneOffset.UTC))
        assertTrue(AppOpenPolicy.canShow(LocalDate.parse("2026-11-16"), now, ZoneOffset.UTC))
    }
}

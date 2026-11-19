package uz.xumoyun.core.common.crash

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReportGateTest {
    private class Recorder : CrashSink {
        val seen = mutableListOf<Throwable>()
        override fun record(throwable: Throwable) {
            seen += throwable
        }
    }

    @Test fun enabledByDefaultAndRecords() {
        val rec = Recorder()
        val gate = CrashReportGate(rec)
        val boom = IllegalStateException("boom")
        assertTrue(gate.report(boom))
        assertEquals(listOf<Throwable>(boom), rec.seen)
    }

    @Test fun disabledGateSendsNothing() {
        val rec = Recorder()
        val gate = CrashReportGate(rec)
        gate.enabled = false
        assertFalse(gate.report(RuntimeException("x")))
        assertTrue(rec.seen.isEmpty())
    }

    @Test fun sinkFailureNeverPropagates() {
        val gate = CrashReportGate(object : CrashSink {
            override fun record(throwable: Throwable) = throw IllegalArgumentException("sink down")
        })
        assertFalse(gate.report(RuntimeException("x")))
    }

    @Test fun noopSinkAcceptsReports() {
        assertTrue(CrashReportGate(NoopCrashSink).report(RuntimeException("x")))
    }
}

package com.machiav3lli.backup.utils

import com.machiav3lli.backup.data.dbs.entity.Schedule
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.Calendar

class ScheduleUtilsTest {
    @Test
    fun `simple correct 7-days interval`() {
        val timePlaced = cal(2026, Calendar.NOVEMBER, 21, 10).timeInMillis
        // same day but later
        val now = cal(2026, Calendar.NOVEMBER, 21, 11).timeInMillis

        val (timeToRun, timeLeft) = calcRuntimeDiff(
            schedule(
                interval = 7,
                timePlaced = timePlaced,
            ),
            nowMs = now,
            fakeMin = 0,
        )

        // next run
        val expected = cal(2026, Calendar.NOVEMBER, 28, 14)

        Assertions.assertEquals(expected.timeInMillis, timeToRun)
        Assertions.assertEquals(expected.timeInMillis - now, timeLeft)
    }

    @Test
    fun `schedule does not shift on advanced now`() {
        val timePlaced = cal(2026, Calendar.NOVEMBER, 21, 10).timeInMillis
        val sched = schedule(
            interval = 7,
            timePlaced = timePlaced,
        )

        // next run
        val run1 = calcRuntimeDiff(
            sched,
            nowMs = cal(2026, Calendar.NOVEMBER, 21, 11).timeInMillis,
            fakeMin = 0
        ).first
        // next run from a later day
        val run2 = calcRuntimeDiff(
            sched,
            nowMs = cal(2026, Calendar.NOVEMBER, 23, 11).timeInMillis,
            fakeMin = 0
        ).first

        val expected = cal(2026, Calendar.NOVEMBER, 28, 14)
        Assertions.assertEquals(expected.timeInMillis, run1)
        Assertions.assertEquals(run1, run2)
    }

    @Test
    fun `schedule adds one day, when interval missed - same day`() {
        val timePlaced = cal(2026, Calendar.NOVEMBER, 21, 10).timeInMillis
        // past first run time
        val now = cal(2026, Calendar.DECEMBER, 10, 11).timeInMillis

        val timeToRun = calcRuntimeDiff(
            schedule(
                interval = 7,
                timePlaced = timePlaced,
            ),
            nowMs = now,
            fakeMin = 0,
        ).first

        val expected = cal(2026, Calendar.DECEMBER, 10, 14)
        Assertions.assertEquals(expected.timeInMillis, timeToRun)
    }

    @Test
    fun `schedule adds one day, when interval missed - next day`() {
        val timePlaced = cal(2026, Calendar.NOVEMBER, 21, 10).timeInMillis
        // past first run time
        val now = cal(2026, Calendar.DECEMBER, 10, 19).timeInMillis

        val timeToRun = calcRuntimeDiff(
            schedule(
                interval = 7,
                timePlaced = timePlaced,
            ),
            nowMs = now,
            fakeMin = 0,
        ).first

        val expected = cal(2026, Calendar.DECEMBER, 11, 14)
        Assertions.assertEquals(expected.timeInMillis, timeToRun)
    }

    // HELPERS

    private fun schedule(
        timePlaced: Long,
        interval: Int = 7,
        timeHour: Int = 14,
    ): Schedule =
        Schedule(
            id = 1,
            enabled = true,
            name = "test",
            timeHour = timeHour,
            timeMinute = 0,
            interval = interval,
            timePlaced = timePlaced,
        )

    private fun cal(year: Int, month: Int, day: Int, hour: Int): Calendar {
        val c = Calendar.getInstance()
        c.clear()
        c.set(year, month, day, hour, 0, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c
    }
}

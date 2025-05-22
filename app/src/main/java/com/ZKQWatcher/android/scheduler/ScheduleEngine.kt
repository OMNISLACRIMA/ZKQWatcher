// scheduler/ScheduleEngine.kt
package com.ZKQWatcher.android.scheduler

import com.ZKQWatcher.android.model.Account
import kotlin.random.Random

object ScheduleEngine {

    data class Segment(val accId: Int, val startMin: Int, val endMin: Int)

    private const val DAY_START = 10 * 60   // 10:00
    private const val NIGHT_START = 22 * 60 // 22:00
    private const val MINUTES_IN_DAY = 24 * 60

    /** 每天调用一次，返回全天所有片段（单位分钟） */
    fun buildDaily(
        dayAccounts: List<Account>,
        nightAccounts: List<Account>,
        segmentsPerAcc: Int
    ): List<Segment> {
        val segs = mutableListOf<Segment>()
        segs += fillGroup(dayAccounts, DAY_START, NIGHT_START, segmentsPerAcc)
        segs += fillGroup(nightAccounts, NIGHT_START, DAY_START + MINUTES_IN_DAY, segmentsPerAcc)
        return segs
    }

    private fun fillGroup(
        accs: List<Account>,
        winStart: Int,
        winEnd: Int,
        nSeg: Int
    ): List<Segment> {
        if (accs.isEmpty()) return emptyList()
        val window = winEnd - winStart      // in minutes (could be >1440 for night wrap handled above)
        val taken = mutableListOf<Pair<Int, Int>>()  // existing segments (start,end)
        val result = mutableListOf<Segment>()

        for (acc in accs) {
            val totalMin = (acc.hoursPerDay * 60).toInt()
            repeat(nSeg) { idx ->
                val remaining = totalMin - (idx * totalMin / nSeg)
                val len = if (idx == nSeg - 1) remaining else Random.nextInt(15, remaining / (nSeg - idx) + 1)
                // 随机找位置不重叠
                var attempt = 0
                while (attempt++ < 500) {
                    val start = Random.nextInt(winStart, winEnd - len)
                    val end = start + len
                    if (taken.none { overlap(it.first, it.second, start, end) }) {
                        taken += start to end
                        result += Segment(acc.id, start % MINUTES_IN_DAY, end % MINUTES_IN_DAY)
                        break
                    }
                }
            }
        }
        return result
    }

    private fun overlap(s1: Int, e1: Int, s2: Int, e2: Int) =
        !(e1 <= s2 || e2 <= s1)   // 闭区间外

}

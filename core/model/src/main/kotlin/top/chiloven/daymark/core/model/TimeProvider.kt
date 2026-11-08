package top.chiloven.daymark.core.model

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/**
 * Supplies the current calendar date. Injecting this makes date-sensitive
 * behavior deterministic in tests and lets the app refresh at midnight.
 */
interface TimeProvider {

    fun today(): LocalDate

    fun zoneId(): ZoneId

}

class SystemTimeProvider(
    private val clock: Clock = Clock.systemDefaultZone(),
) : TimeProvider {

    override fun today(): LocalDate =
        LocalDate.now(clock)

    override fun zoneId(): ZoneId =
        clock.zone

}

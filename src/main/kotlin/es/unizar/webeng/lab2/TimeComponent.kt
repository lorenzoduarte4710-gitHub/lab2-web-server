package es.unizar.webeng.lab2

import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * JSON body of `GET /time`.
 *
 * @property time current date and time in [zone], without offset.
 * @property zone time-zone id the time refers to, for example `Europe/Madrid`.
 */
data class TimeDTO(
    val time: LocalDateTime,
    val zone: String,
)

/** Source of the current time. Tests can provide their own implementation to fix the clock. */
interface TimeProvider {
    /** Returns the current date and time in [zone], by default the server zone. */
    fun now(zone: ZoneId = ZoneId.systemDefault()): LocalDateTime
}

/** [TimeProvider] backed by the system clock. */
@Service
class TimeService : TimeProvider {
    override fun now(zone: ZoneId): LocalDateTime = LocalDateTime.now(zone)
}

/** Wraps this time and the [zone] it belongs to into a [TimeDTO]. */
fun LocalDateTime.toDTO(zone: ZoneId): TimeDTO = TimeDTO(time = this, zone = zone.id)

/**
 * Serves `GET /time`. The optional `zone` parameter selects the time zone;
 * an unknown id fails the conversion to [ZoneId] and Spring answers `400 Bad Request`.
 */
@RestController
class TimeController(
    private val service: TimeProvider,
) {
    @GetMapping("/time")
    fun time(
        @RequestParam(name = "zone", required = false) zone: ZoneId?,
    ): TimeDTO {
        val zoneId = zone ?: ZoneId.systemDefault()
        return service.now(zoneId).toDTO(zoneId)
    }
}

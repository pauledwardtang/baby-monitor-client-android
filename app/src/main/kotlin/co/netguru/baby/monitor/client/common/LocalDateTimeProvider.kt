package co.netguru.baby.monitor.client.common

import javax.inject.Inject
import org.threeten.bp.LocalDateTime

class LocalDateTimeProvider @Inject constructor() {
    fun now(): LocalDateTime = LocalDateTime.now()
}

package co.netguru.baby.monitor.client.feature.batterylevel

import co.netguru.baby.monitor.client.feature.firebasenotification.FirebaseNotificationSender
import co.netguru.baby.monitor.client.feature.firebasenotification.NotificationType
import javax.inject.Inject
import timber.log.Timber

class NotifyLowBatteryUseCase @Inject constructor(
    private val notificationSender: FirebaseNotificationSender,
) {
    fun notifyLowBattery(title: String, text: String) =
        notificationSender.broadcastNotificationToFcm(
            title = title,
            text = text,
            notificationType = NotificationType.LOW_BATTERY_NOTIFICATION,
        ).also {
            Timber.d("notifyLowBattery($title, $text)")
        }
}

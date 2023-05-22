package co.netguru.baby.monitor.client.feature.communication.websocket

import com.google.gson.Gson
import com.google.gson.JsonParseException
import javax.inject.Inject
import timber.log.Timber

class MessageParser @Inject constructor(
    private val gson: Gson,
) {
    fun parseWebSocketMessage(event: RxWebSocketClient.Event.Message): Message? {
        return try {
            gson.fromJson(event.message, Message::class.java)
        } catch (e: JsonParseException) {
            Timber.w(e)
            null
        }
    }
}

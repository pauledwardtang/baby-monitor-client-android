package co.netguru.baby.monitor.client.feature.communication.internet

import io.reactivex.Single
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import timber.log.Timber

class CheckInternetConnectionUseCase @Inject constructor() {

    fun hasInternetConnection(): Single<Boolean> {
        return Single.fromCallable {
            var isConnected = false
            try {
                val socket = Socket()
                val socketAddress = InetSocketAddress(GOOGLE_DNS_IP, PORT)

                socket.connect(socketAddress, TIMEOUT_MS)
                socket.close()

                isConnected = true
            } catch (e: IOException) {
                Timber.i(e)
            }
            isConnected
        }
    }

    companion object {
        private const val TIMEOUT_MS = 1500
        private const val PORT = 53
        private const val GOOGLE_DNS_IP = "8.8.8.8"
    }
}

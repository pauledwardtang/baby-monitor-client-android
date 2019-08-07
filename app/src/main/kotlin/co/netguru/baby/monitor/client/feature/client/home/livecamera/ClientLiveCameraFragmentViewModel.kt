package co.netguru.baby.monitor.client.feature.client.home.livecamera

import android.arch.lifecycle.ViewModel
import android.content.Context
import co.netguru.baby.monitor.client.common.view.CustomSurfaceViewRenderer
import co.netguru.baby.monitor.client.data.communication.webrtc.CallState
import co.netguru.baby.monitor.client.feature.communication.webrtc.client.RtcClient
import co.netguru.baby.monitor.client.feature.communication.websocket.CustomWebSocketClient
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.lang.RuntimeException
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

class ClientLiveCameraFragmentViewModel @Inject constructor(): ViewModel() {

    private var currentCall: RtcClient? = null
    val callInProgress = AtomicBoolean(false)
    private val compositeDisposable = CompositeDisposable()

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()
        currentCall?.let(this::callCleanup)
        callInProgress.set(false)
    }

    fun startCall(
            context: Context,
            liveCameraRemoteRenderer: CustomSurfaceViewRenderer,
            client: CustomWebSocketClient,
            listener: (state: CallState) -> Unit
    ) {
        callInProgress.set(true)
        currentCall = RtcClient(client).apply {

            startCall(context, listener)
                    .subscribeOn(Schedulers.newThread())
                    .subscribeBy(
                            onComplete = {
                                Timber.i("Call started")
                            },
                            onError = {
                                callCleanup(this)
                                Timber.e(it, "Error during startCall")

                            }
                    ).addTo(compositeDisposable)
            remoteView = liveCameraRemoteRenderer
        }
    }

    private fun callCleanup(rtcClient: RtcClient) {
            rtcClient.cleanup()
            callInProgress.set(false)
    }
}
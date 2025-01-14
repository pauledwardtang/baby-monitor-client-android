package co.netguru.baby.monitor.client.feature.client.home

import androidx.appcompat.app.AppCompatActivity
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import co.netguru.baby.monitor.RxSchedulersOverrideRule
import co.netguru.baby.monitor.client.data.DataRepository
import co.netguru.baby.monitor.client.data.client.ChildDataEntity
import co.netguru.baby.monitor.client.feature.babynotification.SnoozeNotificationUseCase
import co.netguru.baby.monitor.client.feature.communication.internet.CheckInternetConnectionUseCase
import co.netguru.baby.monitor.client.feature.communication.websocket.Message
import co.netguru.baby.monitor.client.feature.communication.websocket.Message.Companion.RESET_ACTION
import co.netguru.baby.monitor.client.feature.communication.websocket.MessageParser
import co.netguru.baby.monitor.client.feature.communication.websocket.RxWebSocketClient
import co.netguru.baby.monitor.client.feature.voiceAnalysis.VoiceAnalysisUseCase
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import java.net.URI
import junit.framework.TestCase.assertEquals
import org.junit.Rule
import org.junit.Test

class ClientHomeViewModelTest {

    @get:Rule
    val schedulersRule = RxSchedulersOverrideRule()

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val childLiveData: LiveData<ChildDataEntity> = MutableLiveData()
    private val dataRepository: DataRepository = mock {
        on { getChildLiveData() }.doReturn(childLiveData)
        on { getChildData() }.doReturn(Maybe.just(ChildDataEntity(address = "address")))
    }
    private val rxWebSocketClient: RxWebSocketClient = mock()

    private val sendBabyNameUseCase: SendBabyNameUseCase = mock {
        on { streamBabyName(rxWebSocketClient) }.doReturn(Completable.complete())
    }
    private val snoozeNotificationUseCase: SnoozeNotificationUseCase = mock()
    private val checkInternetConnectionUseCase: CheckInternetConnectionUseCase = mock()
    private val restartAppUseCase: RestartAppUseCase = mock()
    private val messageParser: MessageParser = mock()
    private val voiceAnalysisUseCase = mock<VoiceAnalysisUseCase>()
    private val urifier: (address: String) -> URI = mock {
        val uri = mock<URI>()
        on { invoke(any()) }.doReturn(uri)
    }

    private val clientHomeViewModel = ClientHomeViewModel(
        dataRepository,
        sendBabyNameUseCase,
        snoozeNotificationUseCase,
        checkInternetConnectionUseCase,
        restartAppUseCase,
        rxWebSocketClient,
        voiceAnalysisUseCase,
        messageParser,
    )

    @Test
    fun `should handle web socket open connection`() {
        val selectedChildAvailabilityObserver: Observer<Boolean> = mock()
        whenever(rxWebSocketClient.events(any())).doReturn(
            Observable.just(
                RxWebSocketClient.Event.Open,
            ),
        )
        clientHomeViewModel.selectedChildAvailability.observeForever(
            selectedChildAvailabilityObserver,
        )

        clientHomeViewModel.openSocketConnection(urifier)

        verify(selectedChildAvailabilityObserver).onChanged(true)
        verify(sendBabyNameUseCase).streamBabyName(rxWebSocketClient)
        verify(voiceAnalysisUseCase).sendInitialVoiceAnalysisOption(rxWebSocketClient)
    }

    @Test
    fun `should handle web socket closed connection`() {
        val selectedChildAvailabilityObserver: Observer<Boolean> = mock()
        val closeEvent: RxWebSocketClient.Event.Close = mock()
        whenever(rxWebSocketClient.events(any())).doReturn(
            Observable.just(
                closeEvent,
            ),
        )
        clientHomeViewModel.selectedChildAvailability.observeForever(
            selectedChildAvailabilityObserver,
        )

        clientHomeViewModel.openSocketConnection(urifier)

        verify(selectedChildAvailabilityObserver).onChanged(false)
    }

    @Test
    fun `should notify observers about web socket reset action`() {
        val resetActionObserver: Observer<String> = mock()
        val resetAction: RxWebSocketClient.Event.Message = mock()
        whenever(rxWebSocketClient.events(any())).doReturn(
            Observable.just(
                resetAction,
            ),
        )
        whenever(messageParser.parseWebSocketMessage(resetAction)).doReturn(
            Message(action = RESET_ACTION),
        )
        clientHomeViewModel.webSocketAction.observeForever(
            resetActionObserver,
        )

        clientHomeViewModel.openSocketConnection(urifier)

        verify(resetActionObserver).onChanged(
            check {
                assertEquals(RESET_ACTION, it)
            },
        )
    }

    @Test
    fun `should snooze notifications`() {
        val disposable = mock<Disposable>()
        whenever(snoozeNotificationUseCase.snoozeNotifications()).doReturn(disposable)
        clientHomeViewModel.snoozeNotifications()

        verify(snoozeNotificationUseCase).snoozeNotifications()
    }

    @Test
    fun `should check internet connection status`() {
        val internetConnectionObserver: Observer<Boolean> = mock()
        whenever(checkInternetConnectionUseCase.hasInternetConnection()).doReturn(Single.just(true))
        clientHomeViewModel.internetConnectionAvailability.observeForever(
            internetConnectionObserver,
        )

        clientHomeViewModel.checkInternetConnection()

        verify(checkInternetConnectionUseCase).hasInternetConnection()
        verify(internetConnectionObserver).onChanged(true)
    }

    @Test
    fun `should restart app`() {
        val activity: AppCompatActivity = mock()
        whenever(restartAppUseCase.restartApp(activity)).doReturn(Completable.complete())

        clientHomeViewModel.restartApp(activity)

        verify(restartAppUseCase).restartApp(activity)
    }

    @Test
    fun `should handle error while sending baby name on webSocket open`() {
        val errorObserver: Observer<Throwable> = mock()
        whenever(sendBabyNameUseCase.streamBabyName(rxWebSocketClient)).doReturn(
            Completable.error(
                RuntimeException(),
            ),
        )
        whenever(rxWebSocketClient.events(any())).doReturn(
            Observable.just(
                RxWebSocketClient.Event.Open,
            ),
        )
        clientHomeViewModel.errorAction.observeForever(
            errorObserver,
        )

        clientHomeViewModel.openSocketConnection(urifier)

        verify(errorObserver).onChanged(any())
    }

    @Test
    fun `should handle error while sending voice analysis option on webSocket open`() {
        val errorObserver: Observer<Throwable> = mock()

        whenever(voiceAnalysisUseCase.sendInitialVoiceAnalysisOption(rxWebSocketClient)).doReturn(
            Completable.error(
                RuntimeException(),
            ),
        )
        whenever(rxWebSocketClient.events(any())).doReturn(
            Observable.just(
                RxWebSocketClient.Event.Open,
            ),
        )
        clientHomeViewModel.errorAction.observeForever(
            errorObserver,
        )

        clientHomeViewModel.openSocketConnection(urifier)

        verify(errorObserver).onChanged(any())
    }
}

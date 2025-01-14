package co.netguru.baby.monitor.client.feature.communication.pairing

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import co.netguru.baby.monitor.RxSchedulersOverrideRule
import co.netguru.baby.monitor.client.common.LocalDateTimeProvider
import co.netguru.baby.monitor.client.data.DataRepository
import co.netguru.baby.monitor.client.feature.client.home.SendFirebaseTokenUseCase
import co.netguru.baby.monitor.client.feature.communication.websocket.Message
import co.netguru.baby.monitor.client.feature.communication.websocket.MessageParser
import co.netguru.baby.monitor.client.feature.communication.websocket.RxWebSocketClient
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import java.net.URI
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.threeten.bp.LocalDateTime

class PairingUseCaseTest {

    @get:Rule
    val schedulersRule = RxSchedulersOverrideRule()

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val dataRepository: DataRepository = mock()
    private val rxWebSocketClient: RxWebSocketClient = mock()
    private val address = mock<URI>()
    private val message = "message"
    private val pairingCode = "1234"
    private val messageParser: MessageParser = mock()
    private val sendFirebaseTokenUseCase: SendFirebaseTokenUseCase = mock {
        on { sendFirebaseToken(rxWebSocketClient) }.doReturn(Completable.complete())
    }
    private val localDateTimeProvider: LocalDateTimeProvider = mock {
        val localDateTime: LocalDateTime = mock()
        on { now() }.doReturn(localDateTime)
    }
    private val pairingCompletedObserver: Observer<Boolean> = mock()
    private val pairingUseCase =
        PairingUseCase(
            messageParser,
            rxWebSocketClient,
            dataRepository,
            localDateTimeProvider,
            sendFirebaseTokenUseCase,
        )

    @Before
    fun setUp() {
        pairingUseCase.pairingCompletedState.observeForever(pairingCompletedObserver)
    }

    @Test
    fun `should send pairing code`() {
        whenever(rxWebSocketClient.events(address)).doReturn(
            Observable.just<RxWebSocketClient.Event>(
                RxWebSocketClient.Event.Open,
            ),
        )
        whenever(rxWebSocketClient.send(any())).doReturn(Completable.complete())

        pairingUseCase.pair(address, pairingCode)

        verify(rxWebSocketClient).send(
            check {
                assertEquals(pairingCode, it.pairingCode)
            },
        )
    }

    @Test
    fun `should properly handle new service`() {
        whenever(rxWebSocketClient.events(address)).doReturn(
            Observable.just(
                RxWebSocketClient.Event.Message(message),
            ),
        )
        whenever(messageParser.parseWebSocketMessage(any())).doReturn(
            Message(pairingApproved = true),
        )
        whenever(dataRepository.putChildData(any())).doReturn(Completable.complete())
        whenever(dataRepository.insertLogToDatabase(any())).doReturn(Completable.complete())

        pairingUseCase.pair(address, pairingCode)

        verify(dataRepository).putChildData(any())
        verify(dataRepository).insertLogToDatabase(any())
        verify(sendFirebaseTokenUseCase).sendFirebaseToken(rxWebSocketClient)
        verify(pairingCompletedObserver).onChanged(true)
    }

    @Test
    fun `should return false on config fail`() {
        whenever(rxWebSocketClient.events(address)).doReturn(
            Observable.just<RxWebSocketClient.Event>(
                RxWebSocketClient.Event.Message(message),
            ),
        )
        whenever(messageParser.parseWebSocketMessage(any())).doReturn(
            Message(pairingApproved = true),
        )
        whenever(dataRepository.doesChildDataExists(any())).doReturn(Single.error(Throwable()))
        whenever(dataRepository.putChildData(any())).doReturn(Completable.error(Throwable()))
        whenever(dataRepository.insertLogToDatabase(any())).doReturn(Completable.error(Throwable()))
        pairingUseCase.pairingCompletedState.observeForever(pairingCompletedObserver)

        pairingUseCase.pair(address, pairingCode)

        verify(pairingCompletedObserver).onChanged(false)
    }

    @Test
    fun `should disconnect while not approved`() {
        whenever(rxWebSocketClient.events(address)).doReturn(
            Observable.just<RxWebSocketClient.Event>(
                RxWebSocketClient.Event.Message(message),
            ),
        )
        whenever(messageParser.parseWebSocketMessage(any())).doReturn(
            Message(pairingApproved = false),
        )

        pairingUseCase.pair(address, pairingCode)

        verifyZeroInteractions(dataRepository, dataRepository)
        verify(pairingCompletedObserver).onChanged(false)
        verify(rxWebSocketClient).dispose()
    }
}

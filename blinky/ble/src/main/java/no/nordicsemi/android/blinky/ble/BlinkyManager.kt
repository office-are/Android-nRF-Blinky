package no.nordicsemi.android.blinky.ble

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import no.nordicsemi.android.blinky.spec.Blinky
import no.nordicsemi.android.blinky.spec.BlinkySpec
import no.nordicsemi.android.blinky.spec.exception.BlinkyException
import no.nordicsemi.kotlin.ble.client.android.CentralManager
import no.nordicsemi.kotlin.ble.client.android.Peripheral
import no.nordicsemi.kotlin.ble.client.exception.InvalidAttributeException
import no.nordicsemi.kotlin.ble.core.ConnectionState.Disconnected.Reason
import kotlin.uuid.ExperimentalUuidApi

/**
 * A Bluetooth LE implementation of the [Blinky] interface.
 *
 * The manager is responsible for setting up the Bluetooth LE connection to the peripheral
 * with the LED Button Service (LBS), which becomes available once the connection is established.
 *
 * Use [connect] to initiate the connection to the peripheral.
 * @param centralManager The central manager to use to connect to the peripheral.
 * @param peripheral The peripheral.
 */
@OptIn(ExperimentalUuidApi::class)
class BlinkyManager(
    private val centralManager: CentralManager,
    private val peripheral: Peripheral,
): Blinky {

    override suspend fun connect(
        block: suspend CoroutineScope.(Blinky.State) -> Unit,
    ): Unit = withContext(Dispatchers.IO) {

        var isConnected = false
        var retryCount = 0
        val maxRetries = 2 // 1回だけ自動リトライする

        while (!isConnected && retryCount <= maxRetries) {
            try {
                // 【重要】OS標準の10秒タイムアウトを待たず、2秒で強制的に見切りをつける
                // 1秒でも動きそうだが、余裕をもって2秒とする
                withTimeout(2000L) {
                    centralManager.connect(peripheral)
                }

                // --- 物理接続成功 ---

                // 前回組み込んだキャッシュリフレッシュ（成功時のみ実行）
                try {
                    peripheral.refreshCache()
                } catch (e: Exception) {
                    // refreshCacheの失敗は無視
                }

                isConnected = true // ループを抜ける

            } catch (e: TimeoutCancellationException) {
                // 3秒経過しても繋がらない場合ここに来る（OSがゴースト状態に陥っている可能性大）
                retryCount++
                if (retryCount > maxRetries) {
                    // リトライ上限を超えたら、本当のエラーとしてUIに返す
                    throw BlinkyException.Timeout()
                }

                // 強制キャンセルの例外によってOSの内部状態がリセットされることを期待し、
                // ほんの少し（300ms）待ってから、自動的に2回目の接続（ループ）を試みる
                delay(300L)

            } catch (e: Exception) {
                // その他の致命的なエラーは即終了
                throw BlinkyException.ConnectionFailed()
            }
        }

        // 3. 次に、Blinkyプロファイル（LED Button Service）をインストール・探索する
        peripheral.profile(
            serviceUuid = BlinkySpec.SERVICE_UUID,
            required = true,
        ) { remoteService ->
            val ledButtonService = LedButtonServiceImpl(remoteService, this)

            // Give the user control over the Blinky.
            try {
                block(ledButtonService)
            } catch (e: CancellationException) {
                // Don't disconnect when services were invalidated.
                if (e.cause !is InvalidAttributeException) {
                    peripheral.disconnect()
                }
                throw e
            }
        }

        // 4. 切断されるまで待機
        val reason = peripheral.awaitDisconnection()
        if (reason == Reason.RequiredServiceNotFound) {
            throw BlinkyException.NotSupported()
        }
        if (reason != Reason.Success) {
            throw BlinkyException.LinkLoss()
        }
    }
}
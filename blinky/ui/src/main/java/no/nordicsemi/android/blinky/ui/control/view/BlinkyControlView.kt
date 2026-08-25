package no.nordicsemi.android.blinky.ui.control.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth // ★追加
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card // ★追加
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.OutlinedIconToggleButton
import androidx.compose.material3.Slider // ★追加
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text // ★追加
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@OptIn(ExperimentalMaterial3Api::class) // ★ 2. 関数の直前に追加
@Composable
internal fun BlinkyControlView(
    ledState: Boolean,
    onStateChanged: (Boolean) -> Unit,
    onBlink: () -> Unit,
    sliderState: Int,
    onSliderChanged: (Int) -> Unit,
    onIncrement: () -> Unit, // ★ 追加
    onDecrement: () -> Unit, // ★ 追加
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LedControlView(
            state = ledState,
            enabled = true,
            onStateChanged = onStateChanged,
            onBlink = onBlink,
        )

        // ★追加: スライダー用のUIカード
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "出力電圧 %.1fV".format(sliderState / 10f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                    )

                // ★ 横並び(Row)から、縦並び(Column)のレイアウトに変更
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // --- [+] ボタン（上段） ---
                    val incInteraction = remember { MutableInteractionSource() }
                    val isIncPressed by incInteraction.collectIsPressedAsState()

                    LaunchedEffect(isIncPressed) {
                        if (isIncPressed) {
                            onIncrement()
                            delay(400)
                            while (isIncPressed) {
                                onIncrement()
                                delay(100)
                            }
                        }
                    }

                    Button(
                        onClick = { },
                        interactionSource = incInteraction,
                        modifier = Modifier
                            .fillMaxWidth(0.6f) // ★ 幅を親の60%に広げる（元の約3倍）
                            .height(64.dp)      // ★ 押しやすいように高さも少し大きく
                    ) {
                        Text("+", fontSize = 32.sp) // ★ 文字も大きく
                    }

                    Spacer(modifier = Modifier.height(32.dp)) // ★ スライダーとの間隔

                    // --- スライダー（中段） ---
                    // ★ BoxWithConstraints で高さを取得し、スライダーの長さに適用する
                    BoxWithConstraints(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Slider(
                            value = sliderState.toFloat(),
                            onValueChange = { newValue ->
                                onSliderChanged(newValue.toInt())
                            },
                            valueRange = 80f..120f,
                            steps = 19,
                            // ★ 1. バーや目盛りの色を設定
                            colors = SliderDefaults.colors(
                                // 選択されている側のバーの色（例: 青、または primary）
                                activeTrackColor = Color.LightGray,

                                // 選択されていない側のバーの色（例: 薄いグレー）
                                inactiveTrackColor = Color.LightGray,

                                // （任意）選択側の目盛りの色
                                activeTickColor = Color.DarkGray,

                                // （任意）未選択側の目盛りの色
                                inactiveTickColor = Color.DarkGray
                            ),
                            modifier = Modifier
                                .width(maxHeight)
                                .rotate(-90f)
                                .scale(scaleX = 1f, scaleY = 3f),
                        // ★ 丸い Box を直接定義
                            thumb = {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp) // 丸のサイズ（直径）
                                        .background(
                                            color = MaterialTheme.colorScheme.primary, // 色
                                            shape = CircleShape // 真円にする
                                        )
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // --- [-] ボタン（下段） ---
                    val decInteraction = remember { MutableInteractionSource() }
                    val isDecPressed by decInteraction.collectIsPressedAsState()

                    LaunchedEffect(isDecPressed) {
                        if (isDecPressed) {
                            onDecrement()
                            delay(400)
                            while (isDecPressed) {
                                onDecrement()
                                delay(100)
                            }
                        }
                    }

                    Button(
                        onClick = { },
                        interactionSource = decInteraction,
                        modifier = Modifier
                            .fillMaxWidth(0.6f) // ★ 幅を親の60%に広げる
                            .height(64.dp)
                    ) {
                        Text("-", fontSize = 32.sp)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BlinkyControlViewPreview() {
    var bindingState by rememberSaveable { mutableStateOf(false) }
    var previewSliderValue by rememberSaveable { mutableStateOf(128) } // プレビュー用

    BlinkyControlView(
        ledState = true,
        onStateChanged = {},
        onBlink = {},
        // ★追加: プレビュー用のダミーデータ
        sliderState = previewSliderValue,
        onSliderChanged = { previewSliderValue = it },
        onIncrement = { if (previewSliderValue < 120) previewSliderValue++ }, // ★ 追加
        onDecrement = { if (previewSliderValue > 80) previewSliderValue-- },  // ★ 追
        modifier = Modifier.padding(16.dp),
    )
}

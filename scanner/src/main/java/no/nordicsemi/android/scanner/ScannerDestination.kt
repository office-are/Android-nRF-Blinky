package no.nordicsemi.android.scanner

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import no.nordicsemi.android.scanner.view.BlinkyScanner

@Serializable
data object ScannerKey: NavKey

fun EntryProviderScope<NavKey>.scannerEntry(
    onDeviceSelected: (identifier: String, name: String?, myData8bit: Int) -> Unit
) = entry<ScannerKey> {
    BlinkyScanner(
        onDeviceSelected = { device, name, myData8bit ->
            onDeviceSelected(device, name, myData8bit)
        }
    )
}
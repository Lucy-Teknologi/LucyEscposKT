package app.lucys.lib.lucyescposkt.ui.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lucys.lib.lucyescposkt.R
import app.lucys.lib.lucyescposkt.android.escpos.AndroidEPConnectionFactory
import app.lucys.lib.lucyescposkt.android.escpos.imageWithLabel
import app.lucys.lib.lucyescposkt.core.escpos.EPTabPosition
import app.lucys.lib.lucyescposkt.core.escpos.escpos
import app.lucys.lib.lucyescposkt.core.printer.PrinterModel
import app.lucys.lib.lucyescposkt.data.BluetoothPrinterScanner
import app.lucys.lib.lucyescposkt.data.CoilImageLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@HiltViewModel
class MainViewModel @Inject constructor(
    private val scanner: BluetoothPrinterScanner,
    private val loader: CoilImageLoader,
) : ViewModel() {
    private val _devices = MutableStateFlow(scanner.getPairedDevices())
    val devices = _devices.asStateFlow()

    fun refresh() {
        _devices.update { scanner.getPairedDevices() }
    }

    @OptIn(ExperimentalTime::class)
    fun connect(device: PrinterModel) {
        val connection = AndroidEPConnectionFactory().create(device.connectionSpec)
        viewModelScope.launch {
            val image = loader.load(R.drawable.ic_android_black_24dp)
            val command = escpos(device.characterCount) {
                center {
                    bold {
                        text("BOLD")
                        tall {
                            text("TALL BOLD")
                            wide { text("WIDE TALL BOLD") }
                        }
                        wide { text("WIDE BOLD") }
                    }
                    text("NORMAL")
                }

                tab(EPTabPosition.Fixed(value = 27, spacing = 0)) {
                    setLeft("HELLO")
                    setRight("WORLD")
                    flush()
                    setLeft("25 September 2025")
                    setRight("12:00")
                    flush()
                    text("Queued At")
                    setLeft("23 November 2020")
                    setRight("12:00")
                }

                tab(EPTabPosition.Fixed(value = 27, spacing = 0)) {
                    setLeft("HELLO")
                    setRight("WORLD")
                    flush()
                    setLeft("25 September 2025")
                    setRight("12:00")
                    flush()
                    text("Queued At")
                    setLeft("23 November 2020")
                    setRight("12:00")
                }

                tab(EPTabPosition.Fixed(value = 27, spacing = 0)) {
                    setLeft("HELLO")
                    setRight("WORLD")
                    flush()
                    setLeft("25 September 2025")
                    setRight("12:00")
                    flush()
                    text("Queued At")
                    setLeft("23 November 2020")
                    setRight("12:00")
                }

                tab(EPTabPosition.Fixed(value = 27, spacing = 0)) {
                    setLeft("HELLO")
                    setRight("WORLD")
                    flush()
                    setLeft("25 September 2025")
                    setRight("12:00")
                    flush()
                    text("Queued At")
                    setLeft("23 November 2020")
                    setRight("12:00")
                }

                tab(EPTabPosition.Fixed(value = 27, spacing = 0)) {
                    setLeft("HELLO")
                    setRight("WORLD")
                    flush()
                    setLeft("25 September 2025")
                    setRight("12:00")
                    flush()
                    text("Queued At")
                    setLeft("23 November 2020")
                    setRight("12:00")
                }

                tab(EPTabPosition.Fixed(value = 27, spacing = 0)) {
                    setLeft("HELLO")
                    setRight("WORLD")
                    flush()
                    setLeft("25 September 2025")
                    setRight("12:00")
                    flush()
                    text("Queued At")
                    setLeft("23 November 2020")
                    setRight("12:00")
                }

                tab(EPTabPosition.Fixed(value = 27, spacing = 0)) {
                    setLeft("HELLO")
                    setRight("WORLD")
                    flush()
                    setLeft("25 September 2025")
                    setRight("12:00")
                    flush()
                    text("Queued At")
                    setLeft("23 November 2020")
                    setRight("12:00")
                }

                bullet("*", indent = 2, spacing = 1) {
                    text("Item 1")
                    text("Item 2")
                }

                center {
                    image?.let {
                        imageWithLabel(bitmap = it, label = "Sample Image")
                    }
                }

                feedAndCut()
            }

            connection.connect()
            val res = connection.send(command, 3.seconds)
            Log.d("MainViewModel", res.toString())
            connection.disconnect()
        }
    }
}
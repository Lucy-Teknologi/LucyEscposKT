package app.lucys.lib.lucyescposkt.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lucys.lib.lucyescposkt.android.escpos.AndroidEPConnectionFactory
import app.lucys.lib.lucyescposkt.core.escpos.EPTabPosition
import app.lucys.lib.lucyescposkt.core.escpos.escpos
import app.lucys.lib.lucyescposkt.core.printer.PrinterModel
import app.lucys.lib.lucyescposkt.data.BluetoothPrinterScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val scanner: BluetoothPrinterScanner,
) : ViewModel() {
    private val _devices = MutableStateFlow(scanner.getPairedDevices())
    val devices = _devices.asStateFlow()

    fun refresh() {
        _devices.update { scanner.getPairedDevices() }
    }

    fun connect(device: PrinterModel) {
        val connection = AndroidEPConnectionFactory().create(device.connectionSpec)
        val command = escpos(device.characterCount) {
            center {
                bold {
                    text("BOLD")

                    tall {
                        text("TALL BOLD")

                        wide {
                            text("WIDE TALL BOLD")
                        }
                    }

                    wide {
                        text("WIDE BOLD")
                    }
                }

                text("NORMAL")

                wide {
                    text("WIDE")

                    tall {
                        text("TALL WIDE")
                    }
                }

                tall {
                    text("TALL")
                }
            }

            tab(EPTabPosition.Fixed(value = 16, spacing = 1)) {
                setLeft("HELLO")
                setRight("WORLD")
                flush()
                setLeft("Created At:")
                setRight("5 NOVEMBER 2025")
                flush()
                text("Queued At")
                setLeft("5 NOVEMBER 2025")
                setRight("12:00")
            }
            feedAndCut()
        }

        viewModelScope.launch {
            connection.connect()
            connection.send(command)
            connection.disconnect()
        }
    }
}
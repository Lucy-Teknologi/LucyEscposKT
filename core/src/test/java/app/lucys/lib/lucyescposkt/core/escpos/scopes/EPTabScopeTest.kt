package app.lucys.lib.lucyescposkt.core.escpos.scopes

import app.lucys.lib.lucyescposkt.core.escpos.EPTabPosition
import app.lucys.lib.lucyescposkt.core.escpos.escpos
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.time.Duration.Companion.seconds

class EPTabScopeTest {
    @Test
    fun testTabScope() {
        runBlocking {
            val res = withTimeout(3.seconds) {
                escpos(32) {
                    tab(EPTabPosition.Fixed(20)) {
                        setLeft("Hello")
                        setRight("World")
                    }
                }
            }

            assert(res.isNotEmpty())
            println("MESSAGE:")
            res.forEach { byte ->
                print(byte)
            }
        }
    }

    /**
     * Verifies that when a single word exceeds `leftMaxLength` (e.g. 10 chars limit on 32 cpl),
     * `accumulateTexts` chunks the long word across lines cleanly within milliseconds.
     */
    @Test(timeout = 3000)
    fun testTabScope_LongWord_WrapsCleanlyWithoutInfiniteLoop() {
        runBlocking {
            // value = 10, spacing = 2 -> leftMaxLength = 8
            // "SuperLongWordExceedingLimit" is 27 chars (> 8)
            val result = escpos(32) {
                tab(EPTabPosition.Fixed(10)) {
                    setLeft("SuperLongWordExceedingLimit")
                    setRight("Test")
                }
            }
            assert(result.isNotEmpty())
        }
    }

    /**
     * Verifies that when `set()` is called multiple times within a single `tab { ... }` block,
     * all rows are accumulated and rendered into the command output.
     */
    @Test(timeout = 3000)
    fun testTabScope_MultipleRows_RendersAllRows() {
        runBlocking {
            val result = escpos(32) {
                tab(EPTabPosition.Fixed(14)) {
                    set("Nama Printer", ": Epson")
                    set("Tipe / Koneksi", ": Bluetooth")
                    set("Ukuran Kertas", ": 58 mm")
                    set("Karakter/Baris", ": 32 CPL")
                    set("Waktu Cetak", ": 2026-09-02")
                }
            }
            val outputString = String(result)
            assert(outputString.contains("Nama"))
            assert(outputString.contains("Printer"))
            assert(outputString.contains("Epson"))
            assert(outputString.contains("Tipe"))
            assert(outputString.contains("Koneksi"))
            assert(outputString.contains("Bluetooth"))
            assert(outputString.contains("Ukuran"))
            assert(outputString.contains("Kertas"))
            assert(outputString.contains("58 mm"))
            assert(outputString.contains("Karakter"))
            assert(outputString.contains("32 CPL"))
            assert(outputString.contains("Waktu"))
            assert(outputString.contains("Cetak"))
            assert(outputString.contains("2026-09-02"))
        }
    }
}
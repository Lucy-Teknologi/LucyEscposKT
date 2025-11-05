package app.lucys.lib.lucyescposkt.core.escpos.scopes

import app.lucys.lib.lucyescposkt.core.escpos.escpos
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class EPStyleScopeTest {
    @Test
    fun testSingleScope() = runBlocking {
        try {
            val array = withTimeout(3.seconds) {
                escpos(32) {
                    bold { text("Hello world") }
                }
            }
            print(array)
            assert(true)
        } catch (e: Exception) {
            e.printStackTrace()
            assert(false)
        }
    }

    @Test
    fun testMultiScope() = runBlocking {
        try {
            val array = withTimeout(3.seconds) {
                escpos(32) {
                    bold {
                        tall { text("Hello world") }
                    }
                }
            }
            println("MESSAGE:")
            println(array.toString())
            assert(true)
        } catch (e: Exception) {
            e.printStackTrace()
            assert(false)
        }
    }
}
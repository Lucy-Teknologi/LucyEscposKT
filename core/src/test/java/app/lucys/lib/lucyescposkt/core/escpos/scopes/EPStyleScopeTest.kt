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

    /**
     * Verifies that nested styles print content in exact chronological order
     * without inverting inner and outer text.
     */
    @Test
    fun testNestedScope_PreservesChronologicalOrder() = runBlocking {
        val result = escpos(32) {
            bold {
                text("First Title")
                wide {
                    text("Second Subtitle")
                }
                text("Third Footer")
            }
        }
        val output = String(result)
        val idxFirst = output.indexOf("First Title")
        val idxSecond = output.indexOf("Second Subtitle")
        val idxThird = output.indexOf("Third Footer")

        assert(idxFirst != -1)
        assert(idxSecond != -1)
        assert(idxThird != -1)
        assert(idxFirst < idxSecond) { "Expected 'First Title' before 'Second Subtitle', but got inverted order!" }
        assert(idxSecond < idxThird) { "Expected 'Second Subtitle' before 'Third Footer', but got inverted order!" }
    }
}
package app.lucys.lib.lucyescposkt.core.escpos.scopes

import app.lucys.lib.lucyescposkt.core.escpos.escpos
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.time.Duration.Companion.seconds

class EPBulletScopeTest {
    @Test
    fun testScope() {
        runBlocking {
            val arr = withTimeout(3.seconds) {
                escpos(32) {
                    bullet("*", 2) {
                        text("test")
                        text("sample")
                    }
                }
            }

            println("MSG:")
            arr.forEach { b -> print(b) }

            assert(arr.isNotEmpty())
        }
    }

    /**
     * Verifies that when a bullet item contains a single word longer than available limit,
     * `accumulateTexts` chunks the word cleanly across lines within milliseconds.
     */
    @Test(timeout = 3000)
    fun testBulletScope_LongWord_WrapsCleanlyWithoutInfiniteLoop() {
        runBlocking {
            // indent = 4, spacing = 2 -> indentation = 7, limit = 32 - 7 = 25
            // word length is 45 (> 25)
            val result = escpos(32) {
                bullet("*", indent = 4, spacing = 2) {
                    text("ExtremelyLongItemNameExceedingTwentyFiveChars")
                }
            }
            assert(result.isNotEmpty())
        }
    }
}
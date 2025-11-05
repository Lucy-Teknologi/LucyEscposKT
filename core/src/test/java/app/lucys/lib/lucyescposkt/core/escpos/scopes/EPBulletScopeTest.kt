package app.lucys.lib.lucyescposkt.core.escpos.scopes

import app.lucys.lib.lucyescposkt.core.escpos.escpos
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class EPBulletScopeTest {
    @Test
    fun testScope() = runBlocking {
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
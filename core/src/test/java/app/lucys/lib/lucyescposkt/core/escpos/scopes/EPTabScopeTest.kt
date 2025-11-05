package app.lucys.lib.lucyescposkt.core.escpos.scopes

import app.lucys.lib.lucyescposkt.core.escpos.EPTabPosition
import app.lucys.lib.lucyescposkt.core.escpos.escpos
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class EPTabScopeTest {
    @Test
    fun testTabScope() = runBlocking {
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
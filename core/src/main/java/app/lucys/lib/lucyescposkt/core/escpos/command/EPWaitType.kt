package app.lucys.lib.lucyescposkt.core.escpos.command

/**
 * Wait type to use when waiting for the printer to be ready.
 * [WAIT] for synchronous wait, will wait until paper status can be read.
 * [RT] for real time, will check real time status to check if printer is ready.
 */
enum class EPWaitType {
    RT, WAIT
}
package com.ZKQWatcher.android

import java.io.DataOutputStream

/**
 * Extremely small helper for ‘su’ calls.
 */
object RootPermissionUtil {

    /** Execute a single shell line as root; returns true if it exited with 0. */
    fun exec(cmd: String): Boolean = try {
        val proc = Runtime.getRuntime().exec("su")
        DataOutputStream(proc.outputStream).use { os ->
            os.writeBytes("$cmd\nexit\n"); os.flush()
        }
        proc.waitFor() == 0
    } catch (t: Throwable) {
        t.printStackTrace()
        false
    }

    /** Convenience wrapper used by BackgroundService. */
    fun killZKPProcesses() {
        exec("pkill -f azikaban || killall azikaban")
    }
}

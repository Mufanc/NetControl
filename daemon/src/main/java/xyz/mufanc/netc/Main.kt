package xyz.mufanc.netc

import android.app.ActivityThread
import android.os.Looper
import android.system.Os
import xyz.mufanc.aproc.annotation.AProcEntry
import java.io.File

private val DATA_DIRECTORY = File("/data/adb/netc")

@AProcEntry
object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        check(Os.getuid() == 0) { "Run the daemon as root" }
        require(args.size == 1) { "Module directory expected" }

        val moduleDirectory = File(args.single()).canonicalFile

        check(moduleDirectory.isDirectory) { "Invalid module directory" }
        check(DATA_DIRECTORY.mkdirs() || DATA_DIRECTORY.isDirectory) { "Cannot create data directory" }

        Looper.prepareMainLooper()

        val context = ActivityThread.systemMain().systemContext
        HttpServer(moduleDirectory, Apps(context), Firewall(context, DATA_DIRECTORY, FileAppRegistry()))

        Looper.loop()
    }
}

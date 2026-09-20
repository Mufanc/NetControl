package xyz.mufanc.netc

import android.os.FileObserver
import android.os.Handler
import android.os.Looper
import android.util.XmlHidden
import org.xmlpull.v1.XmlPullParser
import java.io.File

private const val REFRESH_DELAY_MILLIS = 250L
private const val PER_USER_RANGE = 100_000

private val PACKAGES = File("/data/system/packages.list")
private val USERS = File("/data/system/users/userlist.xml")

internal class FileAppRegistry : AppRegistry {

    private val mHandler = Handler(Looper.getMainLooper())
    private val mRefreshTask = Runnable(::refresh)
    private val mObservers = listOf(
        observe(PACKAGES.parentFile!!, PACKAGES.name),
        observe(USERS.parentFile!!, USERS.name),
    )

    private var mAppIds = readAppIds()
    private var mUserIds = readUserIds()
    private var mOnChanged: (() -> Unit)? = null

    @Synchronized
    override fun installedAppIds(): Set<Int> {
        return mAppIds
    }

    @Synchronized
    override fun resolveUids(appIds: IntArray): IntArray {
        return mUserIds.flatMap { userId ->
            appIds.map { appId -> userId * PER_USER_RANGE + appId }
        }.toIntArray()
    }

    @Synchronized
    override fun start(onChanged: () -> Unit) {
        check(mOnChanged == null) { "Registry already started" }

        mOnChanged = onChanged
        mObservers.forEach(FileObserver::startWatching)
    }

    private fun observe(directory: File, name: String): FileObserver {
        return object : FileObserver(directory, CLOSE_WRITE or CREATE or MOVED_TO or DELETE) {
            override fun onEvent(event: Int, path: String?) {
                if (path == name) scheduleRefresh()
            }
        }
    }

    private fun scheduleRefresh() {
        mHandler.removeCallbacks(mRefreshTask)
        mHandler.postDelayed(mRefreshTask, REFRESH_DELAY_MILLIS)
    }

    private fun refresh() {
        try {
            val appIds = readAppIds()
            val userIds = readUserIds()
            val onChanged: (() -> Unit)?

            synchronized(this) {
                mAppIds = appIds
                mUserIds = userIds
                onChanged = mOnChanged
            }

            onChanged?.invoke()
        } catch (err: Exception) {
            System.err.println("App registry refresh failed: ${err.javaClass.simpleName}: ${err.message}")
        }
    }

    private fun readAppIds(): Set<Int> {
        return PACKAGES.useLines { lines ->
            lines.mapNotNull { line ->
                line.split(' ', limit = 3).getOrNull(1)?.toIntOrNull()?.rem(PER_USER_RANGE)
            }.toSet()
        }
    }

    private fun readUserIds(): Set<Int> {
        val result = HashSet<Int>()

        USERS.inputStream().use { input ->
            val parser = XmlHidden.resolvePullParser(input)

            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG && parser.name == "user") {
                    parser.getAttributeValue(null, "id")?.toIntOrNull()?.let(result::add)
                }
            }
        }

        check(result.isNotEmpty()) { "No Android users found" }
        return result
    }
}

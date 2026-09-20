package xyz.mufanc.netc

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManagerHidden
import android.graphics.Bitmap
import android.graphics.Canvas
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.ByteArrayOutputStream

@Serializable
internal data class AppInfo(
    @SerialName("appid") val appId: Int,
    val label: String,
    val packages: List<String>,
    val system: Boolean,
)

internal class Apps(private val mContext: Context) {

    private val mEntries = HashMap<Int, ApplicationInfo>()

    @Synchronized
    fun list(): List<AppInfo> {
        val pm = mContext.packageManager
        val groups = pm.getInstalledPackages(PackageManagerHidden.MATCH_ANY_USER)
            .mapNotNull { pkg -> pkg.applicationInfo?.let { Triple(it.uid % 100000, pkg.packageName, it) } }
            .groupBy { it.first }
        val next = HashMap<Int, ApplicationInfo>()
        val result = groups.map { (appId, packages) ->
            val info = packages.first().third

            next[appId] = info

            AppInfo(
                appId = appId,
                label = info.loadLabel(pm).toString(),
                packages = packages.map { it.second }.sorted(),
                system = packages.all { it.third.flags and ApplicationInfo.FLAG_SYSTEM != 0 },
            )
        }

        mEntries.clear()
        mEntries.putAll(next)

        return result
    }

    @Synchronized
    fun icon(appid: Int): ByteArray? {
        val info = mEntries[appid] ?: return null
        val drawable = info.loadIcon(mContext.packageManager)
        val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)

        return try {
            drawable.setBounds(0, 0, 96, 96)
            drawable.draw(Canvas(bitmap))

            ByteArrayOutputStream().use { out ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out))
                out.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
    }
}

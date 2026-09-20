package xyz.mufanc.netc

import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManagerHidden
import android.os.ServiceManager
import android.util.AtomicFile
import dev.rikka.tools.refine.Refine
import java.io.File

private const val FIRST_APPLICATION_UID = 10_000
private const val LAST_APPLICATION_UID = 19_999
private const val BITMAP_SIZE = (LAST_APPLICATION_UID - FIRST_APPLICATION_UID + 8) / 8

internal class Firewall(context: Context, directory: File, private val mRegistry: AppRegistry) {
    private val mConnectivity: ConnectivityManagerHidden
    private val mBitmapFile = File(directory, "blacklist.bin")
    private val mBitmapStore = AtomicFile(mBitmapFile)
    private val mBitmap = loadBitmap()
    private val mChain: Int

    init {
        ServiceManager.waitForService(Context.CONNECTIVITY_SERVICE)

        mConnectivity = Refine.unsafeCast(context.getSystemService(ConnectivityManager::class.java))
        mChain = selectChain()
        reconcile()
        mRegistry.start(::refresh)
    }

    @Synchronized
    fun blockedAppIds(): IntArray {
        return (FIRST_APPLICATION_UID..LAST_APPLICATION_UID)
            .filter(::isBlocked)
            .toIntArray()
    }

    @Synchronized
    fun setBlocked(appId: Int, blocked: Boolean) {
        require(appId in FIRST_APPLICATION_UID..LAST_APPLICATION_UID)
        if (isBlocked(appId) == blocked) return

        val bit = appId - FIRST_APPLICATION_UID
        val mask = 1 shl (bit and 7)

        mBitmap[bit shr 3] = if (blocked) {
            (mBitmap[bit shr 3].toInt() or mask).toByte()
        } else {
            (mBitmap[bit shr 3].toInt() and mask.inv()).toByte()
        }

        saveBitmap()
        sync()
    }

    @Synchronized
    private fun reconcile() {
        val installed = mRegistry.installedAppIds()
        var changed = false

        for (appId in blockedAppIds()) {
            if (appId !in installed) {
                val bit = appId - FIRST_APPLICATION_UID
                mBitmap[bit shr 3] = (mBitmap[bit shr 3].toInt() and (1 shl (bit and 7)).inv()).toByte()
                changed = true
            }
        }

        if (changed) saveBitmap()
        sync()
    }

    @Synchronized
    private fun sync() {
        mConnectivity.replaceFirewallChain(mChain, mRegistry.resolveUids(blockedAppIds()))
        mConnectivity.setFirewallChainEnabled(mChain, true)
    }

    private fun refresh() {
        try {
            reconcile()
        } catch (err: Exception) {
            System.err.println("Firewall sync failed: ${err.javaClass.simpleName}: ${err.message}")
        }
    }

    private fun selectChain(): Int {
        return intArrayOf(
            ConnectivityManagerHidden.FIREWALL_CHAIN_OEM_DENY_3,
            ConnectivityManagerHidden.FIREWALL_CHAIN_OEM_DENY_2,
            ConnectivityManagerHidden.FIREWALL_CHAIN_OEM_DENY_1,
        ).firstOrNull { !mConnectivity.getFirewallChainEnabled(it) }
            ?: error("No unused OEM firewall chain")
    }

    private fun loadBitmap(): ByteArray {
        if (!mBitmapFile.exists()) return ByteArray(BITMAP_SIZE)

        return mBitmapStore.readFully().also {
            check(it.size == BITMAP_SIZE) { "Invalid blacklist bitmap" }
        }
    }

    private fun saveBitmap() {
        val output = mBitmapStore.startWrite()

        try {
            output.write(mBitmap)
            mBitmapStore.finishWrite(output)
        } catch (err: Exception) {
            mBitmapStore.failWrite(output)
            throw err
        }
    }

    private fun isBlocked(appId: Int): Boolean {
        val bit = appId - FIRST_APPLICATION_UID
        return mBitmap[bit shr 3].toInt() and (1 shl (bit and 7)) != 0
    }
}

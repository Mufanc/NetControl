package xyz.mufanc.netc

internal interface AppRegistry {
    fun installedAppIds(): Set<Int>

    fun resolveUids(appIds: IntArray): IntArray

    fun start(onChanged: () -> Unit)
}

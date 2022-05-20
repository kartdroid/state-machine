package me.kartdroid.logging.internal

import me.kartdroid.logging.Logger

internal class GenericLogger(
    override val tag: String,
    override val verboseMask: Int,
    private val prefixCallSiteInfo: Boolean
) : Logger {

    class Builder : Logger.Builder() {
        override fun build(): Logger {
            return GenericLogger(tag, verboseMask = verboseMask, prefixCallSiteInfo = prefixCallSiteInfo)
        }
    }

    override fun trace(lazyMessage: () -> String?) = applyFilter(Logger.Level.TRACE) {
        println(appendCallSiteInfo(lazyMessage = lazyMessage))
    }

    override fun debug(lazyMessage: () -> String?) = applyFilter(Logger.Level.DEBUG) {
        println(appendCallSiteInfo(lazyMessage = lazyMessage))
    }

    override fun info(lazyMessage: () -> String?) = applyFilter(Logger.Level.INFO) {
        println(appendCallSiteInfo(lazyMessage = lazyMessage))
    }

    override fun warn(lazyMessage: () -> String?) = applyFilter(Logger.Level.WARN) {
        println(appendCallSiteInfo(lazyMessage = lazyMessage))
    }

    override fun error(lazyMessage: () -> String?) = applyFilter(Logger.Level.ERROR) {
        println(appendCallSiteInfo(forceApply = true, lazyMessage = lazyMessage))
    }

    override fun error(throwable: Throwable?, lazyMessage: () -> String?) = applyFilter(Logger.Level.ERROR) {
        println(appendCallSiteInfo(forceApply = true, lazyMessage = lazyMessage, throwable = throwable))
    }

    private inline fun applyFilter(level: Logger.Level, executionBlock: () -> Unit) {
        if (isLevelEnabled(level)) {
            executionBlock.invoke()
        }
    }

    private inline fun appendCallSiteInfo(
        forceApply: Boolean = false,
        lazyMessage: () -> String?,
        throwable: Throwable? = null
    ): String {
        return if (forceApply || prefixCallSiteInfo) {
            val stackTrace = Throwable().stackTrace
            val className = stackTrace[1].className.substring(stackTrace[1].className.lastIndexOf(".") + 1)
            val methodName = stackTrace[1].methodName
            val lineNumber = stackTrace[1].lineNumber
            "$tag: [$className:$methodName@$lineNumber] ${lazyMessage() ?: ""} : ${throwable?.message ?: ""}"
        } else {
            lazyMessage() ?: ""
        }
    }
}
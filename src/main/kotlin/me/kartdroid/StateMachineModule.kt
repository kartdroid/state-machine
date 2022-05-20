package me.kartdroid

import me.kartdroid.logging.Logger
import me.kartdroid.logging.internal.GenericLogger

object StateMachineModule {
    private var loggerBuilder: Logger.Builder =
        GenericLogger.Builder()
            .tag("kartdroid.SM")
            .maxVerboseLevel(Logger.Level.ERROR)

    @JvmSynthetic
    internal var logger: Logger = loggerBuilder.build()

    fun setLogLevel(debugLevel: Logger.Level = Logger.Level.ERROR) {
        logger =
            loggerBuilder.maxVerboseLevel(debugLevel).build()
    }
}
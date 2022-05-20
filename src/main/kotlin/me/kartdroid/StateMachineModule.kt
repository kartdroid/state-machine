package me.kartdroid

import me.kartdroid.logging.Logger
import me.kartdroid.logging.internal.GenericLogger


/**
 * @author [Karthick Chinnathambi](https://github.com/kartdroid)
 * @since 20/05/22.
 */
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
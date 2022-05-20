package me.kartdroid.logging

/**
 * An API for logging messages.
 *
 * Each Logging method accepts a lambda which is computed ONLY IF the corresponding [Level]
 * is enabled
 */
interface Logger {

    /**
     * Levels of Logging, Ordered in terms of Verbosity from the Least to the Most
     */
    enum class Level(
        /**
         * The mask associated with a particular verbose [Level]
         * If the mask bit is set while creating the logg
         */
        val mask: Int
    ) {
        /**
         * Level that corresponds to [error] logging
         */
        ERROR(1 shl MASK_0),

        /**
         * Level that corresponds to [warn] logging
         */
        WARN(1 shl MASK_1),

        /**
         * Level that corresponds to [info] logging
         */
        INFO(1 shl MASK_2),

        /**
         * Level that corresponds to [debug] logging
         */
        DEBUG(1 shl MASK_3),

        /**
         * Level that corresponds to [trace] logging
         */
        TRACE(1 shl MASK_4),
        ;
    }

    /**
     * A convenience builder class for composing a [Logger] object.
     *
     * @constructor Creates a Builder object with a default [tag].
     */
    abstract class Builder {

        /**
         * A label that is prefixed on every log message
         */
        protected var tag: String = "NXG-QP"

        /**
         * A mask that indicate enabled [Logger.Level]s based on the corresponding Mask Bit status.
         */
        protected var verboseMask: Int = Level.ERROR.mask or Level.WARN.mask or Level.INFO.mask

        /**
         * Enables / Disabled including of Call Site information such as _Class Name_, _Method Name_ and _Line Number_ in
         * Log
         * messages .
         */
        protected open var prefixCallSiteInfo: Boolean = false

        /**
         * The TAG that will be added as prefix in all Logging statements
         */
        fun tag(tag: String) = apply {
            this.tag = tag
        }

        /**
         *  [Level] up-to which logging needs to be enabled , [Level.ERROR] being minimum and
         * [Level.TRACE] being the maximum verbose level.
         *
         * Usage:
         * ```
         * FoundationFactory.defaultPlatformFactory().loggerWith {
         * tag("NXG-QP-FOUNDATION")
         * maxVerboseLevel(Logger.Level.TRACE)
         * }
         * ```
         */
        fun maxVerboseLevel(level: Level) = apply {
            verboseMask = Level.values().fold(Level.ERROR.mask) { accumulateMask, nxtLevel ->
                if (nxtLevel.ordinal <= level.ordinal) {
                    accumulateMask or nxtLevel.mask
                } else {
                    accumulateMask
                }
            }
        }

        /**
         * [Level]s for which logging has to be enabled.
         *
         * This API can be used if we need to enable logs for random levels
         * as opposed to their level of increasing verbosity.
         *
         * Usage :
         *
         * ```
         * FoundationFactory.defaultPlatformFactory().loggerWith {
         * tag("NXG-QP-FOUNDATION")
         * verboseLevels(Logger.Level.INFO, Logger.Level.ERROR)
         * }
         * ```
         */
        fun verboseLevels(vararg levels: Level) = apply {
            verboseMask = levels.fold(0) { accumulateMask, nxtLevel ->
                accumulateMask or nxtLevel.mask
            }
        }

        /**
         * This API can be used if we need ClassName, MethodName and Line number information as part of
         * the Log statement.
         *
         * This is platform specific and may not be available in all Platforms.
         * This is configurable only in debug builds and has no effect in release builds.
         */
        open fun prefixCallSiteInfo(truthValue: Boolean) = apply {
            prefixCallSiteInfo = truthValue
        }

        /**
         * Returns a [Logger] instance from the passed builder configuration
         */
        abstract fun build(): Logger
    }

    /**
     * The String that will be prefixed as a Tag on all log messages
     */
    val tag: String
    /**
     * Mask that contains information on the enabled [Level]s of logging.
     */
    val verboseMask: Int

    /**
     * Checks if the passed Logger [Level] is enabled by checking the underlying Mask Bit.
     */
    fun isLevelEnabled(level: Level): Boolean {
        return (verboseMask and level.mask) == level.mask
    }

    /**
     * Performs a [Level.TRACE] level Logging, of the [String] message, computed by invoking [lazyMessage] lambda.
     *
     * This type of logging should never be compiled into an application except during development.
     * This logging will be stripped off at compile-time in release builds
     *
     *  @param lazyMessage the lambda that is lazily executed, if corresponding level of logging is enabled
     */
    fun trace(lazyMessage: () -> String?)

    /**
     * Performs a [Level.DEBUG] level Logging, of the [String] message, computed by invoking [lazyMessage] lambda.
     *
     * Debug logs might be compiled in but stripped at run-time
     *
     *  @param lazyMessage the lambda that is lazily executed, if corresponding level of logging is enabled
     */
    fun debug(lazyMessage: () -> String?)

    /**
     * Performs a [Level.INFO] level Logging, of the [String] message, computed by invoking [lazyMessage] lambda.
     *
     *  @param lazyMessage the lambda that is lazily executed, if corresponding level of logging is enabled
     */
    fun info(lazyMessage: () -> String?)

    /**
     * Performs a [Level.WARN] level Logging, of the [String] message, computed by invoking [lazyMessage] lambda.
     *
     *  @param lazyMessage the lambda that is lazily executed, if corresponding level of logging is enabled
     */
    fun warn(lazyMessage: () -> String?)

    /**
     * Performs a [Level.ERROR] level Logging, of the [String] message, computed by invoking [lazyMessage] lambda.
     *
     *  @param lazyMessage the lambda that is lazily executed, if corresponding level of logging is enabled
     */
    fun error(lazyMessage: () -> String?)

    /**
     * Performs a [Level.ERROR] level Logging of:
     * <ol>
     *   <li>the stack trace of the passed [throwable] object.</li>
     *   <li>the [String] message, computed by invoking [lazyMessage] lambda.</li>
     * </ol>
     *
     *  @param throwable,  the [Throwable] whose Stack Trace needs to be logged along with the computed message
     *  @param lazyMessage, the lambda that is lazily executed to retrieve a log message, if corresponding level of
     *  logging is enabled
     */
    fun error(throwable: Throwable?, lazyMessage: () -> String?)
}

private const val MASK_0 = 0
private const val MASK_1 = 1
private const val MASK_2 = 2
private const val MASK_3 = 3
private const val MASK_4 = 4
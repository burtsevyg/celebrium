package io.celebrium.utils

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.celebrium.core.config.Configuration
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit


/**
* Объект TimeManager.
* -------------------
*
*
* @author EMurzakaev@it.ru.
*/
object TimeManager {

    /**
     * TODO: документация!
     */
    private val logger = LoggerFactory.getLogger(TimeManager::class.java) as Logger

    /**
     * TODO: документация!
     */
    init {
        logger.level = Level.toLevel(Configuration.config.getString("log_level"))
    }

    /**
     * Ожидание некоторого времени в секундах.
     *
     * Ожидание времени осуществляется путем блокировки текущего потока.
     *
     * @param timeToWait время ожидания.
     */
    @JvmStatic
    @JvmOverloads
    fun waitSomeTime(timeToWait: Int, timeUnit: TimeUnit = TimeUnit.SECONDS, cycleLogging: Boolean = true) {
        logger.debug("Requested waiting for $timeToWait ${timeUnit.name}.")
        val cycles = Configuration.config.getInt("numerics.time_manager_cycle_amount")
        val trueTimeUnit = if (timeToWait / cycles == 0) lowerTimeUnit(timeUnit) else timeUnit
        val trueTimeToWait = trueTimeUnit.convert(timeToWait.toLong(), timeUnit)
        val cycle = trueTimeToWait / cycles
        val modulo = trueTimeToWait % cycle
        if (cycleLogging) {
            if (modulo != 0L) {
                try {
                    trueTimeUnit.sleep(modulo)
                } catch (exception: InterruptedException) {
                    logger.error(exception.message, exception)
                }
            }
            for (i in cycles downTo 1) {
                try {
                    logger.debug("Waiting... ${i * cycle} ${trueTimeUnit.name} left.")
                    trueTimeUnit.sleep(cycle)
                } catch (exception: InterruptedException) {
                    logger.error(exception.message, exception)
                }
            }
        } else {
            try {
                timeUnit.sleep(timeToWait.toLong())
            } catch (exception: InterruptedException) {
                logger.error(exception.message, exception)
            }
        }

    }

    /**
     * Понижение единиц исчисления TimeUnit на один порядок
     *
     * @param timeUnit входной TimeUnit для понижения
     *
     * @throws UnsupportedOperationException исключение при попытке понизить ниже минимального порядка
     *
     * @return TimeUnit с порядком исчисления ниже входного на единицу
     */
    @Throws(UnsupportedOperationException::class)
    private fun lowerTimeUnit(timeUnit: TimeUnit) = when (timeUnit.ordinal) {
        1 -> TimeUnit.NANOSECONDS
        2 -> TimeUnit.MICROSECONDS
        3 -> TimeUnit.MILLISECONDS
        4 -> TimeUnit.SECONDS
        5 -> TimeUnit.MINUTES
        6 -> TimeUnit.HOURS
        else -> {
            logger.warn("Unable to split time. Should have either cycle amount decreased or cycle logging disabled.")
            throw UnsupportedOperationException("Unable to split time.")
        }
    }

}

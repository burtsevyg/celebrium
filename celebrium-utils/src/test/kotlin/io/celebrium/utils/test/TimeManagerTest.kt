package io.celebrium.utils.test

import ch.qos.logback.classic.Logger
import io.celebrium.core.config.Configuration
import io.celebrium.utils.TimeManager
import org.slf4j.LoggerFactory
import org.testng.Assert
import org.testng.annotations.BeforeSuite
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.util.concurrent.TimeUnit

/**
 * Класс TimeManagerTest.
 * ----------------------
 *
 *
 * @author EMurzakaev@it.ru.
 */
object TimeManagerTest {

    /**
     * TODO: документация!
     */
    private val logger = LoggerFactory.getLogger(TimeManagerTest::class.java) as Logger

    /**
     * TODO: документация!
     */
    @BeforeSuite
    fun beforeSuite() = Configuration.init()

    /**
     * TODO: документация!
     */
    @Test(dataProvider = "dp")
    fun waitTest(timeToWait: Int, timeUnit: TimeUnit) {
        val start = System.currentTimeMillis()
        TimeManager.waitSomeTime(timeToWait, timeUnit)
        val total = System.currentTimeMillis() - start
        val min = timeUnit.toMillis(timeToWait.toLong())
        val max = min + 100
        logger.debug("Result of $timeToWait ${timeUnit.name.toLowerCase()}:\nmin : $min\t|\tmax : $max\t|\ttotal : $total")
        Assert.assertTrue(total in min..max)
    }

    /**
     * TODO: документация!
     */
    @DataProvider(parallel = true)
    fun dp() = listOf(
            arrayOf(5000, TimeUnit.MICROSECONDS),
            arrayOf(500, TimeUnit.MILLISECONDS),
            arrayOf(5, TimeUnit.SECONDS)
    ).iterator()

}

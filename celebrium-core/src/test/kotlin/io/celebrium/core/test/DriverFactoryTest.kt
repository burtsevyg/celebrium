package io.celebrium.core.test

import io.celebrium.core.config.Configuration
import io.celebrium.core.driver.DriverFactory
import io.celebrium.core.exceptions.DriverFactoryException
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import org.openqa.selenium.remote.DesiredCapabilities
import org.testng.Assert
import org.testng.annotations.BeforeSuite
import org.testng.annotations.Test

/**
 * DriverFactoryTest.
 *
 * Содержит unit тесты для DriverFactory.
 *
 * @see DriverFactory
 * @author EMurzakaev@it.ru.
 */
object DriverFactoryTest {

    /**
     * TODO: документация!
     */
    @BeforeSuite
    fun beforeSuite() = Configuration.init()

    /**
     * TODO: документация!
     */
    @Test(expectedExceptions = arrayOf(DriverFactoryException::class))
    fun getDriverTest() {
        DriverFactory.getDriver()
    }

    /**
     * TODO: документация!
     */
    @Test
    fun isDriverActiveTest() {
        Assert.assertFalse(DriverFactory.isDriverActive())
    }

    /**
     * TODO: документация!
     */
    @Test
    fun closeDriverTest() {
        DriverFactory.closeDriver()
    }

    /**
     * TODO: документация!
     */
    @Test
    fun capabilitiesTest() {
        Assert.assertNull(DriverFactory.capabilities)
    }

    /**
     * TODO: документация!
     */
    @Test
    fun initDriverTest() {
        val capabilities = DesiredCapabilities.htmlUnit()
        DriverFactory.initDriver(capabilities = capabilities)
        DriverFactory.getDriver()
        DriverFactory.closeDriver()
    }

    /**
     * TODO: документация!
     */
    @Test
    fun registerDriverTest() {
        DriverFactory.registerDriver(HtmlUnitDriver())
        DriverFactory.getDriver()
        DriverFactory.closeDriver()
    }

}

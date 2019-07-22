package io.celebrium.core.driver

import io.celebrium.core.config.Configuration
import io.celebrium.core.exceptions.DriverFactoryException
import org.openqa.selenium.Capabilities
import org.openqa.selenium.WebDriver
import org.openqa.selenium.remote.LocalFileDetector
import org.openqa.selenium.remote.RemoteWebDriver
import ru.stqa.selenium.factory.WebDriverPool

/**
 * Объект DriverFactory.
 * ---------------------
 *
 * TODO: документация!
 *
 * @author EMurzakaev@it.ru.
 */
object DriverFactory {

    /**
     * TODO: документация!
     */
    private val drivers = ThreadLocal<WebDriver>()

    /**
     * TODO: документация!
     */
    var capabilities: Capabilities? = null
        private set

    /**
     * TODO: документация!
     */
    @JvmStatic
    @JvmOverloads
    fun initDriver(gridHubUrl: String? = null, capabilities: Capabilities): WebDriver {
        val driver = if (gridHubUrl.isNullOrEmpty()) {
            WebDriverPool.DEFAULT.getDriver(capabilities)
        } else {
            WebDriverPool.DEFAULT.getDriver(gridHubUrl, capabilities)
        }
        gridHubUrl?.let { (driver as RemoteWebDriver).fileDetector = LocalFileDetector() }
        this.capabilities = capabilities
        drivers.set(driver)
        return driver
    }

    /**
     * TODO: документация!
     */
    @JvmStatic
    fun registerDriver(driver: WebDriver) = drivers.set(driver)

    /**
     * TODO: документация!
     */
    @JvmStatic
    fun getDriver(): WebDriver = drivers.get() ?: throw DriverFactoryException(Configuration.config.getString("error.strings.driver_factory_no_initialized_driver"))

    /**
     * TODO: документация!
     */
    @JvmStatic
    fun closeDriver() {
        drivers.get()?.let {
            it.quit()
            drivers.remove()
        }
    }

    /**
     * TODO: документация!
     */
    @JvmStatic
    fun isDriverActive(): Boolean {
        drivers.get()?.let {
            try {
                it.currentUrl
                return true
            } catch (exception: Exception) {
                return false
            }
        }
        return false
    }

}

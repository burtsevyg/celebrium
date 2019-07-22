package io.celebrium.web.test

import com.typesafe.config.Config
import io.celebrium.core.config.Configuration
import io.celebrium.web.page.WebPage
import org.testng.annotations.BeforeSuite
import org.testng.annotations.Test

/**
 * WebPageTest.
 *
 *
 * @author EMurzakaev@it.ru.
 */
object WebPageTest {

    /**
     * TODO: документация!
     */
    @BeforeSuite
    fun beforeSuite() = Configuration.init()

    /**
     * TODO: документация!
     */
    @Test
    fun locatorsTest() {

    }

    class Page(config: Config) : WebPage(config)

}

package io.celebrium.utils

import io.celebrium.core.driver.DriverFactory
import org.openqa.selenium.JavascriptExecutor
import java.io.File
import java.util.*

/**
 * Класс JSExecutor.
 * -----------------
 *
 *
 * @author EMurzakaev@it.ru.
 */
object JSExecutor {

    /**
     * Метод выполнения javascript из заданного файла.
     *
     * @param driver   экземпляр webdriver'а.
     * @param fileName имя файла, содержащего скрипт.
     * @return строковый результат выполнения js, завернутый в Optional.
     */
    @JvmStatic
    fun executeJSFromFile(fileName: String): Optional<Any> {
        val js = File(fileName).readText(Charsets.UTF_8)
        return executeJS(js)
    }

    /**
     * Метод выполнения javascript.
     *
     * @param javaScript скрипт в виде строки.
     * @param parameters параметры.
     * @return строковый результат выполнения js, завернутый в Optional.
     */
    @JvmStatic
    fun executeJS(javaScript: String, vararg parameters: Any): Optional<Any> {
        val jse = DriverFactory.getDriver() as JavascriptExecutor
        val result = jse.executeScript(javaScript, *parameters)
        return Optional.ofNullable(result)
    }

}

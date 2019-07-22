package io.celebrium.utils

import io.celebrium.core.driver.DriverFactory
import org.openqa.selenium.WebElement
import ru.yandex.qatools.ashot.AShot
import ru.yandex.qatools.ashot.Screenshot
import ru.yandex.qatools.ashot.coordinates.WebDriverCoordsProvider
import ru.yandex.qatools.ashot.cropper.indent.IndentCropper
import ru.yandex.qatools.ashot.cropper.indent.IndentFilerFactory
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.imageio.ImageIO

/**
 * Класс ScreenShooter.
 * --------------------
 *
 *
 * @author EMurzakaev@it.ru.
 */
object ScreenShooter {

    /**
     * Экземпляр класса AShot с навтроенной стратегией снимков.
     */
    private val aShot: AShot = AShot()
            .coordsProvider(WebDriverCoordsProvider())
            .imageCropper(
                    IndentCropper().addIndentFilter(IndentFilerFactory.blur())
            )

    /**
     * Метод снятия снимка экрана.
     *
     * @return массив байтов.
     */
    @JvmStatic
    fun takeScreenshot(): ByteArray {
        val driver = DriverFactory.getDriver()
        return saveScreenShot(aShot.takeScreenshot(driver))
    }

    /**
     * Метод снятия снимка отдельного элемента на экране.
     *
     * @param element элемент, который необходимо снять.
     * @return массив байтов.
     */
    @JvmStatic
    fun takeScreenshot(element: WebElement): ByteArray {
        val driver = DriverFactory.getDriver()
        return saveScreenShot(aShot.takeScreenshot(driver, element))
    }

    /**
     * Метод сохранения снимка экрана.
     *
     * Снимок прикрепляется к отчету, созданному с помощью
     * [Allure](http://allure.qatools.ru/ "Allure").
     *
     * @param screenshot снимок экрана.
     * @return массив байтов.
     */
    private fun saveScreenShot(screenshot: Screenshot): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        try {
            ImageIO.write(screenshot.image, "png", byteArrayOutputStream)
        } catch (e: IOException) {
        }
        val data = byteArrayOutputStream.toByteArray()
        return data
    }

}

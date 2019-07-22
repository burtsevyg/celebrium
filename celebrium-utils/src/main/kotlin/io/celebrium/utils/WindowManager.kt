package io.celebrium.utils

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.celebrium.core.config.Configuration
import io.celebrium.core.driver.DriverFactory
import org.openqa.selenium.*
import org.openqa.selenium.support.ui.ExpectedCondition
import org.openqa.selenium.support.ui.WebDriverWait
import org.slf4j.LoggerFactory
import org.testng.Assert
import java.util.*

/**
 * Класс WindowManager.
 * --------------------
 *
 * Данный класс предназначен для обработки работы WebDriver с окна.
 * Содержит метода октрытия нового окна, переключения между окнами, закрытия окон.
 *
 * @param windowName название текущего окна.
 * @author EMurzakaev@it.ru.
 */
class WindowManager(windowName: String) {

    /**
     * Экземпляр логгера.
     */
    private val logger = LoggerFactory.getLogger(WindowManager::class.java) as Logger

    /**
     * Хранилище пар "Название-ключ страницы - Хэндл страницы".
     */
    private val windowHandles = HashMap<String, String>()

    /**
     * Название активного окна.
     */
    var activeWindow = windowName
        private set

    /**
     * Хендл текущего окна.
     */
    var activeWindowHandle = DriverFactory.getDriver().windowHandle?: throw Exception("Cannot get active window handle")
        private set

    /**
     * Инициализация WindowManager.
     */
    init {
        windowHandles.put(activeWindow, activeWindowHandle)
        logger.level = Level.toLevel(Configuration.config.getString("log_level"))
    }

    /**
     * Открытие нового окна.
     *
     * @param windowName название окна.
     */
    fun openNewWindow(windowName: String) {
        logger.debug("Try to open new window \"$windowName\"")
        JSExecutor.executeJS("window.open();")
        switchToWindow(windowName)
    }

    /**
     * Открытие ссылки в новом окне.
     *
     * @param windowName название окна.
     * @param link ссылка.
     */
    fun openLinkInNewWindow(windowName: String, link: String) {
        openNewWindow(windowName)
        DriverFactory.getDriver().get(link)
    }

    /**
     * Переключение на окно.
     *
     * Метод предполагает переключение как на существующее окно, так и на новое. Новое окно
     * может быть открыто, например, после нажатия на какой-либо элемент в текущем окне. Метод
     * позволяет отследить открытие нового окна, получить его хендл и добавить в 'windowHandles'.
     * Время ожидания открытия нового окна задается в конфигурации по ключу
     * 'timeouts.window_manager_open_new_window_timeout'. В случае неудачи переключения на окно
     * из-за 'WebDriverException' будет осуществлено ожидание, заданное в конфигурации по ключу
     * 'timeouts.window_manager_between_attempts_timeout', и переповтор переключения на окно.
     *
     * В случае, когда окно содержится в 'windowHandles', происходит переключение на данное
     * окно, путем вызова метода 'switchTo'.
     *
     * @see WebDriver.switchTo
     * @param windowName название окна.
     */
    fun switchToWindow(windowName: String) {
        logger.debug("Try to switch to window \"$windowName\"")
        val driver = DriverFactory.getDriver()
        windowHandles[windowName]?.let {
            logger.debug("Window handles contains window \"$windowName\". Try to switch...")
            driver.switchTo().window(it)
            activeWindow = windowName
            activeWindowHandle = driver.windowHandle
            logger.debug("Active window : \"$activeWindow\". Active window handle : \"$activeWindowHandle\". All windows : \n[\n\t${windowHandles.map { "${it.key} : ${it.value}" }.joinToString("\n\t")}\n]")
        }
        if (windowHandles[windowName].isNullOrEmpty()) {
            logger.debug("Window handles not contains window \"$windowName\".")
            val width = driver.manage().window().size.width
            val height = driver.manage().window().size.height

            // TODO: выделить в отдельную функцию с рекрсией повторов?
            try {
                val windowHandlesSet = windowHandles.values.toSet()
                logger.debug("Current windows : \n[\n\t${windowHandles.map { "${it.key} : ${it.value}" }.joinToString("\n\t")}\n]")
                val openTimeout = Configuration.config.getInt("timeouts.window_manager_open_new_window_timeout") * 1000L
                driver.switchTo().window(
                        WebDriverWait(driver, openTimeout).until(anyWindowOtherThan(windowHandlesSet))
                )
                driver.manage().window().size = Dimension(width + 1, height + 1)
                activeWindow = windowName
                activeWindowHandle = driver.windowHandle
                windowHandles.put(activeWindow, activeWindowHandle)
                logger.debug("Active window : \"$activeWindow\". Active window handle : \"$activeWindowHandle\". All windows : \n[\n\t${windowHandles.map { "${it.key} : ${it.value}" }.joinToString("\n\t")}\n]")
            } catch (exception: TimeoutException) {
                val message = Configuration.config.getString("error.strings.window_manager_cannot_open_new_window")
                Assert.fail(message, exception)
            } catch (exception: WebDriverException) {
                exception.message?.let {
                    if (it.contains("No window with id:")) {
                        val timeout = Configuration.config.getInt("timeouts.window_manager_between_attempts_timeout")
                        TimeManager.waitSomeTime(timeout)
                        // TODO: рекурсия повтора перехода на окно
                        switchToWindow(windowName)
                    }
                }
                val message = Configuration.config.getString("error.strings.window_manager_cannot_open_new_window")
                Assert.fail(message, exception)
            }
        }
    }

    /**
     * Закрытие текущего активного окна.
     *
     * В случае, когда из доступных окон текущее окно единственное, будет вызван метод
     * закрытия драйвера 'DriverFactory.closeDriver()'. В случае, когда текущее окно не
     * единственное, будет осуществлено закрытие текущего окна и переключение на любое
     * из доступных окон.
     *
     * @see DriverFactory.closeDriver()
     */
    fun closeCurrentWindow() {
        logger.debug("Try to close current window \"$activeWindow\" ($activeWindowHandle)...")
        if (windowHandles.size == 1) {
            DriverFactory.closeDriver()
        } else {
            val (anotherWindow, _)= windowHandles.filterNot { it.key == activeWindow }.toList()[0]
            closeCurrentWindowAndSwitch(anotherWindow)
        }
    }

    /**
     * Закрытие текущего активного окна с переключение на другое окно.
     *
     * Перед выполнением метода происходит проверка наличия окна 'windowName' в 'windowHandles'.
     * Переключение на окно осуществляется путем вызова метода 'switchToWindow'.
     *
     * @see switchToWindow
     * @param windowName название окна, на которое необходимо переключиться.
     */
    fun closeCurrentWindowAndSwitch(windowName: String) {
        val errorMessage = Configuration.config.getString("error.strings.window_manager_not_contains_window")
        Assert.assertTrue(windowHandles.containsKey(windowName), errorMessage)
        logger.debug("Try to close current window \"$activeWindow\" ($activeWindowHandle)...")
        DriverFactory.getDriver().close()
        waitWhenCurrentWindowClosing()
        windowHandles.remove(activeWindow)
        switchToWindow(windowName)
    }

    /**
     * Обработка случаев, когда текущее окно было закрыто по каким-либо причинам.
     *
     * Перед выполнением метода происходит проверка наличия окна 'windowName' в 'windowHandles'.
     * Переключение на окно осуществляется путем вызова метода 'switchToWindow'.
     *
     * @see switchToWindow
     * @param windowName название окна, на которое необходимо переключиться.
     */
    fun autoCloseCurrentWindowAndSwitch(windowName: String) {
        val errorMessage = Configuration.config.getString("error.strings.window_manager_not_contains_window")
        Assert.assertTrue(windowHandles.containsKey(windowName), errorMessage)
        waitWhenCurrentWindowClosing()
        windowHandles.remove(activeWindow)
        switchToWindow(windowName)
    }

    /**
     * Проверка того закрыто ли текущее активное окно.
     *
     * Проверка осуществляется путем попытки переключиться на текущее окно. В случае
     * успешного переключения функция вернет 'false' и 'true' в противном случае.
     *
     * @return false в случае, когда окно все еще открыто, true в противном случае.
     */
    fun isWindowClosed(): Boolean = try {
        DriverFactory.getDriver().switchTo().window(activeWindowHandle)
        false
    } catch (_: NoSuchWindowException) {
        true
    }

    /**
     * Ожидание появления хендла нового окна.
     *
     * Данная функция проверяет наличие нового хендла окна у driver'а путем получения
     * набора хендлов окон с последующим удалением старого набора хендлов `oldWindows`.
     * В случае присутствия нового хендла, не содержащегося в старом наборе, данный
     * метод вернет его, обернув в экземпляр `ExpectedCondition`.
     *
     * @see ExpectedCondition
     * @param oldWindows набор хендлов окон.
     * @return экземпляр ExpectedCondition.
     */
    private fun anyWindowOtherThan(oldWindows: Set<String>): ExpectedCondition<String?> = ExpectedCondition { driver ->
        driver?.let {
            val handles = driver.windowHandles
            handles.removeAll(oldWindows)
            if (handles.size > 0) handles.iterator().next() else null
        }
    }

    /**
     * Ожидание закрытия текущего активного окна.
     *
     * Проверка закрытия окна осуществляется путем вызова метода 'isWindowClosed'.
     * В том случае, когда окно не закрылось осуществляется ожидание, заданное в
     * конфигурации, и переповтор проверки закрытия окна.
     *
     * @see isWindowClosed
     */
    private fun waitWhenCurrentWindowClosing() {
        logger.debug("Wait closing current window \"$activeWindow\" ($activeWindowHandle)...")
        for (i in 0..5) {
            if (isWindowClosed()) {
                logger.debug("Current window closed.")
                break
            }
            logger.debug("Current window not closed. Try again.")
            val timeout = Configuration.config.getInt("timeouts.window_manager_between_close_attempts_timeout")
            TimeManager.waitSomeTime(timeout)
        }
        if (!isWindowClosed()) {
            Assert.fail("Не удалось дождаться закрытия окна с ключом \"" + activeWindow + "\"")
        }
    }

}

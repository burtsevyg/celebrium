package io.celebrium.web.action

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.celebrium.core.action.ActionType
import io.celebrium.core.config.Configuration
import io.celebrium.utils.JSExecutor
import io.celebrium.web.page.Locators
import io.celebrium.web.plugin.WebPluginAPI
import org.openqa.selenium.NotFoundException
import org.openqa.selenium.TimeoutException
import org.openqa.selenium.WebDriverException
import org.slf4j.LoggerFactory

/**
 * Класс MouseOver.
 * -------------------
 *
 * Экземпляр данного класса отвечает за выполнение наведения указателя мыши на элемент веб-страницы.
 * Конструктору класса необходимо передать локаторы элементов и список плагинов.
 *
 * Пример использования в WebPage:
 * ```kotlin
 * TODO: пример!
 * ```
 *
 * @param locators локаторы элементов.
 * @param plugins плагины.
 * @author EMurzakaev@it.ru.
 */
class MouseOver(locators: Locators, plugins: List<WebPluginAPI>) : ActionBuilder<MouseOver>(ActionType.MOUSE_OVER, locators, plugins) {

    /**
     * Инициализация экземпляра MouseOver.
     *
     * Если в конфигурации ключ `enable_action_default_title.default_mouse_over_action_title` равен true,
     * то полю `title` присваивается заголовок действия по умолчанию.
     * Так же инициализируется логгер.
     */
    init {
        if (Configuration.config.getBoolean("enable_action_default_title.default_${type.toString().toLowerCase()}_action_title")) {
            title = Configuration.config.getString("strings.default_${type.toString().toLowerCase()}_action_title")
        }
        logger = LoggerFactory.getLogger(MouseOver::class.java) as Logger
        logger.level = Level.toLevel(Configuration.config.getString("log_level"))
    }

    /**
     * Выполнение действия.
     *
     * Перед и после выполнения действия вызываются методы `beforeAction`
     * и `afterAction` у зарегистрированных плагинов.
     *
     * @see WebPluginAPI.beforeAction
     * @see WebPluginAPI.afterAction
     */
    fun perform() {
        logger.debug("Perform action $type")
        plugins.forEach { it.beforeAction(this) }
        perform(timeout)
        plugins.forEach { it.afterAction(this) }
    }

    /**
     * Выполнение действия.
     *
     * Поиск элементов осуществляется путем вызова метода `innerFindElements`. В случае отсутствия
     * требуемых элементов на странице осуществляется вызов метода `retry`, корректировка таймаута
     * и повтор действия.
     *
     * После нахождения элементов по заданному локатору осуществляется выполнение js кода наведения
     * курсора мыши на первый элемент.
     *
     * @see ActionBuilder.innerFindElements
     * @see ActionBuilder.retry
     * @param timeout таймаут выполнения действия.
     */
    private fun perform(timeout: Long) {
        val (elements, findTime) = innerFindElements()
        try {
            if (elements == null) {
                throw NotFoundException()
            }
            logger.debug("Perform mouse over action")
            val js = "var event = document.createEvent(\"MouseEvent\");\n" +
                    "event.initMouseEvent(\"mouseover\", true, true, null, 0, 0, 0, 100, 100, true, true, true, null, 1, null);\n" +
                    "arguments[0].dispatchEvent(event);"
            JSExecutor.executeJS(js, elements[0])
        } catch (exception: WebDriverException) {
            val time = retry()
            logger.trace("Current timeout : $timeout. Time left : ${timeout - (time + findTime)}.")
            if (timeout - (time + findTime) > 0) {
                perform(timeout - (time + findTime))
            } else {
                handleException(TimeoutException("Timeout (${super.timeout / 1000} s) mouse over on element located with : ${getLocator()}", exception))
            }
        }
    }

}

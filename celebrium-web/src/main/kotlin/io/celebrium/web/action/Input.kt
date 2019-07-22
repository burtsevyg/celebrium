package io.celebrium.web.action

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.celebrium.core.action.ActionType
import io.celebrium.core.config.Configuration
import io.celebrium.web.page.Locators
import io.celebrium.web.plugin.WebPluginAPI
import org.openqa.selenium.NotFoundException
import org.openqa.selenium.TimeoutException
import org.openqa.selenium.WebDriverException
import org.slf4j.LoggerFactory

/**
 * Класс InputBuilder.
 * -------------------
 *
 * Экземпляр данного класса отвечает за выполнение ввода значения в элемент веб-страницы.
 * Конструктору класса необходимо передать локаторы элементов и список плагинов.
 *
 * Пример использования в WebPage:
 * ```kotlin
 * input()
 *      .errorMessage("Произошла ошибка во время выполнения ввода!")
 *      .errorDescription("Ошибка ввода значения \"12345\"")
 *      .template("Строка ввода")
 *      .clear()
 *      .value("12345")
 *      .perform()
 * ```
 *
 * @param locators локаторы элементов.
 * @param plugins плагины.
 * @author EMurzakaev@it.ru.
 */
class InputBuilder(locators: Locators, plugins: List<WebPluginAPI>) : ActionBuilder<InputBuilder>(ActionType.INPUT, locators, plugins) {

    /**
     * Инициализация экземпляра InputBuilder.
     *
     * Если в конфигурации ключ `enable_action_default_title.default_input_action_title` равен true,
     * то полю `title` присваивается заголовок действия по умолчанию.
     * Так же инициализируется логгер.
     */
    init {
        if (Configuration.config.getBoolean("enable_action_default_title.default_${type.toString().toLowerCase()}_action_title")) {
            title = Configuration.config.getString("strings.default_${type.toString().toLowerCase()}_action_title")
        }
        logger = LoggerFactory.getLogger(InputBuilder::class.java) as Logger
        logger.level = Level.toLevel(Configuration.config.getString("log_level"))
    }

    /**
     * Установть значение для <code>value</code>.
     *
     * @see ActionBuilder.value
     * @param value значение для <code>value</code>.
     * @return ссылку на InputBuilder.
     */
    fun value(value: String) = apply {
        super.value = value
    }

    /**
     * Установть значение true для <code>clear</code>.
     *
     * @see ActionBuilder.clear
     * @return ссылку на InputBuilder.
     */
    fun clear() = apply {
        super.clear = true
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
     * После нахождения элементов по заданному локатору осуществляется ввод значения `value` в первый элемент.
     *
     * @see ActionBuilder.innerFindElements
     * @see ActionBuilder.retry
     * @param timeout таймаут выполнения действия.
     */
    private fun perform(timeout: Long) {
        if (value.isEmpty()) {
            logger.warn(Configuration.config.getString("warn.strings.using_input_without_set_value"))
        }
        val (elements, findTime) = innerFindElements()
        try {
            if (elements == null) {
                throw NotFoundException()
            }
            logger.debug("Perform input action")
            if (clear) {
                elements[0].clear()
            }
            elements[0].sendKeys(value)
        } catch (exception: WebDriverException) {
            val time = retry()
            logger.trace("Current timeout : $timeout. Time left : ${timeout - (time + findTime)}.")
            if (timeout - (time + findTime) > 0) {
                perform(timeout - (time + findTime))
            } else {
                handleException(TimeoutException("Timeout (${super.timeout / 1000} s) input to element located with : ${getLocator()}", exception))
            }
        }
    }

}

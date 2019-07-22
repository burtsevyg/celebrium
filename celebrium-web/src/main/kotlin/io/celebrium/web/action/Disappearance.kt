package io.celebrium.web.action

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.celebrium.core.action.ActionType
import io.celebrium.core.config.Configuration
import io.celebrium.web.page.Locators
import io.celebrium.web.plugin.WebPluginAPI
import org.openqa.selenium.TimeoutException
import org.openqa.selenium.WebDriverException
import org.slf4j.LoggerFactory

/**
 * Класс DisappearanceBuilder.
 * ---------------------------
 *
 * Экземпляр данного класса отвечает за ожидание исчезновения элемента на веб-странице.
 * Конструктору класса необходимо передать локаторы элементов и список плагинов.
 *
 * Пример использования в WebPage:
 * ```kotlin
 * val result: Boolean = disappearance()
 *      .template("Кнопка поиска")
 *      .parameters("Найти")
 *      .timeout(10)
 *      .require()
 *      .perform()
 * ```
 * Метод `.perform()` возвращает Boolean как результат выполнения операции, true - элемент исчез со страницы,
 * false - не исчез со страницы за отведенный таймаут.
 * При установке `.require()` в случае, когда элемент по истечению таймаута все еще находитсяна странице,
 * будет обработана ошибка. В случае отсутствия параметра `.require()` при нахождении элемента и истечению
 * таймаута будет возвращен `false`.
 *
 * @param locators локаторы элементов.
 * @param plugins плагины.
 * @author EMurzakaev@it.ru.
 */
class DisappearanceBuilder(locators: Locators, plugins: List<WebPluginAPI>) : ActionBuilder<DisappearanceBuilder>(ActionType.DISAPPEARANCE, locators, plugins) {

    /**
     * Инициализация экземпляра DisappearanceBuilder.
     *
     * Если в конфигурации ключ `enable_action_default_title.default_disappearance_action_title` равен true,
     * то полю `title` присваивается заголовок действия по умолчанию.
     * Так же инициализируется логгер.
     */
    init {
        if (Configuration.config.getBoolean("enable_action_default_title.default_${type.toString().toLowerCase()}_action_title")) {
            title = Configuration.config.getString("strings.default_${type.toString().toLowerCase()}_action_title")
        }
        logger = LoggerFactory.getLogger(DisappearanceBuilder::class.java) as Logger
        logger.level = Level.toLevel(Configuration.config.getString("log_level"))
    }

    /**
     * Установка обязательности исчезновения элемента на странице.
     *
     * Если в билдере выбран данный параметр, а элемент по истечению таймаута все
     * еще присутствует на странице то будет обработана ошибка в `handleException`.
     *
     * @see ActionBuilder.require
     * @see ActionBuilder.handleException
     * @return ссылку на билдер.
     */
    fun require() = apply {
        this.require = true
    }

    /**
     * Выполнение действия.
     *
     * Перед и после выполнения действия вызываются методы `beforeAction`
     * и `afterAction` у зарегистрированных плагинов.
     *
     * @see WebPluginAPI.beforeAction
     * @see WebPluginAPI.afterAction
     * @return Boolean значение, true - элемент исчез со страницы, false - не исчез.
     */
    fun perform(): Boolean {
        logger.debug("Perform action $type")
        plugins.forEach { it.beforeAction(this) }
        val result = perform(timeout)
        plugins.forEach { it.afterAction(this) }
        return result
    }

    /**
     * Выполнение действия.
     *
     * Поиск элементов осуществляется путем вызова метода `innerFindElements`. В случае отсутствия
     * требуемых элементов на странице осуществляется вызов метода `retry`, корректировка таймаута
     * и повтор действия.
     *
     * @see ActionBuilder.innerFindElements
     * @see ActionBuilder.retry
     * @param timeout таймаут выполнения действия.
     * @return Boolean значение, true - элемент исчез со страницы, false - не исчез.
     */
    private fun perform(timeout: Long): Boolean {
        logger.debug("Wait for disappearance elements located with ${getLocator()}")
        val (elements, findTime) = innerFindElements()
        var result = false
        try {
            if (elements != null && elements.any { it.isDisplayed }) {
                throw WebDriverException()
            }
            logger.debug("Elements not presents")
            result = true
        } catch (exception: WebDriverException) {
            val time = retry()
            logger.trace("Current timeout : $timeout. Time left : ${timeout - (time + findTime)}.")
            if (timeout - (time + findTime) > 0) {
                return perform(timeout - (time + findTime))
            } else {
                if (require) {
                    handleException(TimeoutException("Timeout (${super.timeout / 1000} s) waiting disappearance elements located with : ${getLocator()}", exception))
                }
            }
        }
        return result
    }

}

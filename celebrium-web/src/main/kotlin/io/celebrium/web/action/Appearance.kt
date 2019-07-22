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
import org.openqa.selenium.WebElement
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Класс AppearanceBuilder.
 * ------------------------
 *
 * Экземпляр данного класса отвечает за появление элемента на веб-странице.
 * Конструктору класса необходимо передать локаторы элементов и список плагинов.
 *
 * Пример использования в WebPage:
 * ```kotlin
 * val element: Optional<WebElement> = appearance()
 *      .template("Кнопка поиска")
 *      .parameters("Найти")
 *      .require()
 *      .perform()
 * ```
 * Метод `.perform()` возвращает `Optional<WebElement>` как результат выполнения операции.
 * При установке `.require()` в случае не нахождения элемента на странице будет обработана ошибка.
 * В случае отсутствия параметра `.require()` при не нахождении элемента будет возвращен `Optional.empty<WebElement>()`.
 *
 * @param locators локаторы элементов.
 * @param plugins плагины.
 * @author EMurzakaev@it.ru.
 */
class AppearanceBuilder(locators: Locators, plugins: List<WebPluginAPI>) : ActionBuilder<AppearanceBuilder>(ActionType.APPEARANCE, locators, plugins) {

    /**
     * Инициализация экземпляра AppearanceBuilder.
     *
     * Если в конфигурации ключ `enable_action_default_title.default_appearance_action_title` равен true,
     * то полю `title` присваивается заголовок действия по умолчанию.
     * Так же инициализируется логгер.
     */
    init {
        if (Configuration.config.getBoolean("enable_action_default_title.default_${type.toString().toLowerCase()}_action_title")) {
            title = Configuration.config.getString("strings.default_${type.toString().toLowerCase()}_action_title")
        }
        logger = LoggerFactory.getLogger(AppearanceBuilder::class.java) as Logger
        logger.level = Level.toLevel(Configuration.config.getString("log_level"))
    }

    /**
     * Установка обязательности появления элемента на странице.
     *
     * Если в билдере выбран данный параметр, а элемент не найден на странице,
     * то будет обработана ошибка в `handleException`.
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
     * @return Optional, хранящий экземпляр WebElement в случае нахождения элемента на странице,
     * пустой Optional, если элемент не присутствует на странице.
     */
    fun perform(): Optional<WebElement> {
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
     * и повтор действия. В качетсве результа выполнения метода возвращаетя первый из найденных
     * элементов, завернутый в `Optional`.
     *
     * @see ActionBuilder.innerFindElements
     * @see ActionBuilder.retry
     * @param timeout таймаут выполнения действия.
     * @return Optional, хранящий экземпляр WebElement в случае нахождения элемента на странице,
     * пустой Optional, если элемент не присутствует на странице.
     */
    private fun perform(timeout: Long): Optional<WebElement> {
        logger.debug("Wait for appearance elements located with ${getLocator()}")
        val (elements, findTime) = innerFindElements()
        try {
            if (elements == null) {
                throw NotFoundException()
            }
            logger.debug("Elements presents")
            return Optional.of(elements[0])
        } catch (exception: WebDriverException) {
            val time = retry()
            logger.trace("Current timeout : $timeout. Time left : ${timeout - (time + findTime)}.")
            return if (timeout - (time + findTime) > findTime) {
                perform(timeout - (time + findTime))
            } else {
                if (require) {
                    handleException(TimeoutException("Timeout (${super.timeout / 1000} s) waiting appearance elements located with : ${getLocator()}", exception))
                }
                Optional.empty()
            }
        }
    }

}

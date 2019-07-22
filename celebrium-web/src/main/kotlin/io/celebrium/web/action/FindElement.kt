package io.celebrium.web.action

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.celebrium.core.action.ActionType
import io.celebrium.core.config.Configuration
import io.celebrium.web.page.Locators
import io.celebrium.web.plugin.WebPluginAPI
import org.openqa.selenium.TimeoutException
import org.openqa.selenium.WebElement
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Класс FindElementBuilder.
 * -------------------------
 *
 * Экземпляр данного класса отвечает за поиск элементов на веб-странице.
 * Конструктору класса необходимо передать локаторы элементов и список плагинов.
 *
 * Пример использования в WebPage:
 * ```kotlin
 * val element: Optional<WebElement> = findElement()
 *      .template("Кнопка поиска")
 *      .parameters("Найти")
 *      .timeout(2)
 *      .findFirst()
 * ```
 * Содержит различные методы поиска элементов на странице. Методы `findFirst`, `findLast`, `findAll`
 * в качестве результата возвращают экземпляр `Optional`, который может как содержать веб-элемент
 * (список веб-элементов), так и может не содержать результат выполнения поиска. Методы `getFirst`,
 * `getLast`, `getAll` в качестве результата возвращают экземпляры `WebElement` или `List<WebElement`.
 * В случае, когда вызываются методы `getFirst`, `getLast`, `getAll` и элементы не находятся на странице,
 * будет вызвана исключительная ситуация, которая будет обработана в `ActionBuilder.handleException`.
 *
 * @see ActionBuilder.handleException
 * @param locators локаторы элементов.
 * @param plugins плагины.
 * @author EMurzakaev@it.ru.
 */
class FindElementBuilder(locators: Locators, plugins: List<WebPluginAPI>) : ActionBuilder<FindElementBuilder>(ActionType.FIND_ELEMENT, locators, plugins) {

    /**
     * Инициализация экземпляра FindElementBuilder.
     *
     * Если в конфигурации ключ `enable_action_default_title.default_find_element_action_title` равен true,
     * то полю `title` присваивается заголовок действия по умолчанию.
     * Так же инициализируется логгер.
     */
    init {
        if (Configuration.config.getBoolean("enable_action_default_title.default_${type.toString().toLowerCase()}_action_title")) {
            title = Configuration.config.getString("strings.default_${type.toString().toLowerCase()}_action_title")
        }
        logger = LoggerFactory.getLogger(FindElementBuilder::class.java) as Logger
        logger.level = Level.toLevel(Configuration.config.getString("log_level"))
    }

    /**
     * Поиск последнего элемента.
     *
     * @return элемент, обернутый в Optional.
     */
    fun findLast(): Optional<WebElement> = findAll().map { elements -> elements.last() }

    /**
     * Поиск последнего элемента.
     *
     * Элемент обязательно должен присутствовать на странице, в противном случае будет
     * вызвана исключительная ситуация и обработана в `ActionBuilder.handleException`.
     *
     * @return элемент.
     */
    fun getLast(): WebElement? = getAll().lastOrNull()

    /**
     * Поиск первого элемента.
     *
     * @return элемент, обернутый в Optional.
     */
    fun findFirst(): Optional<WebElement> = findAll().map { elements -> elements.first() }

    /**
     * Поиск первого элемента.
     *
     * Элемент обязательно должен присутствовать на странице, в противном случае будет
     * вызвана исключительная ситуация и обработана в `ActionBuilder.handleException`.
     *
     * @return элемент.
     */
    fun getFirst(): WebElement? = getAll().firstOrNull()

    /**
     * Поиск элементов, подходящих по локатору.
     *
     * @return список элементов, обернутый в Optional.
     */
    fun findAll(): Optional<List<WebElement>> = Optional.ofNullable(findElements(false))

    /**
     * Поиск элементов, подходящих по локатору.
     *
     * Элементы обязательно должны присутствовать на странице, в противном случае будет
     * вызвана исключительная ситуация и обработана в `ActionBuilder.handleException`.
     *
     * @return список элементов.
     */
    fun getAll(): List<WebElement> = findElements(true).orEmpty()

    /**
     * Поиск элементов на странице.
     *
     * @param assertOnFail ключ, отвечающий за вызов исключения в случае отсутствия элементов на странице.
     * @return список веб-элементов, найденных по локатору.
     */
    private fun findElements(assertOnFail: Boolean): List<WebElement>? {
        logger.debug("Perform action $type")
        plugins.forEach { it.beforeAction(this) }
        val elements = retryFindElements(timeout)
        if (elements == null && assertOnFail) {
            handleException(TimeoutException("Timeout (${super.timeout / 1000} s) waiting element located with : ${getLocator()}"))
        }
        plugins.forEach { it.afterAction(this) }
        return elements
    }

    /**
     * Поиск элементов на странице.
     *
     * Поиск элементов осуществляется путем вызова метода `innerFindElements`. В случае отсутствия
     * требуемых элементов на странице осуществляется вызов метода `retry`, корректировка таймаута
     * и повтор действия. В качетсве результа выполнения метода возвращаются все найденные элементы..
     *
     * @see ActionBuilder.innerFindElements
     * @see ActionBuilder.retry
     * @param timeout таймаут выполнения действия.
     * @return список веб-элементов, найденных по локатору.
     */
    private fun retryFindElements(timeout: Long): List<WebElement>? {
        val (elements, findTime) = innerFindElements()
        if (elements == null) {
            val totalTime = retry()
            logger.trace("Current timeout : $timeout. Time left : ${timeout - (totalTime + findTime)}.")
            if (timeout - (totalTime + findTime) > 0) {
                return retryFindElements(timeout - (totalTime + findTime))
            }
        }
        return elements
    }

}

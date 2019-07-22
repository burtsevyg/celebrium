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

/**
 * Класс GetAttributeBuilder.
 * --------------------------
 *
 * Экземпляр данного класса отвечает за выполнение получение аттрибута элемента веб-страницы.
 * Конструктору класса необходимо передать локаторы элементов и список плагинов.
 *
 * Пример использования в WebPage:
 * ```kotlin
 * TODO: пример
 * ```
 *
 * @param locators локаторы элементов.
 * @param plugins плагины.
 * @author EMurzakaev@it.ru.
 */
class GetAttributeBuilder(locators: Locators, plugins: List<WebPluginAPI>) : ActionBuilder<GetAttributeBuilder>(ActionType.GET_ATTRIBUTE, locators, plugins) {

    /**
     * Инициализация экземпляра GetAttributeBuilder.
     *
     * Если в конфигурации ключ `enable_action_default_title.default_get_attribute_action_title` равен true,
     * то полю `title` присваивается заголовок действия по умолчанию.
     * Так же инициализируется логгер.
     */
    init {
        if (Configuration.config.getBoolean("enable_action_default_title.default_${type.toString().toLowerCase()}_action_title")) {
            title = Configuration.config.getString("strings.default_${type.toString().toLowerCase()}_action_title")
        }
        logger = LoggerFactory.getLogger(GetAttributeBuilder::class.java) as Logger
        logger.level = Level.toLevel(Configuration.config.getString("log_level"))
    }

    /**
     * Установть значение для <code>attribute</code>.
     *
     * @see ActionBuilder.attribute
     * @param attributeName значение для <code>attribute</code>.
     * @return ссылку на GetAttributeBuilder.
     */
    fun attribute(attributeName: String) = apply {
        attribute = attributeName
    }

    /**
     * Получение аттрибута элемента на странице.
     *
     * Перед и после выполнения действия вызываются методы `beforeAction`
     * и `afterAction` у зарегистрированных плагинов.
     *
     * @see WebPluginAPI.beforeAction
     * @see WebPluginAPI.afterAction
     * @return аттрибут элемента. В случае отсутствия атрибута у элемента - пустая строка.
     */
    fun get(): String {
        logger.debug("Perform action $type")
        plugins.forEach { it.beforeAction(this) }
        val result = getElementAttribute(timeout)
        plugins.forEach { it.actionResult(this, result) }
        plugins.forEach { it.afterAction(this) }
        return result
    }

    /**
     * Получение аттрибута элемента за определенное время..
     *
     * Данный метод путем вызова метода `innerFindElements` находит элементы на веб-странице
     * по заданному локатору. В случае ненахождения элементов или возникновения ошибок данный
     * метод повторяет поиск до тех пор, пока элементы не будут найдены либо не закончится таймаут.
     * В случае окончания таймаута `timeout` происходит вызов обработчика ошибок `handleException`
     * в классе `ActionBuilder`, в качестве резульата будет возвращен пустой `result`:
     * ```kotlin
     * var result: String? = null
     * ```
     * В случае, когда элементы найдены, происходит вызов метода `getAttribute` у первого найденного
     * элемента с передачей в качестве параметра необходимого аттрибута `attribute`.
     *
     * @see ActionBuilder.innerFindElements
     * @see ActionBuilder.handleException
     * @see WebElement.getAttribute
     * @param timeout таймаут выполнения действия.
     * @return значение атрибута элемента на веб-странице. В случае отсутствия атрибута у элемента - пустая строка.
     */
    private fun getElementAttribute(timeout: Long): String {
        if (attribute.isEmpty()) {
            logger.warn(Configuration.config.getString("warn.strings.using_get_attribute_without_set_attribute_value"))
        }
        val (elements, findTime) = innerFindElements()
        var result: String? = null
        try {
            if (elements == null) {
                throw NotFoundException()
            }
            result = elements[0].getAttribute(attribute)
            logger.debug("Attribute \"$attribute\" of element located by ${getLocator()} : \"$result\"")
        } catch (exception: WebDriverException) {
            val time = retry()
            if (timeout - (time + findTime) > 0) {
                return getElementAttribute(timeout - (time + findTime))
            } else {
                handleException(TimeoutException("Timeout (${super.timeout / 1000} s) get attribute of element located with : ${getLocator()}", exception))
            }
        }
        return result.orEmpty()
    }

}

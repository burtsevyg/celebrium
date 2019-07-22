package io.celebrium.web.action

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.celebrium.core.action.ActionType
import io.celebrium.core.config.Configuration
import io.celebrium.core.driver.DriverFactory
import io.celebrium.web.page.Locators
import io.celebrium.web.plugin.WebPluginAPI
import org.openqa.selenium.NotFoundException
import org.openqa.selenium.TimeoutException
import org.openqa.selenium.WebDriverException
import org.openqa.selenium.interactions.Actions
import org.slf4j.LoggerFactory

/**
 * Класс ClickBuilder.
 * -------------------
 *
 * Экземпляр данного класса отвечает за выполнение нажатия на элемент веб-страницы.
 * Конструктору класса необходимо передать локаторы элементов и список плагинов.
 *
 * Пример использования в WebPage:
 * ```kotlin
 * click()
 *      .type(ClickType.DOUBLE_CLICK)
 *      .template("Кнопка поиска")
 *      .parameters("Найти")
 *      .perform()
 * ```
 *
 * @param locators локаторы элементов.
 * @param plugins плагины.
 * @author EMurzakaev@it.ru.
 */
class ClickBuilder(locators: Locators, plugins: List<WebPluginAPI>) : ActionBuilder<ClickBuilder>(ActionType.CLICK, locators, plugins) {

    /**
     * Инициализация экземпляра ClickBuilder.
     *
     * Если в конфигурации ключ `enable_action_default_title.default_click_action_title` равен true,
     * то полю `title` присваивается заголовок действия по умолчанию.
     * Так же инициализируется логгер.
     */
    init {
        if (Configuration.config.getBoolean("enable_action_default_title.default_${type.toString().toLowerCase()}_action_title")) {
            title = Configuration.config.getString("strings.default_${clickType.toString().toLowerCase()}_action_title")
        }
        logger = LoggerFactory.getLogger(ClickBuilder::class.java) as Logger
        logger.level = Level.toLevel(Configuration.config.getString("log_level"))
    }

    /**
     * Установть значение для <code>clickType</code>.
     *
     * @see ActionBuilder.clickType
     * @param clickType тип нажатия на элемент.
     * @return ссылку на ClickBuilder.
     */
    fun type(clickType: ClickType) = apply {
        super.clickType = clickType
        if (Configuration.config.getBoolean("enable_action_default_title.default_${type.toString().toLowerCase()}_action_title")) {
            title = Configuration.config.getString("strings.default_${clickType.toString().toLowerCase()}_action_title")
        }
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
     * После нахождения элементов по заданному локатору осуществляется нажатие на первый элемент.
     * Тип нажатия задается методом `type`, в зависимости от типа выполняется нажатие на элемент
     * левой, правой кнопкой мыши либо жвойное нажатие левой кнопкой мыши на элемент.
     *
     * @see ActionBuilder.innerFindElements
     * @see ActionBuilder.retry
     * @see type
     * @param timeout таймаут выполнения действия.
     */
    private fun perform(timeout: Long) {
        val (elements, findTime) = innerFindElements()
        try {
            if (elements == null) {
                throw NotFoundException()
            }
            logger.debug("Perform ${clickType.toString().toLowerCase()} action")
            when (clickType) {
                ClickType.LEFT_CLICK -> elements[0].click()
                ClickType.RIGHT_CLICK ->
                    Actions(DriverFactory.getDriver())
                            .contextClick(elements[0])
                            .perform()
                ClickType.DOUBLE_CLICK ->
                    Actions(DriverFactory.getDriver())
                            .doubleClick(elements[0])
                            .perform()
            }
        } catch (exception: WebDriverException) {
            val time = retry()
            logger.trace("Current timeout : $timeout. Time left : ${timeout - (time + findTime)}.")
            if (timeout - (time + findTime) > 0) {
                perform(timeout - (time + findTime))
            } else {
                handleException(TimeoutException("Timeout (${super.timeout / 1000} s) click on element located with : ${getLocator()}", exception))
            }
        }
    }

}

/**
 * Типы нажатия на элемент на странице.
 *
 *
 * @author EMurzakaev@it.ru.
 */
enum class ClickType {
    /**
     * Нажатие на элемент левой кнопкой мыши.
     */
    LEFT_CLICK,
    /**
     * Нажатие на элемент правой кнопкой мыши.
     */
    RIGHT_CLICK,
    /**
     * Двойное наждатие на элемент левой кнопкой мыши.
     */
    DOUBLE_CLICK

}

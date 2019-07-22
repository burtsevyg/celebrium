package io.celebrium.web.action

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.celebrium.core.action.ActionType
import io.celebrium.core.config.Configuration
import io.celebrium.core.driver.DriverFactory
import io.celebrium.web.page.Locators
import io.celebrium.web.plugin.WebPluginAPI
import org.openqa.selenium.Keys
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.interactions.CompositeAction
import org.slf4j.LoggerFactory

/**
 * Класс SendKeyBuilder.
 * ---------------------
 *
 * Экземпляр данного класса отвечает за выполнение нажатия клавиш Keys.
 * Конструктору класса необходимо передать локаторы элементов и список плагинов.
 *
 * Пример использования в WebPage:
 * ```kotlin
 * TODO: пример
 * ```
 *
 * @see Keys
 * @param locators локаторы элементов.
 * @param plugins плагины.
 * @author EMurzakaev@it.ru.
 */
class SendKeyBuilder(locators: Locators, plugins: List<WebPluginAPI>) : ActionBuilder<SendKeyBuilder>(ActionType.SEND_KEY, locators, plugins) {

    /**
     * Инициализация экземпляра SendKeyBuilder.
     *
     * Если в конфигурации ключ `enable_action_default_title.default_send_key_action_title` равен true,
     * то полю `title` присваивается заголовок действия по умолчанию.
     * Так же инициализируется логгер.
     */
    init {
        if (Configuration.config.getBoolean("enable_action_default_title.default_${type.toString().toLowerCase()}_action_title")) {
            title = Configuration.config.getString("strings.default_${type.toString().toLowerCase()}_action_title")
        }
        logger = LoggerFactory.getLogger(SendKeyBuilder::class.java) as Logger
        logger.level = Level.toLevel(Configuration.config.getString("log_level"))
        keys = listOf()
    }

    /**
     * Установть значение для <code>key</code>.
     *
     * @see ActionBuilder.keys
     * @param key значение для <code>key</code>.
     * @return ссылку на SendKeyBuilder.
     */
    fun key(key: Keys) = apply {
        keys = keys?.plus(key)
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
        val action = CompositeAction()
        super.keys?.forEach { key ->
            action.addAction(
                    Actions(DriverFactory.getDriver())
                            .sendKeys(key)
                            .build()
            )
        }
        action.perform()
        plugins.forEach { it.afterAction(this) }
    }

}

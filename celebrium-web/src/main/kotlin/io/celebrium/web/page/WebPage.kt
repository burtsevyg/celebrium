package io.celebrium.web.page

import ch.qos.logback.classic.Logger
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.celebrium.core.config.Configuration
import io.celebrium.core.page.AbstractPage
import io.celebrium.web.action.*
import io.celebrium.web.plugin.DefaultWebPlugin
import io.celebrium.web.plugin.WebPluginAPI
import org.openqa.selenium.By
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Класс WebPage.
 * --------------
 *
 *
 *
 * ```kotlin
 * TODO: пример
 * class MainPage(fileName: String) : WebPage(fileName) {
 *    ...
 * }
 * ```
 *
 *
 * @author EMurzakaev@it.ru.
 */
abstract class WebPage : AbstractPage<WebPluginAPI> {

    /**
     * Конструктор.
     *
     * В случае вызова подобного конструктора локаторам будет передан пустой конфигурационный файл.
     */
    constructor() {
        locators = Locators(ConfigFactory.empty())
    }

    /**
     * Конструктор с указанием файла конфигурации страницы.
     *
     * @param fileName название файла конфигурации страницы.
     */
    constructor(fileName: String) {
        init(fileName)
    }

    /**
     * Конструктор с указанием конфигурации страницы.
     *
     * @param config конфигурация страницы.
     */
    constructor(config: Config) {
        init(config)
    }

    /**
     * Список плагинов страницы.
     */
    override var plugins: List<WebPluginAPI> = ArrayList()
    /**
     * Xpath локаторы веб элементов на странице.
     */
    lateinit var locators: Locators

    /**
     * Инициализировать page object.
     *
     * @param fileName путь до файла, содержащего конфигурацию.
     */
    override final fun init(fileName: String) {
        init(ConfigFactory.parseFile(File(fileName)).resolve())
    }

    /**
     * Инициализировать page object.
     *
     * @param config конфигурация.
     */
    override final fun init(config: Config) {
        if (!config.hasPath("xpath")) {
            throw RuntimeException(Configuration.config.getString("error.strings.no_xpaths_present_in_config_of_page"))
        }
        locators = Locators(config.getConfig("xpath"))
        WebPage.defaultPluginsSingletons.forEach { plugin -> registerPlugin(plugin) }
        WebPage.defaultPlugins.forEach { plugin -> registerPlugin(plugin.newInstance()) }
    }

    /**
     * Ожидание появления элемента на странице.
     *
     * @return экземпляр <code>AppearanceBuilder</code>.
     */
    fun appearance() = AppearanceBuilder(locators, plugins)

    /**
     * Получение атрибута элемента.
     *
     * @return экземпляр <code>GetAttributeBuilder</code>.
     */
    fun attribute() = GetAttributeBuilder(locators, plugins)

    /**
     * Нажатие по элементу.
     *
     * @return экземпляр <code>ClickBuilder</code>.
     */
    fun click() = ClickBuilder(locators, plugins)

    /**
     * Ожидание исчезновения элемента.
     *
     * @return экземпляр <code>CDisappearanceBuilder</code>.
     */
    fun disappearance() = DisappearanceBuilder(locators, plugins)

    /**
     * Поиск элементов на странице.
     *
     * @return экземпляр <code>FindElementBuilder</code>.
     */
    fun findElement() = FindElementBuilder(locators, plugins)

    /**
     * Получение текста элементов.
     *
     * @return экземпляр <code>GetTextBuilder</code>.
     */
    fun text() = GetTextBuilder(locators, plugins)

    /**
     * Ввод текста в текстовое поле.
     *
     * @return экземпляр <code>InputBuilder</code>.
     */
    fun input() = InputBuilder(locators, plugins)

    /**
     * Наведение указателя мыши на элемент.
     *
     * @return экземпляр <code>MouseOver</code>.
     */
    fun mouseOver() = MouseOver(locators, plugins)

    /**
     * Выбор значения из выпадающего списка.
     *
     * @return экземпляр <code>SelectBuilder</code>.
     */
    fun select() = SelectBuilder(locators, plugins)

    /**
     * нажатие клавиш.
     *
     * @return экземпляр <code>SendKeyBuilder</code>.
     */
    fun sendKeys() = SendKeyBuilder(locators, plugins)

    /**
     * Объект-компаьон.
     */
    companion object {

        /**
         * Логгер.
         */
        val logger: Logger = LoggerFactory.getLogger(WebPage::class.java) as Logger

        /**
         * Список классов плагинов по умолчанию.
         *
         * В методе инициализации страницы для страницы будет создан и зарегистрирован новый экземпляр плагина.
         */
        var defaultPlugins: List<Class<out WebPluginAPI>> = listOf(DefaultWebPlugin::class.java)

        /**
         * Список плагинов по умолчанию.
         *
         * В методе инициализации страницы для страницы будет зарегистрирован каждый экземпляр плагина.
         */
        var defaultPluginsSingletons: List<WebPluginAPI> = emptyList()

        /**
         * Регистрация класса плагина по умолчанию.
         *
         * Плагин должен реализовывать интерфейс WebPluginAPI.
         *
         * @see WebPluginAPI
         * @param plugin класс плагина.
         */
        @JvmStatic
        fun <T : WebPluginAPI> registerDefaultPlugin(plugin: Class<T>) {
            logger.info("Register plugin ${plugin.name}")
            defaultPlugins = defaultPlugins.plus(plugin)
        }

        /**
         * Регистрация класса плагина по умолчанию.
         *
         * @see WebPluginAPI
         * @param plugin плагин, экземпляр класса, реализующего интерфейс WebPluginAPI.
         */
        @JvmStatic
        fun registerDefaultPlugin(plugin: WebPluginAPI) {
            logger.info("Register plugin ${plugin.javaClass.name}")
            defaultPluginsSingletons = defaultPluginsSingletons.plus(plugin)
        }

    }

}

/**
 * Класс Locators.
 * ---------------
 *
 * Модель, хранящая список локаторов элементов на странице.
 *
 * @param xpath конфигурация, содержащая xpath строки.
 * @author EMurzakaev@it.ru.
 */
data class Locators(private val xpath: Config) {

    /**
     * Получение xpath выражения из конфигурации по названию шаблона и параметрам.
     *
     * @param templateName название xpath шаблона.
     * @param parameters параметры для xpath.
     * @return xpath выражение в виде строки.
     */
    fun getXpath(templateName: String, parameters: List<String>?): String {
        if (parameters != null) {
            return getXpath(templateName, *parameters.toTypedArray())
        } else {
            return getXpath(templateName)
        }
    }

    /**
     * Получение xpath выражения из конфигурации по названию шаблона и параметрам.
     *
     * @param templateName название xpath шаблона.
     * @param parameters параметры для xpath.
     * @return xpath выражение в виде строки.
     */
    fun getXpath(templateName: String, vararg parameters: String): String {
        if (!xpath.hasPath(templateName)) {
            val message = Configuration.config.getString("error.strings.no_xpath_present_in_config_of_page").replace(Regex("\\\$templateName"), templateName)
            throw RuntimeException(message)
        }
        if (parameters.asList().isEmpty()) {
            return xpath.getString(templateName)
        } else {
            return String.format(xpath.getString(templateName), *parameters)
        }
    }

    /**
     * Построение xpath выражения по названию шаблона и параметрам.
     *
     * @see By
     * @param templateName название xpath шаблона.
     * @param parameters параметры для xpath.
     * @return экземпляр класса By.
     */
    fun buildXpath(templateName: String, parameters: List<String>?): By = By.xpath(getXpath(templateName, parameters))

    /**
     * Построение xpath выражения по названию шаблона и параметрам.
     *
     * @see By
     * @param templateName название xpath шаблона.
     * @param parameters параметры для xpath.
     * @return экземпляр класса By.
     */
    fun buildXpath(templateName: String, vararg parameters: String): By = By.xpath(getXpath(templateName, *parameters))

}

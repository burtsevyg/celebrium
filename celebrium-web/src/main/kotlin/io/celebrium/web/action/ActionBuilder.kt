package io.celebrium.web.action

import ch.qos.logback.classic.Logger
import io.celebrium.core.action.AbstractAction
import io.celebrium.core.action.ActionType
import io.celebrium.core.config.Configuration
import io.celebrium.core.driver.DriverFactory
import io.celebrium.core.test.AssertType
import io.celebrium.core.test.Error
import io.celebrium.utils.JSExecutor
import io.celebrium.web.page.Locators
import io.celebrium.web.plugin.WebPluginAPI
import org.openqa.selenium.*
import org.testng.Assert
import java.lang.Exception
import java.util.concurrent.TimeUnit
import java.util.regex.Matcher

/**
 * Класс ActionBuilder.
 * --------------------
 *
 * Является общей частью для экшен билдеров.
 *
 * @param type тип действия.
 * @param locators локаторы элементов.
 * @param plugins плагины.
 * @author EMurzakaev@it.ru.
 */
abstract class ActionBuilder<T : ActionBuilder<T>>(override val type: ActionType, protected val locators: Locators, protected val plugins: List<WebPluginAPI>) : AbstractAction {

    /**
     * Логгер.
     */
    lateinit var logger: Logger
        protected set

    /**
     * Тип ошибки.
     */
    var assertType: AssertType = AssertType.BLOCK
        protected set
    /**
     * Аттрибут, используется в GetAttributeBuilder.
     *
     * @see GetAttributeBuilder
     */
    var attribute: String = ""
        protected set
    /**
     * Параметр, отвечающий за необходимость очистки поля перед вводом в него значения.
     * Используется в InputBuilder.
     *
     * @see InputBuilder
     */
    var clear: Boolean = false
        protected set
    /**
     * Тип нажатия кнопки мыши. Используется в ClickBuilder.
     *
     * @see ClickBuilder
     */
    var clickType: ClickType = ClickType.LEFT_CLICK
        protected set
    /**
     * Сообщение об ошибке.
     */
    var errorMessage: String? = null
        protected set
    /**
     * Дополнительное сообщение об ошибке.
     */
    var errorDescription: String? = null
        protected set
    /**
     * Параметр, отвечающий за отключение вложений.
     */
    var disableAttachments: Boolean = false
        protected set
    /**
     * Список кнопок, которые необходимо нажать. Используется в SendKeyBuilder.
     *
     * @see SendKeyBuilder
     */
    var keys: List<Keys>? = null
        protected set
    /**
     * Список параметров, подставляемых в шаблон xpath выражения.
     */
    var parameters: List<String>? = null
        protected set
    /**
     * Параметр, отвечающий за необходимость появления/исчезновения элемента на странице.
     * Используется в AppearanceBuilder и DisappearanceBuilder.
     *
     * @see AppearanceBuilder
     * @see DisappearanceBuilder
     */
    var require: Boolean = false
        protected set
    /**
     * Блок кода, выполняемый в промежутках попыток выполнить действие.
     * Используется в методе <code>retry</code>.
     */
    var retry: () -> Unit = {}
        protected set
    /**
     * Минимальное временя выполнения действия.
     * Используется в методе <code>retry</code>.
     */
    var retryMinTime: Long = TimeUnit.SECONDS.toMillis(Configuration.config.getLong("timeouts.min_action_timeout"))
        protected set
    /**
     * Название шаблона xpath выражения.
     *
     * @see Locators
     */
    var template: String? = null
        protected set
    /**
     * Таймаут времени выполнения действия в миллисекундах.
     * По умолчанию берется таймаут из конфигурации (в конфигурации таймаут указан в секундах).
     *
     * @see Configuration
     */
    var timeout: Long = TimeUnit.SECONDS.toMillis(Configuration.config.getLong("timeouts.default_action_timeout"))
        protected set
    /**
     * Заголовок выполняемого действия.
     * Может использоваться в отчетах. Инициализируется в билдерах-наследниках, значения берутся из конфигурации.
     */
    var title: String = ""
        protected set
    /**
     * Значение, выбираемое или заполняемое действием.
     * Используется в InputBuilder и SelectBuilder.
     *
     * @see InputBuilder
     * @see SelectBuilder
     */
    var value: String = ""
        protected set
    /**
     * Параметр, отвечающий за видимость элемента на странице.
     *
     * Действия click, input, select не могут быть произведены с невидимым элементом.
     * Для действий findElement, text, attribute, mouseOver в случае невидимости элемента
     * необходимо задать этот параметр false.
     *
     * @see io.celebrium.web.page.WebPage.click
     * @see io.celebrium.web.page.WebPage.input
     * @see io.celebrium.web.page.WebPage.select
     * @see io.celebrium.web.page.WebPage.findElement
     * @see io.celebrium.web.page.WebPage.text
     * @see io.celebrium.web.page.WebPage.attribute
     * @see io.celebrium.web.page.WebPage.mouseOver
     */
    var visible: Boolean = true
        protected set
    /**
     * Xpath выражение, используется при поиске элемента (-ов) на странице.
     */
    var xpath: String? = null
        protected set

    /**
     * Установть значение для <code>assertType</code>.
     *
     * @param init значение для <code>assertType</code>.
     * @return ссылку на ActionBuilder.
     */
    @Suppress("UNCHECKED_CAST")
    fun assertType(init: AssertType): T {
        assertType = init
        return this as T
    }

    /**
     * Установть значение для <code>errorMessage</code>.
     *
     * @param init значение для <code>errorMessage</code>.
     * @return ссылку на ActionBuilder.
     */
    @Suppress("UNCHECKED_CAST")
    fun errorMessage(init: String): T {
        errorMessage = init
        return this as T
    }

    /**
     * Установить значение для <code>errorDescription</code>.
     *
     * @param init значение для <code>errorDescription</code>.
     * @return ссылку на ActionBuilder.
     */
    @Suppress("UNCHECKED_CAST")
    fun errorDescription(init: String): T {
        errorDescription = init
        return this as T
    }

    /**
     * Установть значение для <code>disableAttachments</code>.
     *
     * @param init значение для <code>disableAttachments</code>.
     * @return ссылку на ActionBuilder.
     */
    @Suppress("UNCHECKED_CAST")
    fun disableAttachments(): T {
        disableAttachments = true
        return this as T
    }

    /**
     * Установть значение для <code>parameters</code>.
     *
     * @param init значение для <code>parameters</code>.
     * @return ссылку на ActionBuilder.
     */
    @Suppress("UNCHECKED_CAST")
    fun parameters(vararg init: String): T {
        parameters = init.asList()
        return this as T
    }

    /**
     * Установть значение для <code>repeatCount</code>.
     *
     * Метод поддерживает задание минимального времени выполнения повтора в секундах.
     * По умолчанию данное время берет в конфигурации. Значение `min_action_timeout`
     * в конфигурации должно быть указано в секундах.
     *
     * @param init значение для <code>repeatCount</code>.
     * @param minRetryTime минимальное время выполнения повтора в секундах.
     * @return ссылку на ActionBuilder.
     */
    @JvmOverloads
    @Suppress("UNCHECKED_CAST")
    fun retry(init: () -> Unit, minRetryTime: Long = Configuration.config.getLong("timeouts.min_action_timeout")): T {
        retry = init
        retryMinTime = TimeUnit.SECONDS.toMillis(minRetryTime)
        return this as T
    }

    /**
     * Установть значение для <code>template</code>.
     *
     * @param init значение для <code>template</code>.
     * @return ссылку на ActionBuilder.
     */
    @Suppress("UNCHECKED_CAST")
    fun template(init: String): T {
        template = init
        return this as T
    }

    /**
     * Установть значение для <code>timeout</code> в секундах.
     *
     * @param init значение для <code>timeout</code>.
     * @return ссылку на ActionBuilder.
     */
    @JvmOverloads
    @Suppress("UNCHECKED_CAST")
    fun timeout(init: Int, timeUnit: TimeUnit = TimeUnit.SECONDS): T {
        timeout = timeUnit.toMillis(init.toLong())
        return this as T
    }

    /**
     * Установть значение для <code>title</code>.
     *
     * @param init значение для <code>title</code>.
     * @return ссылку на ActionBuilder.
     */
    @Suppress("UNCHECKED_CAST")
    fun title(init: String): T {
        title = init
        return this as T
    }

    /**
     * Установть значение для <code>title</code>.
     *
     * @param init значение для <code>title</code>.
     * @param parameters параметры, которые будут подставлены в `init`.
     * @return ссылку на ActionBuilder.
     */
    @Suppress("UNCHECKED_CAST")
    fun titlef(init: String, vararg parameters: Any): T {
        title = String.format(init, *parameters)
        return this as T
    }

    /**
     * Установть значение для <code>xpath</code>.
     *
     * @param init значение для <code>xpath</code>.
     * @return ссылку на ActionBuilder.
     */
    @Suppress("UNCHECKED_CAST")
    fun xpath(init: String): T {
        xpath = init
        return this as T
    }

    /**
     * Установть значение для <code>visible</code>.
     *
     * @param value значение для <code>visible</code>.
     * @return ссылку на ActionBuilder.
     */
    @Suppress("UNCHECKED_CAST")
    fun visible(value: Boolean): T {
        visible = value
        return this as T
    }

    /**
     * Получение заголовка действия.
     *
     * Имеется возможность в заголовке указать шаблоны, которые будут заменены определенными значениями.
     * Шаблон `$template` соответствует шаблоны, используемому для поиска элементов на странице,
     * `$parameters` - параметры, подставляемые в шаблон поиска, `$value` - значение используемое для
     * ввода в поле ввода или выбора из выпадающего списка.
     *
     * @return заголовок действия.
     */
    fun title(): String {
        title = title.replace(Regex("\\\$attribute"), Matcher.quoteReplacement(attribute.orEmpty()))
        title = title.replace(Regex("\\\$keys"), Matcher.quoteReplacement(keys?.joinToString(", ", transform = Keys::name).orEmpty()))
        title = title.replace(Regex("\\\$template"), Matcher.quoteReplacement(template.orEmpty()))
        title = title.replace(Regex("\\\$parameters"), Matcher.quoteReplacement(parameters.orEmpty().toString()))
        title = title.replace(Regex("\\\$value"), Matcher.quoteReplacement(value.orEmpty()))
        return title
    }

    /**
     * Получение локатора нахождения элемента на странице.
     *
     * Элемент на странице можно найти двумя способами: по xpath, по шаблоны с параметрами.
     * Шаблоны <code>locators</code> хранятся в конфигурациях веб-страниц и передаются билдеру в конструкторе.
     *
     * @see Locators
     * @return локатор нахождения элемента на странице.
     */
    protected fun getLocator(): By {
        if (template != null) {
            return locators.buildXpath(template!!, parameters)
        } else {
            return By.xpath(xpath)
        }
    }

    /**
     * Скролл до элемента на веб-странице.
     *
     * Данный метод выполняет JavaScript со скроллингом до элемента на странице.
     *
     * @see JSExecutor
     * @param element элемент на веб-странице.
     */
    protected fun scrollToElement(element: WebElement) {
        JSExecutor.executeJS("arguments[0].scrollIntoView(false);", element)
    }

    /**
     * Выполнение кода <code>retry</code>.
     *
     * Данный метод вызывается в случае неуспешного выполнения действие и выполняется в
     * промежутках выполнения повторов действия. Метод должен выполняться не менее времени
     * <code>retryMinTime</code>, чье значение устанавливается в конфигурации в мс.
     * В случае когда код <code>retry</code> был выполнене менее чем за время
     * <code>getRetryMinTimeout</code>< будет осуществлено ожидание оставшегося времени.
     * Если код <code>retry</code> выполнялся более минимального времени, ожидание
     * осуществленно не будет. Метод возвращает время, затраченное на выполнение кода
     * <code>retry</code> для того, чтобы иметь возможность корректировать таймаут действия.
     *
     *
     * @see retry
     * @see retryMinTime
     * @return время, затраченное на выполнение кода.
     */
    protected fun retry(): Long {
        logger.debug("Some error in $type action. Retry...")
        logger.trace("Start retry function")
        val startTime = System.currentTimeMillis()
        retry.invoke()
        val endTime = System.currentTimeMillis() - startTime
        logger.trace("End retry function. End time : $endTime ms")
        logger.trace("Retry min time : $retryMinTime")
        if (endTime < retryMinTime) {
            Thread.sleep(retryMinTime - endTime)
        }
        val totalTime = System.currentTimeMillis() - startTime
        logger.trace("Total retry timeout : $totalTime ms")
        logger.debug("End retry function")
        return totalTime
    }

    /**
     * Поиск элементов на странице.
     *
     * Поиск осуществляется путем вызова метода `innerFindElements` с передачей
     * в качестве параметра локатора, получено методом `getLocator()`.
     *
     * @see getLocator
     * @return кортеж из 2 элементов - список элементов и время, затраченное на поиск элементов.
     */
    protected fun innerFindElements(): Pair<List<WebElement>?, Long> = innerFindElements(getLocator())

    /**
     * Поиск элементов на странице.
     *
     * Данный метод используется в классах наследниках для получения списка элементов на
     * странице и дальнейшего выполнения действий со списком элементов. Поиск элементов
     * на странице занимает некоторое время. Для корректировки таймаута выполнения действия
     * данный метод возвращает кортеж из списка элементов и времени, затраченного на поиск элементов.
     *
     * Поиск элементов осуществляется по локатору <code>locator</code>.
     * Драйвер для поиска элементов берется из <code>DriverFactory.getDriver</code>.
     * После нахождения элементов осуществляется скролл посредствам вызова метода `scrollToElement`.
     *
     * @see scrollToElement
     * @see DriverFactory.getDriver
     * @param locator локатор, по которому находятся элементы.
     * @return кортеж из 2 элементов - список элементов и время, затраченное на поиск элементов.
     */
    protected fun innerFindElements(locator: By): Pair<List<WebElement>?, Long> {
        val startTime = System.currentTimeMillis()
        var bufferElements: List<WebElement>?
        try {
            logger.debug("Find elements located with : " + locator)
            bufferElements = DriverFactory.getDriver().findElements(locator)
            logger.debug("Result of find elements located with : \"$locator\" : \n[\n\t${bufferElements.joinToString("\n\t")}\n]")
            val isNotVisible = visible && bufferElements.any {
                scrollToElement(it)
                !it.isDisplayed }
            if (bufferElements.isEmpty() || isNotVisible) {
                bufferElements = null
            }
        } catch (exception: WebDriverException) {
            logger.warn(exception.message)
            bufferElements = null
        }
        val totalTime = System.currentTimeMillis() - startTime
        logger.trace("Performance time : $totalTime ms")
        return Pair(bufferElements, totalTime)
    }

    /**
     * Обработка исключительных ситуаций.
     *
     * Данный метод вызывается в случае возникновения ошибок в ходе выполнения каких-либо действий.
     * В данном методе в зависимости от класса ошибки будет взято сообщение об ошибке по умолчанию.
     * Сообщения об ошибке по умолчанию хранятся в конфигурации <code>Configuration</code>.
     * В данне сообщения можно вставлять шаблоны, которые будут заменены конкретными значениями билдера.
     * Шаблон `$template` соответствует шаблоны, используемому для поиска элементов на странице,
     * `$parameters` - параметры, подставляемые в шаблон поиска, `$timeout` - таймаут выполнения действия,
     * `$value` - значение используемое для ввода в поле ввода или выбора из выпадающего списка.
     *
     * После формирования ошибки <code>Error</code> будет вызван метод <code>onError</code> у
     * зарегистрированных плагинов с передачей указателя на билдер и самой ошибкой <code>Error</code>.
     * В случае возникновения <code>AssertType.BLOCK</code> ошибок, будет вызван <code>Assert.fail</code>.
     *
     * @see Configuration
     * @see Error
     * @see WebPluginAPI.onError
     * @see AssertType
     * @see Assert.fail
     * @param exception исключение, которое необходимо обработать.
     * @throws AssertionError в случае AssertType.BLOCK ошибок.
     */
    override fun handleException(exception: Exception) {
        val action: String = type.name.toLowerCase()
        var defaultErrorMessage: String
        when (exception) {
            is TimeoutException -> defaultErrorMessage = Configuration.config.getString("error.strings.$action.timeout_exception")
            is NoSuchElementException -> defaultErrorMessage = Configuration.config.getString("error.strings.$action.no_such_element_exception")
            is WebDriverException -> defaultErrorMessage = Configuration.config.getString("error.strings.$action.web_driver_exception")
            else -> defaultErrorMessage = ""
        }
        defaultErrorMessage = defaultErrorMessage.replace(Regex("\\\$template"), template.orEmpty())
        defaultErrorMessage = defaultErrorMessage.replace(Regex("\\\$parameters"), parameters.orEmpty().toString())
        defaultErrorMessage = defaultErrorMessage.replace(Regex("\\\$timeout"), (timeout / 1000).toString())
        defaultErrorMessage = defaultErrorMessage.replace(Regex("\\\$value"), value.orEmpty())
        val error = if (errorMessage.isNullOrEmpty()) {
            Error(defaultErrorMessage, errorDescription.orEmpty(), exception)
        } else {
            Error(errorMessage!!, "$defaultErrorMessage\n${errorDescription.orEmpty()}", exception)
        }
        plugins.forEach { it.onError(this, error) }
        if (assertType == AssertType.BLOCK) {
            Assert.fail(error.message + "\n" + error.description, error.throwable)
        }
    }

    /**
     * Печать значений полей ActionBuilder.
     *
     * @return строку, содержащую значения полей ActionBuilder.
     */
    override fun printAction() = "[\n\t" +
            "type : $type\n\t" +
            "assertType : $assertType\n\t" +
            "attribute : $attribute\n\t" +
            "clear : $clear\n\t" +
            "clickType : $clickType\n\t" +
            "errorMessage : $errorMessage\n\t" +
            "errorDescription : $errorDescription\n\t" +
            "disableAttachments : $disableAttachments\n\t" +
            "keys : $keys\n\t" +
            "parameters : $parameters\n\t" +
            "require : $require\n\t" +
            "retry : $retry\n\t" +
            "retryMinTime : $retryMinTime\n\t" +
            "template : $template\n\t" +
            "timeout : $timeout\n\t" +
            "title : $title\n\t" +
            "value : $value\n\t" +
            "visible : $visible\n\t" +
            "xpath : $xpath\n]"

}

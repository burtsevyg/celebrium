package io.celebrium.web.action

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.celebrium.core.action.ActionType
import io.celebrium.core.config.Configuration
import io.celebrium.web.page.Locators
import io.celebrium.web.plugin.WebPluginAPI
import org.openqa.selenium.By
import org.openqa.selenium.NotFoundException
import org.openqa.selenium.TimeoutException
import org.openqa.selenium.WebDriverException
import org.openqa.selenium.support.ui.Select
import org.slf4j.LoggerFactory

/**
 * Класс GetTextBuilder.
 * ---------------------
 *
 * Экземпляр данного класса отвечает за текста элемента (-ов) веб-страницы.
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
class GetTextBuilder(locators: Locators, plugins: List<WebPluginAPI>) : ActionBuilder<GetTextBuilder>(ActionType.GET_TEXT, locators, plugins) {

    /**
     * Инициализация экземпляра GetTextBuilder.
     *
     * Если в конфигурации ключ `enable_action_default_title.default_get_text_action_title` равен true,
     * то полю `title` присваивается заголовок действия по умолчанию.
     * Так же инициализируется логгер.
     */
    init {
        if (Configuration.config.getBoolean("enable_action_default_title.default_${type.toString().toLowerCase()}_action_title")) {
            title = Configuration.config.getString("strings.default_${type.toString().toLowerCase()}_action_title")
        }
        logger = LoggerFactory.getLogger(GetTextBuilder::class.java) as Logger
        logger.level = Level.toLevel(Configuration.config.getString("log_level"))
    }

    /**
     * Получение текста первого элемента на веб-странице.
     *
     * @return текст первого элемента на веб-странице.
     */
    fun getFirst() = perform().firstOrNull() ?: ""

    /**
     * Получение текста последнего элемента на веб-странице.
     *
     * @return текст последнего элемента на веб-странице.
     */
    fun getLast() = perform().lastOrNull() ?: ""

    /**
     * Получение списка, содержащего текст всех элементов, находящихся по заданному локатору.
     *
     * @return список, содержащий текст элементов на веб-странице.
     */
    fun getAll() = perform()

    /**
     * Выполнение действия.
     *
     * Перед и после выполнения действия вызываются методы `beforeAction`
     * и `afterAction` у зарегистрированных плагинов.
     *
     * @see WebPluginAPI.beforeAction
     * @see WebPluginAPI.afterAction
     * @return список, содержащий текст элементов на веб-странице.
     */
    private fun perform(): List<String> {
        logger.debug("Perform action $type")
        plugins.forEach { it.beforeAction(this) }
        val result = getElementText(timeout)
        plugins.forEach { it.actionResult(this, result.joinToString(",\n\t", "[\n\t", "\n]")) }
        plugins.forEach { it.afterAction(this) }
        return result
    }

    /**
     * Получение текста элементов за определенное время..
     *
     * Данный метод путем вызова метода `innerFindElements` находит элементы на веб-странице
     * с целью получение числа элементов, найденных по заданному локатору. В случае ненахождения
     * элементов или возникновения ошибок данный метод повторяет поиск до тех пор, пока элементы
     * не будут найдены либо не закончится таймаут. В случае окончания таймаута `timeout` происходит
     * вызов обработчика ошибок `handleException` в классе `ActionBuilder`, в качестве резульата
     * будет возвращен пустой список `result`:
     * ```kotlin
     * val result: MutableList<String> = mutableListOf()
     * ```
     * В случае, когда элементы найдены, происходит вызов метода `getElementText`, которому
     * передается локатор каждого отдельного элемента и таймаут на поиск и получение текста
     * данного элемента. После получения текста очередного элемента происходит корректировка
     * оставшегося таймаута и получение текста следующего элемента. Как резуьтат метода
     * возвращается список, содержащий текст элементов.
     *
     * @see ActionBuilder.innerFindElements
     * @see ActionBuilder.handleException
     * @param timeout таймаут выполнения действия.
     * @return список, содержащий текст элементов на веб-странице.
     */
    private fun getElementText(timeout: Long): List<String> {
        val baseXpath: String = if (template != null) {
            locators.getXpath(template!!, parameters)
        } else {
            xpath.orEmpty()
        }
        val result: MutableList<String> = mutableListOf()
        val (elements, findTime) = innerFindElements()
        if (elements == null) {
            val totalTime = retry()
            if (timeout - (totalTime + findTime) > 0) {
                return getElementText(timeout - (totalTime + findTime))
            } else {
                handleException(TimeoutException("Timeout (${super.timeout / 1000} s) get text of element located with : ${getLocator()}"))
            }
        } else {
            logger.debug("Start getting text of elements located by \"$baseXpath\". Timeout : $timeout ms.")
            var localTimeout = timeout
            for (index in 1..elements.size) {
                val locator = By.xpath("($baseXpath)[$index]")
                val (text, performTime) = getElementText(locator, localTimeout)
                if (localTimeout - performTime < 0) {
                    handleException(TimeoutException("Timeout (${super.timeout / 1000} s) get text of element located with : $locator"))
                }
                result.add(text)
                localTimeout -= performTime
            }
            logger.trace("Performance time : ${timeout - localTimeout} ms.")
        }
        logger.debug("Result of getting text of elements located by \"$baseXpath\" : \n[\n\t${result.joinToString("\n\t")}\n]")
        return result
    }

    /**
     * Получение текста элемента по локатору за определенное время.
     *
     * Поиск элементов осуществляется путем вызова метода `innerFindElements`. В случае отсутствия
     * требуемых элементов на странице или при возникновении `WebDriverException` осуществляется
     * вызов метода `retry`, корректировка таймаута и повтор действия.
     *
     * После нахождения элементов по заданному локатору берется текст первого элемента.
     * Предполагается, что по данному локатору будет найден только один элемент, его текст вместе
     * со временем, затрачченным на поиск и получение текста, будут возвращены как кортеж.
     *
     * @see ActionBuilder.innerFindElements
     * @see ActionBuilder.retry
     * @see WebDriverException
     * @param locator локатор элемента на веб-странице.
     * @param timeout таймаут выполнения действия.
     * @return кортеж, состоящий из текста веб-элемента, найденного по заданному локатору, и время,
     * затраченное на поиск и получение текста веб-элемента.
     */
    private fun getElementText(locator: By, timeout: Long): Pair<String, Long> {
        logger.trace("Get text of element located by $locator. Current timeout : $timeout ms")
        val (elements, findTime) = innerFindElements(locator)
        try {
            if (elements == null) {
                throw NotFoundException()
            }
            val tag = elements[0].tagName.toLowerCase()
            val text = when (tag) {
                "input", "textarea" -> elements[0].getAttribute("value")
                "select" -> {
                    val select = Select(elements[0])
                    if (select.isMultiple) {
                        elements[0].getAttribute("value")
                    } else {
                        select.firstSelectedOption.getAttribute("innerText")
                    }
                }
                else -> elements[0].getAttribute("innerText")
            }
            logger.trace("Result of getting text of element located by $locator : \"$text\". Find time : $findTime ms")
            return Pair(text, findTime)
        } catch (exception: WebDriverException) {
            val time = retry()
            logger.trace("Current timeout : $timeout. Time left : ${timeout - (time + findTime)}.")
            if (timeout - (time + findTime) > 0) {
                return getElementText(locator, timeout - (time + findTime))
            } else {
                handleException(TimeoutException("Timeout (${super.timeout / 1000} s) get text of element located with : $locator"))
            }
        }
        return Pair("", 0)
    }

}

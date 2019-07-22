package io.celebrium.web.allure

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import io.celebrium.core.action.ActionType
import io.celebrium.core.config.Configuration
import io.celebrium.core.driver.DriverFactory
import io.celebrium.core.test.*
import io.celebrium.utils.FilesManager
import io.celebrium.utils.ScreenShooter
import io.celebrium.web.action.ActionBuilder
import io.celebrium.web.plugin.WebPluginAPI
import ru.yandex.qatools.allure.Allure
import ru.yandex.qatools.allure.annotations.Attachment
import ru.yandex.qatools.allure.annotations.Step
import ru.yandex.qatools.allure.events.*
import ru.yandex.qatools.allure.model.Status
import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Files

/**
 * Класс DefaultWebPlugin.
 * --------------------
 *
 * Плагин, предназначенный для работы с отчетом Allure.
 *
 * TODO: пример подключения!
 *
 * @author EMurzakaev@it.ru.
 */
class AllureWebPlugin : WebPluginAPI, AssertsPluginAPI {

    /**
     * Инициализация плагина.
     *
     * Плагин реализует интерфейс AssertsPluginAPI, регистрируя его к Asserts,
     * плагин подписывается на действия проверок в Asserts.
     */
    init {
        Asserts.registerPlugin(this)
    }

    /**
     * Метод, вызываем перед выполнением действия.
     *
     * В случае, когда заголовок действия не пустой и не отключены вложения, к отчету прикладываются
     * вложения до выполнения действия. Для отдельных вложений есть булевый флаг, указывающий нужно
     * ли прикладывать данное вложение, флаги задаются в конфигурации.
     *
     * @see Configuration
     * @param action экземпляр экшен билдера.
     */
    override fun beforeAction(action: ActionBuilder<*>) {
        if (action.title().isNotEmpty()) {
            Allure.LIFECYCLE.fire(StepStartedEvent(action.title()).withTitle(action.title()))
            if (!action.disableAttachments) {
                val message = Configuration.config.getString("strings.before_action")
                val takeScreenshot = Configuration.config.getBoolean("attachments.screenshot_of_page")
                val htmlPage = Configuration.config.getBoolean("attachments.html_page")
                val browserLogs = Configuration.config.getBoolean("attachments.browser_logs")
                makeAttachments(message, takeScreenshot, htmlPage, browserLogs)
            }
        }
    }

    /**
     * Метод, вызываем после выполнением действия.
     *
     * В случае, когда заголовок действия не пустой и не отключены вложения, к отчету прикладываются
     * вложения после выполнения действия. Для отдельных вложений есть булевый флаг, указывающий нужно
     * ли прикладывать данное вложение, флаги задаются в конфигурации.
     *
     * @see Configuration
     * @param action экземпляр экшен билдера.
     */
    override fun afterAction(action: ActionBuilder<*>) {
        if (action.title().isNotEmpty()) {
            if (!action.disableAttachments) {
                val message = Configuration.config.getString("strings.after_action")
                val takeScreenshot = Configuration.config.getBoolean("attachments.screenshot_of_page")
                val htmlPage = Configuration.config.getBoolean("attachments.html_page")
                val browserLogs = Configuration.config.getBoolean("attachments.browser_logs")
                makeAttachments(message, takeScreenshot, htmlPage, browserLogs)
            }
            Allure.LIFECYCLE.fire(StepFinishedEvent())
        }
    }

    /**
     * Метод, вызываем в случае возникновения ошибки при выполнении действия.
     *
     * При возникновении ошибки к отчету прикладываются вложения для определения ошибки.
     * Заголовок шага, к которому прикладываются вложения, задается в конфигурации.
     * Для отдельных вложений есть булевый флаг, указывающий нужно ли прикладывать данное вложение,
     * флаги задаются в конфигурации.
     *
     * @see Configuration
     * @param action экземпляр экшен билдера.
     * @param error экземпляр Error.
     */
    override fun onError(action: ActionBuilder<*>, error: Error) {
        val message = Configuration.config.getString("strings.attachments_for_error_detection")
        val takeScreenshot = Configuration.config.getBoolean("error.attachments.screenshot_of_page")
        val htmlPage = Configuration.config.getBoolean("error.attachments.html_page")
        val browserLogs = Configuration.config.getBoolean("error.attachments.browser_logs")
        val stackTrace = Configuration.config.getBoolean("error.attachments.stacktrace")
        val errorText = Configuration.config.getString("strings.error_text")
        when (action.assertType) {
            AssertType.BLOCK -> {
                makeAttachments(message, takeScreenshot, htmlPage, browserLogs, stackTrace, error.throwable)
                Allure.LIFECYCLE.fire(StepFailureEvent().withThrowable(AssertionError()))
                Allure.LIFECYCLE.fire(StepFinishedEvent())
            }
            AssertType.SOFT -> {
                if (softErrors.get() == null) {
                    softErrors.set(ArrayList<Error>())
                }
                val newArray = softErrors.get().plus(error)
                softErrors.set(newArray)
                step("${newArray.size}. ${error.message}") {
                    makeAttachments(message, takeScreenshot, htmlPage, browserLogs, stackTrace, error.throwable)
                    Attachments.attach("${error.message}\n${error.description}", AttachmentType.TEXT, errorText)
                    Allure.LIFECYCLE.fire(StepFailureEvent().withThrowable(AssertionError()))
                }
            }
        }
    }

    /**
     * Метод, вызываем билдером для передачи какого-то результата выполнения действия.
     *
     * @param action экземпляр экшен билдера.
     * @param result результат выполнения действия.
     */
    override fun actionResult(action: ActionBuilder<*>, result: Any?) {
        if (action.title().isNotEmpty()) {
            when (action.type) {
                ActionType.GET_TEXT, ActionType.GET_ATTRIBUTE -> {
                    val title = Configuration.config.getString("strings.${action.type.toString().toLowerCase()}_attachment_title")
                    Attachments.attach(result, AttachmentType.TEXT, title)
                }
                else -> {
                }
            }
        }
    }

    /**
     * Метод, вызываемый при проверке условия в Asserts.
     *
     * @param builder экземпляр билдера Asserts.
     * @param method проверяемый метод.
     */
    override fun onCheck(builder: Asserts.Builder, method: AssertMethod) {
        if (!builder.title.isNullOrEmpty()) {
            Allure.LIFECYCLE.fire(StepStartedEvent(builder.title).withTitle(builder.title))
        }
        if (method == AssertMethod.ASSERT_EQUALS) {
            val actual = if (builder.actual is List<*>) {
                builder.actual as List<*>
            } else {
                listOf(builder.actual)
            }
            val expected = if (builder.expected is List<*>) {
                builder.expected as List<*>
            } else {
                listOf(builder.expected)
            }
            val title = Configuration.config.getString("strings.compare_lists_attachment_title")
            Attachments.attach(compareLists(actual, expected), AttachmentType.HTML, title)
        }
    }

    /**
     * Метод, вызываемый в случае успешной проверки в Asserts.
     *
     * @param builder экземпляр билдера Asserts.
     */
    override fun onCheckSuccess(builder: Asserts.Builder) {
        if (!builder.title.isNullOrEmpty()) {
            Allure.LIFECYCLE.fire(StepFinishedEvent())
        }
    }

    /**
     * Метод, вызываемый в случае неуспешной проверки в Asserts.
     *
     * При возникновении ошибки к отчету прикладываются вложения для определения ошибки.
     * Заголовок шага, к которому прикладываются вложения, задается в конфигурации.
     * Для отдельных вложений есть булевый флаг, указывающий нужно ли прикладывать данное вложение,
     * флаги задаются в конфигурации.
     *
     * @see Configuration
     * @param builder экземпляр билдера Asserts.
     * @param error ошибка.
     */
    override fun onCheckFailure(builder: Asserts.Builder, error: Error) {
        val message = Configuration.config.getString("strings.attachments_for_error_detection")
        val takeScreenshot = Configuration.config.getBoolean("error.attachments.screenshot_of_page")
        val htmlPage = Configuration.config.getBoolean("error.attachments.html_page")
        val browserLogs = Configuration.config.getBoolean("error.attachments.browser_logs")
        val stackTrace = Configuration.config.getBoolean("error.attachments.stacktrace")
        val errorText = Configuration.config.getString("strings.error_text")
        val size = if (builder.assertType == AssertType.SOFT) {
            if (softErrors.get() == null) {
                softErrors.set(ArrayList())
            }
            val newArray = softErrors.get().plus(error)
            softErrors.set(newArray)
            newArray.size
        } else {
            0
        }
        val code = {
            makeAttachments(message, takeScreenshot, htmlPage, browserLogs, stackTrace, error.throwable)
            Attachments.attach("${error.message}\n${error.description}", AttachmentType.TEXT, errorText)
            Allure.LIFECYCLE.fire(StepFailureEvent().withThrowable(AssertionError()))
        }
        if (!builder.title.isNullOrEmpty()) {
            code.invoke()
            Allure.LIFECYCLE.fire(StepFinishedEvent())
        } else {
            step("$size. ${error.message}", code)
        }
    }

    /**
     * Прикрепление вложений к отчету.
     *
     * @param message заголовок шага в отчете.
     * @param takeScreenshot параметр, отвечающий за необходимость снятия скриншота страницы.
     * @param htmlPage параметр, отвечающий за необходимость прикрепления html страницы.
     * @param browserLogs параметр, отвечающий за необходимость прикрепления js логов браузера.
     * @param stackTrace параметр, отвечающий за необходимость прикрепления к отчету стектрейса исключения.
     * @param throwable исключение, стектрейс которого необходимо приложить к отчету.
     */
    private fun makeAttachments(message: String, takeScreenshot: Boolean, htmlPage: Boolean, browserLogs: Boolean, stackTrace: Boolean = false, throwable: Throwable? = null) {
        if (takeScreenshot || htmlPage || browserLogs || stackTrace) {
            step(message) {
                if (takeScreenshot) {
                    val screen = ScreenShooter.takeScreenshot()
                    Attachments.attach(screen, AttachmentType.SCREENSHOT)
                }
                if (htmlPage) {
                    val html = DriverFactory.getDriver().pageSource
                    Attachments.attach(html, AttachmentType.HTML)
                }
                if (browserLogs && DriverFactory.isDriverActive()) {
                    val originalLogs = DriverFactory.getDriver().manage().logs()
                    val logs = originalLogs.availableLogTypes.map {
                        originalLogs.get(it).joinToString(separator = "\n", transform = { it.toString() })
                    }.joinToString(separator = "\n")
                    val title = Configuration.config.getString("strings.browser_logs")
                    Attachments.attach(logs, AttachmentType.TEXT, title)
                }
                if (stackTrace) {
                    Attachments.attach(throwable, AttachmentType.THROWABLE)
                }
            }
        }
    }

}

/**
 * Объект, позволяющий прикладывать вложения к отчету.
 *
 * @author EMurzakaev@it.ru.
 */
object Attachments {

    /**
     * Метод прикладывания объекта к отчету.
     *
     * Заголовок вложения будет задан исходя из типа объекта, заголовок берется из конфигурации.
     *
     * @see AttachmentType
     * @see Configuration
     * @param value объект, который необходимо приложить к отчету.
     * @param type тип объекта (по умолчанию текст).
     */
    @JvmStatic
    @JvmOverloads
    fun attach(value: Any?, type: AttachmentType = AttachmentType.TEXT) {
        val title = Configuration.config.getString("strings.attachments_${type.name.toLowerCase()}_default_title")
        attach(value, type, title)
    }

    /**
     * Метод прикладывания объекта к отчету.
     *
     * @see AttachmentType
     * @param value объект, который необходимо приложить к отчету.
     * @param type тип объекта (по умолчанию текст).
     * @param title заголовок вложения в отчете.
     */
    @JvmStatic
    @JvmOverloads
    fun attach(value: Any?, type: AttachmentType = AttachmentType.TEXT, title: String) {
        value.let {
            when (type) {
                AttachmentType.TEXT -> attachText(value.toString(), title)
                AttachmentType.JSON -> attachJson(value.toString(), title)
                AttachmentType.HTML -> attachHtml(value.toString(), title)
                AttachmentType.XML -> attachXml(value.toString(), title)
                AttachmentType.PDF -> attachPdf(value.toString(), title)
                AttachmentType.CSV -> attachCsv(value.toString(), title)
                AttachmentType.SCREENSHOT -> attachScreenShot(value as ByteArray, title)
                AttachmentType.THROWABLE -> attachThrowable(value as Throwable, title)
            }
        }
    }

    /**
     * Метод прикладывания текста к отчету.
     *
     * @param data объект, который необходимо приложить к отчету.
     * @param title заголовок вложения в отчете.
     */
    @Attachment(value = "{1}", type = "text/plain")
    private fun attachText(data: String, title: String) = data

    /**
     * Метод прикладывания json объекта к отчету.
     *
     * В данном методе производится попытка форматирования json объекта,
     * в случае неудачи объект прикладывается в изначальном состоянии.
     *
     * @param data объект, который необходимо приложить к отчету.
     * @param title заголовок вложения в отчете.
     */
    @Attachment(value = "{1}", type = "application/json")
    private fun attachJson(data: String, title: String): String {
        val gson = GsonBuilder().setPrettyPrinting().create()
        return try {
            gson.toJson(JsonParser().parse(data))
        } catch (e: JsonSyntaxException) {
            data
        }
    }

    /**
     * Метод прикладывания скриншота к отчету.
     *
     * @param data объект, который необходимо приложить к отчету.
     * @param title заголовок вложения в отчете.
     */
    @Attachment(value = "{1}", type = "image/png")
    private fun attachScreenShot(data: ByteArray, title: String): ByteArray = data

    /**
     * Метод прикладывания html объекта к отчету.
     *
     * @param data объект, который необходимо приложить к отчету.
     * @param title заголовок вложения в отчете.
     */
    @Attachment(value = "{1}", type = "text/html")
    private fun attachHtml(data: String, title: String): String = data

    /**
     * Метод прикладывания xml объекта к отчету.
     *
     * @param data объект, который необходимо приложить к отчету.
     * @param title заголовок вложения в отчете.
     */
    @Attachment(value = "{1}", type = "text/xml")
    private fun attachXml(data: String, title: String): String = data

    /**
     * Метод прикладывания pdf объекта к отчету.
     *
     * @param data объект, который необходимо приложить к отчету.
     * @param title заголовок вложения в отчете.
     */
    @Attachment(value = "{1}", type = "application/pdf")
    private fun attachPdf(data: String, title: String) = if (File(data).exists()) {
        getBytesFromFile(data)
    } else {
        data
    }

    /**
     * Метод прикладывания csv объекта к отчету.
     *
     * @param data объект, который необходимо приложить к отчету.
     * @param title заголовок вложения в отчете.
     */
    @Attachment(value = "{1}", type = "text/csv")
    private fun attachCsv(data: String, title: String): String = data

    /**
     * Метод прикладывания исключения к отчету.
     *
     * @param data объект, который необходимо приложить к отчету.
     * @param title заголовок вложения в отчете.
     */
    @Attachment(value = "{1}", type = "text/plain")
    private fun attachThrowable(data: Throwable, title: String): String {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        data.printStackTrace(printWriter)
        return stringWriter.toString()
    }

    /**
     * Метод получение массива байтов из файла.
     *
     * @param fileName путь до файла.
     * @return массив байтов, при ошибке - null.
     */
    private fun getBytesFromFile(fileName: String): ByteArray? = try {
        Files.readAllBytes(File(fileName).toPath())
    } catch (ioException: IOException) {
        null
    }

}

/**
 * Тип вложения.
 *
 * @author EMurzakaev@it.ru.
 */
enum class AttachmentType {
    TEXT,
    JSON,
    HTML,
    XML,
    PDF,
    CSV,
    SCREENSHOT,
    THROWABLE
}

/**
 * Объект, позволяющий прикладывать к отчету информацию об окружении.
 *
 * @author EMurzakaev@it.ru.
 */
object Environment {

    /**
     * Таблица свойств окружения.
     */
    private val properties = mutableMapOf<String, Any?>()

    /**
     * Добавление свойства и значения к списку свойств окружения.
     *
     * @param propertyName название свойства
     * @param propertyValue значение свойства.
     */
    @JvmStatic
    fun addProperty(propertyName: String, propertyValue: Any?) = properties.put(propertyName, propertyValue)

    /**
     * Прикрепление информации об окружении к отчету.
     *
     * Отчет Allure берет информацию об окружении из файла environment.properties.
     * Данный файл должен находится в папке, в которой Allure ищет результаты выполнеия тестов.
     * Путь до папки задается переменной "build.dir".
     */
    @JvmStatic
    fun attachEnvironmentInfo() {
        System.getProperty("build.dir")?.let {
            val fileName = it.plus("/environment.properties")
            val propertiesFile = File(fileName)
            if (propertiesFile.exists()) {
                return
            }
            val strBuilder = StringBuilder()
            strBuilder.append("OS=${System.getProperty("os.name")} (version: ${System.getProperty("os.version")})\n")
            strBuilder.append("Java=${System.getProperty("java.version")}\n")
            DriverFactory.capabilities?.let {
                strBuilder.append("Browser=${it.browserName} (version: ${it.version})\n")
            }
            properties.forEach {
                strBuilder.append("${it.key}=${it.value.toString().orEmpty()}\n")
            }
            FilesManager.writeToFile(strBuilder.toString(), fileName)
        }
    }

}

/**
 * Класс-наследник StepEvent, предназначен для обработки "мягких ошибок".
 *
 * @author EMurzakaev@it.ru.
 */
private class MarkStepsAsFailed : StepEvent {

    /**
     * Обработка шага.
     *
     * @param context шаг в отчете.
     */
    override fun process(context: ru.yandex.qatools.allure.model.Step) = processStep(context)

    /**
     * Обработка шага.
     *
     * @param parent шаг в отчете.
     */
    private fun processStep(parent: ru.yandex.qatools.allure.model.Step) {
        parent.steps.forEach {
            processStep(it)
            if (it.status != Status.PASSED)
                parent.status = it.status
        }
    }

}

/**
 * Список мягких ошибок.
 */
val softErrors = ThreadLocal<List<Error>>()

/**
 * Функция, предназначенная для вложения к отчету "мягких" ошибок.
 */
fun showErrors() {
    softErrors.get()?.let {
        val errors = it.mapIndexed { index, error ->
            "${index + 1}. ${error.message}" + if (error.description.isNotEmpty()) {
                " - ${error.description}"
            } else {
                ""
            }
        }.joinToString("\n")
        Allure.LIFECYCLE.fire(MarkStepsAsFailed())
        Allure.LIFECYCLE.fire(TestCaseFailureEvent().withThrowable(AssertionError("\n$errors")))
        softErrors.remove()
    }
}

/**
 * Функция, предназначенная для очистки списка "мягких" ошибок.
 */
fun cleanErrors() = softErrors.remove()

/**
 * Функция создания шага в отчете.
 *
 * @param message заголовок шага в отчете.
 * @param code блок кода, который будет вызван.
 */
@Step("{0}")
fun step(message: String, code: () -> Unit) = code.invoke()

/**
 * Функция создания шага в отчете.
 *
 * @param message заголовок шага в отчете.
 * @param code блок кода, который будет вызван.
 */
@Step("{0}")
fun step(message: String, code: Runnable) = code.run()

/**
 * Функция сравнения списков.
 *
 * @param actual фактический список.
 * @param expected ожидаемый список.
 * @return строку, содержащую html разметку таблицы с результатами сравнения списков.
 */
private fun compareLists(actual: List<*>, expected: List<*>): String {
    val builder = StringBuilder()
    val actualString = Configuration.config.getString("strings.compare_lists_actual_value")
    val expectedString = Configuration.config.getString("strings.compare_lists_expected_value")
    builder.append("<table style=\"margin: 0 auto;\"><thead><tr><th>$actualString</th><th>$expectedString</th></tr></thead><tbody>")
    val size = if (actual.size > expected.size) actual.size else expected.size
    for (i in 0 until size) {
        val actualText = if (i < actual.size) actual[i].toString() else ""
        val expectedText = if (i < expected.size) expected[i].toString() else ""
        builder.append("<tr>")
        if (i < actual.size && i < expected.size) {
            if (actual[i] != expected[i]) {
                builder.append("<td style=\"color: red\">$actualText</td><td style=\"color: red\">$expectedText</td>")
            } else {
                builder.append("<td>$actualText</td><td>$expectedText</td>")
            }
        } else {
            builder.append("<td style=\"color: red\">$actualText</td><td style=\"color: red\">$expectedText</td>")
        }
        builder.append("</tr>")
    }
    builder.append("</tbody></table>")
    return builder.toString()
}

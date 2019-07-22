package io.celebrium.core.test

import io.celebrium.core.config.Configuration
import org.testng.Assert
import java.util.*

/**
 * Класс Asserts.
 * --------------
 *
 *
 * @author EMurzakaev@it.ru.
 */
object Asserts {

    /**
     * TODO: документация!
     */
    private var plugins = listOf<AssertsPluginAPI>()

    /**
     * TODO: документация!
     */
    @JvmStatic
    fun registerPlugin(plugin: AssertsPluginAPI) {
        plugins += plugin
    }

    /**
     * Builder instance.
     */
    @JvmStatic
    fun builder() = Builder()

    /**
     * Assert action builder.
     */
    class Builder {

        /**
         * @value actual object.
         */
        var actual: Any?
            private set
        /**
         * @value expected object.
         */
        var expected: Any?
            private set
        /**
         * @value boolean condition.
         */
        var condition: Boolean
            private set
        /**
         * @value assert type.
         */
        var assertType: AssertType
            private set
        /**
         * @value throwable.
         */
        var throwable: Throwable?
            private set
        /**
         * @value error message.
         */
        var errorMessage: String
            private set
        /**
         * @value error description.
         */
        var errorDescription: String
            private set
        /**
         * @value step title in report.
         */
        var title: String?
            private set

        /**
         * TODO: документация!
         */
        init {
            this.actual = null
            this.expected = null
            this.condition = false
            this.assertType = AssertType.BLOCK
            this.throwable = null
            this.errorMessage = ""
            this.errorDescription = ""
            this.title = null
        }

        /**
         * Actual object setter.
         *
         * @param actual actual object.
         * @return link to the this object.
         */
        fun actual(actual: Any) = apply {
            this.actual = actual
        }

        /**
         * Expected object setter.
         *
         * @param expected expected object.
         * @return link to the this object.
         */
        fun expected(expected: Any) = apply {
            this.expected = expected
        }

        /**
         * Condition setter.
         *
         * @param condition boolean condition.
         * @return link to the this object.
         */
        fun condition(condition: Boolean) = apply {
            this.condition = condition
        }

        /**
         * Assert type setter.
         *
         * @param assertType assert type.
         * @return link to the this object.
         */
        fun assertType(assertType: AssertType) = apply {
            this.assertType = assertType
        }

        /**
         * Throwable object setter.
         *
         * @param throwable throwable object.
         * @return link to the this builder.
         */
        fun throwable(throwable: Throwable) = apply {
            this.throwable = throwable
        }

        /**
         * Error message setter.
         *
         * @param errorMessage error message.
         * @return link to the this object.
         */
        fun errorMessage(errorMessage: String) = apply {
            this.errorMessage = errorMessage
        }

        /**
         * Error description setter.
         *
         * @param errorDescription additional error message.
         * @return link to the this object.
         */
        fun errorDescription(errorDescription: String) = apply {
            this.errorDescription = errorDescription
        }

        /**
         * Step title setter.
         *
         * @param title step title.
         * @return link to the this object.
         */
        fun title(title: String) = apply {
            this.title = title
        }

        /**
         * Check that condition is true.
         */
        fun assertTrue() {
            val defaultMessage = Configuration.config.getString("strings.default_assert_strings.assert_true_error_message")
            val defaultTitle = Configuration.config.getString("strings.default_assert_strings.assert_true_title")
            prepareMessages(defaultMessage, defaultTitle)
            plugins.forEach { it.onCheck(this, AssertMethod.ASSERT_TRUE) }
            check(condition)
        }

        /**
         * Check that condition is false.
         */
        fun assertFalse() {
            val defaultMessage = Configuration.config.getString("strings.default_assert_strings.assert_false_error_message")
            val defaultTitle = Configuration.config.getString("strings.default_assert_strings.assert_false_title")
            prepareMessages(defaultMessage, defaultTitle)
            plugins.forEach { it.onCheck(this, AssertMethod.ASSERT_FALSE) }
            check(!condition)
        }

        /**
         * Check that actual object is null.
         */
        fun assertNull() {
            val defaultMessage = Configuration.config.getString("strings.default_assert_strings.assert_null_error_message")
            val defaultTitle = Configuration.config.getString("strings.default_assert_strings.assert_null_title")
            prepareMessages(defaultMessage, defaultTitle)
            plugins.forEach { it.onCheck(this, AssertMethod.ASSERT_NULL) }
            check(Objects.isNull(actual))
        }

        /**
         * Check that actual object is not null.
         */
        fun assertNotNull() {
            val defaultMessage = Configuration.config.getString("strings.default_assert_strings.assert_not_null_error_message")
            val defaultTitle = Configuration.config.getString("strings.default_assert_strings.assert_not_null_title")
            prepareMessages(defaultMessage, defaultTitle)
            plugins.forEach { it.onCheck(this, AssertMethod.ASSERT_NOT_NULL) }
            check(Objects.nonNull(actual))
        }

        /**
         * Check that actual and expected objects is equals.
         */
        fun assertEquals() {
            val defaultMessage = Configuration.config.getString("strings.default_assert_strings.assert_equals_error_message")
            val defaultTitle = Configuration.config.getString("strings.default_assert_strings.assert_equals_title")
            val errorStr = String.format(defaultMessage, expected, actual)
            prepareMessages(errorStr, defaultTitle)
            plugins.forEach { it.onCheck(this, AssertMethod.ASSERT_EQUALS) }
            check(actual == expected)
        }

        /**
         * Create new error.
         */
        fun fail() {
            plugins.forEach { it.onCheck(this, AssertMethod.ASSERT_FAIL) }
            check(false)
        }

        /**
         * Check condition.
         *
         * @param statement boolean statement for checking.
         */
        private fun check(statement: Boolean) {
            if (!statement) {
                failInternal()
            } else {
                plugins.forEach { it.onCheckSuccess(this) }
            }
        }

        /**
         * Process creating new error.
         */
        private fun failInternal() {
            if (throwable == null) {
                throwable = Throwable(errorMessage + "\n" + errorDescription)
            }
            val error = Error(errorMessage, errorDescription, throwable!!)
            plugins.forEach { it.onCheckFailure(this, error) }
            when (assertType) {
                AssertType.BLOCK -> {
                    Assert.fail(errorMessage, throwable)
                }
                AssertType.SOFT -> {
                }
            }
        }

        /**
         * Prepare messages (error and additional) with default defaultMessage.

         * @param defaultMessage default defaultMessage.
         */
        private fun prepareMessages(defaultMessage: String, defaultTitle: String) {
            val (message, description) = Asserts.getMessages(errorMessage.orEmpty(), errorDescription.orEmpty(), defaultMessage)
            this.errorMessage = message
            this.errorDescription = description
            val enableDefaultTitle = Configuration.config.getBoolean("enable_asserts_default_title")
            if (this.title.isNullOrEmpty() && enableDefaultTitle) {
                this.title = defaultTitle
            }
        }

    }

    /**
     * Метод получения сообщения ошибки.
     *
     * @param message              сообщение.
     * @param additionalMessage    дополнительное сообщение.
     * @param defaultMessage       сообщение по умолчанию.
     * @return экземпляр Tuple2, кооторый содержит сообщение об ошибке и дополнительное сообщение.
     */
    private fun getMessages(message: String, additionalMessage: String, defaultMessage: String): Pair<String, String> {
        val errMessage: String
        val additionalErrMessage: String
        if (message.isEmpty()) {
            errMessage = defaultMessage
            additionalErrMessage = ""
        } else {
            errMessage = message
            if (additionalMessage.isEmpty()) {
                additionalErrMessage = defaultMessage
            } else {
                additionalErrMessage = additionalMessage + "\n" + defaultMessage
            }
        }
        return Pair(errMessage, additionalErrMessage)
    }

}

/**
 * TODO: документация!
 */
enum class AssertMethod {
    ASSERT_TRUE,
    ASSERT_FALSE,
    ASSERT_NULL,
    ASSERT_NOT_NULL,
    ASSERT_EQUALS,
    ASSERT_FAIL
}

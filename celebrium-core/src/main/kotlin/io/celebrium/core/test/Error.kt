package io.celebrium.core.test

/**
 * Класс Error.
 * ------------
 *
 * Модель ошибки, используется в случае возникновения ошибок выполнения действий.
 *
 * @param message сообщение ошибки.
 * @param description описание ошибки.
 * @param throwable перехваченное исключение.
 * @author EMurzakaev@it.ru.
 */
data class Error(val message: String, val description: String, val throwable: Throwable)
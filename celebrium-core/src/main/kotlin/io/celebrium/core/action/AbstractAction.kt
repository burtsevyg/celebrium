package io.celebrium.core.action

import java.lang.Exception

/**
 * Интерфейс AbstractAction.
 * -------------------------
 *
 * Интерфейс содержит методы, которые необходимо реализовать в зависимости
 * от выбранной платформы - веб-приложения, мобильные, десктопные. Для этих
 * платформ выделены общие действия, для каждой конкретной платформы возможны
 * дополнения.
 *
 * @author EMurzakaev@it.ru.
 */
interface AbstractAction {

    /**
     * TODO: документация!
     */
    val type: ActionType

    /**
     * TODO: документация!
     */
    fun printAction(): String

    /**
     * TODO: документация!
     */
    fun handleException(exception: Exception)

}

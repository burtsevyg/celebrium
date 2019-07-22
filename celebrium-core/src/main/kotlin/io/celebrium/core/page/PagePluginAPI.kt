package io.celebrium.core.page

import io.celebrium.core.action.AbstractAction
import io.celebrium.core.test.Error

/**
 * Интерфейс PagePluginAPI.
 * --------------------
 *
 *
 *
 * @author EMurzakaev@it.ru.
 */
interface PagePluginAPI<in T : AbstractAction> {

    /**
     * TODO: документация!
     */
    fun beforeAction(action: T)

    /**
     * TODO: документация!
     */
    fun afterAction(action: T)

    /**
     * TODO: документация!
     */
    fun onError(action: T, error: Error)

    /**
     * TODO: документация!
     */
    fun actionResult(action: T, result: Any?)

}

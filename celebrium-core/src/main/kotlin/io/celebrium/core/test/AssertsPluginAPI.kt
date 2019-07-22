package io.celebrium.core.test

/**
 * Интерфейс AssertsPluginAPI.
 * ---------------------------
 *
 *
 * @author EMurzakaev@it.ru.
 */
interface AssertsPluginAPI {

    /**
     * TODO: документация!
     */
    fun onCheck(builder: Asserts.Builder, method: AssertMethod)

    /**
     * TODO: документация!
     */
    fun onCheckSuccess(builder: Asserts.Builder)

    /**
     * TODO: документация!
     */
    fun onCheckFailure(builder: Asserts.Builder, error: Error)

}

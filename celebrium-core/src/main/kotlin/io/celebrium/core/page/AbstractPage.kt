package io.celebrium.core.page

import com.typesafe.config.Config

/**
 * Интерфейс AbstractPage.
 * -------------------
 *
 * Содержит методы инициализации page object'а.
 *
 * @author EMurzakaev@it.ru.
 */
interface AbstractPage<T : PagePluginAPI<*>> {

    /**
     * TODO: документация!
     */
    var plugins: List<T>

    /**
     * Инициализировать page object.
     *
     * @param fileName путь до файла, содержащего конфигурацию.
     */
    fun init(fileName: String)

    /**
     * Инициализировать page object.
     *
     * @param config конфигурация.
     */
    fun init(config: Config)

    /**
     * TODO: документация!
     */
    fun registerPlugin(plugin: T) {
        plugins = plugins.plus(plugin)
    }

}

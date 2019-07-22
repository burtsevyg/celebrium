package io.celebrium.core.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import java.io.File

/**
 * Объект Configuration.
 * ---------------------
 *
 * Конфигурация, используемая Celebrium.
 *
 * @author EMurzakaev@it.ru.
 */
object Configuration {

    /**
     * TODO: документация!
     */
    lateinit var config: Config
        private set

    /**
     * TODO: документация!
     */
    private val defaultConfig = ConfigFactory.parseResources("default.conf")

    /**
     * TODO: документация!
     */
    @JvmStatic
    fun init() {
        config = defaultConfig.resolve()
    }

    /**
     * TODO: документация!
     */
    @JvmStatic
    fun init(fileName: String) {
        val userConfig = ConfigFactory.parseFile(File(fileName))
        config = userConfig.withFallback(defaultConfig).resolve()
    }

}

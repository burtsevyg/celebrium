package io.celebrium.web.plugin

import io.celebrium.core.test.Error
import io.celebrium.web.action.ActionBuilder
import org.slf4j.LoggerFactory

/**
 * Класс DefaultWebPlugin.
 * --------------------
 *
 * TODO: документация!
 *
 * @author EMurzakaev@it.ru.
 */
class DefaultWebPlugin : WebPluginAPI {

    /**
     * Логгер.
     */
    private val logger = LoggerFactory.getLogger(DefaultWebPlugin::class.java)

    /**
     * TODO: документация!
     */
    override fun beforeAction(action: ActionBuilder<*>) {
    }

    /**
     * TODO: документация!
     */
    override fun afterAction(action: ActionBuilder<*>) {
    }

    /**
     * TODO: документация!
     */
    override fun onError(action: ActionBuilder<*>, error: Error) {
        logger.error("Error!\nMessage : \"${error.message}\".\nDescription : \"${error.description}\".\nBuilder params :\n${action.printAction()}", error.throwable)
    }

    /**
     * TODO: документация!
     */
    override fun actionResult(action: ActionBuilder<*>, result: Any?) {
    }

}

package io.celebrium.web.plugin

import io.celebrium.core.page.PagePluginAPI
import io.celebrium.web.action.ActionBuilder

/**
 * Интерфейс WebPluginAPI.
 * -----------------------
 *
 *
 * @author EMurzakaev@it.ru.
 */
interface WebPluginAPI : PagePluginAPI<ActionBuilder<*>>

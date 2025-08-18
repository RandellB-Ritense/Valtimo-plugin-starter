package __PACKAGE_NAME__.plugin

import com.ritense.plugin.PluginFactory
import com.ritense.plugin.service.PluginService

class __CLASS_PREFIX__Factory(
    pluginService: PluginService
) : PluginFactory< __FUNCTION_PREFIX__>(pluginService) {

    override fun create(): __FUNCTION_PREFIX__{
        return __FUNCTION_PREFIX__()
    }
}

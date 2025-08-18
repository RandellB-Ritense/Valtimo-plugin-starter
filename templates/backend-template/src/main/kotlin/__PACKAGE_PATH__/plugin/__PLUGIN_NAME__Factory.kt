package __PACKAGE_NAME__.plugin

import com.ritense.plugin.PluginFactory
import com.ritense.plugin.service.PluginService

class __CLASS_PREFIX__Factory(
    pluginService: PluginService
) : PluginFactory<__PLUGIN_NAME__>(pluginService) {

    override fun create(): __PLUGIN_NAME__{
        return __PLUGIN_NAME__()
    }
}

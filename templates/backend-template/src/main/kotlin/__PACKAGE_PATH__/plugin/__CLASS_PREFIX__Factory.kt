package __PACKAGE_NAME__.plugin

import com.ritense.plugin.PluginFactory
import com.ritense.plugin.service.PluginService

class __CLASS_PREFIX__Factory(
    pluginService: PluginService
) : PluginFactory<__CLASS_PREFIX__>(pluginService) {

    override fun create(): __CLASS_PREFIX__{
        return __CLASS_PREFIX__()
    }
}

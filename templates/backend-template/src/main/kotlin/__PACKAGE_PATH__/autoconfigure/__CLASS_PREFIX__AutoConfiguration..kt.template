package __PACKAGE_NAME__.autoconfigure

import com.ritense.plugin.service.PluginService
import __PACKAGE_NAME__.plugin.__CLASS_PREFIX__Factory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties
class `__CLASS_PREFIX__AutoConfiguration` {

    @Bean
    fun pluginFactory(
        pluginService: PluginService
    ): __CLASS_PREFIX__Factory {
        return __CLASS_PREFIX__Factory(pluginService)
    }
}

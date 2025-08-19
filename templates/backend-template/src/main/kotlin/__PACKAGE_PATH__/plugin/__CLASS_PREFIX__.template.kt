package __PACKAGE_NAME__.plugin

import com.ritense.plugin.annotation.Plugin
import com.ritense.plugin.annotation.PluginAction
import com.ritense.plugin.annotation.PluginActionProperty
import com.ritense.plugin.annotation.PluginProperty
import com.ritense.processlink.domain.ActivityTypeWithEventName
import io.github.oshai.kotlinlogging.KotlinLogging
import org.camunda.bpm.engine.delegate.DelegateExecution

@Plugin(
    key = "__ARTIFACT_NAME__",
    title = "__PLUGIN_NAME__",
    description = "Boilerplate plugin for Valtimo"
)
@Suppress("UNUSED")
class `__CLASS_PREFIX__.template` {

    @PluginProperty(key = "exampleProperty", secret = false, required = false)
    var exampleProperty: String? = null

    @PluginAction(
        key = "sample-action",
        title = "Sample action",
        description = "A no-op sample action used in the example plugin",
        activityTypes = [ActivityTypeWithEventName.SERVICE_TASK_START]
    )
    fun sampleAction(
        @PluginActionProperty sampleInput: String?,
        execution: DelegateExecution
    ) {
        logger.info { "Executing sample action for business key ${execution.businessKey} with input '$sampleInput' and property '$exampleProperty'" }
        // No-op
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
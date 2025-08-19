import {PluginSpecification} from '@valtimo/plugin';
import {
    __CLASS_PREFIX__PluginConfigurationComponent
} from './components/__ARTIFACT_NAME__-plugin-configuration/__ARTIFACT_NAME__-plugin-configuration.component';
import {PLUGIN_LOGO_BASE64} from "./assets";
import {SampleActionComponent} from "./components/sample-action/sample-action.component";

const __FUNCTION_PREFIX__PluginSpecification: PluginSpecification = {
    pluginId: '__ARTIFACT_NAME__',
    pluginConfigurationComponent: __CLASS_PREFIX__PluginConfigurationComponent,
    pluginLogoBase64: PLUGIN_LOGO_BASE64,
    functionConfigurationComponents: {
        'sample-action': SampleActionComponent
    },
    pluginTranslations: {
        nl: {
            configurationTitle: 'Titel',
            configurationTitleTooltip: 'Scaffolded __PLUGIN_NAME__ plugin',
            title: '__PLUGIN_NAME__',
            description: 'Boilerplate plugin voor configuratie',
            exampleProperty: 'Voorbeeld eigenschap',
            sampleAction: 'Voorbeeld actie',
            sampleInput: 'Voorbeeld input'
        },
        en: {
            configurationTitle: 'Title',
            configurationTitleTooltip: 'Scaffolded __PLUGIN_NAME__ plugin',
            title: '__PLUGIN_NAME__',
            description: 'Boilerplate plugin for configuration',
            exampleProperty: 'Example property',
            sampleAction: 'Voorbeeld actie',
            sampleInput: 'Sample input'
        }
    }
};

export {__FUNCTION_PREFIX__PluginSpecification};

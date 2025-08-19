import {PluginSpecification} from '@valtimo/plugin';
import {__CLASS_PREFIX__PluginConfigurationComponent} from './components/__ARTIFACT_NAME__-plugin-configuration/__ARTIFACT_NAME__-plugin-configuration.component';
import {PLUGIN_LOGO_BASE64} from "./assets";

const __FUNCTION_PREFIX__PluginSpecification: PluginSpecification = {
  pluginId: '__ARTIFACT_NAME__',
  pluginConfigurationComponent: __CLASS_PREFIX__PluginConfigurationComponent,
  pluginLogoBase64: PLUGIN_LOGO_BASE64,
  functionConfigurationComponents: {
    'sample-action': __CLASS_PREFIX__PluginConfigurationComponent
  },
  pluginTranslations: {
    nl: {
      configurationTitle: '__PLUGIN_NAME__',
      configurationTitleTooltip: 'Scaffolded __PLUGIN_NAME__ plugin',
      title: '__PLUGIN_NAME__',
      description: 'Boilerplate plugin voor configuratie',
      exampleProperty: 'Voorbeeld eigenschap',
      baseUrl: 'Basis URL',
      'sample-action': 'Voorbeeld actie'
    },
    en: {
      configurationTitle: '__PLUGIN_NAME__',
      configurationTitleTooltip: 'Scaffolded __PLUGIN_NAME__ plugin',
      title: '__PLUGIN_NAME__',
      description: 'Boilerplate plugin for configuration',
      exampleProperty: 'Example property',
      baseUrl: 'Base URL',
      'sample-action': 'Sample action'
    }
  }
};

export {__FUNCTION_PREFIX__PluginSpecification};

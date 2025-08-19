import {PluginConfigurationData} from '@valtimo/plugin';

interface Config extends PluginConfigurationData{
    configurationTitle: string;
    exampleProperty: string;
    baseUrl: string;
}

interface SampleActionConfig {
    sampleInput: string;
}

export {Config, SampleActionConfig};
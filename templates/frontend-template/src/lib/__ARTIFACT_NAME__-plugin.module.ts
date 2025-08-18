import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {PluginTranslatePipeModule} from '@valtimo/plugin';
import {FormModule, InputModule, ParagraphModule, SelectModule} from '@valtimo/components';
import {__PLUGIN_NAME__PluginConfigurationComponent} from './components/__ARTIFACT_NAME__-plugin-configuration/__ARTIFACT_NAME__-plugin-configuration.component';

@NgModule({
  declarations: [
    __PLUGIN_NAME__PluginConfigurationComponent
  ],
  imports: [
    CommonModule,
    PluginTranslatePipeModule,
    FormModule,
    InputModule,
    SelectModule,
    ParagraphModule
  ],
  exports: [
      __PLUGIN_NAME__PluginConfigurationComponent
  ]
})
export class __CLASS_PREFIX__PluginModule {}

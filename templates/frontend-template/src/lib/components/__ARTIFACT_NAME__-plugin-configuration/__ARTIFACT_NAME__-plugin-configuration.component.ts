import {Component, EventEmitter, Input, OnDestroy, OnInit, Output} from '@angular/core';
import {PluginConfigurationComponent, PluginManagementService, PluginTranslationService} from '@valtimo/plugin';
import {BehaviorSubject, combineLatest, map, Observable, Subscription, take} from 'rxjs';
import {TranslateService} from '@ngx-translate/core';
import {Config} from "../../models";


@Component({
  selector: 'valtimo-__ARTIFACT_NAME__-plugin-configuration',
  templateUrl: './__ARTIFACT_NAME__-plugin-configuration.component.html',
  styleUrls: ['./__ARTIFACT_NAME__-plugin-configuration.component.scss']
})
export class __CLASS_PREFIX__PluginConfigurationComponent implements PluginConfigurationComponent, OnInit, OnDestroy {
  @Input() save$: Observable<void>;
  @Input() disabled$: Observable<boolean>;
  @Input() pluginId: string;
  @Input() prefillConfiguration$: Observable<Config>;


  @Output() valid: EventEmitter<boolean> = new EventEmitter<boolean>();
  @Output() configuration: EventEmitter<Config> = new EventEmitter<Config>();

  private saveSubscription!: Subscription;

  private readonly formValue$ = new BehaviorSubject<Config | null>(null);
  private readonly valid$ = new BehaviorSubject<boolean>(false);

  readonly authenticationPluginSelectItems$: Observable<Array<{ id: string; text: string }>> =
    combineLatest([
      this.pluginManagementService.getPluginConfigurationsByCategory('__ARTIFACT_NAME__-plugin-template'),
      this.translateService.stream('key'),
    ]).pipe(
      map(([configurations]) =>
        configurations.map(configuration => ({
          id: configuration.id,
          text: `${configuration.title} - ${this.pluginTranslationService.instant(
            'title',
            configuration.pluginDefinition.key
          )}`,
        }))
      )
    );

  constructor(
    private readonly pluginManagementService: PluginManagementService,
    private readonly translateService: TranslateService,
    private readonly pluginTranslationService: PluginTranslationService
  ) {}

    ngOnInit(): void {
        this.openSaveSubscription();
    }

    ngOnDestroy() {
        this.saveSubscription?.unsubscribe();
    }


  formValueChange(formValue: Config): void {
    this.formValue$.next(formValue);
    this.handleValid(formValue);
  }

  private handleValid(formValue: Config): void {
    const valid = !!(
      formValue.configurationTitle &&
      formValue.exampleProperty &&
      formValue.baseUrl
    );

    this.valid$.next(valid);
    this.valid.emit(valid);
  }

  private openSaveSubscription(): void {
    this.saveSubscription = this.save$?.subscribe(() => {
      combineLatest([this.formValue$, this.valid$])
        .pipe(take(1))
        .subscribe(([formValue, valid]) => {
          if (valid) {
            this.configuration.emit(formValue);
          }
        });
    });
  }
}

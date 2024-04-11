import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { ContextAlarmInfo, EnumValue, ParameterType, WebappSdkModule, YamcsService } from '@yamcs/webapp-sdk';
import { AlarmLevelComponent } from '../../../shared/alarm-level/alarm-level.component';
import { MarkdownComponent } from '../../../shared/markdown/markdown.component';
import { ParameterCalibrationComponent } from '../../parameters/parameter-calibration/parameter-calibration.component';

@Component({
  standalone: true,
  selector: 'app-parameter-type-detail',
  templateUrl: './parameter-type-detail.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AlarmLevelComponent,
    MarkdownComponent,
    ParameterCalibrationComponent,
    WebappSdkModule,
  ],
})
export class ParameterTypeDetailComponent {

  @Input()
  parameterType: ParameterType;

  constructor(readonly yamcs: YamcsService) {
  }

  getDefaultAlarmLevel(ptype: ParameterType, enumValue: EnumValue) {
    if (ptype && ptype.defaultAlarm) {
      const alarm = ptype.defaultAlarm;
      if (alarm.enumerationAlarm) {
        for (const enumAlarm of alarm.enumerationAlarm) {
          if (enumAlarm.label === enumValue.label) {
            return enumAlarm.level;
          }
        }
      }
    }
  }

  getEnumerationAlarmLevel(contextAlarm: ContextAlarmInfo, enumValue: EnumValue) {
    const alarm = contextAlarm.alarm;
    for (const enumAlarm of alarm.enumerationAlarm) {
      if (enumAlarm.label === enumValue.label) {
        return enumAlarm.level;
      }
    }
  }
}
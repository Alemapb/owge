import { TranslateService } from '@ngx-translate/core';

import { MEDIA_ROUTES, Improvement, LoggerHelper, UserStorage, User } from '@owge/core';
import { WidgetConfirmationDialogComponent } from '@owge/widgets';
import { UniverseGameService } from '@owge/universe';

import { BaseComponent } from './../base/base.component';
import { RunningUpgrade } from './../shared-pojo/running-upgrade.pojo';
import { UpgradeService } from './../service/upgrade.service';
import { ObtainedUpgradePojo } from './../shared-pojo/obtained-upgrade.pojo';
import { Component, OnInit, Input, Output, EventEmitter, ViewChild } from '@angular/core';
import { Upgrade } from './../shared-pojo/upgrade.pojo';

@Component({
  selector: 'app-display-single-upgrade',
  templateUrl: './display-single-upgrade.component.html',
  styleUrls: ['./display-single-upgrade.component.less']
})
export class DisplaySingleUpgradeComponent extends BaseComponent implements OnInit {

  @Input()
  public upgrade: Upgrade;

  @Input()
  public obtainedUpgrade: ObtainedUpgradePojo;

  @Output()
  public onRunningUpgradeDone: EventEmitter<{}> = new EventEmitter();

  @ViewChild('confirmDialog', { static: true }) public confirmDialog: WidgetConfirmationDialogComponent;

  public image: string;
  public runningUpgrade: RunningUpgrade;
  public vConfirmDeleteText: string;

  private _log: LoggerHelper = new LoggerHelper(this.constructor.name);

  constructor(
    private _upgradeService: UpgradeService,
    private _translateService: TranslateService,
    private _universeGameService: UniverseGameService,
    private _userStore: UserStorage<User>
  ) {
    super();
    this.requireUser();
  }

  public ngOnInit() {
    if (this.obtainedUpgrade) {
      this.upgrade = this.obtainedUpgrade.upgrade;
      this._userStore.currentUserImprovements.subscribe(improvement =>
        this.obtainedUpgrade = this._upgradeService.computeReqiredResources(this.obtainedUpgrade, true, improvement)
      );
    }
    this.image = MEDIA_ROUTES.IMAGES_ROOT + this.upgrade.image;
    this._syncIsUpgrading();
  }

  public updateSelectedUpgrade(selected: ObtainedUpgradePojo): void {
    this._upgradeService.registerLevelUp(selected);
  }

  /**
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.2
   * @returns {Promise<void>}
   * @memberof DisplaySingleUpgradeComponent
   */
  public async clickCancelUpgrade(): Promise<void> {
    this.vConfirmDeleteText = await this._translateService.get(
      'UPGRADE.CANCEL_TEXT',
      { upgradeName: this.upgrade.name }
    ).toPromise();
    this.confirmDialog.show();
  }

  public cancelUpgrade(result: boolean): void {
    if (result) {
      this._upgradeService.cancelUpgrade();
    }
  }

  public otherUpgradeAlreadyRunning(): void {
    this.displayError('Ya hay una mejora en curso');
  }

  /**
   * When a it's the running upgrade an it's done, will fire an event
   *
   * @author Kevin Guanche Darias
   */
  public notifyCaller(): void {
    setTimeout(async () => {
      const improvement: Improvement = await this._universeGameService.reloadImprovement();
      this._log.todo(
        [
          'AS upgrade has end, or unit has been deleted, ' +
          'will reload improvements, when websocket becomes available, this should be removed from here',
          improvement
        ]
      );
    }, 5000);
    this.onRunningUpgradeDone.emit();
  }

  private _syncIsUpgrading(): void {
    this._upgradeService.isUpgrading.subscribe(runningUpgrade => this.runningUpgrade = runningUpgrade);
  }

}
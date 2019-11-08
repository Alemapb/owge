import { Component, OnInit } from '@angular/core';
import { first, filter } from 'rxjs/operators';

import { PlanetStore } from '@owge/galaxy';

import { MissionInformationStore } from '../../store/mission-information.store';
import { PlanetPojo } from '../../shared-pojo/planet.pojo';
import { ObtainedUnit } from '../../shared-pojo/obtained-unit.pojo';
import { UnitService } from '../../service/unit.service';

@Component({
  selector: 'app-navigation',
  templateUrl: './navigation.component.html',
  styleUrls: ['./navigation.component.less'],
  providers: [MissionInformationStore]
})
export class NavigationComponent implements OnInit {

  constructor(
    private _missioninformationStore: MissionInformationStore,
    private _unitService: UnitService,
    private _planetStore: PlanetStore
  ) { }

  public ngOnInit(): void {
    this._planetStore.selectedPlanet.pipe(filter(current => !!current)).subscribe(async selectedPlanet => {
      this._missioninformationStore.originPlanet.next(selectedPlanet);
      this._missioninformationStore.availableUnits.next(await this._findObtainedUnits(selectedPlanet));
      this._missioninformationStore.missionSent.pipe(first()).subscribe(async () => {
        this._missioninformationStore.availableUnits.next(await this._findObtainedUnits(selectedPlanet));
      });
    });
  }

  private _findObtainedUnits(planet: PlanetPojo): Promise<ObtainedUnit[]> {
    return this._unitService.findInMyPlanet(planet.id).toPromise();
  }
}
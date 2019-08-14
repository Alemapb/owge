import { BehaviorSubject } from 'rxjs';
import { ResourceManagerService } from './../service/resource-manager.service';

export class AutoUpdatedResources {
    public get currentPrimaryResource(): number {
        return this._currentPrimaryResource;
    }
    private _currentPrimaryResource: number;

    public get currentSecondaryResource(): number {
        return this._currentSecondaryResource;
    }
    private _currentSecondaryResource: number;

    public get currentEnergy(): number {
        return this._currentEnergy;
    }
    private _currentEnergy: number;

    public get currentMaxEnergy(): number {
        return this._currentMaxEnergy;
    }
    private _currentMaxEnergy: number;


    /**
     * Creates an instance of AutoUpdatedResources.
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @param {ResourceManagerService} _resourceManagerService
     * @param {boolean} [startAutoUpdate=true] (as of 0.8.0) Autostart resource sync
     * @memberof AutoUpdatedResources
     */
    constructor(private _resourceManagerService: ResourceManagerService, startAutoUpdate = true) {
        if (startAutoUpdate) {
            this.resourcesAutoUpdate();
        }
    }

    /**
   * Will auto update currentPrimaryResource and currentSecondaryResource properties
   *
   * @todo Add changeEmmiter of the energy resource
   * @returns When changes are made,
   * @author Kevin Guanche Darias
   */
    public resourcesAutoUpdate(): BehaviorSubject<boolean> {
        const changeEmmiter: BehaviorSubject<boolean> = new BehaviorSubject(false);
        this._resourceManagerService.currentPrimaryResource.subscribe(primaryResource => {
            this._currentPrimaryResource = primaryResource;
            changeEmmiter.next(true);
        });
        this._resourceManagerService.currentSecondaryResource.subscribe(
            secondaryResource => {
                this._currentSecondaryResource = secondaryResource;
                changeEmmiter.next(true);
            }
        );
        this._resourceManagerService.currentEnergy.subscribe(currentEnergy => this._currentEnergy = currentEnergy);
        this._resourceManagerService.currentMaxEnergy.subscribe(currentMaxEnergy => this._currentMaxEnergy = currentMaxEnergy);
        return changeEmmiter;
    }

    public availableEnergy(): number {
        return this._currentMaxEnergy - this._currentEnergy;
    }
}

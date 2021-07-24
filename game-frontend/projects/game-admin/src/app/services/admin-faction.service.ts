import { Injectable } from '@angular/core';
import { validContext, WidgetFilter } from '@owge/core';
import { Faction, FactionUnitType } from '@owge/faction';
import {
    AbstractCrudService, CrudConfig, CrudServiceAuthControl, UniverseGameService,
    WithImprovementsCrudMixin, WithRequirementsCrudMixin, WidgetFilterUtil
} from '@owge/universe';
import { Observable } from 'rxjs';
import { take } from 'rxjs/operators';
import { Mixin } from 'ts-mixer';
import { UnitTypeWithOverrides } from '../types/unit-type-with-overrides.type';



export interface AdminFactionService
    extends AbstractCrudService<Faction>, WithRequirementsCrudMixin<number>, WithImprovementsCrudMixin<number> { }

/**
 * Has methods related with the CRUD of the Faction
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 * @class AdminFactionService
 * @extends {AbstractCrudService<Faction>}
 */
@Injectable()
export class AdminFactionService extends AbstractCrudService<Faction> {

    protected _crudConfig: CrudConfig;

    public constructor(protected _universeGameService: UniverseGameService) {
        super(_universeGameService);
        this._crudConfig = this.getCrudConfig();
    }


    /**
     * Will filter the input by the been faction requirement
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @returns
     */
    public async buildFilter(requirementsFetcher?: WithRequirementsCrudMixin): Promise<WidgetFilter<Faction>> {
        return {
            name: 'FILTER.BY_FACTION',
            data: await this.findAll().pipe(take(1)).toPromise(),
            filterAction: async (input, selectedFaction) => {
                await WidgetFilterUtil.loadRequirementsIfRequired(input, requirementsFetcher);
                return WidgetFilterUtil.runRequirementsFilter(
                    input,
                    requirement => requirement.requirement.code === 'BEEN_RACE' && requirement.secondValue === selectedFaction.id
                );
            }
        };
    }


    /**
     * Finds the unit type overrides for the given faction
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @param factionId
     * @since 0.10.0
     * @returns
     */
    public findUnitTypes(factionId: number): Observable<FactionUnitType[]> {
        return this._universeGameService.requestWithAutorizationToContext('admin', 'get', `faction/${factionId}/unitTypes`);
    }

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.10.0
     */
    public saveUnitTypes(factionId: number, overrides: UnitTypeWithOverrides[]): Observable<void> {
        return this._universeGameService.requestWithAutorizationToContext('admin', 'put', `faction/${factionId}/unitTypes`, overrides);
    }

    protected _getEntity(): string {
        return 'faction';
    }

    protected _getContextPathPrefix(): validContext {
        return 'admin';
    }
    protected _getAuthConfiguration(): CrudServiceAuthControl {
        return {
            findAll: true,
            findById: true
        };
    }
}
(AdminFactionService as any) = Mixin(WithImprovementsCrudMixin, WithRequirementsCrudMixin, AdminFactionService as any);

import { Injectable } from '@angular/core';
import { UniverseGameService } from './universe-game.service';
import { StorageOfflineHelper, AbstractWebsocketApplicationHandler, LoadingService } from '@owge/core';
import { TutorialSectionEntry } from '../types/tutorial-section-entry.type';
import { TutorialStore } from '../storages/tutorial.store';
import { UniverseCacheManagerService } from './universe-cache-manager.service';
import { Observable, combineLatest, Subject, ReplaySubject } from 'rxjs';
import { WsEventCacheService } from './ws-event-cache.service';
import { filter, take } from 'rxjs/operators';
import { Router, NavigationEnd } from '@angular/router';

interface EntriesWithVisited extends TutorialSectionEntry {
    isVisited?: boolean;
}

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
@Injectable()
export class TutorialService extends AbstractWebsocketApplicationHandler {

    private _offlineTutorialStore: StorageOfflineHelper<TutorialSectionEntry[]>;
    private _offlineVisitedEntriesStore: StorageOfflineHelper<number[]>;
    private _store: TutorialStore = new TutorialStore;
    private _entries: EntriesWithVisited[];
    private _ready: Subject<boolean> = new ReplaySubject(1);
    private _activeRouterUrl: string;
    private _applicableEntries: Subject<TutorialSectionEntry[]> = new ReplaySubject(1);

    public constructor(
        private _universeGameService: UniverseGameService,
        private _wsEventCacheService: WsEventCacheService,
        private _universeCacheManagerService: UniverseCacheManagerService,
        private _loadingService: LoadingService,
        router: Router
    ) {
        super();
        this._eventsMap = {
            visited_tutorial_entry_change: '_onVisitedTutorialEntryChange'
        };
        combineLatest(this._store.entries, this._store.visitedEntries, (entries, visitedEntries) => {
            if (entries && entries.length) {
                this._entries = entries;
                if (visitedEntries) {
                    this._entries.forEach(entry => entry.isVisited = visitedEntries.some(visited => visited === entry.id));
                }
                this._ready.next(true);
            }
        }).subscribe();
        combineLatest(
            router.events.pipe(filter(event => event instanceof NavigationEnd)),
            _universeGameService.isInGame(),
            (event: NavigationEnd, isInGame) => {
                if (isInGame) {
                    this._activeRouterUrl = event.url;
                }
            }
        ).subscribe();
    }

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @returns
     */
    public findEntries(): Observable<TutorialSectionEntry[]> {
        return this._store.entries.asObservable();
    }

    /**
     * Will emit the applicableEntries <br>
     * Notice: Will emit after each call to triggerTutorial
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @returns
     */
    public findApplicableEntries(): Observable<TutorialSectionEntry[]> {
        return this._applicableEntries.asObservable();
    }

    /**
     * Triggers the tutorial when an event occurs
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public async triggerTutorial(): Promise<void> {
        await this._waitReady();
        if (this._activeRouterUrl) {
            this._applicableEntries.next(this._entries.filter(entry =>
                !entry.isVisited
                && (!entry.htmlSymbol.sectionFrontendPath || entry.htmlSymbol.sectionFrontendPath === this._activeRouterUrl)
            ));
        }
    }

    /**
     * Triggers the tutorial after render (Angular doesn't provide a way to know such thing, so let's just wait)
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @param [tickTime=300]
     * @returns
     */
    public triggerTutorialAfterRender(tickTime = 300): Promise<void> {
        return new Promise(resolve => window.setTimeout(() => this.triggerTutorial().then(resolve), tickTime));
    }

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @param entryId
     */
    public addVisited(entryId: number) {
        this._loadingService.addPromise(this._universeGameService.requestWithAutorizationToContext(
            'game',
            'post',
            'tutorial/visited-entries',
            entryId
        ).toPromise());
    }

    /**
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @returns
     */
    public async createStores(): Promise<void> {
        this._offlineTutorialStore = this._universeCacheManagerService.getStore('tutorial.entries');
        this._offlineVisitedEntriesStore = this._universeCacheManagerService.getStore('tutorial.visited_entries');
    }

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @returns
     */
    public async workaroundSync(): Promise<void> {
        await this._onEntriesChange(await this._wsEventCacheService.findFromCacheOrRun(
            'tutorial_entries_change',
            this._offlineTutorialStore,
            async () => await this._universeGameService.requestWithAutorizationToContext('game', 'get', 'tutorial/entries').toPromise()
        ));
        await this._onVisitedTutorialEntryChange(await this._wsEventCacheService.findFromCacheOrRun(
            'visited_tutorial_entry_change',
            this._offlineVisitedEntriesStore,
            async () => await this._universeGameService.requestWithAutorizationToContext('game', 'get', 'tutorial/visited-entries')
                .toPromise()
        ));
    }

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @returns
     */
    public async workaroundInitialOffline(): Promise<void> {
        await Promise.all([
            this._offlineTutorialStore.doIfNotNull(content => this._onEntriesChange(content)),
            this._offlineVisitedEntriesStore.doIfNotNull(visited => this._onVisitedTutorialEntryChange(visited))
        ]);
    }

    protected async _onEntriesChange(entries: TutorialSectionEntry[]): Promise<void> {
        await this._offlineTutorialStore.save(entries);
        this._store.entries.next(entries);
    }

    protected async _onVisitedTutorialEntryChange(content: number[]): Promise<void> {
        await this._offlineVisitedEntriesStore.save(content);
        this._store.visitedEntries.next(content);
        await this.triggerTutorial();
    }

    private async _waitReady(): Promise<void> {
        await this._ready.pipe(
            filter(isReady => isReady),
            take(1)
        ).toPromise();
    }
}

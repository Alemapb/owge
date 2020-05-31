import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { UniverseGameService, MissionReport, MissionReportResponse, ReportStore } from '@owge/universe';
import { AbstractWebsocketApplicationHandler } from '@owge/core';
import { map, take } from 'rxjs/operators';

@Injectable()
export class ReportService extends AbstractWebsocketApplicationHandler {

  private _currentReports: MissionReport[] = [];
  private _alreadyDownloadedReports: Set<number> = new Set();
  private _reportStore: ReportStore = new ReportStore;

  public constructor(private _universeGameService: UniverseGameService) {
    super();
    this._eventsMap = {
      mission_report_change: '_onChange',
      mission_report_count_change: '_onCountChange'
    };
  }

  public findReports<T extends MissionReport = MissionReport>(): Observable<T[]> {
    return <any>this._reportStore.reports.asObservable();
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   */
  public async workaroundSync(): Promise<void> {
    this._onChange(await this._doDownloadPage().toPromise());
  }

  /**
   * Downlods a page, doesn't return, as we trigger a change in the subject
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @param page
   */
  public async downloadPage(page: number): Promise<void> {
    this._handleReportsDownload(await this._doDownloadPage(page).pipe(
      map(result => result.reports),
      take(1)
    ).toPromise(), page !== 1);
    this._reportStore.reports.next(this._currentReports);
  }


  /**
   * Marks the reports as read
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @param reports
   * @returns
   */
  public markAsRead(reports: MissionReport[]): Observable<void> {
    const reportsIds: number[] = reports.map(current => current.id);
    return this._universeGameService.requestWithAutorizationToContext('game', 'post', 'report/mark-as-read', reportsIds);
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @returns
   */
  public findUserUnreadCount(): Observable<number> {
    return this._reportStore.userUnread.asObservable();
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @returns
   */
  public findEnemyUnreadCount(): Observable<number> {
    return this._reportStore.enemyUnread.asObservable();
  }

  protected _doDownloadPage(page = 1): Observable<MissionReportResponse> {
    return this._universeGameService.requestWithAutorizationToContext<MissionReportResponse>('game', 'get', `report/findMy?page=${page}`);
  }

  protected _onChange(content: MissionReportResponse): void {
    this._onCountChange(content);
    this._handleReportsDownload(content.reports);
    this._reportStore.reports.next(this._currentReports);
  }

  protected _onCountChange(content: MissionReportResponse): void {
    this._reportStore.userUnread.next(content.userUnread);
    this._reportStore.enemyUnread.next(content.enemyUnread);
  }

  private _handleReportsDownload(reports: MissionReport[], isPush = false): void {
    reports.filter(current => !this._alreadyDownloadedReports.has(current.id))
      .map(current => {
        if (isPush) {
          this._currentReports.push(current);
        } else {
          this._currentReports = [current, ...this._currentReports];
        }
        this._alreadyDownloadedReports.add(current.id);
        current.missionDate = new Date(current.missionDate);
        return current;
      });
  }
}

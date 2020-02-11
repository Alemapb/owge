import { Component, OnInit, Input, Output, EventEmitter, OnChanges, ChangeDetectionStrategy, ChangeDetectorRef, OnDestroy } from '@angular/core';
import { DateRepresentation, DateUtil } from '@owge/core';

/**
 * Displays a countdown <br>
 * 
 * <b>As of 0.8.1 it's capable of reseting the countdown time</b>
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.1
 * @export
 * @class CountdownComponent
 * @implements {OnInit}
 */
@Component({
  selector: 'owge-widgets-countdown',
  templateUrl: './widget-countdown.component.html',
  styleUrls: ['./widget-countdown.component.less'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class WidgetCountdownComponent implements OnInit, OnChanges, OnDestroy {

  private intervalId: number;
  private _isDestroyed = false;

  /**
   * If should auto start counting defaults to true
   */
  @Input() public autoStart = true;

  @Input() public targetDate: Date;
  @Output() public timeOver: EventEmitter<{}> = new EventEmitter();

  public get done(): boolean {
    return this._done;
  }

  public time: DateRepresentation;

  private _done = false;

  constructor(private _cdr: ChangeDetectorRef) { }

  public ngOnInit() {
    this._doStart();
  }

  public ngOnChanges() {
    this._doStart();
  }

  public ngOnDestroy(): void {
    this._isDestroyed = true;
  }

  /**
   * Starts counting only if not already started
   *
   * @author Kevin Guanche Darias
   */
  public startCounter(): void {
    if (!this.intervalId) {
      this.intervalId = window.setInterval(() => this._counterRun(), 1000);
    }
  }

  /**
   * Stops the counter, if running
   *
   * @author Kevin Guanche Darias
   */
  public stopCounter(): void {
    if (this.intervalId) {
      window.clearInterval(this.intervalId);
      this.intervalId = null;
    }
  }

  private _doStart(): void {
    if (!(this.targetDate instanceof Date)) {
      throw new Error('targetDate MUST be defined, and MUST be a Date object');
    }
    if (this.autoStart) {
      if (this.intervalId) {
        clearInterval(this.intervalId);
        delete this.intervalId;
      }
      this.startCounter();
    }
  }

  /**
   * Updates the time fields, sets the counter as done when appropiate
   *
   * @author Kevin Guanche Darias
   */
  private _counterRun(): void {
    this._done = false;
    const now = new Date();
    const unixTime = new Date(Math.abs(this.targetDate.getTime() - now.getTime()));
    if (now > this.targetDate) {
      this._done = true;
      this.stopCounter();
      this.timeOver.emit();
    } else {
      this.time = DateUtil.milisToDaysHoursMinutesSeconds(unixTime.getTime());
    }
    if (!this._isDestroyed) {
      this._cdr.detectChanges();
    }
  }
}

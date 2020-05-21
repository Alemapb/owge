import { Injectable } from '@angular/core';
import * as io from 'socket.io-client';

import { LoggerHelper, ProgrammingError } from '@owge/core';

import { AbstractWebsocketApplicationHandler } from '@owge/core';

@Injectable()
export class WebsocketService {

  private static readonly PROTOCOL_VERSION = '0.1.0';

  private _socket: SocketIOClient.Socket;
  private _isFirstConnection = true;
  private _log: LoggerHelper = new LoggerHelper(this.constructor.name);
  private _credentialsToken: string;
  private _eventHandlers: AbstractWebsocketApplicationHandler[] = [];
  private _isAuthenticated = false;

  public addEventHandler(...handler: AbstractWebsocketApplicationHandler[]) {
    this._eventHandlers = this._eventHandlers.concat(handler);
  }

  /**
   * Inits the websocket <br>
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @param {string} [targetUrl] Only required if connecting for the first time
   * @returns {Promise<void>} Solves when the socket is properly connected to the backend
   * @memberof WebsocketService
   */
  public initSocket(targetUrl?: string, jwtToken?: string): Promise<void> {
    this.setAuthenticationToken(jwtToken);
    return new Promise<void>(resolve => {
      if (!this._socket) {
        if (!targetUrl) {
          throw new ProgrammingError('targetUrl MUST be specified at least in first executions');
        }
        this._log.debug('Connecting to remote websocket server', targetUrl);
        this._socket = io.connect(targetUrl);
        this._socket.on('connect', async () => {
          if (this._isFirstConnection) {
            this._log.info('Connection established with success');
            this._isFirstConnection = false;
          } else {
            this._log.info('Reconnected');
          }
          await this.authenticate();
          resolve();
        });
        this._socket.on('disconnect', () => {
          this._log.info('client disconnected');
          this._socket.removeAllListeners();
          delete this._socket;
          this.initSocket(targetUrl, jwtToken);
          this._isAuthenticated = false;
        });
      } else if (!this._socket.connected) {
        this._socket.connect();
      } else {
        this._log.debug('It\'s already connected there is no need to reconnect again');
        resolve();
      }
    });
  }

  public setAuthenticationToken(jwtToken: string): void {
    this._credentialsToken = jwtToken;
  }

  public async authenticate(): Promise<void> {
    if (!this._isAuthenticated) {
      this._log.debug('starting authentication');
      return await new Promise<void>((resolve, reject) => {
        this._socket.emit('authentication', JSON.stringify({
          value: this._credentialsToken,
          protocol: WebsocketService.PROTOCOL_VERSION,
        }));
        this._socket.on('authentication', response => {
          this._socket.removeEventListener('authentication');
          if (response.status === 'ok') {
            this._log.debug('authenticated succeeded');
            this._isAuthenticated = true;
            this._registerSocketHandlers();
            resolve();
          } else {
            this._log.warn('An error occuring while trying to authenticate, response was', response);
            reject(response);
          }
        });
      });
    }
  }

  private async _registerSocketHandlers(): Promise<void> {
    try {
      await Promise.all(this._eventHandlers.map(current => current.workaroundSync()));
    } catch (e) {
      this._log.error('Workaround WS sync failed ', e);
    }
    this._socket.on('deliver_message', message => {
      this._log.debug('An event from backend server received', message);
      if (message && message.status && message.eventName) {
        const eventName = message.eventName;
        const handlers: AbstractWebsocketApplicationHandler[] = this._eventHandlers.filter(
          current => !!current.getHandlerMethod(eventName)
        );
        if (handlers.length) {
          handlers.forEach(async handler => {
            try {
              await handler.execute(eventName, message.value);
            } catch (e) {
              this._log.error(`Handler ${handler.constructor.name} failed for eent ${eventName}`, e);
            }
          });
        } else {
          this._log.error('No handler for event ' + eventName, message);
        }
      } else {
        this._log.warn('Bad message from backend', message);
      }
    });
  }
}

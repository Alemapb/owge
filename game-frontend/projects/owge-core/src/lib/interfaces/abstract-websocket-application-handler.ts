import { LoggerHelper } from '../helpers/logger.helper';
import { ProgrammingError } from '../errors/programming.error';

/**
 * Represents classes that are used to handle deliver_message websocket events
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 */
export abstract class AbstractWebsocketApplicationHandler {
    protected _eventsMap: { [eventName: string]: string } = {};

    protected _log: LoggerHelper = new LoggerHelper(this.constructor.name);

    public async beforeWorkaroundSync(): Promise<void> {
        // NOTICE: Override when have to do something before syncing. Note
        // You can too use the method in WesocketService to register a handler
    }

    /**
     * Actions to run when connected or reconnected to websocket
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @returns
     */
    public async workaroundSync(): Promise<void> {
        // NOTICE: Override to handle the sync call
    }

    /**
     * Actions to run when the system is offline (in the initial load of the socket connection)
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @returns
     */
    public async workaroundInitialOffline(): Promise<void> {
        // NOTICE: Override to handle the sync call
    }

    /**
     * Returns the name of the public function used to handle an event
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @param eventName Name of the event
     * @returns string name of the local public function
     */
    public getHandlerMethod(eventName: string): string {
        const handlerMethod = this._eventsMap[eventName];

        if (handlerMethod && typeof handlerMethod === 'string') {
            if (typeof this[handlerMethod] !== 'function') {
                throw new ProgrammingError(`${this.constructor.name}.${handlerMethod} is not a function`);
            }
            return handlerMethod;
        } else {
            return null;
        }
    }

    /**
     * Returns the map of events
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @returns Key is the eventName, and the value is the method name
     */
    public getEventsMap(): { [eventName: string]: string; } {
        return this._eventsMap;
    }

    /**
     * Executes the action
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @param eventName name of the event to execute
     * @param content Content sent by the socket
     * @returns Promise resolved when the event has been solved inside the method handler
     * @throws {ProgrammingError} When the eventName doesn't have a hander in this websocket handler
     */
    public async execute(eventName: string, content: any): Promise<any> {
        const functionName: string = this.getHandlerMethod(eventName);
        if (functionName && typeof this[functionName] === 'function') {
            this[functionName](content);
        } else {
            throw new ProgrammingError('Handler for ' + eventName + ' NOT found in ' + this.constructor.name);
        }
    }
}

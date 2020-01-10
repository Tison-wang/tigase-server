/**
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
package tigase.eventbus.impl;

public abstract class AbstractHandler {

	/**
	 * Defines what type of event is expected by Handler.
	 */
	public enum Type {
		/**
		 * Only non-XML events. XML events will be ignored.
		 */
		object,
		/**
		 * Only XML events. Non-XML events will be converted to XML.
		 */
		element,
		/**
		 * As is. Without conversion.
		 */
		asIs
	}
	private final String eventName;
	private final String packageName;

	public AbstractHandler(String packageName, String eventName) {
		this.packageName = packageName;
		this.eventName = eventName;
	}

	public abstract void dispatch(Object event, Object source, boolean remotelyGeneratedEvent);

	public String getEventName() {
		return eventName;
	}

	public String getPackageName() {
		return packageName;
	}

	public abstract Type getRequiredEventType();

}

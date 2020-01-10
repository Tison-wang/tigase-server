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
package tigase.server.amp.db;

import tigase.db.DataSource;
import tigase.db.DataSourceAware;
import tigase.xml.Element;
import tigase.xmpp.jid.BareJID;

import java.util.Collection;
import java.util.Date;

/**
 * Created by andrzej on 15.03.2016.
 */
public interface MsgBroadcastRepositoryIfc<T extends DataSource>
		extends DataSourceAware<T> {

	void loadMessagesToBroadcast();

	MsgBroadcastRepository.BroadcastMsg getBroadcastMsg(String id);

	String dumpBroadcastMessageKeys();

	Collection<MsgBroadcastRepository.BroadcastMsg> getBroadcastMessages();

	boolean updateBroadcastMessage(String id, Element msg, Date expire, BareJID recipient);

}

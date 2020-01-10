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
package tigase.server.xmppclient;

import tigase.eventbus.events.ShutdownEvent;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.selector.ClusterModeRequired;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default implementation for cluster environment of SeeOtherHostIfc returning redirect host based on the hash value of
 * the user's JID
 *
 * @author Wojtek
 */
@Bean(name = "seeOtherHost", parent = ClientConnectionManager.class, active = true)
@ClusterModeRequired(active = true)
public class SeeOtherHostHashed
		extends SeeOtherHost {

	private static final Logger log = Logger.getLogger(SeeOtherHostHashed.class.getName());
	//	protected List<BareJID> defaultHost = null;
	protected List<BareJID> connectedNodes = new CopyOnWriteArrayList<BareJID>();

	@Override
	public BareJID findHostForJID(BareJID jid, BareJID host) {
		int hash = Math.abs(jid.hashCode());
		if (defaultHost != null && !defaultHost.isEmpty() &&
				connectedNodes.contains(defaultHost.get(hash % defaultHost.size()))) {
			return defaultHost.get(hash % defaultHost.size());
		} else if (connectedNodes.size() > 0) {
			return connectedNodes.get(hash % connectedNodes.size());
		} else {
			return host;
		}
	}

	@Override
	public void setNodes(List<JID> connectedNodes) {
		synchronized (this) {
			JID[] arr_in = connectedNodes.toArray(new JID[connectedNodes.size()]);
			List<BareJID> list_out = new ArrayList<BareJID>();

			for (int i = 0; i < arr_in.length; i++) {
				BareJID jid = BareJID.bareJIDInstanceNS(null, arr_in[i].getDomain());
				list_out.add(jid);
			}

			setConnectedNodes(list_out);
		}
		super.setNodes(connectedNodes);
	}

	@Override
	protected void nodeShutdown(ShutdownEvent event) {
		super.nodeShutdown(event);
		synchronized (this) {
			setConnectedNodes(new ArrayList<>(this.connectedNodes));
		}
	}

	private void setConnectedNodes(List<BareJID> connectedNodes) {
		connectedNodes = filterNodes(connectedNodes);
		synchronized (this) {
			Collections.sort(connectedNodes);
			this.connectedNodes = new CopyOnWriteArrayList<>(connectedNodes);
		}
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "setting list of connected nodes: {0}", this.connectedNodes);
		}
	}

	private List<BareJID> filterNodes(List<BareJID> list) {
		Iterator<BareJID> it = list.iterator();
		while (it.hasNext()) {
			BareJID jid = it.next();
			if (isNodeShutdown(jid)) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "removing node {0} from see-other-host list as it is during shutdown", jid);
				}
				it.remove();
			}
		}
		return list;
	}
}

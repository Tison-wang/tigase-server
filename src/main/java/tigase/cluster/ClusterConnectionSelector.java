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
package tigase.cluster;

import tigase.cluster.api.ClusterConnectionHandler;
import tigase.cluster.api.ClusterConnectionSelectorIfc;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.server.Packet;
import tigase.server.Priority;
import tigase.xmpp.XMPPIOService;

import java.util.List;
import java.util.Map;

/**
 * Advanced implementation of ClusterConnectionSelectorIfc which separates packets with priority CLUSTER or higher from
 * other packets in cluster connections by using separate connections for them
 *
 * @author andrzej
 */
@Bean(name = "clusterConnectionSelector", parent = ClusterConnectionManager.class, active = true)
public class ClusterConnectionSelector
		implements ClusterConnectionSelectorIfc {

	protected static final String CLUSTER_SYS_CONNECTIONS_PER_NODE_PROP_KEY = "cluster-sys-connections-per-node";

	@ConfigField(desc = "Number of cluster connetions per node", alias = "cluster-connections-per-node")
	private int allConns = ClusterConnectionManager.CLUSTER_CONNECTIONS_PER_NODE_VAL;
	@Inject(nullAllowed = true)
	private ClusterConnectionHandler handler;
	@ConfigField(desc = "Number of system connections per node", alias = "cluster-sys-connections-per-node")
	private int sysConns = 2;

	@Override
	public XMPPIOService<Object> selectConnection(Packet p, ClusterConnection conn) {
		if (conn == null) {
			return null;
		}

		int code = Math.abs(handler.hashCodeForPacket(p));
		List<XMPPIOService<Object>> conns = conn.getConnections();
		if (conns.size() > 0) {
			if (conns.size() > sysConns) {
				if (p.getPriority() != null && p.getPriority().ordinal() <= Priority.CLUSTER.ordinal()) {
					return conns.get(code % sysConns);
				} else {
					return conns.get(sysConns + (code % (conns.size() - sysConns)));
				}
			} else {
				return conns.get(code % conns.size());
			}
		}
		return null;
	}

	@Override
	public void setClusterConnectionHandler(ClusterConnectionHandler handler) {
		this.handler = handler;
	}

	@Override
	public void setProperties(Map<String, Object> props) {
		if (props.containsKey(CLUSTER_SYS_CONNECTIONS_PER_NODE_PROP_KEY)) {
			sysConns = (Integer) props.get(CLUSTER_SYS_CONNECTIONS_PER_NODE_PROP_KEY);
		}
		if (props.containsKey(ClusterConnectionManager.CLUSTER_CONNECTIONS_PER_NODE_PROP_KEY)) {
			allConns = (Integer) props.get(ClusterConnectionManager.CLUSTER_CONNECTIONS_PER_NODE_PROP_KEY);
		}
	}
}

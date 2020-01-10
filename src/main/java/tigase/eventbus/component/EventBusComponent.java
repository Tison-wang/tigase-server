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
package tigase.eventbus.component;

import tigase.cluster.api.ClusterControllerIfc;
import tigase.cluster.api.ClusteredComponentIfc;
import tigase.component.AbstractKernelBasedComponent;
import tigase.component.modules.Module;
import tigase.component.modules.impl.AdHocCommandModule;
import tigase.component.modules.impl.JabberVersionModule;
import tigase.component.modules.impl.XmppPingModule;
import tigase.eventbus.EventBusFactory;
import tigase.eventbus.component.stores.Affiliation;
import tigase.eventbus.component.stores.AffiliationStore;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.selector.ConfigType;
import tigase.kernel.beans.selector.ConfigTypeEnum;
import tigase.kernel.core.Kernel;
import tigase.stats.StatisticsList;
import tigase.xmpp.jid.JID;

import javax.script.ScriptEngineManager;
import java.util.logging.Level;

@Bean(name = "eventbus", parent = Kernel.class, active = true)
@ConfigType({ConfigTypeEnum.DefaultMode, ConfigTypeEnum.SessionManagerMode, ConfigTypeEnum.ConnectionManagersMode,
			 ConfigTypeEnum.ComponentMode})
public class EventBusComponent
		extends AbstractKernelBasedComponent
		implements ClusteredComponentIfc {

	public EventBusComponent() {
	}

	@Override
	public String getDiscoCategory() {
		return "pubsub";
	}

	@Override
	public String getDiscoCategoryType() {
		return "service";
	}

	@Override
	public String getDiscoDescription() {
		return "Distributed EventBus";
	}

	@Override
	public void getStatistics(StatisticsList list) {
		super.getStatistics(list);
	}

	@Override
	public boolean isDiscoNonAdmin() {
		return false;
	}

	@Override
	public boolean isSubdomain() {
		return false;
	}

	@Override
	public void onNodeDisconnected(JID jid) {
		super.onNodeDisconnected(jid);

		Module module = kernel.getInstance(SubscribeModule.ID);
		if (module != null && module instanceof SubscribeModule) {
			((SubscribeModule) module).clusterNodeDisconnected(jid);
		}
		kernel.getInstance(AffiliationStore.class).removeAffiliation(jid);
	}

	@Override
	public void processPacket(tigase.server.Packet packet) {
		super.processPacket(packet);
	}

	@Override
	public void setClusterController(ClusterControllerIfc cl_controller) {
	}

	@Override
	protected void onNodeConnected(JID jid) {
		super.onNodeConnected(jid);

		if (log.isLoggable(Level.FINE)) {
			log.fine("Cluster node " + jid + " added to Affiliation Store");
		}
		kernel.getInstance(AffiliationStore.class).putAffiliation(jid, Affiliation.owner);

		Module module = kernel.getInstance(SubscribeModule.ID);
		if (module != null && module instanceof SubscribeModule) {
			((SubscribeModule) module).clusterNodeConnected(jid);
		}

	}

	@Override
	protected void registerModules(Kernel kernel) {
		kernel.registerBean("scriptEngineManager").asInstance(new ScriptEngineManager()).exec();
		kernel.registerBean("eventBusRegistrar").asInstance(EventBusFactory.getRegistrar()).exec();
		kernel.registerBean("localEventBus").asInstance(EventBusFactory.getInstance()).exec();

		kernel.registerBean(XmppPingModule.class).exec();
		kernel.registerBean(JabberVersionModule.class).exec();
		kernel.registerBean(AdHocCommandModule.class).exec();
		kernel.registerBean(EventbusDiscoveryModule.class).exec();

		// modules
		kernel.registerBean(SubscribeModule.class).exec();
		kernel.registerBean(UnsubscribeModule.class).exec();
		kernel.registerBean(EventPublisherModule.class).exec();
		kernel.registerBean(EventReceiverModule.class).exec();

		// beans
		// kernel.registerBean(ListenerScriptRegistrar.class).exec();
//		kernel.registerBean(AffiliationStore.class).exec();
//		kernel.registerBean("subscriptionStore").asClass(SubscriptionStore.class).exec();
		// ad-hoc commands
		// kernel.registerBean(AddListenerScriptCommand.class).exec();
		// kernel.registerBean(RemoveListenerScriptCommand.class).exec();
	}

}

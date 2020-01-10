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
package tigase.component;

import tigase.component.adhoc.AdHocCommandManager;
import tigase.component.modules.StanzaProcessor;
import tigase.component.modules.impl.config.ConfiguratorCommand;
import tigase.component.responses.AsyncCallback;
import tigase.component.responses.ResponseManager;
import tigase.disco.XMPPService;
import tigase.eventbus.EventBus;
import tigase.eventbus.EventBusFactory;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.RegistrarBean;
import tigase.kernel.core.Kernel;
import tigase.server.AbstractMessageReceiver;
import tigase.server.DisableDisco;
import tigase.server.Packet;

import javax.script.Bindings;
import javax.script.ScriptEngineManager;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractKernelBasedComponent
		extends AbstractMessageReceiver
		implements XMPPService, DisableDisco, RegistrarBean {

	protected final EventBus eventBus = EventBusFactory.getInstance();
	protected final Logger log = Logger.getLogger(this.getClass().getName());
	protected Kernel kernel = null;
	@Inject
	private StanzaProcessor stanzaProcessor;

	public String getComponentVersion() {
		String version = this.getClass().getPackage().getImplementationVersion();
		return version == null ? "0.0.0" : version;
	}

	public Kernel getKernel() {
		return this.kernel;
	}

	@Override
	public void initBindings(Bindings binds) {
		super.initBindings(binds);
		binds.put("kernel", kernel);
	}

	@Override
	public void start() {
		super.start();
	}

	/**
	 * Is this component discoverable by disco#items for domain by non admin users.
	 *
	 * @return <code>true</code> - if yes
	 */
	public abstract boolean isDiscoNonAdmin();

	@Override
	public void processPacket(Packet packet) {
		stanzaProcessor.processPacket(packet);
	}

	@Override
	public void register(Kernel kernel) {
		this.kernel = kernel;

		//kernel.registerBean("component").asInstance(this).exec();
		//kernel.ln("service", kernel, "component");
		kernel.registerBean("adHocCommandManager").asClass(AdHocCommandManager.class).exec();
		kernel.registerBean("scriptCommandProcessor").asClass(ComponenScriptCommandProcessor.class).exec();
		kernel.registerBean("writer").asClass(DefaultPacketWriter.class).exec();
		kernel.registerBean("responseManager").asClass(ResponseManager.class).exec();
		kernel.registerBean("stanzaProcessor").asClass(StanzaProcessor.class).exec();
		kernel.registerBean(ConfiguratorCommand.class).exec();

		registerModules(kernel);
	}

	@Override
	public void unregister(Kernel kernel) {
		this.kernel = null;
	}

	@Override
	public void updateServiceEntity() {
		super.updateServiceEntity();
		this.updateServiceDiscoveryItem(getName(), null, getDiscoDescription(), !isDiscoNonAdmin());
	}

	@Override
	protected ScriptEngineManager createScriptEngineManager() {
		ScriptEngineManager result = super.createScriptEngineManager();
		result.setBindings(new BindingsKernel(kernel));
		return result;
	}

	protected abstract void registerModules(Kernel kernel);

	@Bean(name = "writer", active = true)
	public static final class DefaultPacketWriter
			implements PacketWriter {

		protected final Logger log = Logger.getLogger(this.getClass().getName());
		@Inject(nullAllowed = false, bean = "service")
		private AbstractKernelBasedComponent component;
		@Inject(nullAllowed = false)
		private ResponseManager responseManager;

		@Override
		public void write(Collection<Packet> elements) {
			if (elements != null) {
				for (Packet element : elements) {
					if (element != null) {
						write(element);
					}
				}
			}
		}

		@Override
		public void write(Packet packet) {
			if (log.isLoggable(Level.FINER)) {
				log.finer("Sent: " + packet.getElement());
			}
			component.addOutPacket(packet);
		}

		@Override
		public void write(Packet packet, AsyncCallback callback) {
			if (log.isLoggable(Level.FINER)) {
				log.finer("Sent: " + packet.getElement());
			}
			responseManager.registerResponseHandler(packet, ResponseManager.DEFAULT_TIMEOUT, callback);
			component.addOutPacket(packet);
		}

	}

}

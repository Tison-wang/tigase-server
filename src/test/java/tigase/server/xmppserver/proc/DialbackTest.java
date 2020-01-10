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
package tigase.server.xmppserver.proc;

import junit.framework.TestCase;
import org.junit.Test;
import tigase.component.DSLBeanConfiguratorWithBackwardCompatibility;
import tigase.eventbus.EventBusFactory;
import tigase.io.CertificateContainer;
import tigase.kernel.DefaultTypesConverter;
import tigase.kernel.core.Kernel;
import tigase.server.ConnectionManager;
import tigase.server.Packet;
import tigase.server.xmppserver.*;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.StanzaType;

import java.util.*;

import static tigase.net.IOService.PORT_TYPE_PROP_KEY;

/**
 * @author andrzej
 */
public class DialbackTest
		extends TestCase {

	private Dialback dialback;
	private S2SConnectionHandlerImpl handler = null;
	private Kernel kernel;
	private String local = "local.com";
	private String remote1 = "remote1.com";
	private String remote2 = "remote2.com";

	@Test
	public void testAuthorizationForSingleDomain() throws TigaseStringprepException {
		Queue<Packet> results = new ArrayDeque<>();
		handler.setResults(results);
		dialback.init(handler, new HashMap());

		String key = UUID.randomUUID().toString();

		S2SIOService serv = new S2SIOService();
		serv.setSessionId("sess-id-1");
		Map<String, Object> props = new HashMap<>();
		props.put(PORT_TYPE_PROP_KEY, "accept");
		serv.setSessionData(props);

		Packet p = null;

		Element resultEl = new Element("db:result");
		resultEl.setXMLNS(Dialback.XMLNS_DB_VAL);
		resultEl.setAttribute("from", remote1);
		resultEl.setAttribute("to", local);
		resultEl.setCData(key);
		p = Packet.packetInstance(resultEl);
		dialback.process(p, serv, results);

		Packet r = results.poll();
		resultEl = new Element("db:verify");
		resultEl.setXMLNS(Dialback.XMLNS_DB_VAL);
		resultEl.setAttribute("id", r.getAttributeStaticStr("id"));
		resultEl.setAttribute("from", r.getAttributeStaticStr("to"));
		resultEl.setAttribute("to", r.getAttributeStaticStr("from"));
		resultEl.setAttribute("type", "valid");
		p = Packet.packetInstance(resultEl);
		dialback.process(p, serv, results);

		p = results.poll();
		assertTrue(p.getType() == StanzaType.valid && remote1.equals(p.getStanzaTo().getDomain()));

		serv.getCIDs().forEach((CID cid) -> assertEquals(remote1, cid.getRemoteHost()));
	}

	@Test
	public void testAuthorizationForSingleDomainFailure() throws TigaseStringprepException {
		Queue<Packet> results = new ArrayDeque<>();
		handler.setResults(results);
		dialback.init(handler, new HashMap());

		String key = UUID.randomUUID().toString();

		S2SIOService serv = new S2SIOService();
		serv.setSessionId("sess-id-1");
		Map<String, Object> props = new HashMap<>();
		props.put(PORT_TYPE_PROP_KEY, "accept");
		serv.setSessionData(props);

		Packet p = null;

		Element resultEl = new Element("db:result");
		resultEl.setXMLNS(Dialback.XMLNS_DB_VAL);
		resultEl.setAttribute("from", remote1);
		resultEl.setAttribute("to", local);
		resultEl.setCData(key);
		p = Packet.packetInstance(resultEl);
		dialback.process(p, serv, results);

		Packet r = results.poll();
		resultEl = new Element("db:verify");
		resultEl.setXMLNS(Dialback.XMLNS_DB_VAL);
		resultEl.setAttribute("id", r.getAttributeStaticStr("id"));
		resultEl.setAttribute("from", r.getAttributeStaticStr("to"));
		resultEl.setAttribute("to", r.getAttributeStaticStr("from"));
		resultEl.setAttribute("type", "invalid");
		p = Packet.packetInstance(resultEl);
		dialback.process(p, serv, results);

		p = results.poll();
		assertTrue(p.getType() == StanzaType.invalid && remote1.equals(p.getStanzaTo().getDomain()));

		serv.getCIDs().forEach((CID cid) -> assertNotSame(remote1, cid.getRemoteHost()));
	}

	@Test
	public void testAuthorizationWithMultiplexing() throws TigaseStringprepException {
		Queue<Packet> results = new ArrayDeque<>();
		handler.setResults(results);
		dialback.init(handler, new HashMap());

		String key = UUID.randomUUID().toString();

		S2SIOService serv = new S2SIOService();
		serv.setSessionId("sess-id-1");
		Map<String, Object> props = new HashMap<>();
		props.put(PORT_TYPE_PROP_KEY, "accept");
		serv.setSessionData(props);

		Packet p = null;

		Element resultEl = new Element("db:result");
		resultEl.setXMLNS(Dialback.XMLNS_DB_VAL);
		resultEl.setAttribute("from", remote1);
		resultEl.setAttribute("to", local);
		resultEl.setCData(key);
		p = Packet.packetInstance(resultEl);
		dialback.process(p, serv, results);

		Packet r = results.poll();
		resultEl = new Element("db:verify");
		resultEl.setXMLNS(Dialback.XMLNS_DB_VAL);
		resultEl.setAttribute("id", r.getAttributeStaticStr("id"));
		resultEl.setAttribute("from", r.getAttributeStaticStr("to"));
		resultEl.setAttribute("to", r.getAttributeStaticStr("from"));
		resultEl.setAttribute("type", "valid");
		p = Packet.packetInstance(resultEl);
		dialback.process(p, serv, results);

		p = results.poll();
		assertTrue(p.getType() == StanzaType.valid && remote1.equals(p.getStanzaTo().getDomain()));
		serv.getCIDs().forEach((CID cid) -> assertEquals(remote1, cid.getRemoteHost()));

		key = UUID.randomUUID().toString();

		resultEl = new Element("db:result");
		resultEl.setXMLNS(Dialback.XMLNS_DB_VAL);
		resultEl.setAttribute("from", remote2);
		resultEl.setAttribute("to", local);
		resultEl.setCData(key);
		p = Packet.packetInstance(resultEl);
		dialback.process(p, serv, results);

		r = results.poll();
		resultEl = new Element("db:verify");
		resultEl.setXMLNS(Dialback.XMLNS_DB_VAL);
		resultEl.setAttribute("id", r.getAttributeStaticStr("id"));
		resultEl.setAttribute("from", r.getAttributeStaticStr("to"));
		resultEl.setAttribute("to", r.getAttributeStaticStr("from"));
		resultEl.setAttribute("type", "valid");
		p = Packet.packetInstance(resultEl);
		dialback.process(p, serv, results);

		p = results.poll();
		assertTrue(p.getType() == StanzaType.valid && remote2.equals(p.getStanzaTo().getDomain()));
		assertTrue(serv.getCIDs().stream().anyMatch((CID cid) -> remote1.equals(cid.getRemoteHost())));
		assertTrue(serv.getCIDs().stream().anyMatch((CID cid) -> remote2.equals(cid.getRemoteHost())));
	}

	@Test
	public void testAuthorizationWithMultiplexingWithFailure() throws TigaseStringprepException {
		Queue<Packet> results = new ArrayDeque<>();
		handler.setResults(results);
		dialback.init(handler, new HashMap());

		String key = UUID.randomUUID().toString();

		S2SIOService serv = new S2SIOService();
		serv.setSessionId("sess-id-1");
		Map<String, Object> props = new HashMap<>();
		props.put(PORT_TYPE_PROP_KEY, "accept");
		serv.setSessionData(props);

		Packet p = null;

		Element resultEl = new Element("db:result");
		resultEl.setXMLNS(Dialback.XMLNS_DB_VAL);
		resultEl.setAttribute("from", remote1);
		resultEl.setAttribute("to", local);
		resultEl.setCData(key);
		p = Packet.packetInstance(resultEl);
		dialback.process(p, serv, results);

		Packet r = results.poll();
		resultEl = new Element("db:verify");
		resultEl.setXMLNS(Dialback.XMLNS_DB_VAL);
		resultEl.setAttribute("id", r.getAttributeStaticStr("id"));
		resultEl.setAttribute("from", r.getAttributeStaticStr("to"));
		resultEl.setAttribute("to", r.getAttributeStaticStr("from"));
		resultEl.setAttribute("type", "valid");
		p = Packet.packetInstance(resultEl);
		dialback.process(p, serv, results);

		p = results.poll();
		assertTrue(p.getType() == StanzaType.valid && remote1.equals(p.getStanzaTo().getDomain()));
		serv.getCIDs().forEach((CID cid) -> assertEquals(remote1, cid.getRemoteHost()));

		key = UUID.randomUUID().toString();

		resultEl = new Element("db:result");
		resultEl.setXMLNS(Dialback.XMLNS_DB_VAL);
		resultEl.setAttribute("from", remote2);
		resultEl.setAttribute("to", local);
		resultEl.setCData(key);
		p = Packet.packetInstance(resultEl);
		dialback.process(p, serv, results);

		r = results.poll();
		resultEl = new Element("db:verify");
		resultEl.setXMLNS(Dialback.XMLNS_DB_VAL);
		resultEl.setAttribute("id", r.getAttributeStaticStr("id"));
		resultEl.setAttribute("from", r.getAttributeStaticStr("to"));
		resultEl.setAttribute("to", r.getAttributeStaticStr("from"));
		resultEl.setAttribute("type", "invalid");
		p = Packet.packetInstance(resultEl);
		dialback.process(p, serv, results);

		p = results.poll();
		assertTrue(p.getType() == StanzaType.invalid && remote2.equals(p.getStanzaTo().getDomain()));
		assertTrue(serv.getCIDs().stream().anyMatch((CID cid) -> remote1.equals(cid.getRemoteHost())));
		assertTrue(serv.getCIDs().stream().allMatch((CID cid) -> !remote2.equals(cid.getRemoteHost())));
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Map<String, Object> props = new HashMap<>();
		props.put("name", "s2s");

		kernel = new Kernel();
		kernel.setName("s2s");
		kernel.setForceAllowNull(true);
		kernel.registerBean(DefaultTypesConverter.class).exec();
		kernel.registerBean(DSLBeanConfiguratorWithBackwardCompatibility.class).exportable().exec();
		kernel.getInstance(DSLBeanConfiguratorWithBackwardCompatibility.class).setProperties(props);
		kernel.registerBean("eventBus").asInstance(EventBusFactory.getInstance()).exportable().exec();
		kernel.registerBean(ConnectionManager.PortsConfigBean.class).exec();
		kernel.registerBean(CIDConnections.CIDConnectionsOpenerService.class).exportable().exec();
		kernel.registerBean(S2SRandomSelector.class).exportable().exec();
		kernel.registerBean(DialbackImpl.class).exportable().exec();
		kernel.registerBean(CertificateContainer.class).exportable().exec();
		kernel.registerBean("service").asClass(S2SConnectionHandlerImpl.class).setActive(true).exec();

		try {
			handler = kernel.getInstance(S2SConnectionHandlerImpl.class);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
		dialback = kernel.getInstance(Dialback.class);
	}

	public static class DialbackImpl
			extends Dialback {

		@Override
		public boolean skipTLSForHost(String hostname) {
			return true;
		}

		@Override
		protected boolean wasVerifyRequested(S2SIOService serv, String domain) {
			return true;
		}

	}

	public static class S2SConnectionHandlerImpl
			extends S2SConnectionManager {

		private Queue<Packet> results;

		public S2SConnectionHandlerImpl() {
		}

		public void setResults(Queue<Packet> results) {
			this.results = results;
		}

		@Override
		public boolean isTlsRequired(String domain) {
			return false;
		}

		@Override
		public HashSet<Integer> getDefPorts() {
			return new HashSet<>();
		}

		@Override
		protected CIDConnections createNewCIDConnections(CID cid) throws NotLocalhostException, LocalhostException {

			CIDConnections conns = new CIDConnections(cid, this, new S2SRandomSelector(), 5, 5, 5, 5000) {

				@Override
				public void sendHandshakingOnly(Packet verify_req) {
					results.offer(verify_req);
				}

				@Override
				public boolean sendControlPacket(String sessionId, Packet packet) {
					return results.offer(packet);
				}

				@Override
				public void sendPacket(Packet packet) {
					results.offer(packet);
				}

			};
			cidConnections.put(cid, conns);
			return conns;
		}
	}
}

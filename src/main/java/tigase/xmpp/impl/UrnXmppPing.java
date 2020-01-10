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
package tigase.xmpp.impl;

import tigase.db.NonAuthUserRepository;
import tigase.kernel.beans.Bean;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.server.xmppsession.SessionManager;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.PacketErrorTypeException;
import tigase.xmpp.XMPPProcessorAbstract;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.jid.JID;

import java.util.Map;
import java.util.Queue;
import java.util.logging.Logger;

/**
 * XEP-0199: XMPP Ping
 *
 * @author <a href="mailto:bmalkow@tigase.org">Bartosz Małkowski</a>
 * @version $Rev$
 */
@Bean(name = UrnXmppPing.ID, parent = SessionManager.class, active = true)
public class UrnXmppPing
		extends XMPPProcessorAbstract {

	private static final Logger log = Logger.getLogger(UrnXmppPing.class.getName());
	private static final String[][] ELEMENTS = {{Iq.ELEM_NAME, "ping"}};
	private static final String XMLNS = "urn:xmpp:ping";
	protected static final String ID = XMLNS;
	private static final Element[] DISCO_FEATURES = {new Element("feature", new String[]{"var"}, new String[]{XMLNS})};
	private static final String[] XMLNSS = {XMLNS};

	@Override
	public String id() {
		return ID;
	}

	@Override
	public void processFromUserToServerPacket(JID connectionId, Packet packet, XMPPResourceConnection session,
											  NonAuthUserRepository repo, Queue<Packet> results,
											  Map<String, Object> settings) {
		results.offer(packet.okResult((Element) null, 0));
	}

	@Override
	public void processNullSessionPacket(Packet packet, NonAuthUserRepository repo, Queue<Packet> results,
										 Map<String, Object> settings) throws PacketErrorTypeException {
		results.offer(Authorization.SERVICE_UNAVAILABLE.getResponseMessage(packet, "Service not available.", true));
	}

	@Override
	public void processServerSessionPacket(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
										   Queue<Packet> results, Map<String, Object> settings) {
		results.offer(packet.okResult((Element) null, 0));
	}

	@Override
	public Element[] supDiscoFeatures(final XMPPResourceConnection session) {
		return DISCO_FEATURES;
	}

	@Override
	public String[][] supElementNamePaths() {
		return ELEMENTS;
	}

	@Override
	public String[] supNamespaces() {
		return XMLNSS;
	}
}

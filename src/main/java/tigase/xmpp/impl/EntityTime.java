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
import tigase.xmpp.*;
import tigase.xmpp.impl.annotation.DiscoFeatures;
import tigase.xmpp.impl.annotation.Handle;
import tigase.xmpp.impl.annotation.Handles;
import tigase.xmpp.impl.annotation.Id;
import tigase.xmpp.jid.JID;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Queue;
import java.util.TimeZone;

import static tigase.xmpp.impl.EntityTime.ID;

/**
 * This supports the implementation of <a href='http://xmpp.org/extensions/xep-0202.html'>XEP-0202</a>: Entity Time.
 */
@Id(EntityTime.XMLNS)
@Handles({@Handle(path = {Iq.ELEM_NAME, EntityTime.TIME}, xmlns = EntityTime.XMLNS)})
@DiscoFeatures({EntityTime.XMLNS})
@Bean(name = ID, parent = SessionManager.class, active = true)
public class EntityTime
		extends XMPPProcessorAbstract {

	protected static final String XMLNS = "urn:xmpp:time";

	protected static final String TIME = "time";

	protected final static String ID = XMLNS;

	private static String getUtcOffset() {
		SimpleDateFormat sdf = new SimpleDateFormat("Z");
		sdf.setTimeZone(TimeZone.getDefault());
		String dateTimeString = sdf.format(new Date());

		return dateTimeString.substring(0, 3) + ":" + dateTimeString.substring(3);
	}

	private static String getUtcTime() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		return sdf.format(new Date());
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public void processFromUserOutPacket(JID connectionId, Packet packet, XMPPResourceConnection session,
										 NonAuthUserRepository repo, Queue<Packet> results,
										 Map<String, Object> settings) throws PacketErrorTypeException {
		super.processFromUserOutPacket(connectionId, packet, session, repo, results, settings);
	}

	@Override
	public void processFromUserToServerPacket(JID connectionId, Packet packet, XMPPResourceConnection session,
											  NonAuthUserRepository repo, Queue<Packet> results,
											  Map<String, Object> settings) throws PacketErrorTypeException {
		if (packet.getStanzaTo() != null && packet.getStanzaFrom() != null &&
				packet.getStanzaTo().equals(packet.getStanzaFrom())) {
			processFromUserOutPacket(connectionId, packet, session, repo, results, settings);
		} else if (packet.getType() == StanzaType.get) {
			sendTimeResult(packet, results);
		} else {
			results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet, "Message type is incorrect", true));
		}
	}

	@Override
	public void processNullSessionPacket(Packet packet, NonAuthUserRepository repo, Queue<Packet> results,
										 Map<String, Object> settings) throws PacketErrorTypeException {
		if (packet.getType() == StanzaType.get) {
			sendTimeResult(packet, results);
		} else if (packet.getType() == StanzaType.set) {
			results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet, "Message type is incorrect", true));
		} else {
			super.processNullSessionPacket(packet, repo, results, settings);
		}
	}

	@Override
	public void processServerSessionPacket(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
										   Queue<Packet> results, Map<String, Object> settings)
			throws PacketErrorTypeException {
	}

	@Override
	public void processToUserPacket(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
									Queue<Packet> results, Map<String, Object> settings)
			throws PacketErrorTypeException {
		super.processToUserPacket(packet, session, repo, results, settings);
	}

	private void sendTimeResult(Packet packet, Queue<Packet> results) {
		Packet resp = packet.okResult((Element) null, 0);

		Element time = new Element("time", new String[]{"xmlns"}, new String[]{XMLNS});

		Element tzo = new Element("tzo");
		tzo.setCData(getUtcOffset());
		time.addChild(tzo);

		Element utc = new Element("utc");
		utc.setCData(getUtcTime());
		time.addChild(utc);

		resp.getElement().addChild(time);
		results.offer(resp);
	}

}

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
package tigase.server.amp.action;

import tigase.db.RepositoryFactory;
import tigase.kernel.beans.Bean;
import tigase.server.Command;
import tigase.server.Packet;
import tigase.server.Presence;
import tigase.server.amp.ActionResultsHandlerIfc;
import tigase.server.amp.AmpComponent;
import tigase.server.amp.AmpFeatureIfc;
import tigase.server.amp.db.MsgBroadcastRepository;
import tigase.server.amp.db.MsgBroadcastRepositoryIfc;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.StanzaType;
import tigase.xmpp.jid.JID;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.server.amp.cond.ExpireAt.NAME;

/**
 * @author andrzej
 */
@Bean(name = "broadcast", parent = AmpComponent.class, active = true)
public class Broadcast
		implements AmpFeatureIfc {

	private static final Logger log = Logger.getLogger(Broadcast.class.getName());
	private static final String name = "broadcast";
	private final SimpleDateFormat formatter;
	private final SimpleDateFormat formatter2;
	private MsgBroadcastRepositoryIfc repo = null;
	private ActionResultsHandlerIfc resultsHandler;

	{
		formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		formatter2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		formatter2.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	public boolean preprocess(Packet packet) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "processing packet = {0}", packet.toString());
		}
		if (packet.getElemName() == Presence.ELEM_NAME) {
			sendBroadcastMessage(packet.getStanzaFrom());
			return true;
		}

		Element broadcast = packet.getElement().getChild("broadcast", "http://tigase.org/protocol/broadcast");
		if (broadcast == null || packet.getAttributeStaticStr(FROM_CONN_ID) != null) {
			return false;
		}

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "processing broadcast packet = {0}", packet);
		}

		if (repo != null) {
			if (packet.getStanzaTo().getResource() == null) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "setting broadcast request for user {0}", packet.getStanzaTo());
				}
				Element amp = packet.getElement().getChild("amp", AMP_XMLNS);
				Element rule = null;
				for (Element elem : amp.getChildren()) {
					if ("rule".equals(elem.getName()) &&
							"expire-at".equals(elem.getAttributeStaticStr(CONDITION_ATT))) {
						rule = elem;
						break;
					}
				}
				if (rule != null) {
					String value = rule.getAttributeStaticStr("value");
					Date expire = null;
					try {
						if (value != null) {
							if (value.contains(".")) {
								synchronized (formatter) {
									expire = formatter.parse(value);
								}
							} else {
								synchronized (formatter2) {
									expire = formatter2.parse(value);
								}
							}

							packet.getElement().removeAttribute(TO_CONN_ID);
							packet.getElement().removeAttribute(TO_RES);
							packet.getElement().removeAttribute(OFFLINE);
							packet.getElement().removeAttribute(FROM_CONN_ID);
							packet.getElement().removeAttribute(EXPIRED);

							Element msg = packet.getElement().clone();
							msg.removeAttribute("to");

							String msgId = packet.getAttributeStaticStr("id");

							MsgBroadcastRepository.BroadcastMsg bmsg = repo.getBroadcastMsg(msgId);
							boolean needToBroadcast = bmsg == null || !bmsg.needToSend(packet.getStanzaTo());

							repo.updateBroadcastMessage(msgId, msg, expire, packet.getStanzaTo().getBareJID());

							if (needToBroadcast) {
								Packet broadcastCmd = Command.BROADCAST_TO_ONLINE.getPacket(packet.getPacketTo(),
																							JID.jidInstanceNS(
																									"sess-man",
																									packet.getPacketTo()
																											.getDomain(),
																									null),
																							StanzaType.get, name);
								Command.addFieldValue(broadcastCmd, "to", packet.getStanzaTo().toString());
								msg = packet.getElement().clone();
								msg.removeAttribute("to");
								msg.setAttribute("xmlns", "http://tigase.org/protocol/broadcast");
								broadcastCmd.getElement().addChild(msg);

								resultsHandler.addOutPacket(broadcastCmd);
							}
						}
					} catch (ParseException ex) {
						log.info("Incorrect " + NAME + " condition value for rule: " + rule);
					}
					return true;
				}
			} else {
				String msgId = packet.getAttributeStaticStr("id");
				MsgBroadcastRepository.BroadcastMsg msg = repo.getBroadcastMsg(msgId);
				if (msg != null) {
					packet.getElement().removeChild(broadcast);
					msg.markAsSent(packet.getStanzaTo());
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "marking broadcast of message = {0} for user {1} as done, result = {2}",
								new Object[]{msgId, packet.getStanzaTo(), msg.needToSend(packet.getStanzaTo())});
					}
				} else {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "not found broadcast request with id = {0} for user {1}, keys = {2}",
								new Object[]{msgId, packet.getStanzaTo(), repo.dumpBroadcastMessageKeys()});
					}

				}
				return packet.getPacketTo() == null ||
						!packet.getPacketTo().getDomain().equals(packet.getPacketFrom().getDomain());
			}
		} else {
			log.log(Level.FINEST, "repository is NULL !!");
		}
		return false;
	}

	public void sendBroadcastMessage(JID jid) {
		if (repo != null) {
			for (Object o : repo.getBroadcastMessages()) {
				MsgBroadcastRepository.BroadcastMsg msg = (MsgBroadcastRepository.BroadcastMsg) o;
				if (msg.getDelay(TimeUnit.MILLISECONDS) > 0 && msg.needToSend(jid)) {
					try {
						sendBroadcastMessage(jid, msg);
					} catch (TigaseStringprepException ex) {
						log.log(Level.WARNING, "should not happen, contact developer", ex);
					}
				}
			}
		}
	}

	public void sendBroadcastMessage(JID jid, MsgBroadcastRepository.BroadcastMsg msg)
			throws TigaseStringprepException {
		Element msgEl = msg.msg.clone();
		msgEl.setAttribute("to", jid.toString());
		Packet p = Packet.packetInstance(msgEl);
		resultsHandler.addOutPacket(p);
	}

	@Override
	public String getName() {
		return name;
	}

	public Map<String, Object> getDefaults(Map<String, Object> params) {
		Map<String, Object> defs = new HashMap<String, Object>();
		String db_uri = (String) params.get(AMP_MSG_REPO_URI_PARAM);
		String db_cls = (String) params.get(AMP_MSG_REPO_CLASS_PARAM);

		if (db_uri == null) {
			db_uri = (String) params.get(RepositoryFactory.GEN_USER_DB_URI);
		}
		if (db_uri != null) {
			defs.put(AMP_MSG_REPO_URI_PROP_KEY, db_uri);
		}
		if (db_cls != null) {
			defs.put(AMP_MSG_REPO_CLASS_PROP_KEY, db_cls);
		}

		return defs;
	}

	public void setRepo(MsgBroadcastRepositoryIfc repo) {
		repo.loadMessagesToBroadcast();
		this.repo = repo;
	}

	public void setActionResultsHandler(ActionResultsHandlerIfc handler) {
		this.resultsHandler = handler;
	}
}

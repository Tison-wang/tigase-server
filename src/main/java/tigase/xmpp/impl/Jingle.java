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
import tigase.xmpp.*;
import tigase.xmpp.jid.BareJID;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Describe class Jingle here.
 * <br>
 * Created: Wed Feb 21 23:05:34 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
@Bean(name = Jingle.ID, parent = SessionManager.class, active = false)
public class Jingle
		extends XMPPProcessor
		implements XMPPProcessorIfc {

	protected static final String ID = "http://jabber.org/protocol/jingle";
	private static final Logger log = Logger.getLogger(Jingle.class.getName());
	private static final String[] JINGLE_PATH = {Iq.ELEM_NAME, "jingle"};
	private static final String[][] ELEMENTS = {JINGLE_PATH, JINGLE_PATH, JINGLE_PATH, {Iq.ELEM_NAME, "session"}};
	private static final String[] XMLNSS = {"http://jabber.org/protocol/jingle",
											"http://www.xmpp.org/extensions/xep-0166.html#ns",
											"http://www.xmpp.org/extensions/xep-0167.html#ns",
											"http://www.google.com/session"};

	@Override
	public String id() {
		return ID;
	}

	@Override
	public void process(final Packet packet, final XMPPResourceConnection conn,
						final NonAuthUserRepository nonAuthUserRepo, final Queue<Packet> results,
						final Map<String, Object> settings) throws XMPPException {
		if (conn == null) {
			return;
		}
		try {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Received packet: " + packet);
			}

			// Not needed anymore. Packet filter does it for all stanzas.
//    // For all messages coming from the owner of this account set
//    // proper 'from' attribute. This is actually needed for the case
//    // when the user sends a message to himself.
//    if (packet.getFrom().equals(conn.getConnectionId())) {
//      packet.getElement().setAttribute("from", conn.getJID());
//    } // end of if (packet.getFrom().equals(session.getConnectionId()))
			BareJID id = packet.getStanzaTo().getBareJID();

			if (conn.isUserId(id)) {

				// Yes this is message to 'this' client
				// Make sure we send it to right client connection - the connection
				// which supports jingle - this Yate specific code....
				List<XMPPResourceConnection> res = conn.getParentSession().getActiveResources();
				XMPPResourceConnection session = conn;

				if ((res != null) && (res.size() > 1)) {

					// If there are more than 1 connection for this user
					// let's look for a connection with jingle flag set...
					for (XMPPResourceConnection sess : res) {
						if (sess.getSessionData("jingle") != null) {
							session = sess;

							break;
						}
					}
				}

				Packet result = packet.copyElementOnly();

				result.setPacketTo(session.getConnectionId());
				result.setPacketFrom(packet.getTo());
				results.offer(result);
			} else {

				// This is message to some other client
				results.offer(packet.copyElementOnly());
			}    // end of else
		} catch (NotAuthorizedException e) {
			log.warning("NotAuthorizedException for packet: " + packet);
			results.offer(
					Authorization.NOT_AUTHORIZED.getResponseMessage(packet, "You must authorize session first.", true));
		}    // end of try-catch
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

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
import tigase.xmpp.impl.annotation.*;

import java.util.Map;
import java.util.Queue;
import java.util.logging.Logger;

import static tigase.xmpp.impl.SessionBind.XMLNS;

/**
 * Describe class SessionBind here.
 * <br>
 * Created: Mon Feb 20 22:43:59 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
@Id(XMLNS)
@Handle(path = {Iq.ELEM_NAME, "session"}, xmlns = XMLNS)
@StreamFeatures(@StreamFeature(elem = "session", xmlns = XMLNS, children = {"optional"}))
@DiscoFeatures({XMLNS})
@Bean(name = SessionBind.XMLNS, parent = SessionManager.class, active = true)
public class SessionBind
		extends AnnotatedXMPPProcessor
		implements XMPPProcessorIfc {

	protected static final String XMLNS = "urn:ietf:params:xml:ns:xmpp-session";
	private static final String SESSION_KEY = "Session-Set";
	private static final Logger log = Logger.getLogger(SessionBind.class.getName());

	@Override
	public void process(final Packet packet, final XMPPResourceConnection session, final NonAuthUserRepository repo,
						final Queue<Packet> results, final Map<String, Object> settings) throws XMPPException {
		if (session == null) {
			return;
		}    // end of if (session == null)
		if (!session.isAuthorized()) {
			results.offer(session.getAuthState().getResponseMessage(packet, "Session is not yet authorized.", false));

			return;
		}    // end of if (!session.isAuthorized())

		// Element request = packet.getElement();
		StanzaType type = packet.getType();

		switch (type) {
			case set:
				session.putSessionData(SESSION_KEY, "true");
				results.offer(packet.okResult((String) null, 0));

				break;

			default:
				results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet, "Session type is incorrect", false));

				break;
		}    // end of switch (type)
	}

	@Override
	public Element[] supStreamFeatures(final XMPPResourceConnection session) {
		if ((session != null) && (session.getSessionData(SESSION_KEY) == null) && session.isAuthorized()) {
			return super.supStreamFeatures(session);
		} else {
			return null;
		}    // end of if (session.isAuthorized()) else
	}
}    // SessionBind

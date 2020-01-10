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
import tigase.server.Command;
import tigase.server.Packet;
import tigase.server.xmppsession.SessionManager;
import tigase.vhosts.VHostItem;
import tigase.xml.Element;
import tigase.xmpp.*;

import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Describe class StartTLS here.
 * <br>
 * Created: Fri Mar 24 07:22:57 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
@Bean(name = StartTLS.ID, parent = SessionManager.class, active = true)
public class StartTLS
		extends XMPPProcessor
		implements XMPPProcessorIfc, XMPPPreprocessorIfc {
	// private static final String TLS_STARTED_KEY = "TLS-Started";

	public static final String EL_NAME = "starttls";

	protected static final String ID = EL_NAME;
	private static final String[][] ELEMENTS = {{EL_NAME}, {"proceed"}, {"failure"}};
	private static final Logger log = Logger.getLogger(StartTLS.class.getName());
	private static final String XMLNS = "urn:ietf:params:xml:ns:xmpp-tls";
	private static final String[] XMLNSS = {XMLNS, XMLNS, XMLNS};
	private static final Element[] F_REQUIRED = {
			new Element("starttls", new Element[]{new Element("required")}, new String[]{Packet.XMLNS_ATT},
						new String[]{XMLNS})};
	private static final Element[] F_NOT_REQUIRED = {
			new Element(EL_NAME, new String[]{Packet.XMLNS_ATT}, new String[]{XMLNS})};

	private Element failure = new Element("failure", new String[]{Packet.XMLNS_ATT}, new String[]{XMLNS});
	private Element proceed = new Element("proceed", new String[]{Packet.XMLNS_ATT}, new String[]{XMLNS});

	@Override
	public String id() {
		return ID;
	}

	@Override
	public void process(final Packet packet, final XMPPResourceConnection session, final NonAuthUserRepository repo,
						final Queue<Packet> results, final Map<String, Object> settings) {
		if (session == null) {
			return;
		}    // end of if (session == null)
		if (packet.isElement("starttls", XMLNS)) {
			if (session.getSessionData(ID) != null) {

				// Somebody tries to activate multiple TLS layers.
				// This is possible and can even work but this can also be
				// a DOS attack. Blocking it now, unless someone requests he wants
				// to have multiple layers of TLS for his connection
				log.log(Level.WARNING, "Multiple TLS requests, possible DOS attack, closing connection: {0}", packet);
				results.offer(packet.swapFromTo(failure, null, null));
				results.offer(Command.CLOSE.getPacket(packet.getTo(), packet.getFrom(), StanzaType.set,
													  session.nextStanzaId()));

				return;
			}
			session.putSessionData(ID, "true");

			Packet result = Command.STARTTLS.getPacket(packet.getTo(), packet.getFrom(), StanzaType.set,
													   session.nextStanzaId(), Command.DataType.submit);

			Command.setData(result, proceed);
			results.offer(result);
		} else {
			log.log(Level.WARNING, "Unknown TLS element: {0}", packet);
			results.offer(packet.swapFromTo(failure, null, null));
			results.offer(
					Command.CLOSE.getPacket(packet.getTo(), packet.getFrom(), StanzaType.set, session.nextStanzaId()));
		}    // end of if (packet.getElement().getName().equals("starttls")) else
	}

	@Override
	public String[][] supElementNamePaths() {
		return ELEMENTS;
	}

	@Override
	public String[] supNamespaces() {
		return XMLNSS;
	}

	@Override
	public Element[] supStreamFeatures(final XMPPResourceConnection session) {

		// If session does not exist, just return null, we don't provide features
		// for non-existen stream, the second condition means that the TLS
		// has not been yet completed for the user connection.
		if ((session != null) && (session.getSessionData(ID) == null)) {
			VHostItem vhost = session.getDomain();

			if ((vhost != null) && session.isTlsRequired()) {
				return F_REQUIRED;
			} else {
				return F_NOT_REQUIRED;
			}
		} else {
			return null;
		}    // end of if (session.isAuthorized()) else
	}

	@Override
	public boolean preProcess(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
							  Queue<Packet> results, Map<String, Object> settings) {
		boolean stop = false;

		if ((session == null) || session.isServerSession()) {
			return stop;
		}

		VHostItem vhost = session.getDomain();

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "VHost: {0}", new Object[]{vhost});
		}

		// Check whether the TLS has been completed
		// and the packet is allowed to be processed.
		if ((vhost != null) && session.isTlsRequired() && (session.getSessionData(ID) == null) &&
				!packet.isElement(EL_NAME, XMLNS)) {
			stop = true;
		}

		return stop;
	}
}    // StartTLS


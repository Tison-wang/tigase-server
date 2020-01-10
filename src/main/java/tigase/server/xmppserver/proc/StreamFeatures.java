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

import tigase.kernel.beans.Bean;
import tigase.net.ConnectionType;
import tigase.server.Packet;
import tigase.server.xmppserver.S2SConnectionManager;
import tigase.server.xmppserver.S2SIOService;
import tigase.xml.Element;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created: Dec 9, 2010 2:00:25 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
@Bean(name = "streamFeatures", parent = S2SConnectionManager.class, active = true)
public class StreamFeatures
		extends S2SAbstractProcessor {

	private static final Logger log = Logger.getLogger(StreamFeatures.class.getName());

	@Override
	public int order() {
		return Order.StreamFeatures.ordinal();
	}

	@Override
	public boolean process(Packet p, S2SIOService serv, Queue<Packet> results) {
		if (p.isElement(FEATURES_EL, FEATURES_NS)) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "{0}, Stream features received: {1}", new Object[]{serv, p});
			}

			return true;
		}

		return false;
	}

	@Override
	public String streamOpened(S2SIOService serv, Map<String, String> attribs) {
		if (attribs.containsKey("version")) {

			// A version=1.0,  sending features
			if (serv.connectionType() == ConnectionType.accept) {

				// Send features only for accept connections
				List<Element> features = handler.getStreamFeatures(serv);
				Element featuresElement = new Element(STREAM_FEATURES_EL);

				featuresElement.addChildren(features);

				// do not send stattls feature to hosts in skip tls list
				if (attribs.containsKey("from")) {
					if (skipTLSForHost(attribs.get("from"))) {
						Element startTls = featuresElement.getChild(START_TLS_EL, START_TLS_NS);
						featuresElement.removeChild(startTls);
					}
				}

				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "{0}, Sending stream features: {1}", new Object[]{serv, featuresElement});
				}

				serv.addPacketToSend(Packet.packetInstance(featuresElement, null, null));
			}
		}

		return null;
	}
}


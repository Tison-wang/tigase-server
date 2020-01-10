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

import tigase.kernel.beans.Bean;
import tigase.server.Packet;
import tigase.server.amp.ActionAbstract;
import tigase.server.amp.AmpComponent;
import tigase.xml.Element;
import tigase.xmpp.PacketErrorTypeException;

/**
 * Created: Apr 27, 2010 5:35:45 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
@Bean(name = "error", parent = AmpComponent.class, active = true)
public class Error
		extends ActionAbstract {

	private static final String name = "error";
	private static final String FAILED_RULES_PATH = "error/failed-rules";
	private static final Element UNDEF_CONDITION = new Element("undefined-condition", new String[]{"xmlns"},
															   new String[]{"urn:ietf:params:xml:ns:xmpp-stanzas"});
	private static final Element FAILED_RULES = new Element("failed-rules", new String[]{"xmlns"},
															new String[]{"http://jabber.org/protocol/amp#errors"});
	private static final Element ERROR_TEMPLATE = new Element("error", new Element[]{UNDEF_CONDITION},
															  new String[]{"type", "code"},
															  new String[]{"modify", "500"});

	@Override
	public boolean execute(Packet packet, Element rule) {
		try {
			Packet result = prepareAmpPacket(packet, rule);
			Element error = ERROR_TEMPLATE.clone();
			Element failed_rules = FAILED_RULES.clone();

			failed_rules.addChild(rule);
			error.addChild(failed_rules);
			result.getElement().addChild(error);
			resultsHandler.addOutPacket(result);
		} catch (PacketErrorTypeException ex) {

			// Ignore
		}

		return false;
	}

	@Override
	public String getName() {
		return name;
	}
}

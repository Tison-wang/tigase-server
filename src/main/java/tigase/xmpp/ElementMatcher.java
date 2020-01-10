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
package tigase.xmpp;

import tigase.server.Packet;
import tigase.xml.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * @author andrzej
 */
public class ElementMatcher {

	private final String[] path;
	private final boolean value;
	private final String xmlns;

	public static ElementMatcher create(String str) {
		List<String> path = new ArrayList<String>();
		String xmlns = null;
		int offset = 0;
		boolean value = !str.startsWith("-");
		if (str.charAt(0) == '-' || str.charAt(0) == '+') {
			str = str.substring(1);
		}
		while (true) {
			String elemName = null;

			int slashIdx = str.indexOf('/', offset);
			int sIdx = str.indexOf('[', offset);
			if (slashIdx < 0) {
				slashIdx = str.length();
			}

			Element c = null;
			if (slashIdx < sIdx || sIdx < 0) {
				elemName = str.substring(offset, slashIdx);
				xmlns = null;
			} else {
				int eIdx = str.indexOf(']', sIdx);
				elemName = str.substring(offset, sIdx);
				xmlns = str.substring(sIdx + 1, eIdx);
				slashIdx = str.indexOf('/', eIdx);
				if (slashIdx < 0) {
					slashIdx = str.length();
				}
			}

			if (elemName != null && !elemName.isEmpty()) {
				path.add(elemName.intern());
			}

			if (slashIdx == str.length()) {
				break;
			}
			offset = slashIdx + 1;
		}
		if (xmlns != null) {
			xmlns = xmlns.intern();
		}

		return new ElementMatcher(path.toArray(new String[0]), xmlns, value);
	}

	public ElementMatcher(String[] path, String xmlns, boolean value) {
		this.path = path;
		this.xmlns = xmlns;
		this.value = value;
	}

	public boolean matches(Packet packet) {
		Element child = packet.getElement().findChildStaticStr(path);
		return child != null && (xmlns == null || xmlns == child.getXMLNS());
	}

	public boolean getValue() {
		return value;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (!value) {
			sb.append('-');
		}
		for (String p : path) {
			sb.append('/');
			sb.append(p);
		}
		if (xmlns != null) {
			sb.append("[");
			sb.append(xmlns);
			sb.append("]");
		}
		return sb.toString();
	}
}

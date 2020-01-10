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
package tigase.disco;

import tigase.xml.Element;

/**
 * Describe class ServiceIdentity here.
 * <br>
 * Created: Sat Feb 10 13:34:54 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ServiceIdentity {

	private String category = null;
	private String name = null;
	private String type = null;

	/**
	 * Creates a new <code>ServiceIdentity</code> instance.
	 */
	public ServiceIdentity(String category, String type, String name) {
		this.category = category;
		this.type = type;
		this.name = name;
	}

	public String getCategory() {
		return category;
	}

	public String getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public Element getElement() {
		return new Element("identity", new String[]{"category", "type", "name"}, new String[]{category, type, name});
	}

}

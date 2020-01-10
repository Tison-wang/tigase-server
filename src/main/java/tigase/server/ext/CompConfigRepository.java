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
package tigase.server.ext;

import tigase.annotations.TigaseDeprecated;
import tigase.db.DBInitException;
import tigase.db.comp.ConfigRepository;

import java.util.Map;

/**
 * Created: Oct 3, 2009 2:00:30 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
@Deprecated
@TigaseDeprecated(since="8.0.0")
public class CompConfigRepository
		extends ConfigRepository<CompRepoItem> {

	@Override
	public void destroy() {
		// Nothing to do
	}

	@Override
	public String[] getDefaultPropetyItems() {
		return CompRepoDefaults.getDefaultPropetyItems();
	}

	@Override
	public String getPropertyKey() {
		return CompRepoDefaults.getPropertyKey();
	}

	@Override
	public String getConfigKey() {
		return CompRepoDefaults.getConfigKey();
	}

	@Override
	public CompRepoItem getItemInstance() {
		return CompRepoDefaults.getItemInstance();
	}

	@Deprecated
	@Override
	public void initRepository(String resource_uri, Map<String, String> params) throws DBInitException {
		// Nothing to do
	}

	@Override
	public String validateItem(CompRepoItem item) {
		String result = super.validateItem(item);
		if (result == null) {
			result = item.validate();
		}
		return result;
	}
}

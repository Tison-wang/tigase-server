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
package tigase.server.extdisco;

import tigase.component.AbstractKernelBasedComponent;
import tigase.component.modules.impl.AdHocCommandModule;
import tigase.component.modules.impl.DiscoveryModule;
import tigase.db.comp.ComponentRepository;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.core.Kernel;

import javax.script.Bindings;

/**
 * Created by andrzej on 06.09.2016.
 */
@Bean(name = "ext-disco", parent = Kernel.class, active = false)
public class ExternalServiceDiscoveryComponent
		extends AbstractKernelBasedComponent {

	@Inject
	private ComponentRepository<ExtServiceDiscoItem> repo;

	@Override
	public String getDiscoDescription() {
		return "External Service Discovery component";
	}

	@Override
	public void initBindings(Bindings binds) {
		super.initBindings(binds);
		binds.put(ComponentRepository.COMP_REPO_BIND, repo);
	}

	@Override
	public boolean isDiscoNonAdmin() {
		return false;
	}

	@Override
	protected void registerModules(Kernel kernel) {
		kernel.registerBean(AdHocCommandModule.class).exec();
		kernel.registerBean(DiscoveryModule.class).exec();
	}
}

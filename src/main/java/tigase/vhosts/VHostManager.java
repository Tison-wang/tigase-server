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
package tigase.vhosts;

import tigase.db.DBInitException;
import tigase.db.DataSource;
import tigase.db.DataSourceHelper;
import tigase.db.TigaseDBException;
import tigase.db.comp.AbstractSDComponentRepositoryBean;
import tigase.db.comp.ComponentRepository;
import tigase.db.comp.ComponentRepositoryDataSourceAware;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.RegistrarBean;
import tigase.kernel.beans.selector.ConfigType;
import tigase.kernel.beans.selector.ConfigTypeEnum;
import tigase.kernel.core.Kernel;
import tigase.server.AbstractComponentRegistrator;
import tigase.server.ServerComponent;
import tigase.stats.StatisticsContainer;
import tigase.stats.StatisticsList;
import tigase.util.reflection.ReflectionHelper;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import javax.script.Bindings;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Describe class VHostManager here.
 * <br>
 * Created: Fri Nov 21 14:28:20 2008
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
@Bean(name = "vhost-man", parent = Kernel.class, active = true, exportable = true)
@ConfigType({ConfigTypeEnum.DefaultMode, ConfigTypeEnum.SessionManagerMode, ConfigTypeEnum.ConnectionManagersMode, ConfigTypeEnum.ComponentMode})
public class VHostManager
		extends AbstractComponentRegistrator<VHostListener>
		implements VHostManagerIfc, StatisticsContainer, RegistrarBean {

	private static final Logger log = Logger.getLogger(VHostManager.class.getName());

	@Inject
	protected ComponentRepository<VHostItem> repo = null;
	private long getComponentsForLocalDomainCalls = 0;
	private long getComponentsForNonLocalDomainCalls = 0;
	// private ServiceEntity serviceEntity = null;
	private String identity_type = "generic";
	private long isAnonymousEnabledCalls = 0;
	private long isLocalDomainCalls = 0;
	private Kernel kernel;
	private LinkedHashSet<VHostListener> localDomainsHandlers = new LinkedHashSet<VHostListener>(10);
	private LinkedHashSet<VHostListener> nameSubdomainsHandlers = new LinkedHashSet<VHostListener>(10);
	private LinkedHashSet<VHostListener> nonLocalDomainsHandlers = new LinkedHashSet<VHostListener>(10);
	private ConcurrentSkipListSet<String> registeredComponentDomains = new ConcurrentSkipListSet<String>();

	/**
	 * Creates a new <code>VHostManager</code> instance.
	 */
	public VHostManager() {
	}

	@Override
	public void addComponentDomain(String domain) {
		registeredComponentDomains.add(domain);
	}

	@Override
	public void componentAdded(VHostListener component) {
		component.setVHostManager(this);
		if (component.handlesLocalDomains()) {
			localDomainsHandlers.add(component);
		}
		if (component.handlesNonLocalDomains()) {
			nonLocalDomainsHandlers.add(component);
		}
		if (component.handlesNameSubdomains()) {
			nameSubdomainsHandlers.add(component);
		}
	}

	@Override
	public void componentRemoved(VHostListener component) {
		localDomainsHandlers.remove(component);
		nonLocalDomainsHandlers.remove(component);
		nameSubdomainsHandlers.remove(component);
	}

	@Override
	public void initBindings(Bindings binds) {
		super.initBindings(binds);
		binds.put(ComponentRepository.COMP_REPO_BIND, repo);
		binds.put("kernel", kernel);
	}

	@Override
	public void removeComponentDomain(String domain) {
		registeredComponentDomains.remove(domain);
	}

	@Override
	public void register(Kernel kernel) {
		this.kernel = kernel;
	}

	@Override
	public void unregister(Kernel kernel) {

	}

	@Override
	public List<JID> getAllVHosts() {
		List<JID> list = new ArrayList<JID>();

		try {
			for (VHostItem item : repo.allItems()) {
				list.add(item.getVhost());
			}
		} catch (TigaseDBException ex) {
			Logger.getLogger(VHostManager.class.getName()).log(Level.SEVERE, null, ex);
		}

		return list;
	}

	@Override
	public ServerComponent[] getComponentsForLocalDomain(String domain) {
		++getComponentsForLocalDomainCalls;

		VHostItem vhost = repo.getItem(domain);

		if (vhost == null) {

			// This is not a local domain.
			// Maybe this is a 'name' subdomain: 'pubsub'.domain.name
			int idx = domain.indexOf('.');

			if (idx > 0) {
				String name = domain.substring(0, idx);
				String basedomain = domain.substring(idx + 1);
				VHostListener listener = components.get(name);

				if ((listener != null) && listener.handlesNameSubdomains() && isLocalDomain(basedomain)) {
					return new ServerComponent[]{listener};
				}
			}

			return null;
		} else {

//    // First check whether the domain has special configuration
//    // specifying what components are for this domain:
//    String[] comps = vhost.getComps();
//    if (comps != null && comps.length > 0) {
//      !!
//    }
			// Return all components for local domains and components selected
			// for this specific domain
			LinkedHashSet<ServerComponent> results = new LinkedHashSet<ServerComponent>(10);

			// are there any components explicitly bound to this domain?
			String[] comps = vhost.getComps();

			if ((comps != null) && (comps.length > 0)) {
				for (String name : comps) {
					VHostListener listener = components.get(name);

					if (listener != null) {
						results.add(listener);
					}
				}
			}

			// if not, then add any generic handlers
			if (results.size() == 0) {
				results.addAll(localDomainsHandlers);
			}
			if (results.size() > 0) {
				return results.toArray(new ServerComponent[results.size()]);
			} else {
				return null;
			}
		}
	}

	@Override
	public ServerComponent[] getComponentsForNonLocalDomain(String domain) {
		++getComponentsForNonLocalDomainCalls;

		// Return components for non-local domains
		if (nonLocalDomainsHandlers.size() > 0) {
			return nonLocalDomainsHandlers.toArray(new ServerComponent[nonLocalDomainsHandlers.size()]);
		} else {
			return null;
		}
	}

	@Override
	public BareJID getDefVHostItem() {
		Iterator<VHostItem> vhosts = repo.iterator();

		if ((vhosts != null) && vhosts.hasNext()) {
			return vhosts.next().getVhost().getBareJID();
		}

		return getDefHostName();
	}

	@Override
	public String getDiscoCategoryType() {
		return identity_type;
	}

	@Override
	public String getDiscoDescription() {
		return "VHost Manager";
	}

	@Override
	public void getStatistics(StatisticsList list) {
		list.add(getName(), "Number of VHosts", repo.size(), Level.FINE);
		list.add(getName(), "Checks: is local domain", isLocalDomainCalls, Level.FINER);
		list.add(getName(), "Checks: is anonymous domain", isAnonymousEnabledCalls, Level.FINER);
		list.add(getName(), "Get components for local domain", getComponentsForLocalDomainCalls, Level.FINER);
		list.add(getName(), "Get components for non-local domain", getComponentsForNonLocalDomainCalls, Level.FINER);
	}

	@Override
	public VHostItem getVHostItem(String domain) {
		return repo.getItem(domain);
	}

	@Override
	public VHostItem getVHostItemDomainOrComponent(String domain) {
		VHostItem item = getVHostItem(domain);
		if (item == null) {
			int idx = domain.indexOf('.');

			if (idx > 0) {
				String name = domain.substring(0, idx);
				String basedomain = domain.substring(idx + 1);
				VHostListener listener = components.get(name);
				if (listener != null && listener.handlesNameSubdomains()) {
					item = getVHostItem(basedomain);
				}
			}
		}
		return item;
	}

	@Override
	public boolean isAnonymousEnabled(String domain) {
		++isAnonymousEnabledCalls;

		VHostItem vhost = repo.getItem(domain);

		if (vhost == null) {
			return false;
		} else {
			return vhost.isAnonymousEnabled();
		}
	}

	@Override
	public boolean isCorrectType(ServerComponent component) {
		return component instanceof VHostListener;
	}

	@Override
	public boolean isLocalDomain(String domain) {
		++isLocalDomainCalls;

		return repo.contains(domain);
	}

	@Override
	public boolean isLocalDomainOrComponent(String domain) {
		boolean result = isLocalDomain(domain);

		if (!result) {
			result = registeredComponentDomains.contains(domain);
		}
		if (!result) {
			int idx = domain.indexOf('.');

			if (idx > 0) {
				String name = domain.substring(0, idx);
				String basedomain = domain.substring(idx + 1);
				VHostListener listener = components.get(name);

				result = ((listener != null) && listener.handlesNameSubdomains() && isLocalDomain(basedomain));
			}
		}

		return result;
	}

	@Override
	public void setName(String name) {
		super.setName(name);
	}

	public void initializeRepository() throws TigaseDBException {
		// loading all items
		repo.reload();

		List<VHostItem> items = new ArrayList<VHostItem>(repo.allItems());
		for (VHostItem item : items) {
			// if there is no S2S secret set for vhost, then we need to generate it
			if (item.getS2sSecret() == null) {
				String secret = generateSecret();
				item.setS2sSecret(secret);
				repo.addItem(item);
			}
		}
	}

	public String generateSecret() {
		String random = UUID.randomUUID().toString();
		return random;
	}

	public ComponentRepository<VHostItem> getComponentRepository() {
		return repo;
	}

	@Bean(name = "vhostRepository", parent = VHostManager.class, active = true)
	public static class DefVHostRepositoryBean
			extends AbstractSDComponentRepositoryBean<VHostItem> {

		private static DataSourceHelper.Matcher matcher = (Class clazz) -> {
			return ReflectionHelper.classMatchesClassWithParameters(clazz, ComponentRepositoryDataSourceAware.class,
																	new Type[]{VHostItem.class, DataSource.class});
		};
		private ComponentRepository<VHostItem> repo = null;

		@Override
		protected Class<? extends ComponentRepositoryDataSourceAware<VHostItem, DataSource>> findClassForDataSource(
				DataSource dataSource) throws DBInitException {
			Class cls = DataSourceHelper.getDefaultClass(ComponentRepository.class, dataSource.getResourceUri(),
														 matcher);
			return (Class<ComponentRepositoryDataSourceAware<VHostItem, DataSource>>) cls;
		}

	}
}


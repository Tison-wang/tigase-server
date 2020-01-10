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
package tigase.cluster.repo;

import tigase.annotations.TigaseDeprecated;
import tigase.cluster.ClusterConnectionManager;
import tigase.db.DBInitException;
import tigase.db.comp.ConfigRepository;
import tigase.eventbus.EventBus;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigAlias;
import tigase.kernel.beans.config.ConfigAliases;
import tigase.kernel.beans.config.ConfigField;
import tigase.sys.ShutdownHook;
import tigase.sys.TigaseRuntime;
import tigase.util.dns.DNSResolverFactory;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version 5.2.0, 13/03/09
 */
@ConfigAliases({@ConfigAlias(field = "items", alias = "cluster-nodes")})
public class ClConConfigRepository
		extends ConfigRepository<ClusterRepoItem>
		implements ShutdownHook, Initializable, UnregisterAware {

	private static final Logger log = Logger.getLogger(ClConConfigRepository.class.getName());

	@ConfigField(desc = "Automatically remove obsolote items", alias = "repo-auto-remove-obsolete-items")
	protected boolean auto_remove_obsolete_items = true;
	protected boolean firstLoadDone = false;
	protected long lastReloadTime = 0;
	protected long lastReloadTimeFactor = 10;
	@Inject
	private EventBus eventBus;

	public ClConConfigRepository() {
		autoReloadInterval = 15;

		if (getItem(DNSResolverFactory.getInstance().getDefaultHost()) == null) {
			ClusterRepoItem item = getItemInstance();

			item.initFromPropertyString(DNSResolverFactory.getInstance().getDefaultHost());
			addItem(item);
		}
	}

	@Override
	public void destroy() {
		// Nothing to do
	}

	@Override
	public String[] getDefaultPropetyItems() {
		return ClConRepoDefaults.getDefaultPropetyItems();
	}

	@Override
	public String getName() {
		return "Cluster repository clean-up";
	}

	@Override
	public String getPropertyKey() {
		return ClConRepoDefaults.getPropertyKey();
	}

	@Override
	public String getConfigKey() {
		return ClConRepoDefaults.getConfigKey();
	}

	@Override
	public ClusterRepoItem getItemInstance() {
		return ClConRepoDefaults.getItemInstance();
	}

	@Deprecated
	@TigaseDeprecated(since = "8.0.0")
	@Override
	public void initRepository(String resource_uri, Map<String, String> params) throws DBInitException {
		// Nothing to do
	}

	@Override
	public void reload() {
		super.reload();

		String host = DNSResolverFactory.getInstance().getDefaultHost();

		// we check if we already realoded repo from repository and have all items (own item will have
		// correct update time), if so we set flag that first load was made and if there was only one item
		// we send even that cluster was initiated
		if (!firstLoadDone) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST,
						"First Cluster repository reload done: {0}, items size: {1}, last updated own item: {2}",
						new Object[]{firstLoadDone, items.size(), items.get(host).getLastUpdate()});
			}

			if (items.get(host) != null && items.get(host).getLastUpdate() > 0) {
				firstLoadDone = true;

				if (items.size() == 1) {
					eventBus.fire(new ClusterConnectionManager.ClusterInitializedEvent());
				}

			}
		}

		ClusterRepoItem item = getItem(host);
		try {
			item = (item != null) ? (ClusterRepoItem) (item.clone()) : null;
		} catch (CloneNotSupportedException ex) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.SEVERE, "Cloning of ClusterRepoItem has failed", ex);
			}
		}

		if (item == null) {
			item = getItemInstance();
			item.setHostname(host);
		}
		item.setSecondaryHostname(DNSResolverFactory.getInstance().getSecondaryHost());
		item.setLastUpdate(System.currentTimeMillis());
		item.setCpuUsage(TigaseRuntime.getTigaseRuntime().getCPUUsage());
		item.setMemUsage(TigaseRuntime.getTigaseRuntime().getHeapMemUsage());
		storeItem(item);

		if (auto_remove_obsolete_items) {
			// we remove item after 6 * autoreload * 1000, because it may happen that we loaded an item
			// which would be removed on the next execution but it could be updated in the mean while
			removeObsoloteItems(6000);
		}

	}

	public void itemLoaded(ClusterRepoItem item) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Item loaded: {0}", item);
		}
		if (System.currentTimeMillis() - item.getLastUpdate() <= 5000 * autoReloadInterval &&
				clusterRecordValid(item)) {
			addItem(item);
		} else {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST,
						"Removing stale item: {0}; current time: {1}, last update: {2} ({3}), diff: {4}, autoreload {5}",
						new Object[]{item, System.currentTimeMillis(), item.getLastUpdate(),
									 new Date(item.getLastUpdate()), System.currentTimeMillis() - item.getLastUpdate(),
									 5000 * autoReloadInterval});
			}
			if (auto_remove_obsolete_items) {
				removeItem(item.getHostname());
			}
		}
	}

	@Override
	public boolean itemChanged(ClusterRepoItem oldItem, ClusterRepoItem newItem) {
		return !oldItem.getPassword().equals(newItem.getPassword()) || (oldItem.getPortNo() != newItem.getPortNo()) ||
				!Objects.equals(oldItem.getSecondaryHostname(), newItem.getSecondaryHostname());
	}

	@Override
	public String shutdown() {
		String host = DNSResolverFactory.getInstance().getDefaultHost();
		removeItem(host);
		return "== " + "Removing cluster_nodes item: " + host + "\n";
	}

	public void storeItem(ClusterRepoItem item) {
	}

	@Override
	public void initialize() {
		super.initialize();
		TigaseRuntime.getTigaseRuntime().addShutdownHook(this);
	}

	@Override
	public void beforeUnregister() {
		TigaseRuntime.getTigaseRuntime().removeShutdownHook(this);
		super.beforeUnregister();
	}

	protected void removeObsoloteItems(long factor) {
		Iterator<ClusterRepoItem> iterator = iterator();
		while (iterator.hasNext()) {
			ClusterRepoItem next = iterator.next();
			if ((next.getLastUpdate() > 0) &&
					System.currentTimeMillis() - next.getLastUpdate() > factor * autoReloadInterval) {
				removeItem(next.getHostname());
			}
		}
	}

	private boolean clusterRecordValid(ClusterRepoItem item) {

		// we ignore faulty addresses
		boolean isCorrect = !item.getHostname().equalsIgnoreCase("localhost");

		if (!isCorrect && log.isLoggable(Level.FINE)) {
			log.log(Level.FINE, "Incorrect entry in cluster table, skipping: {0}", item);
		}
		return isCorrect;
	}

}

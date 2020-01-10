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
import tigase.db.*;
import tigase.db.beans.DataSourceBean;
import tigase.db.comp.ComponentRepository;
import tigase.db.comp.RepositoryChangeListenerIfc;
import tigase.eventbus.EventBus;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;
import tigase.xml.DomBuilderHandler;
import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created: Nov 7, 2009 11:26:10 AM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
@Deprecated
@TigaseDeprecated(since="8.0.0")
public class CompSQLRepository
		implements ComponentRepository<CompRepoItem>, DataSourceAware<DataRepository>, Initializable, UnregisterAware {

	public static final String REPO_URI_PROP_KEY = "repo-uri";

	public static final String TABLE_NAME = "external_component";
	private static final String CONNECTION_TYPE_COLUMN = "connection_type";
	private static final String DOMAIN_COLUMN = "domain";
	private static final Logger log = Logger.getLogger(CompSQLRepository.class.getName());
	private static final String OTHER_DATA_COLUMN = "other_data";
	private static final String PASSWORD_COLUMN = "password";
	private static final String PORT_COLUMN = "port";
	private static final String PROTOCOL_COLUMN = "protocol";
	private static final String REMOTE_DOMAIN_COLUMN = "remote_domain";
	private static final String GET_ITEM_QUERY = "select * from " + TABLE_NAME + " where " + DOMAIN_COLUMN + " = ?";
	private static final String GET_ALL_ITEMS_QUERY = "select * from " + TABLE_NAME;
	private static final String DELETE_ITEM_QUERY = "delete from " + TABLE_NAME + " where (" + DOMAIN_COLUMN + " = ?)";
	private static final String CREATE_TABLE_QUERY =
			"create table " + TABLE_NAME + " (" + "  " + DOMAIN_COLUMN + " varchar(255) NOT NULL," + "  " +
					PASSWORD_COLUMN + " varchar(255) NOT NULL," + "  " + CONNECTION_TYPE_COLUMN + " varchar(127)," +
					"  " + PORT_COLUMN + " int," + "  " + REMOTE_DOMAIN_COLUMN + " varchar(255)," + "  " +
					PROTOCOL_COLUMN + " varchar(127)," + "  " + OTHER_DATA_COLUMN + " TEXT," + "  primary key(" +
					DOMAIN_COLUMN + "))";
	private static final String CHECK_TABLE_QUERY = "select count(*) from " + TABLE_NAME;
	private static final String ADD_ITEM_QUERY =
			"insert into " + TABLE_NAME + " (" + DOMAIN_COLUMN + ", " + PASSWORD_COLUMN + ", " +
					CONNECTION_TYPE_COLUMN + ", " + PORT_COLUMN + ", " + REMOTE_DOMAIN_COLUMN + ", " + PROTOCOL_COLUMN +
					", " +
//			OTHER_DATA_COLUMN + ") " + " values (?, ?, ?, ?, ?, ?, ?)";
					OTHER_DATA_COLUMN + ") " + " (select ?, ?, ?, ?, ?, ?, ? from " + TABLE_NAME + " where " +
					DOMAIN_COLUMN + " = ? HAVING count(*)=0) ";

	private CompConfigRepository configRepo = new CompConfigRepository();
	@Inject
	private DataSourceBean dataSourceBean;
	@ConfigField(desc = "Name of data source to use")
	private String dataSourceName = "default";
	private DataRepository data_repo = null;
	@Inject
	private EventBus eventBus;
	private String tableName = TABLE_NAME;

	public void setDataSourceBean(DataSourceBean dataSourceBean) {
		this.dataSourceBean = dataSourceBean;
		DataSource ds = dataSourceBean.getRepository(dataSourceName);
		if (ds != null && ds instanceof DataRepository) {
			ds.checkSchemaVersion(this, true);
			setDataSource((DataRepository) ds);
		} else {
			log.log(Level.WARNING, "Could not retrieve data source named '{0}'", new Object[]{dataSourceName});
		}
	}

	@Override
	public void initialize() {
		eventBus.registerAll(this);
	}

	@Override
	public void beforeUnregister() {
		eventBus.unregisterAll(this);
	}

	public void onDataSourceChange(DataSourceBean.DataSourceChangedEvent event) {
		if (!event.isCorrectSender(dataSourceBean)) {
			return;
		}

		DataSource ds = event.getNewDataSource();
		if (ds != null && ds instanceof DataRepository) {
			ds.checkSchemaVersion(this, true);
			setDataSource((DataRepository) ds);
		} else {
			log.log(Level.WARNING, "Could not retrieve data source named '{0}'", new Object[]{dataSourceName});
		}
	}

	@Override
	public void addRepoChangeListener(RepositoryChangeListenerIfc<CompRepoItem> repoChangeListener) {
		configRepo.addRepoChangeListener(repoChangeListener);
	}

	@Override
	public void removeRepoChangeListener(RepositoryChangeListenerIfc<CompRepoItem> repoChangeListener) {
		configRepo.removeRepoChangeListener(repoChangeListener);
	}

	@Override
	public void addItemNoStore(CompRepoItem item) {

	}

	@Override
	public void addItem(CompRepoItem item) {
		try {
			PreparedStatement addItemSt = data_repo.getPreparedStatement(null, ADD_ITEM_QUERY);

			synchronized (addItemSt) {
				if ((item.getDomain() != null) && !item.getDomain().isEmpty()) {
					addItemSt.setString(1, item.getDomain());
					addItemSt.setString(8, item.getDomain());
				} else {
					throw new NullPointerException("Null or empty domain name is not allowed");
				}
				if (item.getAuthPasswd() != null) {
					addItemSt.setString(2, item.getAuthPasswd());
				} else {
					throw new NullPointerException("Null password is not allowed");
				}
				if (item.getConnectionType() != null) {
					addItemSt.setString(3, item.getConnectionType().name());
				} else {
					addItemSt.setNull(3, Types.VARCHAR);
				}
				if (item.getPort() > 0) {
					addItemSt.setInt(4, item.getPort());
				} else {
					addItemSt.setNull(4, Types.INTEGER);
				}
				if ((item.getRemoteHost() != null) && !item.getRemoteHost().isEmpty()) {
					addItemSt.setString(5, item.getRemoteHost());
				} else {
					addItemSt.setNull(5, Types.VARCHAR);
				}
				if (item.getXMLNS() != null) {
					addItemSt.setString(6, item.getXMLNS());
				} else {
					addItemSt.setNull(6, Types.VARCHAR);
				}

				String other_data = item.toElement().toString();

				if (other_data != null) {
					addItemSt.setString(7, other_data);
				} else {
					addItemSt.setNull(7, Types.VARCHAR);
				}
				addItemSt.executeUpdate();
			}
		} catch (SQLException e) {
			log.log(Level.WARNING, "Problem adding a new item to DB: " + item.toElement(), e);
		}
	}

	@Override
	public Collection<CompRepoItem> allItems() {
		List<CompRepoItem> result = new ArrayList<CompRepoItem>();

		result.addAll(configRepo.allItems());

		try {
			ResultSet rs = null;
			PreparedStatement getAllItemsSt = data_repo.getPreparedStatement(null, GET_ALL_ITEMS_QUERY);

			synchronized (getAllItemsSt) {
				try {
					rs = getAllItemsSt.executeQuery();
					while (rs.next()) {
						result.add(createItemFromRS(rs));
					}
				} finally {
					data_repo.release(null, rs);
				}
			}
		} catch (SQLException e) {
			log.log(Level.WARNING, "Problem getting elements from DB: ", e);
		}

		return result;
	}

	@Override
	public boolean contains(String key) {
		boolean result = configRepo.contains(key);

		return result;
	}

	@Override
	public void destroy() {
		// This implementation of CompSQLRepository is using shared connection
		// pool to database which is cached by RepositoryFactory and maybe be used
		// in other places, so we can not destroy it.
	}

	@Deprecated
	@Override
	public void getDefaults(Map<String, Object> defs, Map<String, Object> params) {
		configRepo.getDefaults(defs, params);

		String repo_uri = RepositoryFactory.DERBY_REPO_URL_PROP_VAL;

		if (params.get(RepositoryFactory.GEN_USER_DB_URI) != null) {
			repo_uri = (String) params.get(RepositoryFactory.GEN_USER_DB_URI);
		}
		defs.put(REPO_URI_PROP_KEY, repo_uri);
	}

	@Override
	public CompRepoItem getItem(String key) {
		CompRepoItem result = configRepo.getItem(key);

		if (result == null) {
			try {
				ResultSet rs = null;
				PreparedStatement getItemSt = data_repo.getPreparedStatement(null, GET_ITEM_QUERY);

				synchronized (getItemSt) {
					try {
						getItemSt.setString(1, key);
						rs = getItemSt.executeQuery();
						if (rs.next()) {
							result = createItemFromRS(rs);
						}
					} finally {
						data_repo.release(null, rs);
					}
				}
			} catch (SQLException e) {
				log.log(Level.WARNING, "Problem getting element from DB for domain: " + key, e);
			}
		}

		return result;
	}

	@Override
	public CompRepoItem getItemInstance() {
		return configRepo.getItemInstance();
	}

	public void setDataSource(DataRepository data_repo) {
		try {
			checkDB();
			data_repo.initPreparedStatement(CHECK_TABLE_QUERY, CHECK_TABLE_QUERY);
			data_repo.initPreparedStatement(GET_ITEM_QUERY, GET_ITEM_QUERY);
			data_repo.initPreparedStatement(GET_ALL_ITEMS_QUERY, GET_ALL_ITEMS_QUERY);
			data_repo.initPreparedStatement(ADD_ITEM_QUERY, ADD_ITEM_QUERY);
			data_repo.initPreparedStatement(DELETE_ITEM_QUERY, DELETE_ITEM_QUERY);
		} catch (SQLException e) {
			throw new RuntimeException("Could not initialize database: ", e);
		}
	}

	@Deprecated
	@Override
	public void initRepository(String conn_str, Map<String, String> params) throws DBInitException {
		try {
			data_repo = RepositoryFactory.getDataRepository(null, conn_str, params);
			data_repo.checkSchemaVersion(this, true);
			setDataSource(data_repo);
		} catch (Exception e) {
			log.log(Level.WARNING, "Problem initializing database: ", e);
		} finally {

			// Check if DB is correctly setup and contains all required tables.
		}
	}

	@Override
	public Iterator<CompRepoItem> iterator() {
		return allItems().iterator();
	}

	@Override
	public void reload() {

		// Do nothing, no caching, everything is read on demand from DB
	}

	@Override
	public void removeItem(String key) {
		configRepo.removeItem(key);
		try {
			PreparedStatement deleteItemSt = data_repo.getPreparedStatement(null, DELETE_ITEM_QUERY);

			synchronized (deleteItemSt) {
				deleteItemSt.setString(1, key);
				deleteItemSt.executeUpdate();
			}
		} catch (SQLException e) {
			log.log(Level.WARNING, "Can't remove item: " + key, e);
		}
	}

	@Override
	public void removeItemNoStore(String key) {
		
	}

	@Deprecated
	@Override
	public void setProperties(Map<String, Object> properties) {
		configRepo.setProperties(properties);

		String repo_uri = (String) properties.get(REPO_URI_PROP_KEY);

		try {
			initRepository(repo_uri, null);
		} catch (DBInitException ex) {
			log.log(Level.WARNING, "Problem initializing database.", ex);
		}
	}

	@Override
	public int size() {
		int result = configRepo.size();

		return result;
	}

	@Override
	public void store() {

		// Do nothing everything is written on demand to DB
	}

	@Override
	public String validateItem(CompRepoItem item) {
		return null;
	}

	@Override
	public void setAutoloadTimer(long delay) {
	}

	/**
	 * Performs database check, creates missing schema if necessary
	 */
	private void checkDB() throws SQLException {
		data_repo.checkTable(tableName, CREATE_TABLE_QUERY);
	}

	private CompRepoItem createItemFromRS(ResultSet rs) throws SQLException {
		CompRepoItem result = getItemInstance();

		// First init from other parameters, some fixed fields may
		// overwrite fields initialized from other parametrs
		String other = rs.getString(OTHER_DATA_COLUMN);

		if ((other != null) && !other.isEmpty()) {
			Element elem_item = parseElement(other);

			if (elem_item != null) {
				result.initFromElement(elem_item);
			}
		}

		String domain = rs.getString(DOMAIN_COLUMN);

		if ((domain != null) && !domain.isEmpty()) {
			result.setDomain(domain);
		}

		String password = rs.getString(PASSWORD_COLUMN);

		if ((password != null) && !password.isEmpty()) {
			result.setPassword(password);
		}

		int port = rs.getInt(PORT_COLUMN);

		if (port > 0) {
			result.setPort(port);
		}

		String remote_domain = rs.getString(REMOTE_DOMAIN_COLUMN);

		if ((remote_domain != null) && !remote_domain.isEmpty()) {
			result.setRemoteDomain(remote_domain);
		}

		String protocol = rs.getString(PROTOCOL_COLUMN);

		if ((protocol != null) && !protocol.isEmpty()) {
			result.setProtocol(protocol);
		}

		String connection_type = rs.getString(CONNECTION_TYPE_COLUMN);

		if ((connection_type != null) && !connection_type.isEmpty()) {
			result.setConnectionType(connection_type);
		}

		return result;
	}

	private Element parseElement(String data) {
		DomBuilderHandler domHandler = new DomBuilderHandler();
		SimpleParser parser = SingletonFactory.getParserInstance();

		parser.parse(domHandler, data.toCharArray(), 0, data.length());

		Queue<Element> elems = domHandler.getParsedElements();

		if ((elems != null) && (elems.size() > 0)) {
			return elems.poll();
		}

		return null;
	}
}

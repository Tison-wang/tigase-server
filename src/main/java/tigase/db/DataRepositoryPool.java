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
package tigase.db;

import tigase.db.jdbc.DataRepositoryImpl;
import tigase.stats.StatisticsList;
import tigase.stats.StatisticsProviderIfc;
import tigase.util.Version;
import tigase.xmpp.jid.BareJID;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created: Sep 4, 2010 2:13:22 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
@Repository.Meta(supportedUris = {"jdbc:[^:]+:.*"})
public class DataRepositoryPool
		implements DataRepository, DataSourcePool<DataRepository>, StatisticsProviderIfc {

	private static final Logger log = Logger.getLogger(DataRepositoryPool.class.getName());

	// ~--- fields ---------------------------------------------------------------
	private dbTypes database = null;
	private CopyOnWriteArrayList<DataRepository> repoPool = new CopyOnWriteArrayList<DataRepository>();
	private String resource_uri = null;

	public void addRepo(DataRepository repo) {
		repoPool.addIfAbsent(repo);
	}

	@Override
	public boolean automaticSchemaManagement() {
		if (repoPool.isEmpty()) {
			return true;
		}
		return repoPool.get(0).automaticSchemaManagement();
	}

	@Override
	public void checkConnectivity(Duration watchdogTime) {
		repoPool.forEach(repo -> repo.checkConnectivity(watchdogTime));
	}

	public DataRepository takeRepo(BareJID user_id) {
		int idx = user_id != null ? Math.abs(user_id.hashCode() % repoPool.size()) : 0;
		DataRepository result = null;
		try {
			result = repoPool.get(idx);
		} catch (IndexOutOfBoundsException ioobe) {
			result = repoPool.get(0);
		}
		return result;
	}

	public DataRepository takeRepo(int hashCode) {
		int idx = Math.abs(hashCode % repoPool.size());
		DataRepository result = null;
		try {
			result = repoPool.get(idx);
		} catch (IndexOutOfBoundsException ioobe) {
			result = repoPool.get(0);
		}
		return result;
	}

	@Override
	public DataRepository takeRepoHandle(BareJID user_id) {
		return takeRepo(user_id);
	}

	@Override
	public void releaseRepoHandle(DataRepository repo) {
		// addRepo(repo);
	}

	@Override
	public boolean checkSchemaVersion(DataSourceAware<? extends DataSource> datasource, boolean shutdownServer) {
		DataRepository repo = takeRepo(null);

		if (repo != null) {
			return repo.checkSchemaVersion(datasource, shutdownServer);
		} else {
			log.log(Level.WARNING, "repo is NULL, pool empty? - {0}", repoPool.size());
			return false;
		}
	}

	@Override
	public Optional<Version> getSchemaVersion(String component) {
		DataRepository repo = takeRepo(null);

		if (repo != null) {
			return repo.getSchemaVersion(component);
		} else {
			log.log(Level.WARNING, "repo is NULL, pool empty? - {0}", repoPool.size());
			return Optional.empty();
		}
	}

	@Override
	public boolean checkTable(String tableName) throws SQLException {
		DataRepository repo = takeRepo(null);

		if (repo != null) {
			return repo.checkTable(tableName);
		} else {
			log.log(Level.WARNING, "repo is NULL, pool empty? - {0}", repoPool.size());
		}

		return false;
	}

	@Override
	public boolean checkTable(String tableName, String createTableQuery) throws SQLException {
		DataRepository repo = takeRepo(null);

		if (repo != null) {
			return repo.checkTable(tableName, createTableQuery);
		} else {
			log.log(Level.WARNING, "repo is NULL, pool empty? - {0}", repoPool.size());
		}

		return false;
	}

	@Override
	public Statement createStatement(BareJID user_id) throws SQLException {
		DataRepository repo = takeRepo(user_id);

		if (repo != null) {
			return repo.createStatement(user_id);
		} else {
			log.log(Level.WARNING, "repo is NULL, pool empty? - {0}", repoPool.size());
		}

		return null;
	}

	@Override
	public PreparedStatement getPreparedStatement(BareJID user_id, String stIdKey) throws SQLException {
		DataRepository repo = takeRepo(user_id);

		if (repo != null) {
			return repo.getPreparedStatement(user_id, stIdKey);
		} else {
			log.log(Level.WARNING, "repo is NULL, pool empty? - {0}", repoPool.size());
		}

		return null;
	}

	@Override
	public PreparedStatement getPreparedStatement(int hashCode, String stIdKey) throws SQLException {
		DataRepository repo = takeRepo(hashCode);

		if (repo != null) {
			return repo.getPreparedStatement(hashCode, stIdKey);
		} else {
			log.log(Level.WARNING, "repo is NULL, pool empty? - {0}", repoPool.size());
		}

		return null;
	}

	@Override
	public String getResourceUri() {
		if (resource_uri == null && !repoPool.isEmpty()) {
			return takeRepo(null).getResourceUri();
		}
		return resource_uri;
	}

	@Override
	public dbTypes getDatabaseType() {
		return database;
	}

	@Override
	public void getStatistics(String compName, StatisticsList list) {
		list.add(compName, "uri", getResourceUri(), Level.FINE);
		list.add(compName, "connections count", repoPool.size(), Level.FINE);
		for (DataRepository repo : repoPool) {
			if (repo instanceof StatisticsProviderIfc) {
				((StatisticsProviderIfc) repo).getStatistics(compName, list);
			}
		}
	}

	@Override
	public void initPreparedStatement(String stIdKey, String query) throws SQLException {
		for (DataRepository dataRepository : repoPool) {
			dataRepository.initPreparedStatement(stIdKey, query);
		}
	}

	@Override
	public void initPreparedStatement(String stIdKey, String query, int autoGeneratedKeys) throws SQLException {
		for (DataRepository dataRepository : repoPool) {
			dataRepository.initPreparedStatement(stIdKey, query, autoGeneratedKeys);
		}
	}

	@Override
	public void initialize(String resource_uri) throws DBInitException {
		this.resource_uri = resource_uri;

		if (this.database == null) {
			database = DataRepositoryImpl.parseDatabaseType(resource_uri);
		}
	}

	@Override
	@Deprecated
	public void initRepository(String resource_uri, Map<String, String> params) throws DBInitException {
		initialize(resource_uri);

		for (DataRepository dataRepository : repoPool) {
			dataRepository.initRepository(resource_uri, params);
			this.database = dataRepository.getDatabaseType();
		}
	}

	@Override
	public void release(Statement stmt, ResultSet rs) {
		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException sqlEx) {
			}
		}

		if (stmt != null) {
			try {
				stmt.close();
			} catch (SQLException sqlEx) {
			}
		}
	}

	@Override
	public void startTransaction() throws SQLException {
		// TODO Auto-generated method stub
	}

	@Override
	public void commit() throws SQLException {
		// TODO Auto-generated method stub
	}

	@Override
	public void rollback() throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void endTransaction() throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public int getPoolSize() {
		return repoPool.size();
	}
}

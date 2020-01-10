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

import tigase.annotations.TigaseDeprecated;
import tigase.osgi.ModulesManagerImpl;
import tigase.util.reflection.ClassUtilBean;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Describe class RepositoryFactory here.
 * <br>
 * Created: Tue Oct 24 22:13:52 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
@Deprecated
@TigaseDeprecated(since = "8.0.0")
public abstract class RepositoryFactory {

	public static final String AUTH_DOMAIN_POOL_CLASS = "--auth-domain-repo-pool";

	public static final String AUTH_DOMAIN_POOL_CLASS_PROP_KEY = "auth-domain-repo-pool";

	public static final String AUTH_DOMAIN_POOL_CLASS_PROP_VAL = "tigase.db.AuthRepositoryMDImpl";

	public static final String AUTH_REPO_CLASS_PROP_KEY = "auth-repo-class";

	@Deprecated
	public static final String AUTH_REPO_CLASS_PROP_VAL = "tigase.db.jdbc.TigaseCustomAuth";

	public static final String AUTH_REPO_DOMAINS_PROP_KEY = "auth-repo-domains";

	public static final String AUTH_REPO_PARAMS_NODE = "auth-repo-params";

	public static final String AUTH_REPO_POOL_CLASS = "--auth-repo-pool";

	public static final String AUTH_REPO_POOL_CLASS_PROP_DEF = "tigase.db.AuthRepositoryPool";

	public static final String AUTH_REPO_POOL_CLASS_PROP_KEY = "auth-repo-pool";

	public static final String AUTH_REPO_POOL_CLASS_PROP_VAL = null;

	public static final String AUTH_REPO_POOL_SIZE = "--auth-repo-pool-size";

	public static final String AUTH_REPO_POOL_SIZE_PROP_KEY = "auth-repo-pool-size";

	public static final int REPO_POOL_SIZE_FACTOR_PROP_VAL = 4;

	// AuthRepository properties

	public static final int AUTH_REPO_POOL_SIZE_PROP_VAL = Math.max(10, Runtime.getRuntime().availableProcessors() *
			REPO_POOL_SIZE_FACTOR_PROP_VAL);

	public static final String AUTH_REPO_URL_PROP_KEY = "auth-repo-url";

	public static final String DATA_REPO_CLASS_PROP_KEY = "data-repo";

	@Deprecated
	public static final String DATA_REPO_CLASS_PROP_VAL = "tigase.db.jdbc.DataRepositoryImpl";

	public static final String DATA_REPO_POOL_CLASS_PROP_KEY = "data-repo-pool";

	public static final String DATA_REPO_POOL_CLASS_PROP_VAL = "tigase.db.DataRepositoryPool";

	public static final String DATA_REPO_POOL_SIZE = "--data-repo-pool-size";

	public static final String DATA_REPO_POOL_SIZE_PROP_KEY = "data-repo-pool-size";
	public static final int DATA_REPO_POOL_SIZE_PROP_VAL = Math.max(10, Runtime.getRuntime().availableProcessors() *
			REPO_POOL_SIZE_FACTOR_PROP_VAL);
	public static final String DERBY_REPO_CLASS_PROP_VAL = "tigase.db.jdbc.JDBCRepository";

	// DataRepository properties
	public static final String DERBY_REPO_URL_PROP_VAL = "jdbc:derby:tigase-derbydb;create=true";

	// repositories classes and URLs
	public static final String DRUPAL_REPO_URL_PROP_VAL = "jdbc:mysql://localhost/drupal?user=root&password=mypass";
	public static final String DRUPALWP_REPO_CLASS_PROP_VAL = "tigase.db.jdbc.DrupalWPAuth";
	public static final String DUMMY_REPO_CLASS_PROP_VAL = "tigase.db.DummyRepository";
	public static final String GEN_AUTH_DB = "--auth-db";
	public static final String GEN_AUTH_DB_URI = "--auth-db-uri";
	public static final String GEN_USER_DB_PROP_KEY = "user-db";
	public static final String GEN_USER_DB = "--" + GEN_USER_DB_PROP_KEY;
	public static final String GEN_USER_DB_URI_PROP_KEY = "user-db-uri";
	public static final String GEN_USER_DB_URI = "--" + GEN_USER_DB_URI_PROP_KEY;
	public static final String LIBRESOURCE_REPO_CLASS_PROP_VAL = "tigase.db.jdbc.LibreSourceAuth";
	public static final String LIBRESOURCE_REPO_URL_PROP_VAL = "jdbc:postgresql://localhost/libresource?user=demo";
	/** Default MS SQL Server JDBC class */
	public static final String SQLSERVER_REPO_CLASS_PROP_VAL = "tigase.db.jdbc.JDBCRepository";
	/** Default MS SQL Server JDBC connection string */
	public static final String SQLSERVER_REPO_URL_PROP_VAL = "jdbc:sqlserver://localhost:1433;databaseName=tigasedb;user=tigase;password=tigase;schema=dbo";
	public static final String MYSQL_REPO_CLASS_PROP_VAL = "tigase.db.jdbc.JDBCRepository";
	public static final String MYSQL_REPO_URL_PROP_VAL = "jdbc:mysql://localhost/tigase?user=root&password=mypass";
	public static final String PGSQL_REPO_CLASS_PROP_VAL = "tigase.db.jdbc.JDBCRepository";
	public static final String PGSQL_REPO_URL_PROP_VAL = "jdbc:postgresql://localhost/tigase?user=tigase";
	public static final String SHARED_AUTH_REPO_PARAMS_PROP_KEY = "shared-auth-repo-params";
	public static final String SHARED_AUTH_REPO_PROP_KEY = "shared-auth-repo";
	public static final String SHARED_USER_REPO_PARAMS_PROP_KEY = "shared-user-repo-params";
	public static final String SHARED_USER_REPO_PROP_KEY = "shared-user-repo";
	public static final String TIGASE_AUTH_REPO_CLASS_PROP_VAL = "tigase.db.jdbc.TigaseAuth";
	public static final String TIGASE_AUTH_REPO_URL_PROP_VAL = "jdbc:mysql://localhost/tigasedb?user=tigase_user&password=mypass";
	public static final String TIGASE_CUSTOM_AUTH_REPO_CLASS_PROP_VAL = "tigase.db.jdbc.TigaseCustomAuth";
	public static final String USER_DOMAIN_POOL_CLASS = "--user-domain-repo-pool";
	public static final String USER_DOMAIN_POOL_CLASS_PROP_KEY = "user-domain-repo-pool";
	public static final String USER_DOMAIN_POOL_CLASS_PROP_VAL = "tigase.db.UserRepositoryMDImpl";
	public static final String USER_REPO_CLASS_PROP_KEY = "user-repo-class";
	@Deprecated
	public static final String USER_REPO_CLASS_PROP_VAL = "tigase.db.jdbc.JDBCRepository";
	public static final String USER_REPO_DOMAINS_PROP_KEY = "user-repo-domains";
	public static final String USER_REPO_PARAMS_NODE = "user-repo-params";
	public static final String USER_REPO_POOL_CLASS = "--user-repo-pool";
	public static final String USER_REPO_POOL_CLASS_PROP_DEF = "tigase.db.UserRepositoryPool";
	public static final String USER_REPO_POOL_CLASS_PROP_KEY = "user-repo-pool";
	public static final String USER_REPO_POOL_CLASS_PROP_VAL = null;
	public static final String USER_REPO_POOL_SIZE = "--user-repo-pool-size";
	public static final String USER_REPO_POOL_SIZE_PROP_KEY = "user-repo-pool-size";
	public static final int USER_REPO_POOL_SIZE_PROP_VAL = Math.max(10, Runtime.getRuntime().availableProcessors() *
			REPO_POOL_SIZE_FACTOR_PROP_VAL);
	public static final String USER_REPO_URL_PROP_KEY = "user-repo-url";

	// UserRepository properties
	public static final String XML_REPO_CLASS_PROP_VAL = "tigase.db.xml.XMLRepository";
	public static final String XML_REPO_URL_PROP_VAL = "user-repository.xml";
	public static final String DATABASE_TYPE_PROP_KEY = "database-type";
	private static final String REPO_POOL_SIZE_PROP_KEY = "repo-pool-size";
	private static final String DEF_REPO_POOL_SIZE_PROP_KEY = "def-repo-pool-size";
	private static final Logger log = Logger.getLogger(RepositoryFactory.class.getCanonicalName());

	private static final ConcurrentMap<String, UserRepository> user_repos = new ConcurrentHashMap<String, UserRepository>(
			USER_REPO_POOL_SIZE_PROP_VAL);
	private static final ConcurrentMap<String, DataRepository> data_repos = new ConcurrentHashMap<String, DataRepository>(
			DATA_REPO_POOL_SIZE_PROP_VAL);
	private static final ConcurrentMap<String, AuthRepository> auth_repos = new ConcurrentHashMap<String, AuthRepository>(
			AUTH_REPO_POOL_SIZE_PROP_VAL);

	public static AuthRepository getAuthRepository(String class_name, String resource, Map<String, String> params)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException, DBInitException {
		String cls = class_name;

		if (cls == null) {
			cls = System.getProperty(AUTH_REPO_CLASS_PROP_KEY);
			if (cls == null) {
				cls = getRepoClassName(AuthRepository.class, resource);
			}
		}
		if (params == null) {
			params = new LinkedHashMap<String, String>(AUTH_REPO_POOL_SIZE_PROP_VAL);
		}
		cls = getRepoClass(cls);

		AuthRepository repo = auth_repos.get(cls + resource);

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Getting AuthRepository using: {0} for: {1}; repository instance: ",
					new Object[]{cls, resource, repo});
		}

		if (repo == null) {
			String repo_pool_cls = System.getProperty(AUTH_REPO_POOL_CLASS_PROP_KEY, AUTH_REPO_POOL_CLASS_PROP_VAL);
			int repo_pool_size;

			if (params.get(RepositoryFactory.AUTH_REPO_POOL_SIZE_PROP_KEY) != null) {
				repo_pool_size = Integer.parseInt(params.get(RepositoryFactory.AUTH_REPO_POOL_SIZE_PROP_KEY));
				params.put(RepositoryFactory.REPO_POOL_SIZE_PROP_KEY, String.valueOf(repo_pool_size));
			} else if (Integer.getInteger(AUTH_REPO_POOL_SIZE_PROP_KEY) != null) {
				repo_pool_size = Integer.getInteger(AUTH_REPO_POOL_SIZE_PROP_KEY);
				params.put(RepositoryFactory.REPO_POOL_SIZE_PROP_KEY, String.valueOf(repo_pool_size));
			} else {
				repo_pool_size = AUTH_REPO_POOL_SIZE_PROP_VAL;
				params.put(RepositoryFactory.DEF_REPO_POOL_SIZE_PROP_KEY, String.valueOf(repo_pool_size));
			}
			params.put(RepositoryFactory.DATABASE_TYPE_PROP_KEY, class_name);
			if (repo_pool_cls != null) {
				AuthRepositoryPool repo_pool = (AuthRepositoryPool) ModulesManagerImpl.getInstance()
						.forName(repo_pool_cls)
						.newInstance();

				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "No AuthRepository, creating new; repo_pool_cls: {0}, repo_pool_size: {1}",
							new Object[]{repo_pool_cls, repo_pool_size});
				}

				repo_pool.initRepository(resource, params);
				for (int i = 0; i < repo_pool_size; i++) {
					repo = (AuthRepository) ModulesManagerImpl.getInstance().forName(cls).newInstance();
					repo.initRepository(resource, params);
					repo_pool.addRepo(repo);
				}
				repo = repo_pool;
			} else {
				repo = (AuthRepository) ModulesManagerImpl.getInstance().forName(cls).newInstance();
				repo.initRepository(resource, params);
			}
			auth_repos.put(cls + resource, repo);
		}

		return repo;
	}

	public static DataRepository getDataRepository(String class_name, String resource, Map<String, String> params)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException, DBInitException,
				   SQLException {
		String cls = class_name;

		if (cls == null) {
			cls = System.getProperty(DATA_REPO_CLASS_PROP_KEY);
			if (cls == null) {
				cls = getRepoClassName(DataRepository.class, resource);
			}
		}
		if (params == null) {
			params = new LinkedHashMap<String, String>(DATA_REPO_POOL_SIZE_PROP_VAL);
		}
		cls = getRepoClass(cls);

		DataRepository repo = data_repos.get(cls + resource);

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Getting DataRepository: {0} for: {1}; repository instance: ",
					new Object[]{cls, resource, repo});
		}

		if (repo == null) {
			int repo_pool_size;

			if (params.get(RepositoryFactory.DATA_REPO_POOL_SIZE_PROP_KEY) != null) {
				repo_pool_size = Integer.parseInt(params.get(RepositoryFactory.DATA_REPO_POOL_SIZE_PROP_KEY));
			} else if (params.get(RepositoryFactory.REPO_POOL_SIZE_PROP_KEY) != null) {
				repo_pool_size = Integer.parseInt(params.get(RepositoryFactory.REPO_POOL_SIZE_PROP_KEY));
			} else if (Integer.getInteger(DATA_REPO_POOL_SIZE_PROP_KEY) != null) {
				repo_pool_size = Integer.getInteger(DATA_REPO_POOL_SIZE_PROP_KEY);
			} else if (params.get(RepositoryFactory.DEF_REPO_POOL_SIZE_PROP_KEY) != null) {
				repo_pool_size = Integer.parseInt(params.get(RepositoryFactory.DEF_REPO_POOL_SIZE_PROP_KEY));
			} else {
				repo_pool_size = Integer.getInteger(DATA_REPO_POOL_SIZE_PROP_KEY, DATA_REPO_POOL_SIZE_PROP_VAL);
			}
			params.put(RepositoryFactory.DATABASE_TYPE_PROP_KEY, class_name);
			String repo_pool_cls = System.getProperty(DATA_REPO_POOL_CLASS_PROP_KEY, DATA_REPO_POOL_CLASS_PROP_VAL);

			DataRepositoryPool repo_pool = (DataRepositoryPool) ModulesManagerImpl.getInstance()
					.forName(repo_pool_cls)
					.newInstance();

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "No DataRepository, creating new; repo_pool_cls: {0}, repo_pool_size: {1}",
						new Object[]{repo_pool_cls, repo_pool_size});
			}

			repo_pool.initRepository(resource, params);
			for (int i = 0; i < repo_pool_size; i++) {
				repo = (DataRepository) ModulesManagerImpl.getInstance().forName(cls).newInstance();
				repo.initRepository(resource, params);
				repo_pool.addRepo(repo);
			}
			repo = repo_pool;
			data_repos.put(cls + resource, repo);
		}

		return repo;
	}

	/**
	 * Method returns class which would be by default used as implementation of class
	 *
	 * @param cls
	 * @param uri
	 *
	 * @return
	 *
	 * @throws tigase.db.DBInitException
	 */
	public static <T extends Class<? extends Repository>> T getRepoClass(T cls, String uri) throws DBInitException {
		Set<T> classes = getRepoInternalClasses(cls);
		Set<T> supported = new HashSet<T>();
		for (T clazz : classes) {
			Repository.Meta annotation = clazz.getAnnotation(Repository.Meta.class);
			if (annotation != null) {
				String[] supportedUris = annotation.supportedUris();
				if (supportedUris != null) {
					for (String supportedUri : supportedUris) {
						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST,
									"checking if {0} for {1} supports {2} while it supports {3} result = {4}",
									new Object[]{clazz.getCanonicalName(), cls.getCanonicalName(), uri, supportedUri,
												 Pattern.matches(supportedUri, uri)});
						}
						if (Pattern.matches(supportedUri, uri)) {
							supported.add(clazz);
						}
					}
				} else {
					supported.add(clazz);
				}
			} else {
				supported.add(clazz);
			}
		}
		if (supported.isEmpty()) {
			throw new DBInitException("[RepositoryFactory] Not found class supporting uri = " + uri);
		}
		T result = null;
		for (T clazz : supported) {
			if (result == null) {
				result = clazz;
			} else {
				Repository.Meta ar = (Repository.Meta) result.getAnnotation(Repository.Meta.class);
				Repository.Meta ac = (Repository.Meta) clazz.getAnnotation(Repository.Meta.class);
				if (ac == null) {
					continue;
				}
				if ((ar == null && ac != null) || (!ar.isDefault() &&
						((ar.supportedUris() == null && ac.supportedUris() != null) || ac.isDefault()))) {
					result = clazz;
				}
			}
		}
		return result;
	}

	public static String getRepoClass(String repo_name) {
		String result = repo_name;

		if (repo_name.equals("mysql")) {
			result = MYSQL_REPO_CLASS_PROP_VAL;
		}
		if (repo_name.equals("pgsql")) {
			result = PGSQL_REPO_CLASS_PROP_VAL;
		}
		if (repo_name.equals("derby")) {
			result = DERBY_REPO_CLASS_PROP_VAL;
		}
		if (repo_name.equals("tigase-custom-auth") || repo_name.equals("tigase-custom") ||
				repo_name.equals("custom-auth")) {
			result = TIGASE_CUSTOM_AUTH_REPO_CLASS_PROP_VAL;
		}
		if (repo_name.equals("tigase-auth")) {
			result = TIGASE_AUTH_REPO_CLASS_PROP_VAL;
		}
		if (repo_name.equals("drupal") || repo_name.equals("wp")) {
			result = DRUPALWP_REPO_CLASS_PROP_VAL;
		}
		if (repo_name.equals("libresource")) {
			result = LIBRESOURCE_REPO_CLASS_PROP_VAL;
		}
		if (repo_name.equals("sqlserver")) {
			result = SQLSERVER_REPO_CLASS_PROP_VAL;
		}

		return result;
	}

	/**
	 * Returns name of class which would be used as repository implementation
	 *
	 * @param cls - interface class needs to implement
	 * @param uri - uri which needs to be supported by implementation
	 *
	 * @return
	 *
	 * @throws DBInitException
	 */
	public static String getRepoClassName(Class cls, String uri) throws DBInitException {
		Class result = getRepoClass(cls, uri);
		return result.getCanonicalName();
	}

	/**
	 * Method returns internal (available in server classpath) implementation of classes extending or implementing class
	 * passed as cls parameter.
	 *
	 * @param cls - class for which we look for implementations of extensions
	 *
	 * @return set of classes matching criteria
	 */
	private static <T extends Class<? extends Repository>> Set<T> getRepoInternalClasses(T cls) {
		HashSet<T> result = new HashSet<T>();
		for (Class<?> clazz : ClassUtilBean.getInstance().getAllClasses()) {
			if (cls.isAssignableFrom(clazz)) {
				result.add((T) clazz);
			}
		}
		return result;
	}

	public static UserRepository getUserRepository(String class_name, String resource, Map<String, String> params)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException, DBInitException {
		String cls = class_name;

		if (resource == null) {
			resource = System.getProperty(GEN_USER_DB_URI_PROP_KEY);
		}

		if (cls == null) {
			cls = System.getProperty(USER_REPO_CLASS_PROP_KEY);
			if (cls == null) {
				cls = getRepoClassName(UserRepository.class, resource);
				if (cls == null) {
					cls = System.getProperty(GEN_USER_DB_PROP_KEY);
				}
			}
		}
		if (params == null) {
			params = new LinkedHashMap<String, String>(USER_REPO_POOL_SIZE_PROP_VAL);
		}
		cls = getRepoClass(cls);

		UserRepository repo = user_repos.get(cls + resource);

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Getting UserRepository: {0} for: {1}; repository instance: ",
					new Object[]{cls, resource, repo});
		}

		if (repo == null) {
			String repo_pool_cls = System.getProperty(USER_REPO_POOL_CLASS_PROP_KEY, USER_REPO_POOL_CLASS_PROP_VAL);
			int repo_pool_size;

			if (params.get(RepositoryFactory.USER_REPO_POOL_SIZE_PROP_KEY) != null) {
				repo_pool_size = Integer.parseInt(params.get(RepositoryFactory.USER_REPO_POOL_SIZE_PROP_KEY));
				params.put(RepositoryFactory.REPO_POOL_SIZE_PROP_KEY, String.valueOf(repo_pool_size));
			} else if (Integer.getInteger(USER_REPO_POOL_SIZE_PROP_KEY) != null) {
				repo_pool_size = Integer.getInteger(USER_REPO_POOL_SIZE_PROP_KEY);
				params.put(RepositoryFactory.REPO_POOL_SIZE_PROP_KEY, String.valueOf(repo_pool_size));
			} else {
				repo_pool_size = USER_REPO_POOL_SIZE_PROP_VAL;
				params.put(RepositoryFactory.DEF_REPO_POOL_SIZE_PROP_KEY, String.valueOf(repo_pool_size));
			}

			params.put(RepositoryFactory.DATABASE_TYPE_PROP_KEY, class_name);

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "No UserRepository, creating new; repo_pool_cls: {0}, repo_pool_size: {1}",
						new Object[]{repo_pool_cls, repo_pool_size});
			}

			if (repo_pool_cls != null) {
				UserRepositoryPool repo_pool = (UserRepositoryPool) ModulesManagerImpl.getInstance()
						.forName(repo_pool_cls)
						.newInstance();

				repo_pool.initRepository(resource, params);
				for (int i = 0; i < repo_pool_size; i++) {
					repo = (UserRepository) ModulesManagerImpl.getInstance().forName(cls).newInstance();
					repo.initRepository(resource, params);
					repo_pool.addRepo(repo);
				}
				repo = repo_pool;
			} else {
				repo = (UserRepository) ModulesManagerImpl.getInstance().forName(cls).newInstance();
				repo.initRepository(resource, params);
			}
			user_repos.put(cls + resource, repo);
		}

		return repo;
	}

}    // RepositoryFactory


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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

/**
 * Base interface which should be implemented by every repository to have one common interface
 *
 * @author andrzej
 */
public interface Repository {

	/**
	 * Method is deprecated and should not be user any more.
	 * <br>
	 * The method is called to initialize the data repository. Depending on the implementation all the initialization
	 * parameters can be passed either via <code>resource_uri</code> parameter as the database connection string or via
	 * <code>params</code> map if the required repository parameters are more complex or both.
	 *
	 * @param resource_uri value in most cases representing the database connection string.
	 * @param params is a <code>Map</code> with repository properties necessary to initialize and perform all the
	 * functions. The initialization parameters are implementation dependent.
	 *
	 * @throws tigase.db.DBInitException if there was an error during repository initialization. Some implementations,
	 * though, perform so called lazy initialization so even though there is a problem with the underlying repository it
	 * may not be signaled through this method call.
	 */
	@Deprecated
	@TigaseDeprecated(since = "8.0.0")
	default void initRepository(String resource_uri, Map<String, String> params) throws DBInitException {

	}

	/**
	 * Meta created to add possibility to retrieve information about implementation of repository (ie. supported
	 * database URI)
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public static @interface Meta {

		/**
		 * Is it default implementation for supported URIs?
		 *
		 * @return true if yes
		 */
		boolean isDefault() default false;

		/**
		 * Method returns and array of strings with regexps matching URIs which are supported by annotated class.
		 *
		 * @return string array of regular expressions
		 */
		String[] supportedUris();
	}

	/**
	 * Annotation provides information about schema (ie. database schema) required by annotated class.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public static @interface SchemaId {

		/**
		 * Method returns identifiers of a schema (ie. database schema) required by annotated class.
		 *
		 * @return id of a schema
		 */
		String id();

		/**
		 * Method returns name of a schema required by annotated class. * <br> Result of this method is intended to be
		 * displayed to the user.
		 *
		 * @return name of a schema
		 */
		String name();

		/**
		 * Method indicates whether schema definition is stored externally to the class (for example in {@code .sql} files.
		 *
		 * @return true if the schema definitions are stored in {@code .sql} files.
		 */
		boolean external() default true;
	}

}

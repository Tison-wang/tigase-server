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

public interface ClusterRepoConstants {

	public static final String CPU_USAGE_COLUMN = "cpu_usage";

	public static final String HOSTNAME_COLUMN = "hostname";

	public static final String SECONDARY_HOSTNAME_COLUMN = "secondary";

	public static final String LASTUPDATE_COLUMN = "last_update";

	public static final String MEM_USAGE_COLUMN = "mem_usage";

	public static final String PASSWORD_COLUMN = "password";

	public static final String PORT_COLUMN = "port";

	public static final String REPO_URI_PROP_KEY = "repo-uri";

	public static final String TABLE_NAME = "tig_cluster_nodes";
}


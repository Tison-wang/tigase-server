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
package tigase.server.bosh;

import java.util.logging.Level;

/**
 * Describe class Constants here.
 * <br>
 * Created: Tue Jun  5 22:22:09 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class Constants {

	public static final String BOSH_VERSION = "1.6";

	protected static final String ACCEPT_ATTR = "accept";

	protected static final String ACK_ATTR = "ack";

	protected static final String AUTHID_ATTR = "authid";

	protected static final String BATCH_QUEUE_TIMEOUT_KEY = "batch-queue-timeout";

	protected static final long BATCH_QUEUE_TIMEOUT_VAL = 100;

	protected static final String BODY_EL_NAME = "body";

	protected static final String[] BODY_EL_PATH = {"body"};

	protected static final long BOSH_SESSION_CLOSE_DELAY_DEF_VAL = 0;

	protected static final String BOSH_SESSION_CLOSE_DELAY_PROP_KEY = "bosh-session-close-delay";

	protected static final String BOSH_XMLNS = "http://jabber.org/protocol/httpbind";

	protected static final String CACHE_ATTR = "cache";

	protected static final String CACHE_ID_ATTR = "cache-id";

	protected static final String CHARSETS_ATTR = "charsets";

	protected static final String CONCURRENT_REQUESTS_PROP_KEY = "concurrent-requests";

	protected static final int CONCURRENT_REQUESTS_PROP_VAL = 2;

	protected static final String CONTENT_ATTR = "content";

	protected static final String CONTENT_TYPE_DEF = "text/xml; charset=utf-8";

	protected static final String FROM_ATTR = "from";

	protected static final String HOLD_ATTR = "hold";

	protected static final String HOLD_REQUESTS_PROP_KEY = "hold-requests";

	protected static final int HOLD_REQUESTS_PROP_VAL = 1;

	/**
	 * Name of custom attribute to pass name of host to which BOSH connection is established
	 */
	protected static final String HOST_ATTR = "host";

	protected static final String INACTIVITY_ATTR = "inactivity";

	protected static final String LANG_ATTR = "xml:lang";

	protected static final String PRE_BIND_ATTR = "prebind";
	protected static final String SESSION_ID_ATTR = "session-id";
	protected static final String USER_ID_ATTR = "jid";

	protected static final String MAX_BATCH_SIZE_KEY = "max-batch-size";

	protected static final String MAX_INACTIVITY_PROP_KEY = "max-inactivity";

	protected static final long MAX_INACTIVITY_PROP_VAL = 10;

	protected static final int MAX_PACKETS = 15;

	protected static final String MAX_PAUSE_PROP_KEY = "max-inactivity";

	protected static final long MAX_PAUSE_PROP_VAL = 10;

	protected static final String MAX_SESSION_WAITING_PACKETS_KEY = "max-session-waiting-packets";

	protected static final int MAX_SESSION_WAITING_PACKETS_VAL = 100;

	protected static final String MAX_WAIT_DEF_PROP_KEY = "max-wait";

	protected static final long MAX_WAIT_DEF_PROP_VAL = 30;

	protected static final String MAXPAUSE_ATTR = "maxpause";

	protected static final String MIN_POLLING_PROP_KEY = "min-polling";

	protected static final long MIN_POLLING_PROP_VAL = 10;

	protected static final String POLLING_ATTR = "polling";

	protected static final String REQUESTS_ATTR = "requests";

	protected static final String RESTART_ATTR = "xmpp:restart";

	protected static final String RID_ATTR = "rid";

	protected static final String ROUTE_ATTR = "route";

	protected static final String SECURE_ATTR = "secure";

	protected static final String SEND_NODE_HOSTNAME_KEY = "send-node-hostname";

	protected static final boolean SEND_NODE_HOSTNAME_VAL = true;

	protected static final String SID_ATTR = "sid";

	protected static final String TO_ATTR = "to";

	protected static final String VER_ATTR = "ver";

	protected static final String WAIT_ATTR = "wait";

	protected static final String XMLNS_CLIENT_VAL = "jabber:client";

	protected static final int MAX_BATCH_SIZE_VAL = MAX_PACKETS;

	protected static final String SID_LOGGER_KEY = "sid-logger-level";
	protected static final String SID_LOGGER_VAL = Level.OFF.toString();

	protected enum CacheAction {
		on,
		off,
		set,
		add,
		get,
		get_all,
		remove;
	}

//protected static final String CACHE_ON = "on";
//protected static final String CACHE_OFF = "off";
//protected static final String CACHE_SET = "set";
//protected static final String CACHE_ADD = "add";
//protected static final String CACHE_GET = "get";
//protected static final String CACHE_GET_ALL = "get-all";
//protected static final String CACHE_REMOVE = "remove";
}


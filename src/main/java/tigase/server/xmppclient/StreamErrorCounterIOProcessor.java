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
package tigase.server.xmppclient;

import tigase.kernel.beans.Bean;
import tigase.server.Packet;
import tigase.stats.CounterValue;
import tigase.stats.StatisticsList;
import tigase.xml.Element;
import tigase.xmpp.StreamError;
import tigase.xmpp.XMPPIOService;

import java.io.IOException;
import java.util.logging.Level;

/**
 * @author andrzej
 */
@Bean(name = StreamErrorCounterIOProcessor.ID, parent = ClientConnectionManager.class, active = false)
public class StreamErrorCounterIOProcessor
		implements XMPPIOProcessor {

	public static final String ID = "stream-error-counter";

	private String compName;
	private ErrorStatisticsHolder holder = new ErrorStatisticsHolder();

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void getStatistics(StatisticsList list) {
		holder.getStatistics(compName, list);
	}

	@Override
	public Element[] supStreamFeatures(XMPPIOService service) {
		return null;
	}

	@Override
	public boolean processIncoming(XMPPIOService service, Packet packet) {
		return false;
	}

	@Override
	public boolean processOutgoing(XMPPIOService service, Packet packet) {
		return false;
	}

	@Override
	public void packetsSent(XMPPIOService service) throws IOException {
	}

	@Override
	public void processCommand(XMPPIOService service, Packet packet) {
	}

	@Override
	public boolean serviceStopped(XMPPIOService service, boolean streamClosed) {
		return false;
	}

	@Override
	public void streamError(XMPPIOService service, StreamError streamError) {
		if (streamError == null) {
			streamError = StreamError.UndefinedCondition;
		}

		holder.count(streamError);
	}

	public static class ErrorStatisticsHolder {

		private static final String[] ERROR_NAMES;

		static {
			int count = StreamError.values().length;
			ERROR_NAMES = new String[count];
			StreamError[] vals = StreamError.values();
			for (int i = 0; i < vals.length; i++) {
				String name = vals[i].getCondition();
				StringBuilder sb = new StringBuilder();
				for (String part : name.split("-")) {
					sb.append(Character.toUpperCase(part.charAt(0)));
					sb.append(part.substring(1));
				}
				ERROR_NAMES[i] = sb.toString();
			}
		}

		private final CounterValue[] counters;

		public static String[] getErrorNames() {
			return ERROR_NAMES;
		}

		public ErrorStatisticsHolder() {
			counters = new CounterValue[ERROR_NAMES.length];
			for (int i = 0; i < counters.length; i++) {
				counters[i] = new CounterValue(ERROR_NAMES[i], Level.FINER);
			}
		}

		public void count(StreamError val) {
			counters[val.ordinal()].inc();
		}

		public void getStatistics(String compName, StatisticsList list) {
			for (CounterValue c : counters) {
				list.add(compName, "StreamErrorStats/" + c.getName() + "ErrorsNumber", c.getValue(), c.getLevel());
			}
		}
	}
}

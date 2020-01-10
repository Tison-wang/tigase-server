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

import tigase.kernel.beans.Bean;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.beans.selector.ClusterModeRequired;
import tigase.kernel.beans.selector.ConfigType;
import tigase.kernel.beans.selector.ConfigTypeEnum;
import tigase.kernel.core.Kernel;
import tigase.server.Command;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.server.ReceiverTimeoutHandler;
import tigase.server.xmppclient.ClientConnectionManager;
import tigase.server.xmppclient.SeeOtherHostIfc.Phase;
import tigase.stats.StatisticsList;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.*;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import javax.script.Bindings;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;

import static tigase.server.bosh.Constants.*;

/**
 * Describe class BoshConnectionManager here.
 * <br>
 * Created: Sat Jun 2 12:24:29 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
@Bean(name = "bosh", parent = Kernel.class, active = true)
@ConfigType({ConfigTypeEnum.DefaultMode, ConfigTypeEnum.ConnectionManagersMode})
@ClusterModeRequired(active = false)
public class BoshConnectionManager
		extends ClientConnectionManager
		implements BoshSessionTaskHandler, BoshIOService.ConfigProvider {

	public static final String BOSH_CLOSE_CONNECTION_PROP_KEY = "bosh-close-connection";
	public static final String BOSH_EXTRA_HEADERS_FILE_PROP_KEY = "bosh-extra-headers-file";
	public static final String BOSH_EXTRA_HEADERS_FILE_PROP_VAL = "etc/bosh-extra-headers.txt";
	public static final String CLIENT_ACCESS_POLICY_FILE_PROP_KEY = "client-access-policy-file";
	public static final String CLIENT_ACCESS_POLICY_FILE_PROP_VAL = "etc/client-access-policy.xml";
	private static final Logger log = Logger.getLogger(BoshConnectionManager.class.getName());
	private static final int DEF_PORT_NO = 5280;

	private static Handler sidFilehandler;
	protected final Map<UUID, BoshSession> sessions = new ConcurrentSkipListMap<UUID, BoshSession>();
	private int[] PORTS = {DEF_PORT_NO};
	@ConfigField(desc = "Batch queue timeout", alias = BATCH_QUEUE_TIMEOUT_KEY)
	private long batch_queue_timeout = BATCH_QUEUE_TIMEOUT_VAL;
	@ConfigField(desc = "Delay before closing BOSH session", alias = BOSH_SESSION_CLOSE_DELAY_PROP_KEY)
	private long bosh_session_close_delay = BOSH_SESSION_CLOSE_DELAY_DEF_VAL;
	private String clientAccessPolicy = null;
	@ConfigField(desc = "Client access policy file", alias = CLIENT_ACCESS_POLICY_FILE_PROP_KEY)
	private String clientAccessPolicyFile = CLIENT_ACCESS_POLICY_FILE_PROP_VAL;
	@ConfigField(desc = "Close BOSH connections", alias = BOSH_CLOSE_CONNECTION_PROP_KEY)
	private boolean closeConnections = false;
	@ConfigField(desc = "Maximal number of concurrent quests", alias = CONCURRENT_REQUESTS_PROP_KEY)
	private int concurrent_requests = CONCURRENT_REQUESTS_PROP_VAL;
	private String extraHeaders = null;
	@ConfigField(desc = "Extra headers file", alias = BOSH_EXTRA_HEADERS_FILE_PROP_KEY)
	private String extraHeadersFile = BOSH_EXTRA_HEADERS_FILE_PROP_VAL;
	@ConfigField(desc = "Maximal number of hold requests", alias = HOLD_REQUESTS_PROP_KEY)
	private int hold_requests = HOLD_REQUESTS_PROP_VAL;
	@ConfigField(desc = "Limit of number of packets waiting to send for a single session", alias = MAX_SESSION_WAITING_PACKETS_KEY)
	private int maxSessionWaitingPackets = MAX_SESSION_WAITING_PACKETS_VAL;
	@ConfigField(desc = "Maximal size of batch", alias = MAX_BATCH_SIZE_KEY)
	private int max_batch_size = MAX_BATCH_SIZE_VAL;
	@ConfigField(desc = "Maximal allowed time of inactivity", alias = MAX_INACTIVITY_PROP_KEY)
	private long max_inactivity = MAX_INACTIVITY_PROP_VAL;
	@ConfigField(desc = "Maximal allowed pause time", alias = MAX_PAUSE_PROP_KEY)
	private long max_pause = MAX_PAUSE_PROP_VAL;
	@ConfigField(desc = "Maximal allowed wait time", alias = MAX_WAIT_DEF_PROP_KEY)
	private long max_wait = MAX_WAIT_DEF_PROP_VAL;
	@ConfigField(desc = "Minimal allowed polling time", alias = MIN_POLLING_PROP_KEY)
	private long min_polling = MIN_POLLING_PROP_VAL;
	@ConfigField(desc = "Should send node hostname a BOSH element attribute", alias = SEND_NODE_HOSTNAME_KEY)
	private boolean sendNodeHostname = SEND_NODE_HOSTNAME_VAL;
	@ConfigField(desc = "SID logger level", alias = SID_LOGGER_KEY)
	private String sidLoggerLevel = SID_LOGGER_VAL;
	private ReceiverTimeoutHandler startedHandler = newStartedHandler();

	;
	private ReceiverTimeoutHandler stoppedHandler = newStoppedHandler();

	// This should be actually a multi-thread save variable.
	// Changing it to

	protected static void setupSidlogger(Level lvl) {
		if (!Level.OFF.equals(lvl)) {
			Filter bslf = new BoshSidLoggerFilter();

			Logger BoshConnectionManagerLogger = Logger.getLogger(BoshConnectionManager.class.getName());
			Logger BoshSessionLogger = Logger.getLogger(BoshSession.class.getName());

			if (BoshConnectionManagerLogger.getLevel() == null || BoshSessionLogger.getLevel() == null ||
					BoshConnectionManagerLogger.getLevel().intValue() < lvl.intValue()) {
				BoshConnectionManagerLogger.setLevel(lvl);
				BoshConnectionManagerLogger.setFilter(bslf);
				BoshSessionLogger.setLevel(lvl);
				BoshSessionLogger.setFilter(bslf);

				BoshConnectionManagerLogger.getParent().setFilter(bslf);
			}

			try {
				if (null == sidFilehandler) {
					sidFilehandler = new FileHandler("logs/bosh_sid.log", 10000000, 5, false);
					sidFilehandler.setLevel(lvl);
					sidFilehandler.setFilter(bslf);
					BoshConnectionManagerLogger.getParent().addHandler(sidFilehandler);
				}
			} catch (IOException ex) {
				log.log(Level.CONFIG, "Error creating BOSH SID logger" + ex);
			}
		}
	}

	@Override
	public boolean addOutStreamClosed(Packet packet, BoshSession bs, boolean withTimeout) {
		packet.setPacketFrom(getFromAddress(bs.getSid().toString()));
		packet.setPacketTo(bs.getDataReceiver());
		packet.initVars(packet.getPacketFrom(), packet.getPacketTo());
		bs.close();
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "{0} : {1} ({2})",
					new Object[]{BOSH_OPERATION_TYPE.REMOVE, bs.getSid(), "Closing bosh session"});
		}

		sessions.remove(bs.getSid());

		if (withTimeout) {
			return addOutPacketWithTimeout(packet, stoppedHandler, 15l, TimeUnit.SECONDS);
		} else {
			return addOutPacket(packet);
		}
	}

	@Override
	public boolean addOutStreamOpen(Packet packet, BoshSession bs) {
		packet.initVars(getFromAddress(bs.getSid().toString()), bs.getDataReceiver());

		return addOutPacketWithTimeout(packet, startedHandler, 15l, TimeUnit.SECONDS);
	}

	@Override
	public void cancelSendQueueTask(BoshSendQueueTask tt) {
		tt.cancel();
	}

	@Override
	public void cancelTask(BoshTask tt) {
		tt.cancel();
	}

	@Override
	public void processPacket(final Packet packet) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Processing packet: {0}", packet.toString());
		}
		super.processPacket(packet);
	}

	@Override
	public Queue<Packet> processSocketData(XMPPIOService<Object> srv) {
		BoshIOService serv = (BoshIOService) srv;
		Packet p = null;

		while ((p = serv.getReceivedPackets().poll()) != null) {
			Queue<Packet> out_results = new ArrayDeque<Packet>(2);
			BoshSession bs = null;
			String sid_str = null;

			synchronized (sessions) {
				if (log.isLoggable(Level.FINER)) {
					log.log(Level.FINER, "Processing packet: {0}, type: {1}",
							new Object[]{p.getElemName(), p.getType()});
				}
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Processing socket data: {0}", p);
				}
				sid_str = p.getAttributeStaticStr(SID_ATTR);

				UUID sid = null;

				if (sid_str == null) {
					String hostname = p.getAttributeStaticStr(Packet.TO_ATT);

					if ((hostname != null) && isLocalDomain(hostname)) {
						if (!isAllowed(srv, hostname)) {
							if (log.isLoggable(Level.FINE)) {
								log.log(Level.FINE, "Policy violation. Closing connection: {0}", p);
							}
							try {
								serv.sendErrorAndStop(Authorization.NOT_ALLOWED, StreamError.PolicyViolation, p,
													  "Policy violation.");
							} catch (IOException e) {
								log.log(Level.WARNING, "Problem sending invalid hostname error for sid =  " + sid, e);
							}
						} else {
							bs = new BoshSession(getDefVHostItem().getDomain(),
												 JID.jidInstanceNS(routings.computeRouting(hostname)), this,
												 sendNodeHostname ? getDefHostName().getDomain() : null,
												 maxSessionWaitingPackets);
							sid = bs.getSid();
							sessions.put(sid, bs);

							if (log.isLoggable(Level.FINE)) {
								log.log(Level.FINE, "{0} : {1} ({2})",
										new Object[]{BOSH_OPERATION_TYPE.CREATE, sid, "Socket bosh session"});
							}
						}
					} else {
						try {
							serv.sendErrorAndStop(Authorization.NOT_ALLOWED, hostname == null
																			 ? StreamError.ImproperAddressing
																			 : StreamError.HostUnknown, p,
												  "Invalid hostname.");
						} catch (IOException e) {
							log.log(Level.WARNING, "Problem sending invalid hostname error for sid =  " + sid, e);
						}
					}
				} else {
					try {
						sid = UUID.fromString(sid_str);
						bs = sessions.get(sid);
					} catch (IllegalArgumentException e) {
						log.log(Level.WARNING, "Problem processing socket data, sid =  " + sid_str +
								" does not conform to the UUID string representation.", e);
					}
				}
			}
			try {
				if (bs != null) {
					synchronized (bs) {
						if (sid_str == null) {
							bs.init(p, serv, max_wait, min_polling, max_inactivity, concurrent_requests, hold_requests,
									max_pause, max_batch_size, batch_queue_timeout, out_results);
						} else {
							bs.processSocketPacket(p, serv, out_results);
						}
					}
				} else {
					if (log.isLoggable(Level.FINE)) {
						log.log(Level.FINE, "{0} : {1} ({2})",
								new Object[]{BOSH_OPERATION_TYPE.INVALID_SID, sid_str, "Invalid SID"});
					}
					serv.sendErrorAndStop(Authorization.ITEM_NOT_FOUND, null, p, "Invalid SID");
				}
				addOutPackets(out_results, bs);
			} catch (IOException e) {
				log.log(Level.WARNING, "Problem processing socket data for sid =  " + sid_str, e);
			}

			// addOutPackets(out_results);
		}    // end of while ()

		return null;
	}

	@Override
	public BoshSendQueueTask scheduleSendQueueTask(final BoshSession bs, long delay) {
		BoshSendQueueTask bt = new BoshSendQueueTask(bs);

		addTimerTask(bt, delay);

		// boshTasks.schedule(bt, delay);
		return bt;
	}

	@Override
	public BoshTask scheduleTask(BoshSession bs, long delay) {
		BoshTask bt = new BoshTask(bs, this);

		addTimerTask(bt, delay);

		// boshTasks.schedule(bt, delay);
		return bt;
	}

	@Override
	public void serviceStarted(XMPPIOService<Object> service) {
		super.serviceStarted(service);
	}

	@Override
	public boolean serviceStopped(XMPPIOService<Object> xmppService) {
		BoshIOService service = (BoshIOService) xmppService;
		boolean result = super.serviceStopped(service);

		UUID sid = service.getSid();

		if (sid != null) {
			BoshSession bs = sessions.get(sid);

			if (bs != null) {
				if (log.isLoggable(Level.FINE)) {
					log.log(Level.FINE, "{0} : {1} ({2})",
							new Object[]{BOSH_OPERATION_TYPE.REMOVE, bs.getSid(), "Closing bosh session"});
				}

				bs.disconnected(service);
			}
		}
		return result;
	}

	@Override
	public void writeRawData(BoshIOService ios, String data) {
		super.writeRawData(ios, data);
	}

	@Override
	public void xmppStreamClosed(XMPPIOService<Object> serv) {
		if (log.isLoggable(Level.FINER)) {
			log.finer("Stream closed.");
		}
	}

	@Override
	public String[] xmppStreamOpened(XMPPIOService<Object> serv, Map<String, String> attribs) {
		if (log.isLoggable(Level.FINE)) {
			log.fine("Ups, what just happened? Stream open. Hey, this is a Bosh connection manager." +
							 " c2s and s2s are not supported on the same port as Bosh yet.");
		}

		return new String[] { "<?xml version='1.0'?><stream:stream" + " xmlns='jabber:client'" +
				" xmlns:stream='http://etherx.jabber.org/streams'" + " id='1'" + " from='" + getDefVHostItem() + "'" +
				" version='1.0' xml:lang='en'>" + "<stream:error>" +
				"<invalid-namespace xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>" +
				"<text xmlns='urn:ietf:params:xml:ns:xmpp-streams' xml:lang='langcode'>" +
				"Ups, what just happened? Stream open. Hey, this is a Bosh connection manager. " +
				"c2s and s2s are not supported on the same port... yet." + "</text>" + "</stream:error>" +
				"</stream:stream>" };
	}

	@Override
	public String getDiscoCategoryType() {
		return "c2s";
	}

	@Override
	public String getDiscoDescription() {
		return "Bosh connection manager";
	}

	/**
	 * Returns full jid of passed BoshSession instance
	 *
	 * @param bs {@link BoshSession} for which JID should be retrieved
	 *
	 * @return JID address related to particular {@link BoshSession}
	 */
	@Override
	public JID getJidForBoshSession(BoshSession bs) {
		return getFromAddress(bs.getSid().toString());
	}

	@Override
	public Element getSeeOtherHostError(Packet packet, BareJID destination) {

		XMPPIOService<Object> xmppioService = getXMPPIOService(packet);

		Integer redirect_port = (Integer) xmppioService.getSessionData().get(FORCE_REDIRECT_TO_KEY);

		return see_other_host_strategy.getStreamError("urn:ietf:params:xml:ns:xmpp-streams", destination,
													  redirect_port);

	}

	@Override
	public BareJID getSeeOtherHostForJID(Packet packet, BareJID fromJID, Phase ph) {
		if (see_other_host_strategy == null) {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("no see-other-host implementation set");
			}

			return null;
		}
		if (!see_other_host_strategy.isEnabled(vHostManager.getVHostItem(fromJID.getDomain()), ph)) {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("see-other-host not enabled for the Phase: " + ph.toString());
			}

			return null;
		}

		BareJID see_other_host = see_other_host_strategy.findHostForJID(fromJID, getDefHostName());

		if (log.isLoggable(Level.FINEST)) {
			log.finest("using = " + see_other_host_strategy.getClass().getCanonicalName() + "for jid = " +
							   fromJID.toString() + " got = " +
							   ((see_other_host != null) ? see_other_host.toString() : "null") + " in phase: " +
							   ph.toString());
		}

		XMPPIOService<Object> xmppioService = getXMPPIOService(packet);
		Integer redirect_port = xmppioService != null
								? (Integer) xmppioService.getSessionData().getOrDefault(FORCE_REDIRECT_TO_KEY, -1)
								: -1;

		return ((see_other_host != null) &&
				(redirect_port > 0 || see_other_host_strategy.isRedirectionRequired(getDefHostName(), see_other_host)))
			   ? see_other_host
			   : null;
	}

	@Override
	public void getStatistics(StatisticsList list) {
		super.getStatistics(list);
		if (list.checkLevel(Level.FINEST)) {

			// Be careful here, the size() for this map is expensive to count
			list.add(getName(), "Bosh sessions", sessions.size(), Level.FINEST);
		}
	}

	public void setSidLoggerLevel(String loggerLevel) {
		Level level = Level.OFF;
		if (loggerLevel != null) {
			level = Level.parse(loggerLevel);
		}
		this.sidLoggerLevel = level.getName();
		setupSidlogger(level);
	}

	@Override
	public void initBindings(Bindings binds) {
		super.initBindings(binds);
		binds.put("boshCM", this);
	}

	@Override
	public void initialize() {
		if (extraHeaders == null && extraHeadersFile != null) {
			setExtraHeadersFile(extraHeadersFile);
		}
		if (clientAccessPolicy == null && clientAccessPolicyFile != null) {
			setClientAccessPolicyFile(clientAccessPolicyFile);
		}
		super.initialize();
	}

	@Override
	public boolean isCloseConnections() {
		return closeConnections;
	}

	@Override
	public String getClientAccessPolicy() {
		return clientAccessPolicy;
	}

	public void setClientAccessPolicyFile(String clientAccessPolicyFile) {
		this.clientAccessPolicyFile = clientAccessPolicyFile;
		try {
			BufferedReader br = new BufferedReader(new FileReader(clientAccessPolicyFile));
			String line = br.readLine();
			StringBuilder sb = new StringBuilder();

			while (line != null) {
				sb.append(line).append(BoshIOService.EOL);
				line = br.readLine();
			}
			br.close();
			clientAccessPolicy = sb.toString();
		} catch (Exception ex) {
			log.log(Level.WARNING, "Problem reading client access policy file: " + clientAccessPolicyFile, ex);
		}
	}

	@Override
	public String getExtraHeaders() {
		return extraHeaders;
	}

	public void setExtraHeadersFile(String extraHeadersFile) {
		this.extraHeadersFile = extraHeadersFile;
		try {
			BufferedReader br = new BufferedReader(new FileReader(extraHeadersFile));
			String line = br.readLine();
			StringBuilder sb = new StringBuilder();

			while (line != null) {
				sb.append(line).append(BoshIOService.EOL);
				line = br.readLine();
			}
			br.close();
			extraHeaders = sb.toString();
		} catch (Exception ex) {
			log.log(Level.WARNING, "Problem reading Bosh extra headers file: " + extraHeadersFile, ex);
		}
	}

	@Override
	protected void setupWatchdogThread() {
		// having watchdog for bosh connections is not needed
	}

	protected Map<String, String> preBindSession(Map<String, String> attr) {
		String hostname = attr.get(TO_ATTR);

		Queue<Packet> out_results = new ArrayDeque<Packet>(2);

		BoshSession bs = new BoshSession(getDefVHostItem().getDomain(),
										 JID.jidInstanceNS(routings.computeRouting(hostname)), this,
										 sendNodeHostname ? getDefHostName().getDomain() : null,
										 maxSessionWaitingPackets);

		String jid = attr.get(FROM_ATTR);
		String uuid = UUID.randomUUID().toString();
		JID userId = JID.jidInstanceNS(jid);
		if (null == userId.getResource()) {
			userId = userId.copyWithResourceNS(uuid);
			attr.put(FROM_ATTR, userId.toString());
			bs.setUserJid(jid);
		}
		long rid = (long) (Math.random() * 10000000);

		attr.put(RID_ATTR, Long.toString(rid));

		UUID sid = bs.getSid();
		sessions.put(sid, bs);
		if (log.isLoggable(Level.FINE)) {
			log.log(Level.FINE, "{0} : {1} ({2})", new Object[]{BOSH_OPERATION_TYPE.CREATE, bs.getSid(), "Pre-bind"});
		}

		attr.put(SID_ATTR, sid.toString());

		Packet p = null;
		try {
			Element el = new Element("body");
			el.setAttributes(attr);
			p = Packet.packetInstance(el);
			p.setPacketTo(getComponentId().copyWithResource(sid.toString()));
		} catch (TigaseStringprepException ex) {
			Logger.getLogger(BoshConnectionManager.class.getName()).log(Level.SEVERE, null, ex);
		}
		bs.init(p, null, max_wait, min_polling, max_inactivity, concurrent_requests, hold_requests, max_pause,
				max_batch_size, batch_queue_timeout, out_results, true);
		addOutPackets(out_results, bs);

		attr.put("hostname", getDefHostName().toString());

		return attr;
	}

	/**
	 * Method adds packets to the output queue stamping it with the appropriate {@link BoshSession} data
	 *
	 * @param out_results collection of {@link Packet} objects to be added to queue
	 * @param bs related {@link BoshSession}
	 */
	protected void addOutPackets(Queue<Packet> out_results, BoshSession bs) {
		for (Packet res : out_results) {
			res.setPacketFrom(getFromAddress(bs.getSid().toString()));
			res.setPacketTo(bs.getDataReceiver());
			if (res.getCommand() != null) {
				switch (res.getCommand()) {
					case STREAM_CLOSED:
					case GETFEATURES:
						res.initVars(res.getPacketFrom(), res.getPacketTo());

						break;

					default:

						// Do nothing...
				}
			}
			addOutPacket(res);
		}
		out_results.clear();
	}

	@Override
	protected JID changeDataReceiver(Packet packet, JID newAddress, String command_sessionId,
									 XMPPIOService<Object> serv) {
		BoshSession session = getBoshSession(packet.getTo());

		if (session != null) {
			String sessionId = session.getSessionId();

			if (sessionId.equals(command_sessionId)) {
				JID old_receiver = session.getDataReceiver();

				session.setDataReceiver(newAddress);

				return old_receiver;
			} else {
				log.info("Incorrect session ID, ignoring data redirect for: " + newAddress);
			}
		}

		return null;
	}

	@Override
	protected ReceiverTimeoutHandler newStartedHandler() {
		return new StartedHandler();
	}

	@Override
	protected void processCommand(Packet packet) {
		BoshSession session = getBoshSession(packet.getTo());

		switch (packet.getCommand()) {
			case USER_LOGIN:
				String jid = Command.getFieldValue(packet, "user-jid");

				if (jid != null) {
					if (session != null) {
						try {
							BareJID fromJID = BareJID.bareJIDInstance(jid);
							BareJID hostJid = getSeeOtherHostForJID(packet, fromJID, Phase.LOGIN);

							if (hostJid != null) {

								XMPPIOService<Object> xmppioService = getXMPPIOService(packet);
								Integer port = (Integer) xmppioService.getSessionData().get(FORCE_REDIRECT_TO_KEY);

								Element streamErrorElement = see_other_host_strategy.getStreamError(
										"urn:ietf:params:xml:ns:xmpp-streams", hostJid, port);
								Packet redirectPacket = Packet.packetInstance(streamErrorElement);

								redirectPacket.setPacketTo(packet.getTo());
								writePacketToSocket(redirectPacket);
								session.sendWaitingPackets();
								session.close();
								if (log.isLoggable(Level.FINE)) {
									log.log(Level.FINE, "{0} : {1} ({2})",
											new Object[]{BOSH_OPERATION_TYPE.REMOVE, session.getSid(),
														 "See other host"});
								}
								sessions.remove(session.getSid());
							} else {
								session.setUserJid(jid);
							}
						} catch (TigaseStringprepException ex) {
							log.log(Level.SEVERE, "user JID violates RFC6122 (XMPP:Address Format): ", ex);
						}
					} else {
						if (log.isLoggable(Level.FINE)) {
							log.log(Level.FINE, "Missing XMPPIOService for USER_LOGIN command: {0}", packet);
						}
					}
				} else {
					log.log(Level.WARNING, "Missing user-jid for USER_LOGIN command: {0}", packet);
				}

				break;

			case CLOSE:
				if (session != null) {
					if (log.isLoggable(Level.FINER)) {
						log.log(Level.FINER, "Closing session for command CLOSE: {0}", session.getSid());
					}
					try {
						List<Element> err_el = packet.getElement().getChildrenStaticStr(Iq.IQ_COMMAND_PATH);

						if ((err_el != null) && (err_el.size() > 0)) {
							Element error = new Element("stream:error");

							error.addChild(err_el.get(0));

							Packet condition = Packet.packetInstance(error);

							condition.setPacketTo(packet.getTo());
							writePacketToSocket(condition);
							session.sendWaitingPackets();
							bosh_session_close_delay = 100;
						}
					} catch (TigaseStringprepException ex) {
						Logger.getLogger(BoshConnectionManager.class.getName()).log(Level.SEVERE, null, ex);
					}
					if (bosh_session_close_delay > 0) {
						try {
							Thread.sleep(bosh_session_close_delay);
						} catch (InterruptedException ex) {

							// Intentionally left blank
						}
					}
					session.close();
					if (log.isLoggable(Level.FINE)) {
						log.log(Level.FINE, "{0} : {1} ({2})",
								new Object[]{BOSH_OPERATION_TYPE.REMOVE, session.getSid(),
											 "Closing session for command CLOSE"});
					}
					sessions.remove(session.getSid());
				} else {
					if (log.isLoggable(Level.FINE)) {
						log.log(Level.FINE, "Session does not exist for packet: {0}", packet);
					}
				}

				break;

			case CHECK_USER_CONNECTION:
				if (session != null) {

					// It's ok, the session has been found, respond with OK.
					addOutPacket(packet.okResult((String) null, 0));
				} else {

					// Session is no longer active, respond with an error.
					try {
						addOutPacket(
								Authorization.ITEM_NOT_FOUND.getResponseMessage(packet, "Connection gone.", false));
					} catch (PacketErrorTypeException e) {

						// Hm, error already, ignoring...
						log.log(Level.INFO, "Error packet is not really expected here: {0}", packet);
					}
				}

				break;

			default:
				super.processCommand(packet);

				break;
		}    // end of switch (pc.getCommand())
	}

	@Override
	protected boolean writePacketToSocket(Packet packet) {
		BoshSession session = getBoshSession(packet.getTo());

		if (session != null) {
			synchronized (session) {
				Queue<Packet> out_results = new ArrayDeque<Packet>();

				session.processPacket(packet, out_results);
				addOutPackets(out_results, session);
			}

			return true;
		} else {
			log.info("Session does not exist for packet: " + packet.toString());

			return false;
		}
	}

	/**
	 * Method retrieves {@link BoshSession} related to the particular user address
	 *
	 * @param jid address for which {@link BoshSession} should be returned
	 *
	 * @return a value of {@link BoshSession}
	 */
	protected BoshSession getBoshSession(JID jid) {
		String res = jid.getResource();

		if (res != null) {
			UUID sid = UUID.fromString(res);

			return sessions.get(sid);
		}

		return null;
	}

	@Override
	protected int[] getDefPlainPorts() {
		return PORTS;
	}

	@Override
	protected int[] getDefSSLPorts() {
		return null;
	}

	/**
	 * Method <code>getMaxInactiveTime</code> returns max keep-alive time for inactive connection. For Bosh it does not
	 * make sense to keep the idle connection longer than 10 minutes.
	 *
	 * @return a <code>long</code> value
	 */
	@Override
	protected long getMaxInactiveTime() {
		return 10 * MINUTE;

	}

	@Override
	protected BoshIOService getXMPPIOServiceInstance() {
		return new BoshIOService(this);
	}

	// ~--- get methods ----------------------------------------------------------
	private JID getFromAddress(String id) {
		return JID.jidInstanceNS(getName(), getDefHostName().getDomain(), id);
	}

	protected enum BOSH_OPERATION_TYPE {

		CREATE,
		REMOVE,
		INVALID_SID,
		TIMER;

		private static final Map<String, BOSH_OPERATION_TYPE> nameToValueMap = new HashMap<String, BOSH_OPERATION_TYPE>();

		static {
			for (BOSH_OPERATION_TYPE value : EnumSet.allOf(BOSH_OPERATION_TYPE.class)) {
				nameToValueMap.put(value.name(), value);
			}
		}

		public static BOSH_OPERATION_TYPE forName(String name) {
			return nameToValueMap.get(name);
		}

	}

	// ~--- inner classes --------------------------------------------------------
	private class StartedHandler
			implements ReceiverTimeoutHandler {

		@Override
		public void responseReceived(Packet packet, Packet response) {
			String pb = Command.getFieldValue(packet, PRE_BIND_ATTR);
			boolean prebind = Boolean.valueOf(pb);

			String sessionId = Command.getFieldValue(packet, SESSION_ID_ATTR);
			String userID = Command.getFieldValue(packet, USER_ID_ATTR);

			if (prebind) {
				// we are doing pre-bind, send user-login command, bind resource
				Packet packetOut = Command.USER_STATUS.getPacket(packet.getFrom(), packet.getTo(), StanzaType.get,
																 UUID.randomUUID().toString());

//				Element presence = new Element( "presence" );
				Command.addFieldValue(packetOut, USER_ID_ATTR, userID);
				if (null != sessionId) {
					Command.addFieldValue(packetOut, SESSION_ID_ATTR, sessionId);
				}
				Command.addFieldValue(packetOut, PRE_BIND_ATTR, String.valueOf(prebind));

				addOutPacket(packetOut);
			} else {

				// We are now ready to ask for features....
				addOutPacket(Command.GETFEATURES.getPacket(packet.getFrom(), packet.getTo(), StanzaType.get,
														   UUID.randomUUID().toString(), null));
			}
		}

		@Override
		public void timeOutExpired(Packet packet) {

			// If we still haven't received confirmation from the SM then
			// the packet either has been lost or the server is overloaded
			// In either case we disconnect the connection.
			log.warning("No response within time limit received for a packet: " + packet.toString());

			BoshSession session = getBoshSession(packet.getFrom());

			if (session != null) {
				log.fine("Closing session for timeout: " + session.getSid());
				session.close();
				if (log.isLoggable(Level.FINE)) {
					log.log(Level.FINE, "{0} : {1} ({2})",
							new Object[]{BOSH_OPERATION_TYPE.REMOVE, session.getSid(), "Closing session for timeout"});
				}
				sessions.remove(session.getSid());
			} else {
				log.info("Session does not exist for packet: " + packet.toString());
			}
		}
	}
}

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
package tigase.io;

import tigase.util.log.LogFormatter;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import static tigase.io.SSLContextContainerIfc.*;

/**
 * This is sample class demonstrating how to use <code>tigase.io</code> library for TLS/SSL server connection. This is
 * simple telnet server class which can be run to receive plain connections or SSL connections.
 * <br>
 * Created: Sun Aug  6 22:27:13 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class TelnetServer
		implements SampleSocketThread.SocketHandler {

	private static final Logger log = Logger.getLogger("tigase.io.TelnetServer");
	private static final Charset coder = Charset.forName("UTF-8");
	private static boolean continuous = false;
	private static boolean debug = false;
	private static long delay = 100;
	private static String file = null;
	private static String hostname = "localhost";
	private static int port = 7777;
	private static boolean ssl = false;

	private IOInterface iosock = null;
	private SampleSocketThread reader = null;

	public static String help() {
		return "\n" + "Parameters:\n" + " -?                this help message\n" + " -h hostname       host name\n" +
				" -p port           port number\n" + " -ssl              turn SSL on for all connections\n" +
				" -f file           file with content to send to remote host\n" +
				" -c                continuous sending file content\n" +
				" -t millis         delay between sending file content\n" +
				" -v                prints server version info\n" + " -d [true|false]   turn on|off debug mode\n";
	}

	public static void main(final String[] args) throws Exception {
		parseParams(args);

		if (debug) {
			turnDebugOn();
		}    // end of if (debug)

		if (ssl) {
			Map<String, Object> tls_params = new LinkedHashMap<String, Object>();

			tls_params.put(JKS_KEYSTORE_FILE_KEY, "certs/keystore");
			tls_params.put(JKS_KEYSTORE_PWD_KEY, "keystore");
			tls_params.put(TRUSTSTORE_FILE_KEY, "certs/truststore");
			tls_params.put(TRUSTSTORE_PWD_KEY, "truststore");
			TLSUtil.configureSSLContext(tls_params);
		}    // end of if (ssl)

		TelnetServer server = new TelnetServer(port);

		server.run();
	}

	public static void parseParams(final String[] args) throws Exception {
		if ((args != null) && (args.length > 0)) {
			for (int i = 0; i < args.length; i++) {
				if (args[i].equals("-?")) {
					System.out.print(help());
					System.exit(0);
				}      // end of if (args[i].equals("-h"))

				if (args[i].equals("-v")) {
					System.out.print(version());
					System.exit(0);
				}      // end of if (args[i].equals("-h"))

				if (args[i].equals("-f")) {
					if (i + 1 == args.length) {
						System.out.print(help());
						System.exit(1);
					}    // end of if (i+1 == args.length)
					else {
						file = args[++i];
					}    // end of else
				}      // end of if (args[i].equals("-h"))

				if (args[i].equals("-h")) {
					if (i + 1 == args.length) {
						System.out.print(help());
						System.exit(1);
					}    // end of if (i+1 == args.length)
					else {
						hostname = args[++i];
					}    // end of else
				}      // end of if (args[i].equals("-h"))

				if (args[i].equals("-p")) {
					if (i + 1 == args.length) {
						System.out.print(help());
						System.exit(1);
					}    // end of if (i+1 == args.length)
					else {
						port = Integer.decode(args[++i]);
					}    // end of else
				}      // end of if (args[i].equals("-h"))

				if (args[i].equals("-d")) {
					if ((i + 1 == args.length) || args[i + 1].startsWith("-")) {
						debug = true;
					}    // end of if (i+1 == args.length)
					else {
						++i;
						debug = (args[i].charAt(0) != '-') && (args[i].equals("true") || args[i].equals("yes"));
					}    // end of else
				}      // end of if (args[i].equals("-d"))

				if (args[i].equals("-c")) {
					if ((i + 1 == args.length) || args[i + 1].startsWith("-")) {
						continuous = true;
					}    // end of if (i+1 == args.length)
					else {
						++i;
						continuous = (args[i].charAt(0) != '-') && (args[i].equals("true") || args[i].equals("yes"));
					}    // end of else
				}      // end of if (args[i].equals("-c"))

				if (args[i].equals("-ssl")) {
					if ((i + 1 == args.length) || args[i + 1].startsWith("-")) {
						ssl = true;
					}    // end of if (i+1 == args.length)
					else {
						++i;
						ssl = (args[i].charAt(0) != '-') && (args[i].equals("true") || args[i].equals("yes"));
					}    // end of else
				}      // end of if (args[i].equals("-ssl"))
			}        // end of for (int i = 0; i < args.length; i++)
		}
	}

	public static void turnDebugOn() {
		Map<String, String> properties = new HashMap<String, String>();

		properties.put(".level", "ALL");
		properties.put("handlers", "java.util.logging.ConsoleHandler");
		properties.put("java.util.logging.ConsoleHandler.formatter", LogFormatter.class.getName());
		properties.put("java.util.logging.ConsoleHandler.level", "ALL");

		Set<Map.Entry<String, String>> entries = properties.entrySet();
		StringBuilder buff = new StringBuilder();

		for (Map.Entry<String, String> entry : entries) {
			buff.append(entry.getKey() + "=" + entry.getValue() + "\n");
		}

		try {
			final ByteArrayInputStream bis = new ByteArrayInputStream(buff.toString().getBytes());

			LogManager.getLogManager().readConfiguration(bis);
			bis.close();
		} catch (IOException e) {
			log.log(Level.SEVERE, "Can not configure logManager", e);
		}    // end of try-catch
	}

	public static String version() {
		return "\n" + "-- \n" + "Tigase XMPP Telnet, version: " +
				TelnetServer.class.getPackage().getImplementationVersion() + "\n" +
				"Author:  Artur Hefczyc <artur.hefczyc@tigase.org>\n" + "-- \n";
	}

	/**
	 * Creates a new <code>TelnetServer</code> instance.
	 *
	 * @param port
	 *
	 * @throws IOException
	 */
	public TelnetServer(int port) throws IOException {
		reader = new SampleSocketThread(this);
		reader.start();
		reader.addForAccept(new InetSocketAddress(port));
	}

	@Override
	public void handleIOInterface(IOInterface ioifc) throws IOException {
		ByteBuffer socketInput = ByteBuffer.allocate(ioifc.getSocketChannel().socket().getReceiveBufferSize());
		ByteBuffer tmpBuffer = ioifc.read(socketInput);

		if (ioifc.bytesRead() > 0) {
			tmpBuffer.flip();

			CharBuffer cb = coder.decode(tmpBuffer);

			tmpBuffer.clear();

			if (cb != null) {
				System.out.print(new String(cb.array()));
			}    // end of if (cb != null)
		}      // end of if (socketIO.bytesRead() > 0)

		if (!ioifc.isConnected()) {
			throw new EOFException("Channel has been closed.");
		}

		reader.addIOInterface(ioifc);
	}

	@Override
	public void handleSocketAccept(SocketChannel sc) throws IOException {
		iosock = new SocketIO(sc);

		if (ssl) {
			SSLContextContainerIfc sslContextContainer = TLSUtil.getRootSslContextContainer();
			iosock = new TLSIO(iosock,
							   new JcaTLSWrapper(sslContextContainer.getSSLContext("SSL", null, false), null, null, 0,
												 false, false), ByteOrder.BIG_ENDIAN);
		}    // end of if (ssl)

		reader.addIOInterface(iosock);

		if (file != null) {
			FileReader fr = new FileReader(file);
			char[] file_buff = new char[64 * 1024];
			int res = -1;

			while ((res = fr.read(file_buff)) != -1) {
				ByteBuffer dataBuffer = coder.encode(CharBuffer.wrap(file_buff, 0, res));

				iosock.write(dataBuffer);
			}    // end of while ((res = fr.read(buff)) != -1)

			fr.close();
		}      // end of if (file != null)
	}

	public void run() throws IOException {
		InputStreamReader isr = new InputStreamReader(System.in);
		char[] buff = new char[1024];

		for (; ; ) {
			int res = isr.read(buff);

			if (iosock != null) {
				ByteBuffer dataBuffer = coder.encode(CharBuffer.wrap(buff, 0, res));

				iosock.write(dataBuffer);
			}    // end of if (ioscok != null)
			else {
				System.err.println("Can't write to socket, no open socket.");
			}    // end of if (ioscok != null) else
		}      // end of for (;;)
	}
}    // Telnetserver


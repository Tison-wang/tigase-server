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
package tigase.server;

import org.junit.Ignore;
import org.junit.Test;
import tigase.TestLogger;
import tigase.conf.ConfigReader;
import tigase.db.AuthRepositoryMDImpl;
import tigase.db.UserRepositoryMDImpl;
import tigase.kernel.core.Kernel;
import tigase.server.xmppclient.ClientConnectionManager;
import tigase.server.xmppserver.S2SConnectionManager;
import tigase.server.xmppsession.SessionManager;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by andrzej on 07.03.2016.
 */
@Ignore
public class BootstrapTest {

	private static final Logger log = TestLogger.getLogger(BootstrapTest.class);

	private Map<String, Object> props = new HashMap<>();

	@Test
	public void testNonCluster() throws InterruptedException, ConfigReader.ConfigException {
		props.put("cluster-mode", "false");
		Bootstrap bootstrap = executeTest();
		Thread.sleep(10 * 60 * 1000);
		bootstrap.stop();
	}

	@Test
	public void testCluster() throws ConfigReader.ConfigException {
		props.put("cluster-mode", "true");
		Bootstrap bootstrap = executeTest();
		bootstrap.stop();
	}

	public Bootstrap executeTest() throws ConfigReader.ConfigException {
		Bootstrap bootstrap = new Bootstrap();

		bootstrap.setProperties(getProps());

		bootstrap.start();

		Kernel kernel = bootstrap.getKernel();
		assertNotNull(kernel);

		MessageRouter mr = kernel.getInstance("message-router");
		ClientConnectionManager c2s = kernel.getInstance("c2s");
		S2SConnectionManager s2s = kernel.getInstance("s2s");
		UserRepositoryMDImpl userRepository = kernel.getInstance("userRepository");
		AuthRepositoryMDImpl authRepository = kernel.getInstance("authRepository");
		assertNotNull(mr);
		assertNotNull(c2s);
		assertNotNull(s2s);
		assertNotNull(userRepository);
		assertNotNull(userRepository.getRepo(null));
		assertNotNull(authRepository);
		assertNotNull(authRepository.getRepo("default"));

		assertCommandACL(kernel, "ala-ma-kota", new CmdAcl("LOCAL"));
		assertCommandACL(kernel, "ala-ma-kota1", new CmdAcl("test.com"));
		assertCommandACL(kernel, "ala-ma-kota2", new CmdAcl("ala@test.com"));

		return bootstrap;
	}

	public Map<String, Object> getProps() {
		Map<String, Object> props = new HashMap<>(this.props);

		//props.put("userRepository/repo-uri", "jdbc:postgresql://127.0.0.1/tigase?user=test&password=test&autoCreateUser=true");
		props.put("dataSource/repo-uri",
				  "jdbc:postgresql://127.0.0.1/tigase?user=test&password=test&autoCreateUser=true");
		props.put("sess-man/commands/ala-ma-kota", "LOCAL");
		props.put("sess-man/commands/ala-ma-kota1", "test.com");
		props.put("sess-man/commands/ala-ma-kota2", "ala@test.com");
		props.put("c2s/incoming-filters", "tigase.server.filters.PacketCounter,tigase.server.filters.PacketCounter");

		return props;
	}

	private void assertCommandACL(Kernel kernel, String cmdId, CmdAcl expectedAcl) {
		try {
			SessionManager sm = kernel.getInstance(SessionManager.class);
			Field commandsAcl = BasicComponent.class.getDeclaredField("commandsACL");
			commandsAcl.setAccessible(true);
			Map<String, Set<CmdAcl>> val = (Map<String, Set<CmdAcl>>) commandsAcl.get(sm);
			log.log(Level.FINE, "ACL = " + val);
			Set<CmdAcl> acl = val.get(cmdId);
			assertTrue(acl.stream().filter(a -> a.equals(expectedAcl)).findAny().isPresent());
			log.log(Level.FINE, "" + acl.getClass() + ", " + acl);
			System.out.print("cmd " + cmdId + " = " + acl + ", expected = " + expectedAcl);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}

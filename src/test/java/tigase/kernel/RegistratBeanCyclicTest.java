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
package tigase.kernel;

import org.junit.Assert;
import org.junit.Test;
import tigase.TestLogger;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.RegistrarBean;
import tigase.kernel.core.Kernel;
import tigase.kernel.core.PlantUMLGrapher;
import tigase.kernel.core.RegistrarKernel;

import java.util.logging.Level;
import java.util.logging.Logger;

public class RegistratBeanCyclicTest {

	private static final Logger log = TestLogger.getLogger(RegistratBeanCyclicTest.class);

	@Test
	public void test01() {
		Kernel k = new RegistrarKernel();
		k.setName("root");
		k.registerBean(A.class).exec();

		PlantUMLGrapher gr = new PlantUMLGrapher(k);

		A a = k.getInstance(A.class);
		B b = a.b;
		C c = a.b.c;
		Assert.assertSame(a, b.a);
		Assert.assertSame(a, c.a);
		Assert.assertSame(b, ((Kernel) k.getInstance("a#KERNEL")).getInstance("b"));
		Assert.assertSame(c, ((Kernel) ((Kernel) k.getInstance("a#KERNEL")).getInstance("b#KERNEL")).getInstance("c"));

		log.log(Level.FINE, gr.getDependencyGraph());
	}

	@Bean(name = "a", active = true)
	public static class A
			implements RegistrarBean {

		@Inject
		B b;

		public void register(Kernel kernel) {
			kernel.registerBean(B.class).exec();
		}

		@Override
		public void unregister(Kernel kernel) {
		}
	}

	@Bean(name = "b", active = true)
	public static class B
			implements RegistrarBean {

		@Inject
		A a;

		@Inject
		C c;

		public void register(Kernel kernel) {
			kernel.registerBean(C.class).exec();
			kernel.getParent().ln("service", kernel, "a");
		}

		@Override
		public void unregister(Kernel kernel) {
		}
	}

	@Bean(name = "c", active = true)
	public static class C {

		@Inject
		A a;
	}
}



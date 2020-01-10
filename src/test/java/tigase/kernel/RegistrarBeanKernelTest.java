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
import tigase.component.DSLBeanConfigurator;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.RegistrarBean;
import tigase.kernel.core.DependencyGrapher;
import tigase.kernel.core.Kernel;
import tigase.kernel.core.RegistrarKernel;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by andrzej on 05.03.2016.
 */
public class RegistrarBeanKernelTest {

	private static final Logger log = TestLogger.getLogger(KernelTest.class);

	public RegistrarBeanKernelTest() {
	}

	@Test
	public void test01() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		final RegistrarKernel krnl = new RegistrarKernel();
		krnl.setName("root");
		krnl.registerBean(DefaultTypesConverter.class).exec();
		krnl.registerBean(DSLBeanConfigurator.class).exec();
		krnl.registerBean(RegistrarBeanImpl.class).exec();

		//krnl.startSubKernels();

		DependencyGrapher dg = new DependencyGrapher(krnl);
		log.log(Level.FINE, dg.getDependencyGraph());

		Assert.assertNotNull(krnl.getInstance("RegistrarBean"));

		Kernel rb1k = krnl.getInstance("RegistrarBean#KERNEL");
		Assert.assertNotEquals(null, rb1k.getInstance(Bean1.class));
		Assert.assertEquals(krnl.getInstance("RegistrarBean"), rb1k.getInstance(RegistrarBeanImpl.class));
		Assert.assertNotNull(rb1k.getInstance("service"));

		krnl.setBeanActive("RegistrarBean", false);
		krnl.gc();

		dg = new DependencyGrapher(krnl);
		log.log(Level.FINE, dg.getDependencyGraph());

		Assert.assertTrue(krnl.isBeanClassRegistered("RegistrarBean"));
		boolean exception = false;
		try {
			Assert.assertNull(krnl.getInstance("RegistrarBean#KERNEL"));
		} catch (KernelException ex) {
			// unknow bean - this is what we expect
			exception = true;
		}
		Assert.assertTrue(exception);
	}

	@Test
	public void test02() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		final RegistrarKernel krnl = new RegistrarKernel();
		krnl.setName("root");
		krnl.registerBean(DefaultTypesConverter.class).exec();
		krnl.registerBean(DSLBeanConfigurator.class).exec();
		krnl.registerBean(DummyBean.class).exec();
		krnl.registerBean(RegistrarBeanImplWithLink.class).exec();

		//krnl.startSubKernels();

		DependencyGrapher dg = new DependencyGrapher(krnl);
		log.log(Level.FINE, dg.getDependencyGraph());

		Assert.assertNotNull(krnl.getInstance("RegistrarBean"));

		Kernel rb1k = krnl.getInstance("RegistrarBean#KERNEL");
		Assert.assertNotEquals(null, rb1k.getInstance(Bean1.class));
		Assert.assertEquals(krnl.getInstance("RegistrarBean"), rb1k.getInstance(RegistrarBeanImplWithLink.class));
		Assert.assertNotNull(rb1k.getInstance("service"));
		Assert.assertNotNull(rb1k.getInstance("dummy"));
		Assert.assertNotNull(rb1k.getInstance("DummyBeanUser"));
		Assert.assertNotNull(((DummyBeanUser) rb1k.getInstance("DummyBeanUser")).dummyBean);

		krnl.unregister("DummyBean");

		try {
			// maybe it should still be there but in unresolved state?
			Assert.assertNull(rb1k.getDependencyManager().getBeanConfig("dummy"));
			rb1k.getInstance("dummy");
			Assert.assertTrue(false);
		} catch (KernelException ex) {
			// wrong cause of KernelException (caused by NPE), should be thrown due to fact
			// that "dummy" is DelegatedBeanConfig pointing to removed BeanConfig!
			Assert.assertTrue(ex.getMessage().contains("Unknown bean"));
		}

		Assert.assertNull(((DummyBeanUser) rb1k.getInstance("DummyBeanUser")).dummyBean);

		dg = new DependencyGrapher(krnl);
		log.log(Level.FINE, dg.getDependencyGraph());
	}

	@Test
	public void test03() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		final RegistrarKernel krnl = new RegistrarKernel();
		krnl.setName("root");
		krnl.registerBean(DefaultTypesConverter.class).exec();
		krnl.registerBean(DSLBeanConfigurator.class).exec();
		krnl.registerBean(DummyBean2.class).exec();
		krnl.registerBean(RegistrarBeanImplWithLink2.class).exec();

		//krnl.startSubKernels();

		DependencyGrapher dg = new DependencyGrapher(krnl);
		log.log(Level.FINE, dg.getDependencyGraph());

		Assert.assertNotNull(krnl.getInstance("RegistrarBean"));

		Kernel rb1k = krnl.getInstance("RegistrarBean#KERNEL");
		Assert.assertNotEquals(null, rb1k.getInstance(Bean1.class));
		Assert.assertEquals(krnl.getInstance("RegistrarBean"), rb1k.getInstance(RegistrarBeanImplWithLink2.class));
		Assert.assertNotNull(rb1k.getInstance("service"));
		Assert.assertNotNull(rb1k.getInstance(DummyBean2.class));
		Assert.assertNotNull(rb1k.getInstance("DummyBean2User"));
		Assert.assertNotNull(((DummyBean2User) rb1k.getInstance("DummyBean2User")).dummyBean);

		krnl.unregister("DummyBean2");

		try {
			rb1k.getInstance("DummyBean2");
			Assert.assertTrue(false);
		} catch (KernelException ex) {
			Assert.assertTrue(ex.getMessage().contains("Unknown bean"));
		}

		Assert.assertNull(((DummyBean2User) rb1k.getInstance("DummyBean2User")).dummyBean);

		dg = new DependencyGrapher(krnl);
		log.log(Level.FINE, dg.getDependencyGraph());
	}

	@Test
	public void test04() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		final RegistrarKernel krnl = new RegistrarKernel();
		krnl.setName("root");
		krnl.registerBean(DefaultTypesConverter.class).exec();
		krnl.registerBean(DSLBeanConfigurator.class).exec();
		krnl.registerBean(DummyBean3.class).exec();
		krnl.registerBean(RegistrarBeanImplWithLink3.class).exec();

		//krnl.startSubKernels();

		DependencyGrapher dg = new DependencyGrapher(krnl);
		log.log(Level.FINE, dg.getDependencyGraph());

		Assert.assertNotNull(krnl.getInstance("RegistrarBean"));

		Kernel rb1k = krnl.getInstance("RegistrarBean#KERNEL");
		Assert.assertNotEquals(null, rb1k.getInstance(Bean1.class));
		Assert.assertEquals(krnl.getInstance("RegistrarBean"), rb1k.getInstance(RegistrarBeanImplWithLink3.class));
		Assert.assertNotNull(rb1k.getInstance("service"));
		Assert.assertNotNull(rb1k.getInstance("dummy"));
		Assert.assertNotNull(rb1k.getInstance("DummyBean3User"));
		Assert.assertNotNull(rb1k.getInstance("DummyBean34User"));
		Assert.assertNotNull(((DummyBean3User) rb1k.getInstance("DummyBean3User")).dummyBean);
		Assert.assertNotNull(((DummyBean34User) rb1k.getInstance("DummyBean34User")).dummyBean3User);

		krnl.unregister("DummyBean3");

		try {
			// maybe it should still be there but in unresolved state?
			Assert.assertNull(rb1k.getDependencyManager().getBeanConfig("dummy"));
			rb1k.getInstance("dummy");
			Assert.assertTrue(false);
		} catch (KernelException ex) {
			// wrong cause of KernelException (caused by NPE), should be thrown due to fact
			// that "dummy" is DelegatedBeanConfig pointing to removed BeanConfig!
			Assert.assertTrue(ex.getMessage().contains("Unknown bean"));
		}

		try {
			rb1k.getInstance("DummyBean3User");
			Assert.assertTrue(false);
		} catch (KernelException ex) {
			Assert.assertTrue(true);
		}

		Assert.assertNull(((DummyBean34User) rb1k.getInstance("DummyBean34User")).dummyBean3User);

		krnl.registerBean(DummyBean3.class).exec();
		krnl.ln("DummyBean3", rb1k, "dummy");

		Assert.assertNotNull(((DummyBean3User) rb1k.getInstance("DummyBean3User")).dummyBean);
		Assert.assertNotNull(((DummyBean34User) rb1k.getInstance("DummyBean34User")).dummyBean3User);

		dg = new DependencyGrapher(krnl);
		log.log(Level.FINE, dg.getDependencyGraph());
	}

	@Test
	public void test05() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		final RegistrarKernel krnl = new RegistrarKernel();
		krnl.registerBean(DefaultTypesConverter.class).exportable().exec();
		krnl.setName("root");
		krnl.registerBean(DSLBeanConfigurator.class).exec();
		krnl.registerBean(DummyBean4.class).exec();
		krnl.registerBean(RegistrarBeanImplWithLink4.class).exec();

		//krnl.startSubKernels();

		DependencyGrapher dg = new DependencyGrapher(krnl);
		log.log(Level.FINE, dg.getDependencyGraph());

		Assert.assertNotNull(krnl.getInstance("RegistrarBean"));

		Kernel rb1k = krnl.getInstance("RegistrarBean#KERNEL");
		Assert.assertNotEquals(null, rb1k.getInstance(Bean1.class));
		Assert.assertEquals(krnl.getInstance("RegistrarBean"), rb1k.getInstance(RegistrarBeanImplWithLink4.class));
		Assert.assertNotNull(rb1k.getInstance("service"));
		Assert.assertNotNull(rb1k.getInstance(DummyBean4.class));
		Assert.assertNotNull(rb1k.getInstance("DummyBean4User"));
		Assert.assertNotNull(rb1k.getInstance("DummyBean34User"));
		Assert.assertNotNull(((DummyBean4User) rb1k.getInstance("DummyBean4User")).dummyBean);
		Assert.assertNotNull(((DummyBean34User) rb1k.getInstance("DummyBean34User")).dummyBean4User);

		krnl.unregister("DummyBean4");

		try {
			rb1k.getInstance("DummyBean4");
			Assert.assertTrue(false);
		} catch (KernelException ex) {
			Assert.assertTrue(ex.getMessage().contains("Unknown bean"));
		}

		try {
			rb1k.getInstance("DummyBean4User");
			Assert.assertTrue(false);
		} catch (KernelException ex) {
			Assert.assertTrue(true);
		}

		Assert.assertNull(((DummyBean34User) rb1k.getInstance("DummyBean34User")).dummyBean4User);

		krnl.registerBean(DummyBean4.class).exec();

		Assert.assertNotNull(((DummyBean34User) rb1k.getInstance("DummyBean34User")).dummyBean4User);
		Assert.assertNotNull(((DummyBean4User) rb1k.getInstance("DummyBean4User")).dummyBean);

		dg = new DependencyGrapher(krnl);
		log.log(Level.FINE, dg.getDependencyGraph());
	}

	@Bean(name = "DummyBean", active = true)
	public static class DummyBean {

		public DummyBean() {
		}

	}

	@Bean(name = "DummyBean2", active = true, exportable = true)
	public static class DummyBean2 {

		public DummyBean2() {
		}

	}

	@Bean(name = "DummyBean2User", active = true)
	public static class DummyBean2User {

		@Inject(nullAllowed = true)
		public DummyBean2 dummyBean;

		public DummyBean2User() {
		}

	}

	@Bean(name = "DummyBean3", active = true)
	public static class DummyBean3 {

		public DummyBean3() {
		}

	}

	@Bean(name = "DummyBean34User", active = true)
	public static class DummyBean34User {

		@Inject(nullAllowed = true)
		public DummyBean3User dummyBean3User;

		@Inject(nullAllowed = true)
		public DummyBean4User dummyBean4User;

	}

	@Bean(name = "DummyBean3User", active = true)
	public static class DummyBean3User {

		@Inject
		public DummyBean3 dummyBean;

		public DummyBean3User() {
		}

	}

	@Bean(name = "DummyBean4", active = true, exportable = true)
	public static class DummyBean4 {

		public DummyBean4() {
		}

	}

	@Bean(name = "DummyBean4User", active = true)
	public static class DummyBean4User {

		@Inject
		public DummyBean4 dummyBean;

		public DummyBean4User() {
		}

	}

	@Bean(name = "DummyBeanUser", active = true)
	public static class DummyBeanUser {

		@Inject(nullAllowed = true)
		public DummyBean dummyBean;

		public DummyBeanUser() {
		}

	}

	@Bean(name = "RegistrarBean", active = true)
	public static class RegistrarBeanImpl
			implements RegistrarBean {

		public RegistrarBeanImpl() {

		}

		@Override
		public void register(Kernel kernel) {
			kernel.registerBean(Bean1.class).exec();
		}

		@Override
		public void unregister(Kernel kernel) {
			kernel.unregister("bean1");
		}
	}

	@Bean(name = "RegistrarBean", active = true)
	public static class RegistrarBeanImplWithLink
			implements RegistrarBean {

		public RegistrarBeanImplWithLink() {

		}

		@Override
		public void register(Kernel kernel) {
			kernel.getParent().ln("DummyBean", kernel, "dummy");
			kernel.registerBean(DummyBeanUser.class).exec();
			kernel.registerBean(Bean1.class).exec();
		}

		@Override
		public void unregister(Kernel kernel) {
			kernel.unregister("bean1");
		}
	}

	@Bean(name = "RegistrarBean", active = true)
	public static class RegistrarBeanImplWithLink2
			implements RegistrarBean {

		public RegistrarBeanImplWithLink2() {

		}

		@Override
		public void register(Kernel kernel) {
			kernel.registerBean(DummyBean2User.class).exec();
			kernel.registerBean(Bean1.class).exec();
		}

		@Override
		public void unregister(Kernel kernel) {
			kernel.unregister("bean1");
		}
	}

	@Bean(name = "RegistrarBean", active = true)
	public static class RegistrarBeanImplWithLink3
			implements RegistrarBean {

		public RegistrarBeanImplWithLink3() {

		}

		@Override
		public void register(Kernel kernel) {
			kernel.getParent().ln("DummyBean3", kernel, "dummy");
			kernel.registerBean(DummyBean3User.class).exec();
			kernel.registerBean(DummyBean34User.class).exec();
			kernel.registerBean(Bean1.class).exec();
		}

		@Override
		public void unregister(Kernel kernel) {
			kernel.unregister("bean1");
		}
	}

	@Bean(name = "RegistrarBean", active = true)
	public static class RegistrarBeanImplWithLink4
			implements RegistrarBean {

		public RegistrarBeanImplWithLink4() {

		}

		@Override
		public void register(Kernel kernel) {
			kernel.registerBean(DummyBean4User.class).exec();
			kernel.registerBean(DummyBean34User.class).exec();
			kernel.registerBean(Bean1.class).exec();
		}

		@Override
		public void unregister(Kernel kernel) {
			kernel.unregister("bean1");
		}
	}

}

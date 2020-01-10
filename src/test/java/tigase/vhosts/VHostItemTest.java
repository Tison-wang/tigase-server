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
package tigase.vhosts;

import junit.framework.TestCase;
import org.junit.Assert;
import tigase.TestLogger;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.vhosts.filter.DomainFilterPolicy;
import tigase.xml.Element;
import tigase.xmpp.jid.JID;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Wojciech Kapcia <wojciech.kapcia@tigase.org>
 */
public class VHostItemTest
		extends TestCase {

	private static final Logger log = TestLogger.getLogger(VHostItemTest.class);

	public void testVHostItem() throws TigaseStringprepException {
		assertEquals(new VHostItem("lowercase.com"), new VHostItem("lowercase.com"));
		assertEquals(new VHostItem("CAPITAL.COM"), new VHostItem("capital.com"));
		assertNotSame(new VHostItem("CAPITAL.COM"), new VHostItem("lowercase.com"));
	}

	public void testVHostDomainPolicy() throws TigaseStringprepException {

		Element el;
		VHostItem vHostItem;

		vHostItem = new VHostItem();
		vHostItem.initFromPropertyString("domain1:domain-filter=LOCAL:max-users=1000");
		assertEquals(DomainFilterPolicy.LOCAL, vHostItem.getDomainFilter());
		assertTrue(vHostItem.getDomainFilterDomains() == null);

		vHostItem = new VHostItem();
		vHostItem.initFromPropertyString("domain1:domain-filter=LIST=domain1;domain2;domain3:max-users=1000");
		assertEquals(DomainFilterPolicy.LIST, vHostItem.getDomainFilter());
		assertTrue(Arrays.asList(vHostItem.getDomainFilterDomains()).contains("domain1"));
		assertTrue(Arrays.asList(vHostItem.getDomainFilterDomains()).contains("domain3"));
		assertFalse(Arrays.asList(vHostItem.getDomainFilterDomains()).contains("domain5"));

		vHostItem = new VHostItem();
		el = new Element("vhost", new String[]{"hostname", "domain-filter", "domain-filter-domains"},
						 new String[]{"domain3", "ALL", "domain1;domain2;domain3"});

		vHostItem.initFromElement(el);

		assertEquals(DomainFilterPolicy.ALL, vHostItem.getDomainFilter());
		assertTrue(vHostItem.getDomainFilterDomains() == null);
		assertTrue(vHostItem.toPropertyString().contains("domain-filter=ALL"));

		vHostItem = new VHostItem();
		el = new Element("vhost", new String[]{"hostname", "domain-filter", "domain-filter-domains"},
						 new String[]{"domain3", "BLACKLIST", "domain1;domain2;domain3"});

		vHostItem.initFromElement(el);
		assertEquals(DomainFilterPolicy.BLACKLIST, vHostItem.getDomainFilter());
		assertTrue(Arrays.asList(vHostItem.getDomainFilterDomains()).contains("domain1"));
		assertTrue(Arrays.asList(vHostItem.getDomainFilterDomains()).contains("domain3"));
		assertFalse(Arrays.asList(vHostItem.getDomainFilterDomains()).contains("domain5"));
		assertTrue(vHostItem.toPropertyString().contains("domain-filter=BLACKLIST"));

		vHostItem = new VHostItem();
		el = new Element("vhost", new String[]{"hostname", "domain-filter", "domain-filter-domains"},
						 new String[]{"domain3", "CUSTOM",
									  "4,deny,all;1,allow,self;3,allow,jid,pubsub@test.com;2,allow,jid,admin@test2.com"});

		vHostItem.initFromElement(el);
		assertEquals(DomainFilterPolicy.CUSTOM, vHostItem.getDomainFilter());
		assertTrue(vHostItem.toPropertyString()
						   .contains(
								   "domain-filter=CUSTOM=4,deny,all;1,allow,self;3,allow,jid,pubsub@test.com;2,allow,jid,admin@test2.com"));

		vHostItem = new VHostItem();
		vHostItem.initFromPropertyString(
				"domain1:domain-filter=CUSTOM=4|deny|all;1|allow|self;3|allow|jid|pubsub@test.com;2|allow|jid|admin@test2.com");
		assertEquals(DomainFilterPolicy.CUSTOM, vHostItem.getDomainFilter());
		final String toPropertyString = vHostItem.toPropertyString();
		log.log(Level.FINE, "to property string: " + toPropertyString);
		assertTrue("different", toPropertyString.contains(
				"domain-filter=CUSTOM=4|deny|all;1|allow|self;3|allow|jid|pubsub@test.com;2|allow|jid|admin@test2.com"));

	}

	public void testInitFromPropertyString()
			throws TigaseStringprepException, IllegalAccessException, NoSuchFieldException {
		JID jid = JID.jidInstanceNS("comp1@example.com");
		JID notTrusted = JID.jidInstanceNS("not-trusted@example.com");

		VHostItem item = new VHostItem();
		item.toString();
		Assert.assertNull(item.getTrustedJIDs());
		Assert.assertFalse(item.isTrustedJID(jid));

		item.initFromPropertyString("example.com:trusted-jids=comp1@example.com");
		Assert.assertArrayEquals(new String[]{jid.toString()}, item.getTrustedJIDs().toArray(new String[0]));
		Assert.assertTrue(item.isTrustedJID(jid));
		Assert.assertTrue(item.isTrustedJID(jid.copyWithResource("test")));
		Assert.assertFalse(item.isTrustedJID(notTrusted));

		item = new VHostItem();
		item.initFromPropertyString("example.com:trusted-jids=comp1@example.com,comp2@example.com");
		Assert.assertArrayEquals(new String[]{jid.toString(), "comp2@example.com"},
								 item.getTrustedJIDs().toArray(new String[0]));
		Assert.assertTrue(item.isTrustedJID(jid));
		Assert.assertTrue(item.isTrustedJID(jid.copyWithResource("test")));
		Assert.assertFalse(item.isTrustedJID(notTrusted));

		item = new VHostItem();
		item.initFromPropertyString("example.com:trusted-jids=comp1@example.com;comp2@example.com");
		Assert.assertArrayEquals(new String[]{jid.toString(), "comp2@example.com"},
								 item.getTrustedJIDs().toArray(new String[0]));
		Assert.assertTrue(item.isTrustedJID(jid));
		Assert.assertTrue(item.isTrustedJID(jid.copyWithResource("test")));
		Assert.assertFalse(item.isTrustedJID(notTrusted));

		item = new VHostItem();
		item.toString();
		item.initFromPropertyString("example.com:trusted-jids=example.com");
		item.toString();
		Assert.assertArrayEquals(new String[]{"example.com"}, item.getTrustedJIDs().toArray(new String[0]));
		Assert.assertTrue(item.isTrustedJID(jid));
		Assert.assertTrue(item.isTrustedJID(jid.copyWithResource("test")));
		Assert.assertTrue(item.isTrustedJID(notTrusted));

		//System.setProperty("trusted", "comp3@example.com,comp4@example.com");
		VHostItemDefaults defaults = new VHostItemDefaults();
		Field f = VHostItemDefaults.class.getDeclaredField("trusted");
		f.setAccessible(true);
		f.set(defaults, new ConcurrentSkipListSet<String>());
		defaults.getTrusted().add("comp3@example.com");
		defaults.getTrusted().add("comp4@example.com");
		item = new VHostItem();
		item.initializeFromDefaults(defaults);
		item.toString();
		Assert.assertArrayEquals(new String[]{"comp3@example.com", "comp4@example.com"},
								 item.getTrustedJIDs().toArray(new String[0]));
		Assert.assertTrue(item.isTrustedJID(JID.jidInstanceNS("comp3@example.com")));
		Assert.assertTrue(item.isTrustedJID(JID.jidInstanceNS("comp3@example.com").copyWithResource("test")));
		Assert.assertFalse(item.isTrustedJID(notTrusted));

		item.initFromPropertyString("example.com:trusted-jids=comp1@example.com;comp2@example.com");
		Assert.assertArrayEquals(new String[]{jid.toString(), "comp2@example.com"},
								 item.getTrustedJIDs().toArray(new String[0]));
		Assert.assertTrue(item.isTrustedJID(jid));
		Assert.assertTrue(item.isTrustedJID(jid.copyWithResource("test")));
		Assert.assertFalse(item.isTrustedJID(notTrusted));
		Assert.assertFalse(item.isTrustedJID(JID.jidInstanceNS("comp3@example.com")));
	}

}

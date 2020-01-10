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
package tigase.eventbus.impl;

import tigase.kernel.BeanUtils;
import tigase.kernel.DefaultTypesConverter;
import tigase.kernel.TypesConverter;
import tigase.xml.Element;
import tigase.xml.XMLUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EventBusSerializer
		implements Serializer {

	private static final Logger log = Logger.getLogger(EventBusSerializer.class.getName());
	private TypesConverter typesConverter = new DefaultTypesConverter();

	public <T> T deserialize(final Element element) {
		try {
			final Class<?> cls = Class.forName(element.getName());
			final Object result = cls.newInstance();

			Field[] fields = BeanUtils.getAllFields(cls);
			for (final Field f : fields) {
				if (Modifier.isTransient(f.getModifiers())) {
					continue;
				}
				if (Modifier.isFinal(f.getModifiers())) {
					continue;
				}
				if (Modifier.isStatic(f.getModifiers())) {
					continue;
				}

				try {
					Object value;
					Element v = element.getChild(f.getName());
					if (v == null) {
						continue;
					}

					if (Element.class.isAssignableFrom(f.getType())) {
						if (v.getChildren().size() > 0) {
							value = v.getChildren().get(0);
						} else {
							value = null;
						}
					} else {
						value = typesConverter.convert(XMLUtils.unescape(v.getCData()), f.getType(),
													   f.getGenericType());
					}
					BeanUtils.setValue(result, f, value);
				} catch (IllegalAccessException | InvocationTargetException caught) {
					log.log(Level.WARNING, "Error while deserializing", caught);
				}
			}
			return (T) result;
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
			log.log(Level.WARNING, "Error while deserializing", e);
			return null;
		}
	}

	public Element serialize(final Object object) {
		final Class<?> cls = object.getClass();
		Element e = new Element(cls.getName());

		Field[] fields = BeanUtils.getAllFields(cls);
		for (final Field f : fields) {
			if (Modifier.isTransient(f.getModifiers())) {
				continue;
			}
			if (Modifier.isFinal(f.getModifiers())) {
				continue;
			}
			if (Modifier.isStatic(f.getModifiers())) {
				continue;
			}

			try {
				final Object value = BeanUtils.getValue(object, f);

				if (value == null) {
					continue;
				}

				Element v = new Element(f.getName());
				if (Element.class.isAssignableFrom(f.getType())) {
					v.addChild((Element) value);
				} else {
					String x = typesConverter.toString(value);
					v.setCData(XMLUtils.escape(x));
				}
				e.addChild(v);
			} catch (IllegalAccessException | InvocationTargetException caught) {
				log.log(Level.WARNING, "Error while serializing", e);
			}
		}

		return e;
	}

}

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

import tigase.conf.ConfigReader;
import tigase.kernel.beans.Bean;
import tigase.osgi.ModulesManagerImpl;
import tigase.server.CmdAcl;
import tigase.util.Base64;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.XMLUtils;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Pattern;

@Bean(name = "defaultTypesConverter", active = true)
public class DefaultTypesConverter
		implements TypesConverter {

	private static final String[] decoded = {","};
	private static final String[] encoded = {"\\,"};
	private static final String[] decoded_1 = {","};
	private static final String[] encoded_1 = {"\\,"};

	private final static String regex = "(?<!\\\\)" + Pattern.quote(",");

	public static String escape(String input) {
		if (input != null) {
			return XMLUtils.translateAll(input, decoded, encoded);
		} else {
			return null;
		}
	}

	public static String unescape(String input) {
		if (input != null) {
			return XMLUtils.translateAll(input, encoded_1, decoded_1);
		} else {
			return null;
		}
	}

	/**
	 * Converts value to expected type.
	 *
	 * @param value value to be converted.
	 * @param expectedType class of expected type.
	 * @param <T> expected type.
	 *
	 * @return converted value.
	 */
	public <T> T convert(final Object value, final Class<T> expectedType) {
		return convert(value, expectedType, null);
	}

	public <T> T convert(final Object value, final Type type) {
		if (type instanceof Class) {
			return convert(value, (Class<T>) type);
		}
		if (type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) type;
			if (pt.getRawType() instanceof Class) {
				return convert(value, (Class<T>) pt.getRawType(), pt);
			}
		}

		throw new RuntimeException("Cannot convert to " + type);
	}

	public <T> T convert(Object value, final Class<T> expectedType, Type genericType) {
		try {
			if (value == null) {
				return null;
			}

			if ("null".equals(value)) {
				return null;
			}

			if (value instanceof ConfigReader.Variable) {
				value = ((ConfigReader.Variable) value).calculateValue();
			}

			final Class<?> currentType = value.getClass();

			if (expectedType.isAssignableFrom(currentType) && genericType == null) {
				return expectedType.cast(value);
			}

			T customResult = customConversion(value, expectedType, genericType);
			if (customResult != null) {
				return customResult;
			} else if (expectedType.equals(Class.class)) {
				try {
					return expectedType.cast(ModulesManagerImpl.getInstance().forName(value.toString().trim()));
				} catch (ClassNotFoundException e) {
					throw new RuntimeException("Cannot convert to " + expectedType, e);
				}
			} else if (expectedType.equals(File.class)) {
				return expectedType.cast(new File(value.toString().trim()));
			} else if (expectedType.equals(Level.class)) {
				return expectedType.cast(Level.parse(value.toString().trim()));
			} else if (expectedType.isEnum()) {
				String trimmedValue = value.toString().trim();
				final Class<? extends Enum> enumType = (Class<? extends Enum>) expectedType;
				try {
					final Enum<?> theOneAndOnly = Enum.valueOf(enumType, trimmedValue);
					return expectedType.cast(theOneAndOnly);
				} catch (IllegalArgumentException ex) {
					// if there is no value for the exact type let's make case insensitive lookup
					Optional<Enum<?>> theOneAndOnly = EnumSet.allOf(enumType)
							.stream()
							.filter(val -> trimmedValue.equalsIgnoreCase(((Enum) val).name()))
							.findFirst();
					if (theOneAndOnly.isPresent()) {
						return expectedType.cast(theOneAndOnly.get());
					}
					throw new RuntimeException("Cannot convert to " + expectedType, ex);
				}
			} else if (expectedType.equals(JID.class)) {
				return expectedType.cast(JID.jidInstance(value.toString().trim()));
			} else if (expectedType.equals(BareJID.class)) {
				return expectedType.cast(BareJID.bareJIDInstance(value.toString().trim()));
			} else if (expectedType.equals(CmdAcl.class)) {
				return expectedType.cast(new CmdAcl(value.toString().trim()));
			} else if (expectedType.equals(String.class)) {
				return expectedType.cast(String.valueOf(value.toString().trim()));
			} else if (expectedType.equals(Long.class)) {
				return expectedType.cast(Long.valueOf(value.toString().trim()));
			} else if (expectedType.equals(Integer.class)) {
				return expectedType.cast(Integer.valueOf(value.toString().trim()));
			} else if (expectedType.equals(Boolean.class)) {
				String val = value.toString().trim();
				boolean b = (val.equalsIgnoreCase("yes") || val.equalsIgnoreCase("true") ||
						val.equalsIgnoreCase("on") || val.equals("1"));
				return expectedType.cast(Boolean.valueOf(b));
			} else if (expectedType.equals(Float.class)) {
				return expectedType.cast(Float.valueOf(value.toString().trim()));
			} else if (expectedType.equals(Double.class)) {
				return expectedType.cast(Double.valueOf(value.toString().trim()));
			} else if (expectedType.equals(char.class)) {
				String v = value.toString().trim();
				if (v.length() == 1) {
					return (T) Character.valueOf(v.charAt(0));
				} else {
					throw new RuntimeException("Cannot convert '" + v + "' to char.");
				}
			} else if (expectedType.equals(int.class)) {
				return (T) Integer.valueOf(value.toString().trim());
			} else if (expectedType.equals(byte.class)) {
				return (T) Byte.valueOf(value.toString().trim());
			} else if (expectedType.equals(long.class)) {
				return (T) Long.valueOf(value.toString().trim());
			} else if (expectedType.equals(double.class)) {
				return (T) Double.valueOf(value.toString().trim());
			} else if (expectedType.equals(short.class)) {
				return (T) Short.valueOf(value.toString().trim());
			} else if (expectedType.equals(float.class)) {
				return (T) Float.valueOf(value.toString().trim());
			} else if (expectedType.equals(boolean.class)) {
				String val = value.toString().trim();
				boolean b = (val.equalsIgnoreCase("yes") || val.equalsIgnoreCase("true") ||
						val.equalsIgnoreCase("on") || val.equals("1"));
				return (T) Boolean.valueOf(b);
			} else if (expectedType.equals(Duration.class)) {
				return (T) Duration.parse(value.toString().trim());
			} else if (expectedType.equals(byte[].class) && value.toString().startsWith("string:")) {
				return (T) value.toString().substring(7).getBytes();
			} else if (expectedType.equals(byte[].class) && value.toString().startsWith("base64:")) {
				return (T) Base64.decode(value.toString().substring(7));
			} else if (expectedType.equals(char[].class) && value.toString().startsWith("string:")) {
				return (T) value.toString().substring(7).toCharArray();
			} else if (expectedType.equals(char[].class) && value.toString().startsWith("base64:")) {
				return (T) (new String(Base64.decode(value.toString().substring(7)))).toCharArray();
			} else if (expectedType.isArray()) {
				if (value instanceof Collection) {
					Collection col = (Collection) value;
					Object result = Array.newInstance(expectedType.getComponentType(), col.size());
					Iterator it = col.iterator();
					int i = 0;
					while (it.hasNext()) {
						Object v = it.next();
						if (v instanceof ConfigReader.Variable) {
							v = ((ConfigReader.Variable) v).calculateValue();
						}
						Array.set(result, i, (v instanceof String)
											 ? convert(unescape((String) v), expectedType.getComponentType())
											 : v);
						i++;
					}
					return (T) result;
				} else {
					String[] a_str = value.toString().split(regex);
					Object result = Array.newInstance(expectedType.getComponentType(), a_str.length);
					for (int i = 0; i < a_str.length; i++) {
						Array.set(result, i, convert(unescape(a_str[i]), expectedType.getComponentType()));
					}
					return (T) result;
				}
			} else if (Parcelable.class.isAssignableFrom(expectedType)) {
				try {
					T obj = expectedType.newInstance();
					String[] v = convert(value, String[].class);
					((Parcelable) obj).fillFromString(v);
					return obj;
				} catch (InstantiationException | IllegalAccessException ex) {
					throw new RuntimeException("Unsupported conversion (parcel) to " + expectedType, ex);
				}
			} else if (EnumSet.class.isAssignableFrom(expectedType) && genericType instanceof ParameterizedType) {
				ParameterizedType pt = (ParameterizedType) genericType;
				Type[] actualTypes = pt.getActualTypeArguments();
				if (actualTypes[0] instanceof Class) {
					String[] a_str = value.toString().split(regex);
					HashSet<Enum> result = new HashSet<>();
					for (int i = 0; i < a_str.length; i++) {
						result.add((Enum) convert(unescape(a_str[i]), (Class<?>) actualTypes[0]));
					}

					return (T) EnumSet.copyOf(result);
				}
			} else if (Pattern.class.isAssignableFrom(expectedType)) {
				return (T) Pattern.compile(value.toString());
			} else if (Collection.class.isAssignableFrom(expectedType) && genericType != null) {
				int mod = expectedType.getModifiers();
				if (!Modifier.isAbstract(mod) && !Modifier.isInterface(mod) &&
						genericType instanceof ParameterizedType) {
					ParameterizedType pt = (ParameterizedType) genericType;
					Type[] actualTypes = pt.getActualTypeArguments();
					if (actualTypes[0] instanceof Class) {
						if (value != null && value.getClass().isArray()) {
							try {
								Collection result = (Collection) expectedType.newInstance();
								for (int i = 0; i < Array.getLength(value); i++) {
									result.add(convert(Array.get(value, i), (Class<?>) actualTypes[0]));
								}
								return (T) result;
							} catch (InstantiationException | IllegalAccessException ex) {
								throw new RuntimeException("Unsupported conversion to " + expectedType, ex);
							}
						}
						if (value instanceof Collection) {
							try {
								Collection result = (Collection) expectedType.newInstance();
								for (Object c : ((Collection) value)) {
									result.add(convert(c, (Class<?>) actualTypes[0]));
								}
								return (T) result;
							} catch (InstantiationException | IllegalAccessException ex) {
								throw new RuntimeException("Unsupported conversion to " + expectedType, ex);
							}
						} else {
							String[] a_str = value.toString().split(regex);
							try {
								Collection result = (Collection) expectedType.newInstance();
								for (int i = 0; i < a_str.length; i++) {
									result.add(convert(unescape(a_str[i]), (Class<?>) actualTypes[0]));
								}
								return (T) result;
							} catch (InstantiationException | IllegalAccessException ex) {
								throw new RuntimeException("Unsupported conversion to " + expectedType, ex);
							}
						}
					}
				}
			} else if (Map.class.isAssignableFrom(expectedType) && genericType instanceof ParameterizedType &&
					value instanceof Map) {
				// this is additional support for convertion to type of Map, however value needs to be instance of Map
				// Added mainly for BeanConfigurators to be able to configure Map fields
				int mod = expectedType.getModifiers();
				Map result = null;
				if (!Modifier.isAbstract(mod) && !Modifier.isInterface(mod)) {
					try {
						result = (Map) expectedType.newInstance();
					} catch (InstantiationException | IllegalAccessException ex) {
						throw new RuntimeException("Unsupported conversion to " + expectedType, ex);
					}
				} else if (value instanceof Map) {
					result = new HashMap<>();
				}
				if (result != null) {
					ParameterizedType pt = (ParameterizedType) genericType;
					Type[] actualTypes = pt.getActualTypeArguments();
					for (Map.Entry<String, Object> e : ((Map<String, Object>) value).entrySet()) {
						Object k = convert(unescape(e.getKey()), actualTypes[0]);
						Object v = e.getValue() instanceof String
								   ? convert(unescape((String) e.getValue()), actualTypes[1])
								   : convert(e.getValue(), actualTypes[1]);
						result.put(k, v);
					}
					return (T) result;
				}
			} else {
				// here we try to assign instances of class passed as paramter if possible
				try {
					Class<?> cls = ModulesManagerImpl.getInstance().forName(value.toString());
					if (expectedType.isAssignableFrom(cls)) {
						return (T) cls.newInstance();
					}
				} catch (ClassNotFoundException ex) {
					// ignoring this
				} catch (InstantiationException | IllegalAccessException ex) {
					throw new RuntimeException("Could not instantiate instance of " + value.toString());
				}
			}

			{
				throw new RuntimeException("Unsupported conversion to " + expectedType);
			}
		} catch (TigaseStringprepException e) {
			throw new IllegalArgumentException(e);
		}
	}

	protected <T> T customConversion(final Object value, final Class<T> expectedType, Type genericType) {
		return null;
	}

	/**
	 * Converts object to String.
	 *
	 * @param value object to convert.
	 *
	 * @return text representation of value.
	 */
	public String toString(final Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Parcelable) {
			return toString(((Parcelable) value).encodeToStrings());
		} else if (value instanceof Class<?>) {
			return ((Class<?>) value).getName();
		} else if (value.getClass().isEnum()) {
			return ((Enum) value).name();
		} else if (value instanceof Collection) {
			StringBuilder sb = new StringBuilder();
			Iterator it = ((Collection) value).iterator();
			while (it.hasNext()) {
				sb.append(escape(toString(it.next())));
				if (it.hasNext()) {
					sb.append(',');
				}
			}
			return sb.toString();
		} else if (value.getClass().isArray()) {
			StringBuilder sb = new StringBuilder();
			final int l = Array.getLength(value);
			for (int i = 0; i < l; i++) {
				Object o = Array.get(value, i);
				sb.append(escape(toString(o)));
				if (i + 1 < l) {
					sb.append(',');
				}

			}
			return sb.toString();
		} else if (value instanceof ConfigReader.Variable) {
			return toString(((ConfigReader.Variable) value).calculateValue());
		} else {
			return value.toString();
		}
	}

}

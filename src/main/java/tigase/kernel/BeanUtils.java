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

import tigase.kernel.core.BeanConfig;
import tigase.kernel.core.DependencyManager;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BeanUtils {

	public static Field[] getAllFields(Class<?> klass) {
		List<Field> fields = new ArrayList<Field>();
		fields.addAll(Arrays.asList(klass.getDeclaredFields()));
		if (klass.getSuperclass() != null) {
			fields.addAll(Arrays.asList(getAllFields(klass.getSuperclass())));
		}
		return fields.toArray(new Field[]{});
	}

	public static Method[] getAllMethods(Class<?> klass) {
		List<Method> fields = new ArrayList<Method>();
		fields.addAll(Arrays.asList(klass.getDeclaredMethods()));
		if (klass.getSuperclass() != null) {
			fields.addAll(Arrays.asList(getAllMethods(klass.getSuperclass())));
		}
		return fields.toArray(new Method[]{});
	}

	public static java.lang.reflect.Field getField(BeanConfig bc, String fieldName) {
		final Class<?> cl = bc.getClazz();
		java.lang.reflect.Field[] fields = DependencyManager.getAllFields(cl);
		for (java.lang.reflect.Field field : fields) {
			if (field.getName().equals(fieldName)) {
				return field;
			}
		}
		return null;
	}

	public static Type getGetterSetterMethodsParameterType(Field f) {
		Method getter = prepareGetterMethod(f);
		if (getter == null) {
			return null;
		}
		Class rt = getter.getReturnType();
		Method setter = prepareSetterMethod(f, rt);

		return (setter == null) ? null : getter.getGenericReturnType();
	}

	public static Object getValue(Object fromBean, Field field)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Method setter = BeanUtils.prepareGetterMethod(field);
		if (setter != null) {
			return setter.invoke(fromBean);
		} else {
			field.setAccessible(true);
			return field.get(fromBean);
		}

	}

	public static String prepareAccessorMainPartName(final String fieldName) {
		if (fieldName.length() == 1) {
			return fieldName.toUpperCase();
		}

		String r;
		if (Character.isUpperCase(fieldName.charAt(1))) {
			r = fieldName.substring(0, 1);
		} else {
			r = fieldName.substring(0, 1).toUpperCase();
		}

		r += fieldName.substring(1);

		return r;
	}

	public static Method prepareGetterMethod(Field f) {
		String t = prepareAccessorMainPartName(f.getName());
		@SuppressWarnings("unused") String sm;
		String gm;
		if (f.getType().isPrimitive() && f.getType().equals(boolean.class)) {
			sm = "set" + t;
			gm = "is" + t;
		} else {
			sm = "set" + t;
			gm = "get" + t;
		}

		try {
			Method m = f.getDeclaringClass().getMethod(gm);
			return m;
		} catch (NoClassDefFoundError ex) {
			throw createExceptionForMissingClassForField(f, ex);
		} catch (NoSuchMethodException e) {
			return null;
		}
	}

	public static Method prepareSetterMethod(Field f) {
		return prepareSetterMethod(f, f.getType());
	}

	public static Method prepareSetterMethod(Field f, Class type) {
		String t = prepareAccessorMainPartName(f.getName());
		String sm;
		@SuppressWarnings("unused") String gm;
		if (f.getType().isPrimitive() && f.getType().equals(boolean.class)) {
			sm = "set" + t;
			gm = "is" + t;
		} else {
			sm = "set" + t;
			gm = "get" + t;
		}

		try {
			Method m = f.getDeclaringClass().getMethod(sm, type);
			return m;
		} catch (NoClassDefFoundError ex) {
			throw createExceptionForMissingClassForField(f, ex);
		} catch (NoSuchMethodException e) {
			return null;
			// throw new KernelException("Class " +
			// f.getDeclaringClass().getName() + " has no setter of field " +
			// f.getName(), e);
		}
	}

	public static ArrayList<Method> prepareSetterMethods(Class<?> destination, String fieldName) {
		String t = prepareAccessorMainPartName(fieldName);
		ArrayList<Method> result = new ArrayList<Method>();
		try {
			for (Method m : getAllMethods(destination)) {
				if (m.getName().equals("set" + t)) {
					result.add(m);
				}
			}
			return result;
		} catch (Exception e) {
			return null;
		}
	}

	public static void setValue(Object toBean, Field field, Object valueToSet)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Method setter = BeanUtils.prepareSetterMethod(field);
		if (valueToSet != null && setter == null) {
			setter = BeanUtils.prepareSetterMethod(field, valueToSet.getClass());
		}
		if (setter != null) {
			setter.invoke(toBean, valueToSet);
		} else {
			field.setAccessible(true);
			field.set(toBean, valueToSet);
		}
	}

	public static void setValue(Object toBean, String fieldName, Object valueToSet)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException {
		ArrayList<Method> setters = BeanUtils.prepareSetterMethods(toBean.getClass(), fieldName);

		if (setters == null || setters.isEmpty()) {
			throw new NoSuchMethodException("No setter for property '" + fieldName + "'.");
		}

		for (final Method s : setters) {
			try {
				s.invoke(toBean, valueToSet);
				return;
			} catch (Exception e) {
			}
		}

		throw new IllegalArgumentException(
				"Cannot set value type " + valueToSet.getClass().getName() + " to property '" + fieldName + "'.");
	}

	private static IllegalArgumentException createExceptionForMissingClassForField(Field f, Throwable cause) {
		return new IllegalArgumentException(
				"Missing class in classpath for field '" + f + "' in class " + f.getDeclaringClass() + " from " +
						f.getDeclaringClass()
								.getResource("/" + f.getDeclaringClass().getName().replace('.', '/') + ".class"),
				cause);
	}

	private BeanUtils() {
	}

}

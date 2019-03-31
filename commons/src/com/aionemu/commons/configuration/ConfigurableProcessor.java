package com.aionemu.commons.configuration;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.commons.configuration.transformers.MapTransformer;

/**
 * This class is designed to process classes and interfaces that have fields marked with {@link Property} annotation
 * 
 * @author SoulKeeper
 */
public class ConfigurableProcessor {

	private static final Logger log = LoggerFactory.getLogger(ConfigurableProcessor.class);
	private static final Pattern propertyPattern = Pattern.compile("\\$\\{([^}]+)\\}"); // finds strings enclosed in ${}

	/**
	 * This method is an entry point to the parser logic.<br>
	 * Any object or class that have {@link Property} annotation in it or it's parent class/interface can be submitted here.<br>
	 * If object(new Something()) is submitted, object fields are parsed. (non-static)<br>
	 * If class is submitted(Sotmething.class), static fields are parsed.<br>
	 * <p/>
	 * 
	 * @param object
	 *          Class or Object that has {@link Property} annotations.
	 * @param properties
	 *          Properties that should be used while searching for a {@link Property#key()}
	 */
	public static void process(Object object, Properties properties) {
		Class<?> clazz;

		if (object instanceof Class) {
			clazz = (Class<?>) object;
			object = null;
		} else {
			clazz = object.getClass();
		}

		process(clazz, object, properties);
	}

	/**
	 * This method uses recursive calls to launch search for {@link Property} annotation on itself and parents\interfaces.
	 * 
	 * @param clazz
	 *          Class of object
	 * @param obj
	 *          Object if any, null if parsing class (static fields only)
	 * @param props
	 *          Properties with keys\values
	 */
	private static void process(Class<?> clazz, Object obj, Properties props) {
		processFields(clazz, obj, props);

		// Interfaces can't have any object fields, only static
		// So there is no need to parse interfaces for instances of objects
		// Only classes (static fields) can be located in interfaces
		if (obj == null) {
			for (Class<?> itf : clazz.getInterfaces()) {
				process(itf, obj, props);
			}
		}

		Class<?> superClass = clazz.getSuperclass();
		if (superClass != null && superClass != Object.class) {
			process(superClass, obj, props);
		}
	}

	/**
	 * This method runs through the declared fields watching for the {@link Property} annotation. It also watches for the field modifiers like
	 * {@link java.lang.reflect.Modifier#STATIC} and {@link java.lang.reflect.Modifier#FINAL}
	 * 
	 * @param clazz
	 *          Class of object
	 * @param obj
	 *          Object if any, null if parsing class (static fields only)
	 * @param props
	 *          Properties with keys\values
	 */
	private static void processFields(Class<?> clazz, Object obj, Properties props) {
		for (Field f : clazz.getDeclaredFields()) {
			// Static fields should not be modified when processing object
			if (Modifier.isStatic(f.getModifiers()) && obj != null) {
				continue;
			}

			// Not static field should not be processed when parsing class
			if (!Modifier.isStatic(f.getModifiers()) && obj == null) {
				continue;
			}

			if (f.isAnnotationPresent(Property.class) || f.isAnnotationPresent(com.aionemu.commons.configuration.Properties.class)) {
				// Final fields should not be processed
				if (Modifier.isFinal(f.getModifiers()))
					throw new RuntimeException("Can't process final field " + f.getName() + " of class " + clazz.getName());
				processField(f, obj, props);
			}
		}
	}

	/**
	 * This method takes {@link Property} annotation and sets value according to annotation property. For this reason
	 * {@link #getFieldValue(java.lang.reflect.Field, java.util.Properties)} can be called, however if method sees that there is no need - field can
	 * remain with it's initial value.
	 * 
	 * @param f
	 *          field that is going to be processed
	 * @param obj
	 *          Object if any, null if parsing class (static fields only)
	 * @param props
	 *          Properties with keys & default values
	 */
	private static void processField(Field f, Object obj, Properties props) {
		boolean oldAccessible = f.isAccessible();
		try {
			if (!oldAccessible)
				f.setAccessible(true);
			Property property = f.getAnnotation(Property.class);
			com.aionemu.commons.configuration.Properties properties = f.getAnnotation(com.aionemu.commons.configuration.Properties.class);
			if (property != null) {
				if (properties != null)
					throw new UnsupportedOperationException("Field can only be annotated with @Property or @Properties, not both.");
				String key = Objects.requireNonNull(property.key(), "@Property key must not be empty");
				String value = getValue(key, property.defaultValue(), props);
				if (!Property.DEFAULT_VALUE.equals(value))
					f.set(obj, transform(value, f));
				else
					log.debug("Field " + f.getName() + " of class " + f.getDeclaringClass().getName() + " wasn't modified");
			} else {
				Pattern pattern = Pattern.compile(properties.keyPattern());
				Map<String, String> values = filterProperties(pattern, props);
				f.set(obj, transform(values, f));
			}
		} catch (Exception e) {
			throw new RuntimeException("Error modifying field " + f.getName() + " of " + (obj != null ? obj : f.getDeclaringClass()), e);
		} finally {
			if (!oldAccessible)
				f.setAccessible(false);
		}
	}

	public static Object transform(String value, Field field) throws TransformationException {
		Type[] genericTypeArgs = {};
		if (field.getGenericType() instanceof ParameterizedType)
			genericTypeArgs = ((ParameterizedType) field.getGenericType()).getActualTypeArguments();
		return PropertyTransformerFactory.getTransformer(field.getType()).transform(value, field.getType(), genericTypeArgs);
	}

	private static Map<String, String> filterProperties(Pattern pattern, Properties props) {
		Map<String, String> input = new HashMap<>();
		for (String k : props.stringPropertyNames()) {
			Matcher matcher = pattern.matcher(k);
			if (matcher.find()) {
				String key = matcher.groupCount() > 0 ? matcher.group(1) : k;
				String value = getValue(k, "", props);
				input.put(key, value);
			}
		}
		return input;
	}

	private static Map<?, ?> transform(Map<String, String> values, Field field) throws Exception {
		Type[] genericTypeArgs = {};
		if (field.getGenericType() instanceof ParameterizedType)
			genericTypeArgs = ((ParameterizedType) field.getGenericType()).getActualTypeArguments();
		return MapTransformer.transform(values, field.getType(), genericTypeArgs);
	}

	private static String getValue(String key, String defaultValue, Properties props) {
		String value = props.getProperty(key, defaultValue);
		if (value.trim().equals("\"\""))
			value = "";
		else
			value = replacePropertyPlaceholders(value, props);
		return value;
	}

	private static String replacePropertyPlaceholders(String value, Properties props) {
		Matcher matcher = propertyPattern.matcher(value);
		while (matcher.find()) {
			String completeToken = matcher.group(); // ${property.name}
			String token = matcher.group(1); // property.name
			String replacement = props.getProperty(token);
			value = value.replace(completeToken, replacement == null ? "" : replacement);
		}
		return value;
	}
}

package com.carma.swagger.doclet.parser;

import static com.google.common.collect.Lists.transform;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.carma.swagger.doclet.DocletOptions;
import com.carma.swagger.doclet.model.HttpMethod;
import com.google.common.base.Function;
import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.ExecutableMemberDoc;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.ParameterizedType;
import com.sun.javadoc.ProgramElementDoc;
import com.sun.javadoc.SeeTag;
import com.sun.javadoc.Tag;
import com.sun.javadoc.Type;
import com.sun.javadoc.TypeVariable;

/**
 * The ParserHelper represents a helper class for the parsers
 * @version $Id$
 */
public class ParserHelper {

	private static final String JAX_RS_PATH_PARAM = "javax.ws.rs.PathParam";
	private static final String JAX_RS_QUERY_PARAM = "javax.ws.rs.QueryParam";
	private static final String JAX_RS_HEADER_PARAM = "javax.ws.rs.HeaderParam";
	private static final String JAX_RS_FORM_PARAM = "javax.ws.rs.FormParam";

	private static final Set<String> _JAXRS_PARAM_ANNOTATIONS = new HashSet<String>();
	static {
		// TODO support cookie and matrix params...
		_JAXRS_PARAM_ANNOTATIONS.add(JAX_RS_PATH_PARAM);
		_JAXRS_PARAM_ANNOTATIONS.add(JAX_RS_QUERY_PARAM);
		_JAXRS_PARAM_ANNOTATIONS.add(JAX_RS_HEADER_PARAM);
		_JAXRS_PARAM_ANNOTATIONS.add(JAX_RS_FORM_PARAM);
	}

	/**
	 * This is a set of the FQN of the various JAXRS parameter annotations
	 */
	public static final Set<String> JAXRS_PARAM_ANNOTATIONS = Collections.unmodifiableSet(_JAXRS_PARAM_ANNOTATIONS);

	private static final String JAX_RS_ANNOTATION_PACKAGE = "javax.ws.rs";
	private static final Set<String> JAX_RS_PREFIXES = new HashSet<String>();
	static {
		JAX_RS_PREFIXES.add(JAX_RS_ANNOTATION_PACKAGE);
	}

	private static final String JAX_RS_PATH = "javax.ws.rs.Path";

	private static final String JAX_RS_CONSUMES = "javax.ws.rs.Consumes";
	private static final String JAX_RS_PRODUCES = "javax.ws.rs.Produces";
	private static final String JAX_RS_DEFAULT_VALUE = "javax.ws.rs.DefaultValue";

	@SuppressWarnings("serial")
	static final List<String> PRIMITIVES = new ArrayList<String>() {

		{
			add("byte");
			add("boolean");
			add("int");
			add("integer");
			add("long");
			add("float");
			add("double");
			add("short");
			add("char");
			add("string");
			add("date");
			add("number");
		}
	};

	/**
	 * This gets the allowable values from an enum class doc or null if the classdoc does not
	 * represent an enum
	 * @param typeClassDoc the class doc of the enum class to get the allowable values of
	 * @return The list of allowable values or null if this is not an enum
	 */
	public static List<String> getAllowableValues(ClassDoc typeClassDoc) {
		// TODO use translator to support @XmlEnum values...
		List<String> allowableValues = null;
		if (typeClassDoc != null && typeClassDoc.isEnum()) {
			allowableValues = transform(asList(typeClassDoc.enumConstants()), new Function<FieldDoc, String>() {

				public String apply(FieldDoc input) {
					if (input == null) {
						return null;
					}
					return input.name();
				}
			});
		}
		return allowableValues;
	}

	/**
	 * This gets whether a class doc has an ancestor class that can be processed, e.g.
	 * if its parent is java.lang.Object it returns false.
	 * @param classDoc The class doc
	 * @return True if the class doc class has a parent class that is not java.lang.Object.
	 */
	public static boolean hasAncestor(ClassDoc classDoc) {
		if (classDoc == null) {
			return false;
		}
		// ignore parent object class
		String qName = classDoc.qualifiedName();
		boolean isBaseObject = qName.equals("java.lang.Object");
		return !isBaseObject;
	}

	/**
	 * This gets an annotation value from the given class, it supports looking at super classes
	 * @param classDoc The class to look for the annotation
	 * @param options The doclet options
	 * @param qualifiedAnnotationType The FQN of the annotation to look for
	 * @param keys The keys for the annotation values
	 * @return The value of the annotation or null if none was found
	 */
	public static String getInheritableClassLevelAnnotationValue(ClassDoc classDoc, DocletOptions options, String qualifiedAnnotationType, String... keys) {
		ClassDoc currentClassDoc = classDoc;
		while (currentClassDoc != null) {

			AnnotationParser p = new AnnotationParser(currentClassDoc, options);
			String value = p.getAnnotationValue(qualifiedAnnotationType, keys);

			if (value != null) {
				return value;
			}

			currentClassDoc = currentClassDoc.superclass();
			// ignore parent object class
			if (!ParserHelper.hasAncestor(currentClassDoc)) {
				break;
			}
		}
		return null;
	}

	/**
	 * This gets the default value of the given parameter
	 * @param param The parameter
	 * @param options The doclet options
	 * @return The default value or null if it has no default
	 */
	public static String getDefaultValue(Parameter param, DocletOptions options) {
		AnnotationParser p = new AnnotationParser(param, options);
		String value = p.getAnnotationValue(JAX_RS_DEFAULT_VALUE, "value");
		return value;
	}

	/**
	 * Resolves the @Path for the ClassDoc supporting inheritance
	 * @param classDoc The class to be processed
	 * @param options Doclet options
	 * @return The resolved path
	 */
	public static String resolveClassPath(ClassDoc classDoc, DocletOptions options) {
		String path = ParserHelper.getInheritableClassLevelAnnotationValue(classDoc, options, JAX_RS_PATH, "value");
		return normalisePath(path);
	}

	/**
	 * Resolves the @Path for the MethodDoc respecting the overriden methods
	 * @param methodDoc The method to be processed
	 * @param options Doclet options
	 * @return The resolved path
	 */
	public static String resolveMethodPath(ExecutableMemberDoc methodDoc, DocletOptions options) {

		String path = null;
		while (path == null && methodDoc != null) {
			AnnotationParser p = new AnnotationParser(methodDoc, options);
			path = p.getAnnotationValue(JAX_RS_PATH, "value");
			methodDoc = getAncestorMethod(methodDoc);
		}
		return normalisePath(path);
	}

	/**
	 * This normalises a path to ensure it starts with / and does not end with /
	 * @param path The path to normalize
	 * @return The path or an empty string if given no path
	 */
	private static String normalisePath(String path) {
		if (path != null) {
			path = path.trim();
			if (path.endsWith("/")) {
				path = path.substring(0, path.length() - 1);
			}
			if (!path.isEmpty() && !path.startsWith("/")) {
				path = "/" + path;
			}

			return path;
		}
		return "";
	}

	/**
	 * This verifies that the given numeric values are valid for the given data type and format and returns the comparison.
	 * If not valid it raises an exception. It ignores null/empty values.
	 * @param context Additional description for contextualizing the error message
	 * @param type The data type as per json schema
	 * @param format The data format
	 * @param value1 The first value to check
	 * @param value2 The 2nd value to check
	 * @throws IllegalStateException if the value is invalid.
	 * @return null if either value is null/empty, otherwise -1,0,1 as per standard java compare behaviour.
	 */
	public static Integer compareNumericValues(String context, String type, String format, String value1, String value2) {
		if (value1 == null || value1.trim().isEmpty() || value2 == null || value2.trim().isEmpty()) {
			return null;
		}
		Number val1 = verifyNumericValue(context, type, format, value1);
		Number val2 = verifyNumericValue(context, type, format, value1);
		return Double.compare(val1.doubleValue(), val2.doubleValue());
	}

	/**
	 * This verifies that the given value is valid for the given data type and format for use as a numeric value
	 * which means it must be integer or number type.
	 * If not valid it raises an exception. It ignores null/empty values.
	 * @param context Additional description for contextualizing the error message
	 * @param type The data type as per json schema
	 * @param format The data format
	 * @param value The value to check
	 * @throws IllegalStateException if the value is invalid.
	 * @return The value as a number suitable for the given type and format
	 */
	public static Number verifyNumericValue(String context, String type, String format, String value) {
		if (value == null || value.trim().isEmpty()) {
			return null;
		}
		try {
			if (type.equals("integer")) {
				if (format.equals("int32")) {
					return Integer.parseInt(value);
				} else {
					return Long.parseLong(value);
				}
			} else if (type.equals("number")) {
				if (format.equals("double")) {
					return Double.parseDouble(value);
				} else {
					return Float.parseFloat(value);
				}
			} else if (format != null && format.equals("byte")) {
				return Byte.parseByte(value);
			}
		} catch (NumberFormatException nfe) {
			throw new IllegalStateException("The value was not valid for the type: " + type + " and format: " + format + context, nfe);
		}
		throw new IllegalStateException("Min/Max values are not allowed for the type: " + type + " and format: " + format + context);
	}

	/**
	 * This verifies that the given value is valid for the given data type and format.
	 * If not valid it raises an exception. It ignores null/empty values.
	 * @param context Additional description for contextualizing the error message
	 * @param type The data type as per json schema
	 * @param format The data format
	 * @param value The value to check
	 * @throws IllegalStateException if the value is invalid.
	 */
	public static void verifyValue(String context, String type, String format, String value) {
		if (value == null || value.trim().isEmpty()) {
			return;
		}
		try {
			if (type.equals("integer")) {
				if (format.equals("int32")) {
					Integer.parseInt(value);
				} else {
					Long.parseLong(value);
				}
			} else if (type.equals("number")) {
				if (format.equals("double")) {
					Double.parseDouble(value);
				} else {
					Float.parseFloat(value);
				}
			} else if (format != null && format.equals("byte")) {
				Byte.parseByte(value);
			} else if (type.equals("boolean")) {
				if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
					throw new IllegalStateException("The value was not valid for the type: " + type + " and format: " + format + context);
				}
			}
			// TODO support date and date time
		} catch (NumberFormatException nfe) {
			throw new IllegalStateException("The value was not valid for the type: " + type + " and format: " + format + context, nfe);
		}
	}

	/**
	 * This gets the qualified type name of a javadoc type,
	 * It adds [] onto any array types
	 * @param type The type
	 * @return The qualified type name
	 */
	public static String getQualifiedTypeName(Type type) {
		String qName = type.qualifiedTypeName();
		// handle arrays
		String dimension = type.dimension();
		if (dimension != null && "[]".equals(dimension)) {
			qName = qName + "[]";
		}
		return qName;
	}

	/**
	 * Determines the String representation of the given FQN.
	 * This includes the type as the first item and the format as the 2nd.
	 * Common name Swagger spec 1.2
	 * integer integer, int32
	 * long integer, int64
	 * float number, float
	 * double number, double
	 * string string
	 * byte string, byte
	 * boolean boolean
	 * date string, date
	 * dateTime string, date-time
	 * @param javaType The FQN of the java type to get the swagger type and format of
	 * @param useFqn whether to return a name for the type based on the FQN of the type,
	 *            for example if given a java type of com.me.Data then if useFqn is false it will return Data
	 *            but if true will return com-me-Data
	 * @param options The doclet options
	 * @return An array with the type as the first item and the format as the 2nd.
	 */
	public static String[] typeOf(String javaType, boolean useFqn, DocletOptions options) {
		String[] simpleType = primitiveTypeOf(javaType, options);

		if (simpleType != null) {
			return simpleType;
		} else if (isCollection(javaType)) {
			return new String[] { "array", null };
		} else if (isSet(javaType)) {
			return new String[] { "array", null };
		} else if (isArray(javaType)) {
			return new String[] { "array", null };
		} else if (javaType.equalsIgnoreCase("java.io.File")) {
			// special handling of files, the datatype File is reserved for multipart
			return new String[] { "JavaFile", null };
		} else if (useFqn) {
			String typeName = javaType.replace(".", "-");
			return new String[] { typeName, null };
		} else {

			// support inner classes, for this we use case sensitivity
			// e.g. com.my.Foo.Bar should map to Foo-Bar
			int startPos = -1;
			for (int i = 0; i < javaType.length(); i++) {
				char c = javaType.charAt(i);
				if (Character.isUpperCase(c)) {
					startPos = i;
					break;
				}
			}
			if (startPos == -1) {
				startPos = javaType.lastIndexOf(".") + 1;
				if (startPos > javaType.length() - 1) {
					startPos = -1;
				}
			}

			if (startPos >= 0) {
				String typeName = javaType.substring(startPos).replace(".", "-");
				return new String[] { typeName, null };
			} else {
				return new String[] { javaType, null };
			}
		}
	}

	/**
	 * Determines the String representation of the object Type.
	 * This includes the type as the first item and the format as the 2nd.
	 * Common name Swagger spec 1.2
	 * integer integer, int32
	 * long integer, int64
	 * float number, float
	 * double number, double
	 * string string
	 * byte string, byte
	 * boolean boolean
	 * date string, date
	 * dateTime string, date-time
	 * @param type The java type to get the swagger type and format of
	 * @param useFqn whether to return a name for the type based on the FQN of the type,
	 *            for example if given a java type of com.me.Data then if useFqn is false it will return Data
	 *            but if true will return com-me-Data
	 * @param options The doclet options
	 * @return An array with the type as the first item and the format as the 2nd.
	 */
	public static String[] typeOf(Type type, boolean useFqn, DocletOptions options) {
		String javaType = getQualifiedTypeName(type);
		return typeOf(javaType, useFqn, options);
	}

	/**
	 * This gets the swagger type and format for a javatype but only looks at primitives
	 * @param javaType The java type
	 * @param options the doclet options
	 * @return The swagger type and format or null if the java type is not a primitive
	 */
	public static String[] primitiveTypeOf(String javaType, DocletOptions options) {

		if (javaType.toLowerCase().equals("byte") || javaType.equalsIgnoreCase("java.lang.Byte")) {
			return new String[] { "string", "byte" };
		} else if (javaType.toLowerCase().equals("int") || javaType.toLowerCase().equals("integer") || javaType.equalsIgnoreCase("java.lang.Integer")) {
			return new String[] { "integer", "int32" };
		} else if (javaType.toLowerCase().equals("short") || javaType.equalsIgnoreCase("java.lang.Short")) {
			return new String[] { "integer", "int32" };
		} else if (javaType.toLowerCase().equals("long") || javaType.equalsIgnoreCase("java.lang.Long") || javaType.equalsIgnoreCase("java.math.BigInteger")) {
			return new String[] { "integer", "int64" };
		} else if (javaType.toLowerCase().equals("float") || javaType.equalsIgnoreCase("java.lang.Float")) {
			return new String[] { "number", "float" };
		} else if (javaType.toLowerCase().equals("double") || javaType.equalsIgnoreCase("java.lang.Double")
				|| javaType.equalsIgnoreCase("java.math.BigDecimal")) {
			return new String[] { "number", "double" };
		} else if (javaType.toLowerCase().equals("string") || javaType.equalsIgnoreCase("java.lang.String")) {
			return new String[] { "string", null };
		} else if (javaType.toLowerCase().equals("char") || javaType.equalsIgnoreCase("java.lang.Character")) {
			return new String[] { "string", null };
		} else if (javaType.toLowerCase().equals("boolean") || javaType.equalsIgnoreCase("java.lang.Boolean")) {
			return new String[] { "boolean", null };
		} else if (javaType.equalsIgnoreCase("java.net.URI") || javaType.equalsIgnoreCase("java.net.URL")) {
			return new String[] { "string", null };

		} else if (javaType.toLowerCase().equals("uuid") || javaType.equalsIgnoreCase("java.util.UUID")) {
			return new String[] { "string", "uuid" };
		}

		// see if its a special long type
		for (String prefix : options.getLongTypePrefixes()) {
			if (javaType.startsWith(prefix)) {
				return new String[] { "integer", "int64" };
			}
		}

		// see if its a special int type
		for (String prefix : options.getIntTypePrefixes()) {
			if (javaType.startsWith(prefix)) {
				return new String[] { "integer", "int32" };
			}
		}

		// see if its a special double type
		for (String prefix : options.getDoubleTypePrefixes()) {
			if (javaType.startsWith(prefix)) {
				return new String[] { "number", "double" };
			}
		}

		// see if its a special float type
		for (String prefix : options.getDoubleTypePrefixes()) {
			if (javaType.startsWith(prefix)) {
				return new String[] { "number", "float" };
			}
		}

		// see if its a special string type
		for (String prefix : options.getStringTypePrefixes()) {
			if (javaType.startsWith(prefix)) {
				return new String[] { "string", null };
			}
		}

		// default date handling, note this can be overridden by long/int type prefixes if people wish
		if (javaType.equalsIgnoreCase("java.time.LocalDate") || javaType.equalsIgnoreCase("org.joda.time.LocalDate")
				|| javaType.equalsIgnoreCase("org.joda.time.YearMonthDay")) {
			return new String[] { "string", "date" };

		} else if (javaType.equalsIgnoreCase("java.time.Year") || javaType.equalsIgnoreCase("org.joda.time.Years")
				|| javaType.equalsIgnoreCase("org.joda.time.Months") || javaType.equalsIgnoreCase("org.joda.time.Weeks")
				|| javaType.equalsIgnoreCase("org.joda.time.Days") || javaType.equalsIgnoreCase("org.joda.time.Hours")
				|| javaType.equalsIgnoreCase("org.joda.time.Minutes") || javaType.equalsIgnoreCase("org.joda.time.Seconds")) {
			return new String[] { "integer", "int32" };

		} else if (javaType.toLowerCase().equals("date") || javaType.equalsIgnoreCase("java.util.Date") || javaType.equalsIgnoreCase("java.sql.Date")
				|| javaType.equalsIgnoreCase("java.util.Calendar") || javaType.equalsIgnoreCase("java.time.LocalDateTime")
				|| javaType.equalsIgnoreCase("java.time.OffsetDateTime") || javaType.equalsIgnoreCase("java.time.ZonedDateTime")
				|| javaType.equalsIgnoreCase("org.joda.time.DateMidnight") || javaType.equalsIgnoreCase("org.joda.time.DateTime")
				|| javaType.equalsIgnoreCase("org.joda.time.LocalDateTime") || javaType.equalsIgnoreCase("org.joda.time.MutableDateTime")) {
			return new String[] { "string", "date-time" };

		} else if (javaType.equalsIgnoreCase("java.time.OffsetTime") || javaType.equalsIgnoreCase("java.time.Duration")
				|| javaType.equalsIgnoreCase("org.joda.time.Duration") || javaType.equalsIgnoreCase("java.time.MonthDay")
				|| javaType.equalsIgnoreCase("java.time.Period") || javaType.equalsIgnoreCase("java.time.Month")
				|| javaType.equalsIgnoreCase("java.time.DayOfWeek") || javaType.equalsIgnoreCase("java.time.YearMonth")
				|| javaType.equalsIgnoreCase("java.time.ZoneId") || javaType.equalsIgnoreCase("java.time.ZoneOffset")
				|| javaType.equalsIgnoreCase("java.time.Instant") || javaType.equalsIgnoreCase("java.time.LocalTime")
				|| javaType.equalsIgnoreCase("org.joda.time.LocalTime") || javaType.equalsIgnoreCase("org.joda.time.Instant")
				|| javaType.equalsIgnoreCase("org.joda.time.Interval") || javaType.equalsIgnoreCase("org.joda.time.MonthDay")
				|| javaType.equalsIgnoreCase("org.joda.time.DateTimeZone") || javaType.equalsIgnoreCase("org.joda.time.MutableInterval")
				|| javaType.equalsIgnoreCase("org.joda.time.Partial") || javaType.equalsIgnoreCase("org.joda.time.Period")
				|| javaType.equalsIgnoreCase("org.joda.time.MutablePeriod") || javaType.equalsIgnoreCase("org.joda.time.PeriodType")
				|| javaType.equalsIgnoreCase("org.joda.time.TimeOfDay") || javaType.equalsIgnoreCase("org.joda.time.YearMonth")) {
			return new String[] { "string", null };

		}

		return null;
	}

	/**
	 * This gets the swagger type and format for a javatype but only looks at primitives
	 * @param type The java type
	 * @param options the doclet options
	 * @return The swagger type and format or null if the java type is not a primitive
	 */
	public static String[] primitiveTypeOf(Type type, DocletOptions options) {
		String javaType = getQualifiedTypeName(type);
		return primitiveTypeOf(javaType, options);
	}

	/**
	 * This gets parameterized types of the given type substituting variable types if necessary
	 * @param type The raw type such as Batch&lt;Item&gt; or Batch&lt;T, Y&gt;
	 * @param varsToTypes A map of variable name to types
	 * @return The list of parameterized types
	 */
	public static List<Type> getParameterizedTypes(Type type, Map<String, Type> varsToTypes) {
		ParameterizedType pt = type.asParameterizedType();
		if (pt != null) {
			Type[] typeArgs = pt.typeArguments();
			if (typeArgs != null && typeArgs.length > 0) {
				List<Type> res = new ArrayList<Type>();
				for (Type pType : typeArgs) {
					Type replacedType = getVarType(pType.asTypeVariable(), varsToTypes);
					if (replacedType == null) {
						res.add(pType);
					} else {
						res.add(replacedType);
					}
				}
				return res;
			}
		}
		return Collections.emptyList();
	}

	/**
	 * This gets the type that a container holds
	 * @param type The raw type like Collection&lt;String&gt;
	 * @param varsToTypes A map of variables to types for parameterized types, optional if null parameterized types
	 *            will not be handled
	 * @param classes set of classes
	 * @return The container type or null if not a collection
	 */
	public static Type getContainerType(Type type, Map<String, Type> varsToTypes, Collection<ClassDoc> classes) {
		Type result = null;
		ParameterizedType pt = type.asParameterizedType();
		if (pt != null && (ParserHelper.isCollection(type.qualifiedTypeName()) || ParserHelper.isArray(type))) {
			Type[] typeArgs = pt.typeArguments();
			if (typeArgs != null && typeArgs.length > 0) {
				result = typeArgs[0];
			}
		} else if (ParserHelper.isArray(type)) {
			result = type.getElementType();
		}
		// if its a ref to a param type replace with the type impl
		if (result != null) {
			Type paramType = getVarType(result.asTypeVariable(), varsToTypes);
			if (paramType != null) {
				return paramType;
			}
		}
		// if its a non parameterized array then find the array type
		// note in java 8 we can do this directly, however for now the only solution is to look it up via the model classes
		if (ParserHelper.isArray(type)) {
			ClassDoc foundModel = findModel(classes, type.qualifiedTypeName());
			if (foundModel != null) {
				result = foundModel;
			}
		}

		return result;
	}

	private static Map<String, Class<?>> CLASSES = new HashMap<String, Class<?>>();

	private static Class<?> lookupClass(String javaType) throws ClassNotFoundException {
		if (CLASSES.containsKey(javaType)) {
			return CLASSES.get(javaType);
		}
		Class<?> clazz = Class.forName(javaType);
		CLASSES.put(javaType, clazz);
		return clazz;
	}

	/**
	 * This finds a variable type and returns its impl from the given map
	 * @param var The variable type to find
	 * @param varsToTypes The map of variables to types
	 * @return The result.
	 */
	public static Type getVarType(TypeVariable var, Map<String, Type> varsToTypes) {
		Type res = null;
		if (var != null && varsToTypes != null) {
			Set<Type> processedTypes = new HashSet<Type>();
			Type type = varsToTypes.get(var.qualifiedTypeName());
			while (type != null && !processedTypes.contains(type)) {
				res = type;
				processedTypes.add(type);
				type = varsToTypes.get(type.qualifiedTypeName());
			}
		}
		return res;
	}

	private static Map<String, Boolean> SET_TYPES = new HashMap<String, Boolean>();

	/**
	 * This gets whether the given type is a Set
	 * @param javaType The java type
	 * @return True if this is a Set
	 */
	public static boolean isSet(String javaType) {
		if (SET_TYPES.containsKey(javaType)) {
			return SET_TYPES.get(javaType);
		}
		try {
			Class<?> clazz = lookupClass(javaType);
			boolean res = java.util.Set.class.isAssignableFrom(clazz);
			if (res) {
				SET_TYPES.put(javaType, res);
			}
			return res;
		} catch (ClassNotFoundException ex) {
			return false;
		}
	}

	/**
	 * This gets whether the given type is an Array
	 * @param type The type
	 * @return True if this is an array
	 */
	public static boolean isArray(Type type) {
		return type.dimension() != null && type.dimension().length() > 0;
	}

	/**
	 * This gets whether the given type is an Array
	 * @param javaType The java type
	 * @return True if this is an array
	 */
	public static boolean isArray(String javaType) {
		return javaType.endsWith("[]");
	}

	private static Map<String, Boolean> COLLECTION_TYPES = new HashMap<String, Boolean>();

	/**
	 * This gets whether the given type is a Collection
	 * @param javaType The java type
	 * @return True if this is a collection
	 */
	public static boolean isCollection(String javaType) {
		if (COLLECTION_TYPES.containsKey(javaType)) {
			return COLLECTION_TYPES.get(javaType);
		}
		try {
			Class<?> clazz = lookupClass(javaType);
			boolean res = java.util.Collection.class.isAssignableFrom(clazz);
			if (res) {
				COLLECTION_TYPES.put(javaType, res);
			}
			return res;
		} catch (ClassNotFoundException ex) {
			return false;
		}
	}

	private static Map<String, Boolean> MAP_TYPES = new HashMap<String, Boolean>();

	/**
	 * This gets whether the given type is a Map
	 * @param javaType The java type
	 * @return True if this is a map
	 */
	public static boolean isMap(String javaType) {
		if (MAP_TYPES.containsKey(javaType)) {
			return MAP_TYPES.get(javaType);
		}
		try {
			Class<?> clazz = lookupClass(javaType);
			boolean res = java.util.Map.class.isAssignableFrom(clazz);
			if (res) {
				MAP_TYPES.put(javaType, res);
			}
			return res;
		} catch (ClassNotFoundException ex) {
			return false;
		}
	}

	/**
	 * This gets whether the given parameter is a File data type
	 * @param parameter The parameter
	 * @param options The doclet options
	 * @return True if the parameter is a File data type
	 */
	public static boolean isFileParameterDataType(Parameter parameter, DocletOptions options) {
		if (hasAnnotation(parameter, options.getFileParameterAnnotations(), options)) {
			return true;
		}
		String qName = ParserHelper.getQualifiedTypeName(parameter.type());
		for (String fileType : options.getFileParameterTypes()) {
			if (qName.equals(fileType)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * This gets (for composite param fields) whether the given parameter is a File data type
	 * @param paramMember The field or method that is the parameter
	 * @param type The type of the parameter
	 * @param options The doclet options
	 * @return True if the parameter is a File data type
	 */
	private static boolean isFileParameterDataType(ProgramElementDoc paramMember, Type type, DocletOptions options) {
		if (hasAnnotation(paramMember, options.getFileParameterAnnotations(), options)) {
			return true;
		}
		String qName = ParserHelper.getQualifiedTypeName(type);
		for (String fileType : options.getFileParameterTypes()) {
			if (qName.equals(fileType)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determines the string representation of the parameter type for composite types.
	 * @param returnDefault Whether to return a default value if there is no specific jaxrs param annotation
	 * @param multipart Whether the method the parameter is for consumes multipart
	 * @param paramMember The field or method that is the parameter
	 * @param type The type of the parameter
	 * @param options The doclet options
	 * @return The type of parameter, one of path, header, query, form or body.
	 */
	public static String paramTypeOf(boolean returnDefault, boolean multipart, ProgramElementDoc paramMember, Type type, DocletOptions options) {
		AnnotationParser p = new AnnotationParser(paramMember, options);
		if (p.isAnnotatedBy(JAX_RS_PATH_PARAM)) {
			return "path";
		} else if (p.isAnnotatedBy(JAX_RS_HEADER_PARAM)) {
			return "header";
		} else if (p.isAnnotatedBy(JAX_RS_QUERY_PARAM)) {
			return "query";
		} else if (p.isAnnotatedBy(JAX_RS_FORM_PARAM)) {
			return "form";
		}

		String qName = getQualifiedTypeName(type);

		// bean param and other composites
		for (String compositeAnnotation : options.getCompositeParamAnnotations()) {
			if (p.isAnnotatedBy(compositeAnnotation)) {
				return "composite";
			}
		}
		for (String compositeType : options.getCompositeParamTypes()) {
			if (qName.equals(compositeType)) {
				return "composite";
			}
		}

		// look for form parameter types
		for (String formAnnotation : options.getFormParameterAnnotations()) {
			if (p.isAnnotatedBy(formAnnotation)) {
				return "form";
			}
		}
		for (String formType : options.getFormParameterTypes()) {
			if (qName.equals(formType)) {
				return "form";
			}
		}

		// look for File data types, for multipart these are always form parameter types
		// as per the swagger 1.2 spec
		if (multipart && isFileParameterDataType(paramMember, type, options)) {
			return "form";
		}

		if (!returnDefault) {
			return null;
		}

		// otherwise default to body
		return "body";
	}

	/**
	 * Determines the string representation of the parameter type.
	 * @param multipart Whether the method the parameter is for consumes multipart
	 * @param parameter The parameter to get the type of
	 * @param options The doclet options
	 * @return The type of parameter, one of path, header, query, form or body.
	 */
	public static String paramTypeOf(boolean multipart, Parameter parameter, DocletOptions options) {
		AnnotationParser p = new AnnotationParser(parameter, options);
		if (p.isAnnotatedBy(JAX_RS_PATH_PARAM)) {
			return "path";
		} else if (p.isAnnotatedBy(JAX_RS_HEADER_PARAM)) {
			return "header";
		} else if (p.isAnnotatedBy(JAX_RS_QUERY_PARAM)) {
			return "query";
		} else if (p.isAnnotatedBy(JAX_RS_FORM_PARAM)) {
			return "form";
		}

		String qName = getQualifiedTypeName(parameter.type());

		// bean param and other composites
		for (String compositeAnnotation : options.getCompositeParamAnnotations()) {
			if (p.isAnnotatedBy(compositeAnnotation)) {
				return "composite";
			}
		}
		for (String compositeType : options.getCompositeParamTypes()) {
			if (qName.equals(compositeType)) {
				return "composite";
			}
		}

		// look for form parameter types
		for (String formAnnotation : options.getFormParameterAnnotations()) {
			if (p.isAnnotatedBy(formAnnotation)) {
				return "form";
			}
		}
		for (String formType : options.getFormParameterTypes()) {
			if (qName.equals(formType)) {
				return "form";
			}
		}

		// look for File data types, for multipart these are always form parameter types
		// as per the swagger 1.2 spec
		if (multipart && isFileParameterDataType(parameter, options)) {
			return "form";
		}

		// otherwise default to body
		return "body";
	}

	/**
	 * Determines the string representation of the parameter name.
	 * @param parameter The parameter to get the name of that is used for the api
	 * @param overrideParamNames A map of rawname to override names for parameters
	 * @param paramNameAnnotations List of FQN of annotations that can be used for the parameter name
	 * @param options The doclet options
	 * @return the name of the parameter used by http requests
	 */
	public static String paramNameOf(Parameter parameter, Map<String, String> overrideParamNames, List<String> paramNameAnnotations, DocletOptions options) {

		String rawName = parameter.name();
		AnnotationParser p = new AnnotationParser(parameter, options);
		return paramNameOf(rawName, p, overrideParamNames, paramNameAnnotations, options);
	}

	/**
	 * Determines the string representation of the parameter name.
	 * @param field The parameter field to get the name of that is used for the api
	 * @param overrideParamNames A map of rawname to override names for parameters
	 * @param paramNameAnnotations List of FQN of annotations that can be used for the parameter name
	 * @param options The doclet options
	 * @return the name of the parameter used by http requests
	 */
	public static String fieldParamNameOf(FieldDoc field, Map<String, String> overrideParamNames, List<String> paramNameAnnotations, DocletOptions options) {

		String rawName = field.name();
		AnnotationParser p = new AnnotationParser(field, options);
		return paramNameOf(rawName, p, overrideParamNames, paramNameAnnotations, options);
	}

	private static String paramNameOf(String rawName, AnnotationParser p, Map<String, String> overrideParamNames, List<String> paramNameAnnotations,
			DocletOptions options) {
		String name = null;

		// if there is an override name use that ahead of any annotation
		if (overrideParamNames != null && overrideParamNames.containsKey(rawName)) {
			name = overrideParamNames.get(rawName);
		}
		// look for any of the configured annotations that can determine the parameter name
		if (name == null) {
			for (String paramNameAnnotation : paramNameAnnotations) {
				name = p.getAnnotationValue(paramNameAnnotation, "value");
				if (name != null) {
					break;
				}
			}
		}
		// otherwise use the raw parameter name as defined on the method signature
		if (name == null) {
			name = rawName;
		}

		return name;
	}

	/**
	 * This gets the json views for the given method, it supports deriving the views from an overridden method
	 * @param methodDoc The method to get the json views of
	 * @param options The doclet options
	 * @return The json views for the given method/overridden method or null if there were none
	 */
	public static ClassDoc[] getInheritableJsonViews(ExecutableMemberDoc methodDoc, DocletOptions options) {
		ClassDoc[] result = null;
		while (result == null && methodDoc != null) {
			result = getJsonViews(methodDoc, options);
			methodDoc = getAncestorMethod(methodDoc);
		}
		return result;
	}

	/**
	 * This gets the json views for the given method/field
	 * @param doc The method/field to get the json views of
	 * @param options The doclet options
	 * @return The json views for the given method/field or null if there were none
	 */
	public static ClassDoc[] getJsonViews(com.sun.javadoc.ProgramElementDoc doc, DocletOptions options) {
		AnnotationParser p = new AnnotationParser(doc, options);
		ClassDoc[] viewClasses = p.getAnnotationClassDocValues("com.fasterxml.jackson.annotation.JsonView", "value");
		if (viewClasses == null) {
			viewClasses = p.getAnnotationClassDocValues("org.codehaus.jackson.map.annotate.JsonView", "value");
		}
		return viewClasses;
	}

	/**
	 * This gets the json views for the given method parameter, it supports deriving the views from an overridden method
	 * @param methodDoc The method
	 * @param param The parameter
	 * @param options The doclet options
	 * @return The json views for the given method/overridden method or null if there were none
	 */
	public static ClassDoc[] getInheritableJsonViews(com.sun.javadoc.ExecutableMemberDoc methodDoc, Parameter param, DocletOptions options) {

		int paramIdx = -1;
		int i = 0;
		for (Parameter methodParam : methodDoc.parameters()) {
			if (methodParam.name().equals(param.name())) {
				paramIdx = i;
				break;
			}
			i++;
		}

		if (paramIdx == -1) {
			throw new IllegalArgumentException("Invalid method param: " + param + " it is not a parameter of the given method: " + methodDoc);
		}

		ClassDoc[] result = null;
		while (result == null && methodDoc != null) {
			result = getJsonViews(param, options);
			methodDoc = getAncestorMethod(methodDoc);
			param = methodDoc == null ? null : methodDoc.parameters()[paramIdx];
		}
		return result;
	}

	/**
	 * This gets the json views for the given method parameter
	 * @param param
	 * @param options The doclet options
	 * @return The json views for the given param or null if there were none
	 */
	public static ClassDoc[] getJsonViews(Parameter param, DocletOptions options) {
		AnnotationParser p = new AnnotationParser(param, options);
		ClassDoc[] viewClasses = p.getAnnotationClassDocValues("com.fasterxml.jackson.annotation.JsonView", "value");
		if (viewClasses == null) {
			viewClasses = p.getAnnotationClassDocValues("org.codehaus.jackson.map.annotate.JsonView", "value");
		}
		return viewClasses;
	}

	/**
	 * This gets whether the given method/field has a json view on it
	 * @param doc The method/field to check
	 * @param options The doclet options
	 * @return True if the given method/field has a json view
	 */
	public static boolean hasJsonViews(com.sun.javadoc.ProgramElementDoc doc, DocletOptions options) {
		return getJsonViews(doc, options) != null;
	}

	/**
	 * This checks if a field is part of a resource object serialization view, it is if any of the views on the field
	 * are superclasses of any of the operation views
	 * @param operationViews The operation views that indicate which views apply to the operation.
	 * @param fieldViews The views that are on the getter/field
	 * @return True if the field/getter is part of the view
	 */
	public static boolean isItemPartOfView(ClassDoc[] operationViews, ClassDoc[] fieldViews) {
		if (operationViews != null && fieldViews != null) {
			// check that one of the field views is a superclass of an operation view
			for (ClassDoc operationView : operationViews) {
				for (ClassDoc fieldView : fieldViews) {
					if (isAssignableFrom(fieldView, operationView)) {
						return true;
					}
				}
			}
			return false;
		}
		return true;
	}

	private static boolean isAssignableFrom(ClassDoc superClass, ClassDoc clazz) {
		if (clazz == superClass || clazz.subclassOf(superClass)) {
			return true;
		}
		if (clazz.isInterface()) {
			// check if one of the interfaces it extends match the view class
			ClassDoc[] interfaces = clazz.interfaces();
			if (interfaces != null) {
				for (ClassDoc parentInterface : interfaces) {
					boolean implementsIntf = isAssignableFrom(superClass, parentInterface);
					if (implementsIntf) {
						return true;
					}
				}
			}

		}
		return false;
	}

	/**
	 * This gets the list of consumes mime types from the given method
	 * @param methodDoc The method javadoc
	 * @param options The doclet options
	 * @return The list or null if none were found
	 */
	public static List<String> getConsumes(MethodDoc methodDoc, DocletOptions options) {
		List<String> methodLevel = listInheritableValues(methodDoc, JAX_RS_CONSUMES, "value", options);
		if (methodLevel == null) {
			// look for class level
			return listValues(methodDoc.containingClass(), JAX_RS_CONSUMES, "value", options);
		}
		return methodLevel;
	}

	/**
	 * This gets the list of produces mime types from the given method
	 * @param methodDoc The method javadoc
	 * @param options The doclet options
	 * @return The list or null if none were found
	 */
	public static List<String> getProduces(MethodDoc methodDoc, DocletOptions options) {
		List<String> methodLevel = listInheritableValues(methodDoc, JAX_RS_PRODUCES, "value", options);
		if (methodLevel == null) {
			// look for class level
			return listValues(methodDoc.containingClass(), JAX_RS_PRODUCES, "value", options);
		}
		return methodLevel;
	}

	/**
	 * This gets a list of values from an annotation that uses a string array value, it supports getting it from a superclass method
	 * @param methodDoc The method doc
	 * @param qualifiedAnnotationType The FQN of the annotation
	 * @param annotationValueName The name of the value field of the annotation to use
	 * @param options The doclet options
	 * @return A list of values or null if none were found
	 */
	public static List<String> listInheritableValues(ExecutableMemberDoc methodDoc, String qualifiedAnnotationType, String annotationValueName,
			DocletOptions options) {
		List<String> result = null;
		while (result == null && methodDoc != null) {
			result = listValues(methodDoc, qualifiedAnnotationType, annotationValueName, options);
			methodDoc = getAncestorMethod(methodDoc);
		}
		return result;
	}

	/**
	 * This gets a list of values from an annotation that uses a string array value
	 * @param doc The method/field doc
	 * @param qualifiedAnnotationType The FQN of the annotation
	 * @param annotationValueName The name of the value field of the annotation to use
	 * @param options The doclet options
	 * @return A list of values or null if none were found
	 */
	public static List<String> listValues(com.sun.javadoc.ProgramElementDoc doc, String qualifiedAnnotationType, String annotationValueName,
			DocletOptions options) {
		AnnotationParser p = new AnnotationParser(doc, options);
		String[] vals = p.getAnnotationValues(qualifiedAnnotationType, annotationValueName);
		if (vals != null && vals.length > 0) {
			List<String> res = new ArrayList<String>(vals.length);
			for (String val : vals) {
				if (val != null && val.trim().length() > 0) {
					res.add(val.trim());
				}
			}
			if (!res.isEmpty()) {
				return res;
			}
		}
		return null;
	}

	/**
	 * This gets whether the given type is primitive
	 * @param type The type to check
	 * @param options The doclet options
	 * @return True if the given type is primitive
	 */
	public static boolean isPrimitive(String type, DocletOptions options) {
		if (type == null) {
			return false;
		}
		String[] simpleType = primitiveTypeOf(type, options);
		return simpleType != null && PRIMITIVES.contains(simpleType[0]);
	}

	/**
	 * This gets whether the given type is primitive
	 * @param type The type to check
	 * @param options The doclet options
	 * @return True if the given type is primitive
	 */
	public static boolean isPrimitive(Type type, DocletOptions options) {
		if (type == null) {
			return false;
		}
		String[] simpleType = primitiveTypeOf(type, options);
		return simpleType != null && PRIMITIVES.contains(simpleType[0]);
	}

	/**
	 * This gets whether the given type is a number
	 * @param type The type to check
	 * @param options The doclet options
	 * @return True if the given type is primitive
	 */
	public static boolean isNumber(Type type, DocletOptions options) {
		if (type == null) {
			return false;
		}
		String[] typeFormat = primitiveTypeOf(type, options);
		if (typeFormat != null) {
			String swaggerType = typeFormat[0];
			String format = typeFormat[1];

			if (swaggerType.equals("integer")) {
				return true;
			} else if (swaggerType.equals("number")) {
				return true;
			} else if (format != null && format.equals("byte")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * This gets whether the given method or an overridden method has any of the given tags
	 * @param methodDoc The method doc to get the tag value of
	 * @param matchTags The names of the tags to look for
	 * @return True if the method or an overridden method has any of the given tags
	 */
	public static boolean hasInheritableTag(ExecutableMemberDoc methodDoc, Collection<String> matchTags) {
		boolean result = false;
		while (!result && methodDoc != null) {
			result = hasTag(methodDoc, matchTags);
			methodDoc = getAncestorMethod(methodDoc);
		}
		return result;
	}

	/**
	 * This gets whether the given item has any of the given tags
	 * @param item The javadoc item
	 * @param matchTags The names of the tags to look for
	 * @return True if the item has any of the given tags
	 */
	public static boolean hasTag(com.sun.javadoc.ProgramElementDoc item, Collection<String> matchTags) {
		if (matchTags != null) {
			for (String matchTag : matchTags) {
				Tag[] tags = item.tags(matchTag);
				if (tags != null && tags.length > 0) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * This gets the parameter names for the given method
	 * @param method The method
	 * @return the names of the method parameters or an empty set if the method had none
	 */
	public static Set<String> getParamNames(ExecutableMemberDoc method) {
		Set<String> params = new HashSet<String>();
		for (Parameter parameter : method.parameters()) {
			params.add(parameter.name());
		}
		return params;
	}

	/**
	 * This gets params of the given method that have either any of the matching javadoc tags or annotations
	 * @param method The method
	 * @param params The pre-read params of the method, if null they will be read from the given method
	 * @param javadocTags Csv javadoc tags to look at
	 * @param annotations Annotations to look at
	 * @param options The doclet options
	 * @return A set of param names for the params that have either any of the matching javadoc tags or annotations
	 */
	public static Set<String> getMatchingParams(ExecutableMemberDoc method, Set<String> params, Collection<String> javadocTags, Collection<String> annotations,
			DocletOptions options) {

		Set<String> res = new HashSet<String>();

		// find params based on javadoc tags
		List<String> javaDocParams = getCsvParams(method, params, javadocTags, options);
		res.addAll(javaDocParams);

		// add on params that have one of the param annotations
		Set<String> annotationParams = getInheritableParametersWithAnnotation(method, annotations);
		res.addAll(annotationParams);

		return res;
	}

	private static Set<String> getInheritableParametersWithAnnotation(ExecutableMemberDoc methodDoc, Collection<String> annotations) {
		Set<String> result = new HashSet<String>();
		while (methodDoc != null) {
			Set<String> subResult = getParametersWithAnnotation(methodDoc, annotations);
			result.addAll(subResult);
			methodDoc = getAncestorMethod(methodDoc);
		}
		return result;
	}

	/**
	 * This gets the names of parameters that have any of the given annotations on them
	 * @param method The method
	 * @param annotations The annotations to look for
	 * @return A set of param names with the given annotations
	 */
	private static Set<String> getParametersWithAnnotation(ExecutableMemberDoc method, Collection<String> annotations) {
		Set<String> res = new HashSet<String>();
		for (Parameter p : method.parameters()) {
			for (AnnotationDesc annotation : p.annotations()) {
				String qName = annotation.annotationType().qualifiedTypeName();
				if (annotations.contains(qName)) {
					res.add(p.name());
				}
			}
		}
		return res;
	}

	@SuppressWarnings("javadoc")
	public static interface TypeFilter {

		boolean matches(Type t);
	}

	@SuppressWarnings("javadoc")
	public static class NumericTypeFilter implements TypeFilter {

		private final DocletOptions options;

		public NumericTypeFilter(DocletOptions options) {
			super();
			this.options = options;
		}

		public boolean matches(Type t) {
			return ParserHelper.isNumber(t, this.options);
		}

	}

	/**
	 * This gets the first met parameter with annotations from the method overriding hierarchy
	 * @param methodDoc The method
	 * @param paramIndex Parameter index
	 * @return A parameter from the method overriding hierarchy with annotations
	 */
	public static Parameter getParameterWithAnnotations(ExecutableMemberDoc methodDoc, int paramIndex) {
		final Parameter fallbackParameter = methodDoc.parameters()[paramIndex];
		Parameter parameter;
		boolean found;
		do {
			parameter = methodDoc.parameters()[paramIndex];
			found = (parameter.annotations() != null && parameter.annotations().length > 0);
			methodDoc = getAncestorMethod(methodDoc);
		} while (methodDoc != null && !found);
		return (found) ? parameter : fallbackParameter;
	}

	/**
	 * This gets the values of parameters from either javadoc tags or annotations
	 * @param method The method
	 * @param params The pre-read params of the method, if null they will be read from the given method
	 * @param matchTags The names of the javadoc tags to look for
	 * @param annotations The annotations to look for
	 * @param annotationTypes The types that the annotations should apply to
	 * @param options The doclet options (used for var replacement)
	 * @param valueKeys The attribute names to look for on the annotations as the value
	 * @return A set of param names with the given annotations
	 */
	public static Map<String, String> getParameterValues(ExecutableMemberDoc method, Set<String> params, Collection<String> matchTags,
			Collection<String> annotations, TypeFilter annotationTypes, DocletOptions options, String... valueKeys) {
		Map<String, String> res = new HashMap<String, String>();
		// first add values from javadoc tags
		Map<String, String> javadocVals = getMethodParamNameValuePairs(method, params, matchTags, options);
		res.putAll(javadocVals);
		// add values from annotations
		Map<String, String> annotationVals = getParameterValuesWithAnnotation(method, annotations, annotationTypes, options, valueKeys);
		res.putAll(annotationVals);
		return res;
	}

	/**
	 * This gets the values of annotations of parameters that have any of the given annotations on them
	 * @param methodDoc The method
	 * @param annotations The annotations to look for
	 * @param annotationTypes The types that the annotations should apply to
	 * @param options The doclet options (used for var replacement)
	 * @param valueKeys The attribute names to look for on the annotations as the value
	 * @return A set of param names with the given annotations
	 */
	public static Map<String, String> getParameterValuesWithAnnotation(ExecutableMemberDoc methodDoc, Collection<String> annotations,
			TypeFilter annotationTypes, DocletOptions options, String... valueKeys) {
		Map<String, String> res = new HashMap<String, String>();
		while (methodDoc != null) {
			for (Parameter p : methodDoc.parameters()) {
				String value = new AnnotationParser(p, options).getAnnotationValue(annotations, valueKeys);
				if (value != null && (annotationTypes == null || annotationTypes.matches(p.type()))) {
					res.put(p.name(), value);
				}
			}
			methodDoc = getAncestorMethod(methodDoc);
		}
		return res;
	}

	/**
	 * This gets a map of parameter name to value from a javadoc tag on a method.
	 * This validates that the names of the parameters in each NVP is an actual method parameter
	 * @param method The method
	 * @param params The pre-read params of the method, if null they will be read from the given method
	 * @param matchTags The names of the javadoc tags to look for
	 * @param options The doclet options
	 * @return a map of parameter name to value from a javadoc tag on a method or an empty map if none were found
	 */
	public static Map<String, String> getMethodParamNameValuePairs(ExecutableMemberDoc method, Set<String> params, Collection<String> matchTags,
			DocletOptions options) {

		if (params == null) {
			params = getParamNames(method);
		}

		// add name to value pairs from any matching javadoc tag on the method
		String value = getInheritableTagValue(method, matchTags, options);
		if (value != null) {
			String[] parts = value.split("\\s+");
			if (parts != null && parts.length > 0) {
				if (parts.length % 2 != 0) {
					throw new IllegalStateException(
							"Invalid javadoc parameter on method "
									+ method.name()
									+ " for tags: "
									+ matchTags
									+ ". The value had "
									+ parts.length
									+ " whitespace seperated parts when it was expected to have name value pairs for each parameter, e.g. the number of parts should have been even.");
				}
				Map<String, String> res = new HashMap<String, String>();
				for (int i = 0; i < parts.length; i += 2) {
					String name = parts[i];
					if (!params.contains(name)) {
						throw new IllegalStateException("Invalid javadoc parameter on method " + method.name() + " for tags: " + matchTags + ". The parameter "
								+ name + " is not the name of one of the method parameters.");
					}
					String val = parts[i + 1];
					res.put(name, val);
				}
				return res;
			}
		}

		return Collections.emptyMap();
	}

	/**
	 * This gets a map of parameter name to list of values from a javadoc tag on a method.
	 * This validates that the names of the parameters in each name to list is an actual method parameter
	 * @param method The method
	 * @param params The pre-read params of the method, if null they will be read from the given method
	 * @param matchTags The names of the javadoc tags to look for
	 * @param options The doclet options
	 * @return a map of parameter name to list of values from a javadoc tag on a method or an empty map if none were found
	 */
	public static Map<String, List<String>> getMethodParamNameValueLists(ExecutableMemberDoc method, Set<String> params, Collection<String> matchTags,
			DocletOptions options) {

		if (params == null) {
			params = getParamNames(method);
		}

		// add name to value pairs from any matching javadoc tag on the method
		String value = getInheritableTagValue(method, matchTags, options);
		if (value != null) {
			String[] parts = value.split("\\s+");
			if (parts != null && parts.length > 0) {
				if (parts.length % 2 != 0) {
					throw new IllegalStateException(
							"Invalid javadoc parameter on method "
									+ method.name()
									+ " for tags: "
									+ matchTags
									+ ". The value had "
									+ parts.length
									+ " whitespace seperated parts when it was expected to have name value pairs for each parameter, e.g. the number of parts should have been even.");
				}
				Map<String, List<String>> res = new HashMap<String, List<String>>();
				for (int i = 0; i < parts.length; i += 2) {
					String name = parts[i];
					if (!params.contains(name)) {
						throw new IllegalStateException("Invalid javadoc parameter on method " + method.name() + " for tags: " + matchTags + ". The parameter "
								+ name + " is not the name of one of the method parameters.");
					}
					String val = parts[i + 1];

					List<String> vals = res.get(name);
					if (vals == null) {
						vals = new ArrayList<String>();
						res.put(name, vals);
					}
					vals.add(val);
				}
				return res;
			}
		}

		return Collections.emptyMap();
	}

	/**
	 * This gets a list of parameter names from a method javadoc tag where the value of the tag is in the form
	 * paramName1,paramName2 ... paramNameN
	 * @param method The method
	 * @param params The pre-read params of the method, if null they will be read from the given method
	 * @param matchTags The names of the javadoc tags to look for
	 * @param options The doclet options
	 * @return The list of parameter names or an empty list if there were none
	 */
	public static List<String> getCsvParams(ExecutableMemberDoc method, Set<String> params, Collection<String> matchTags, DocletOptions options) {
		if (params == null) {
			params = getParamNames(method);
		}
		List<String> tagParams = getTagCsvValues(method, matchTags, options);
		// check each param is an actual param of the method
		if (tagParams != null) {
			for (String param : tagParams) {
				if (!params.contains(param)) {
					throw new IllegalStateException("Invalid javadoc parameter on method " + method.name() + " for tags: " + matchTags + ". The parameter "
							+ param + " is not the name of one of the method parameters.");
				}
			}
			return tagParams;
		}
		return Collections.emptyList();
	}

	/**
	 * This gets a csv javadoc tag value as a list of the values in the csv for the first matched tag.
	 * e.g. @myTag 1,2,3 would return a list of 1,2,3 assuming matchTags contained myTag
	 * @param item The javadoc item
	 * @param matchTags The tags to match
	 * @param options The doclet options
	 * @return The csv values of the first matching tags value or an empty list if there were none.
	 */
	public static List<String> getTagCsvValues(ExecutableMemberDoc item, Collection<String> matchTags, DocletOptions options) {
		String value = getInheritableTagValue(item, matchTags, options);
		if (value != null) {
			String[] vals = value.split(",");
			if (vals != null && vals.length > 0) {
				List<String> res = new ArrayList<String>();
				for (String val : vals) {
					if (val != null && val.trim().length() > 0) {
						res.add(options.replaceVars(val.trim()));
					}
				}
				return res.isEmpty() ? null : res;
			}
		}
		return Collections.emptyList();
	}

	/**
	 * This gets the first value on the given item (field or method) that matches either an annotation or a javadoc tag.
	 * This does NOT support method overriding
	 * @param item The javadoc item
	 * @param annotations The FQN of the annotations to check
	 * @param matchTags The collection of tag names of the tag to get a value of
	 * @param options The doclet options
	 * @param valueKeys The names of the attributes of the annotations to look at
	 * @return The value or null if none was found
	 */
	public static String getAnnotationOrTagValue(com.sun.javadoc.ProgramElementDoc item, Collection<String> annotations, Collection<String> matchTags,
			DocletOptions options, String... valueKeys) {

		// first check for an annotation value
		AnnotationParser p = new AnnotationParser(item, options);
		String annotationVal = p.getAnnotationValue(annotations, valueKeys);
		if (annotationVal != null) {
			return annotationVal;
		}

		// now check for a tag value
		return getTagValue(item, matchTags, options);
	}

	/**
	 * Resolves HttpMethod for the MethodDoc respecting the overriden methods
	 * @param methodDoc The method to be processed
	 * @return The resolved HttpMethod
	 */
	public static HttpMethod resolveMethodHttpMethod(ExecutableMemberDoc methodDoc) {
		HttpMethod result = null;
		while (result == null && methodDoc != null) {
			result = HttpMethod.fromMethod(methodDoc);
			methodDoc = getAncestorMethod(methodDoc);
		}
		return result;
	}

	/**
	 * This gets the first sentence tags of a method or its overridden ancestor method
	 * @param methodDoc The method
	 * @return The first sentence tag or null if there is none
	 */
	public static String getInheritableFirstSentenceTags(ExecutableMemberDoc methodDoc) {
		String result = null;
		while (result == null && methodDoc != null) {

			Tag[] fst = methodDoc.firstSentenceTags();
			if (fst != null && fst.length > 0) {
				StringBuilder sentences = new StringBuilder();
				for (Tag tag : fst) {
					sentences.append(tag.text());
				}
				String firstSentences = sentences.toString();
				result = firstSentences;
			}

			methodDoc = getAncestorMethod(methodDoc);
		}
		return result;
	}

	/**
	 * This gets the first sentence tags of a method or its overridden ancestor method
	 * @param methodDoc The method
	 * @return The first sentence tag or null if there is none
	 */
	public static String getInheritableCommentText(ExecutableMemberDoc methodDoc) {
		String result = null;
		while (result == null && methodDoc != null) {

			String commentText = methodDoc.commentText();
			if (commentText != null && !commentText.isEmpty()) {
				result = commentText;
			}

			methodDoc = getAncestorMethod(methodDoc);
		}
		return result;
	}

	/**
	 * Finds a methods definition on any ancestor, be it interface or class.
	 * @param execMethodDoc The method to get the inherited member of
	 * @return Null if there is no ancestor.
	 */
	public static ExecutableMemberDoc getAncestorMethod(ExecutableMemberDoc execMethodDoc) {

		if (execMethodDoc instanceof MethodDoc) {

			MethodDoc methodDoc = (MethodDoc) execMethodDoc;

			// only provides parent class overrides, but does not include interfaces
			if (methodDoc.overriddenMethod() != null) {
				return methodDoc.overriddenMethod();
			}

			// search for method on interfaces by looping through. I arbitrarily decided that
			// the first match wins in case there are multiple. One could consider joining the
			// various documentations...
			for (ClassDoc intf : methodDoc.containingClass().interfaces()) {
				MethodDoc intfMethod = findInterfaceMethod(intf, methodDoc);
				if (intfMethod != null) {
					return intfMethod;
				}
			}

		}
		return null;
	}

	/**
	 * Tries to find a method with the correct signature and name on an interface.
	 * @param object The class or interface on which you want to search for a method
	 * @param method The name and signature of the method you are searching for
	 * @return Null if no method matches.
	 */
	private static MethodDoc findInterfaceMethod(ClassDoc object, MethodDoc method) {
		for (MethodDoc intfMethod : object.methods()) {
			if (intfMethod.name().equals(method.name())) {
				if (intfMethod.flatSignature().equals(method.flatSignature())) {
					// xxx: do we need to consider more compllicated cases like contravariance?
					return intfMethod;
				}
			}
		}
		return null;
	}

	/**
	 * This gets values of any of the javadoc tags that are in the given collection from the method or overridden methods
	 * @param methodDoc The javadoc method to get the tags of
	 * @param matchTags The names of the tags to get
	 * @param options The doclet options
	 * @return A list of tag values or null if none were found
	 */
	public static List<String> getInheritableTagValues(ExecutableMemberDoc methodDoc, Collection<String> matchTags, DocletOptions options) {
		List<String> result = null;
		while (result == null && methodDoc != null) {
			result = getTagValues(methodDoc, matchTags, options);
			methodDoc = getAncestorMethod(methodDoc);
		}
		return result;
	}

	/**
	 * This gets an annotation value from the given class, it supports looking at super classes
	 * @param classDoc The class to look for the annotation
	 * @param matchTags The collection of tag names of the tag to get a value of
	 * @param options The doclet options
	 * @return The value of the annotation or null if none was found
	 */
	public static List<String> getInheritableTagValues(ClassDoc classDoc, Collection<String> matchTags, DocletOptions options) {
		ClassDoc currentClassDoc = classDoc;
		while (currentClassDoc != null) {
			List<String> values = getTagValues(currentClassDoc, matchTags, options);

			if (values != null) {
				return values;
			}

			currentClassDoc = currentClassDoc.superclass();
			// ignore parent object class
			if (!ParserHelper.hasAncestor(currentClassDoc)) {
				break;
			}
		}
		return null;
	}

	/**
	 * This gets values of any of the javadoc tags that are in the given collection
	 * @param item The javadoc item to get the tags of
	 * @param matchTags The names of the tags to get
	 * @param options The doclet options
	 * @return A list of tag values or null if none were found
	 */
	public static List<String> getTagValues(com.sun.javadoc.ProgramElementDoc item, Collection<String> matchTags, DocletOptions options) {
		List<String> res = null;
		if (matchTags != null) {
			Tag[] tags = item.tags();
			if (tags != null && tags.length > 0) {
				for (Tag tag : tags) {
					if (matchTags.contains(tag.name().substring(1))) {
						if (res == null) {
							res = new ArrayList<String>();
						}
						String customValue = tag.text().trim();
						if (customValue.length() > 0) {
							res.add(options.replaceVars(customValue));
						}
					}
				}
			}
		}
		return res == null || res.isEmpty() ? null : res;
	}

	/**
	 * This gets the value of the first tag found from the given MethodDoc respecting the overriden methods
	 * @param methodDoc The method doc to get the tag value of
	 * @param matchTags The collection of tag names of the tag to get a value of
	 * @param options The doclet options
	 * @return The value of the first tag found with the name in the given collection or null if either the tag
	 *         was not present or had no value
	 */
	public static String getInheritableTagValue(ExecutableMemberDoc methodDoc, Collection<String> matchTags, DocletOptions options) {
		String result = null;
		while (result == null && methodDoc != null) {
			result = getTagValue(methodDoc, matchTags, options);
			methodDoc = getAncestorMethod(methodDoc);
		}
		return result;
	}

	/**
	 * This gets an annotation value from the given class, it supports looking at super classes
	 * @param classDoc The class to look for the annotation
	 * @param matchTags The collection of tag names of the tag to get a value of
	 * @param options The doclet options
	 * @return The value of the annotation or null if none was found
	 */
	public static String getInheritableTagValue(ClassDoc classDoc, Collection<String> matchTags, DocletOptions options) {
		ClassDoc currentClassDoc = classDoc;
		while (currentClassDoc != null) {
			String value = getTagValue(currentClassDoc, matchTags, options);

			if (value != null) {
				return value;
			}

			currentClassDoc = currentClassDoc.superclass();
			// ignore parent object class
			if (!ParserHelper.hasAncestor(currentClassDoc)) {
				break;
			}
		}
		return null;
	}

	/**
	 * This gets the value of the first tag found from the given collection of tag names
	 * @param item The item to get the tag value of
	 * @param matchTags The collection of tag names of the tag to get a value of
	 * @param options The doclet options
	 * @return The value of the first tag found with the name in the given collection or null if either the tag
	 *         was not present or had no value
	 */
	public static String getTagValue(com.sun.javadoc.ProgramElementDoc item, Collection<String> matchTags, DocletOptions options) {
		String customValue = null;
		if (matchTags != null) {
			for (String matchTag : matchTags) {
				Tag[] tags = item.tags(matchTag);
				if (tags != null && tags.length > 0) {
					customValue = tags[0].text().trim();
					if (customValue.length() == 0) {
						customValue = null;
					}
					break;
				}
			}
		}
		return options.replaceVars(customValue);
	}

	private static final Set<String> DEPRECATED_TAGS = new HashSet<String>();
	private static final Set<String> DEPRECATED_ANNOTATIONS = new HashSet<String>();
	static {
		DEPRECATED_TAGS.add("deprecated");
		DEPRECATED_TAGS.add("Deprecated");
		DEPRECATED_ANNOTATIONS.add("java.lang.Deprecated");
	}

	/**
	 * this adds custom annotations to the list of Deprecated annotations
	 * @param customDeprecatedAnnotations The string list of fully qualified annotations to be added as deprecated
     */
	public static void addCustomDeprecatedAnnotation(List<String> customDeprecatedAnnotations) {
		DEPRECATED_ANNOTATIONS.addAll(customDeprecatedAnnotations);
	}

    /**
	 * This gets whether the given method or an overridden method has any of the given annotations
	 * @param methodDoc The method doc to check
	 * @param annotations the annotations to check
	 * @param options The doclet options
	 * @return True if the method or an overridden method has one of the given annotations
	 */
	public static boolean hasInheritableAnnotation(ExecutableMemberDoc methodDoc, Collection<String> annotations, DocletOptions options) {
		boolean result = false;
		while (!result && methodDoc != null) {
			result = hasAnnotation(methodDoc, annotations, options);
			methodDoc = getAncestorMethod(methodDoc);
		}
		return result;
	}

	/**
	 * This gets whether the given item has one of the given annotations
	 * @param item The field/method to check
	 * @param annotations the annotations to check
	 * @param options The doclet options
	 * @return True if the item has one of the given annotations
	 */
	public static boolean hasAnnotation(com.sun.javadoc.ProgramElementDoc item, Collection<String> annotations, DocletOptions options) {
		AnnotationParser p = new AnnotationParser(item, options);
		for (String annotation : annotations) {
			if (p.isAnnotatedBy(annotation)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * This gets whether the given parameter has one of the given annotations
	 * @param item The field/method to check
	 * @param annotations the annotations to check
	 * @param options The doclet options
	 * @return True if the parameter has one of the given annotations
	 */
	public static boolean hasAnnotation(Parameter item, Collection<String> annotations, DocletOptions options) {
		AnnotationParser p = new AnnotationParser(item, options);
		for (String annotation : annotations) {
			if (p.isAnnotatedBy(annotation)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * This gets whether the given parameter has an annotation whose FQN begins with one of the given prefixes
	 * @param item The field/method to check
	 * @param prefixes the prefixes to check for
	 * @param options The doclet options
	 * @return True if the parameter has an annotation whose FQN begins with one of the given prefixes
	 */
	public static boolean hasAnnotationWithPrefix(Parameter item, Collection<String> prefixes, DocletOptions options) {
		AnnotationParser p = new AnnotationParser(item, options);
		for (String prefix : prefixes) {
			if (p.isAnnotatedByPrefix(prefix)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * This gets whether the given parameter has a JAXRS annotation
	 * @param item The parameter
	 * @param options The doclet options
	 * @return True if the parameter has the given annotation
	 */
	public static boolean hasJaxRsAnnotation(Parameter item, DocletOptions options) {
		return hasAnnotationWithPrefix(item, JAX_RS_PREFIXES, options);
	}

	/**
	 * This gets whether the given item is marked as deprecated either via a javadoc tag
	 * or an annotation, this supports looking at overridden methods
	 * @param item The item to check
	 * @param options The doclet options
	 * @return True if the item is flagged as deprecated
	 */
	public static boolean isInheritableDeprecated(com.sun.javadoc.MethodDoc item, DocletOptions options) {
		if (hasInheritableTag(item, DEPRECATED_TAGS)) {
			return true;
		}
		return hasInheritableAnnotation(item, DEPRECATED_ANNOTATIONS, options);
	}

	/**
	 * This gets whether the given item is marked as deprecated either via a javadoc tag
	 * or an annotation
	 * @param item The item to check
	 * @param options The doclet options
	 * @return True if the item is flagged as deprecated
	 */
	public static boolean isDeprecated(com.sun.javadoc.ProgramElementDoc item, DocletOptions options) {
		if (hasTag(item, DEPRECATED_TAGS)) {
			return true;
		}
		return hasAnnotation(item, DEPRECATED_ANNOTATIONS, options);
	}

	/**
	 * This gets whether the given parameter is marked as deprecated either via a javadoc tag
	 * or an annotation
	 * @param parameter The parameter to check
	 * @param options The doclet options
	 * @return True if the parameter is flagged as deprecated
	 */
	public static boolean isDeprecated(com.sun.javadoc.Parameter parameter, DocletOptions options) {
		return hasAnnotation(parameter, DEPRECATED_ANNOTATIONS, options);
	}

	/**
	 * This builds a map of FQN to type for all see annotations
	 * on the given items javadoc
	 * @param item The item to get the see types of
	 * @return A map of see types or an empty map if there were no see tags.
	 */
	public static Map<String, Type> readSeeTypes(com.sun.javadoc.ProgramElementDoc item) {
		Map<String, Type> types = new HashMap<String, Type>();
		SeeTag[] seeTags = item.seeTags();
		if (seeTags != null) {
			for (SeeTag seeTag : seeTags) {
				Type type = seeTag.referencedClass();
				types.put(seeTag.referencedClassName(), type);
			}
		}
		return types;
	}

	static final Map<String, String> PRIMITIVE_TO_CLASS = new HashMap<String, String>();
	static {
		PRIMITIVE_TO_CLASS.put("int", java.lang.Integer.class.getName());
		PRIMITIVE_TO_CLASS.put("boolean", java.lang.Boolean.class.getName());
		PRIMITIVE_TO_CLASS.put("float", java.lang.Float.class.getName());
		PRIMITIVE_TO_CLASS.put("double", java.lang.Double.class.getName());
		PRIMITIVE_TO_CLASS.put("char", java.lang.Character.class.getName());
		PRIMITIVE_TO_CLASS.put("long", java.lang.Long.class.getName());
		PRIMITIVE_TO_CLASS.put("byte", java.lang.Byte.class.getName());
		PRIMITIVE_TO_CLASS.put("string", java.lang.String.class.getName());

		PRIMITIVE_TO_CLASS.put(java.math.BigDecimal.class.getName(), java.lang.Double.class.getName());
		PRIMITIVE_TO_CLASS.put(java.math.BigInteger.class.getName(), java.lang.Long.class.getName());
	}

	/**
	 * This finds a model class by the given name
	 * @param classes The model classes
	 * @param qualifiedClassName The FQN of the class
	 * @return {@link ClassDoc} found among all classes processed by the doclet based on a given <code>qualifiedClassName</code>; <code>null</code> if not found
	 */
	public static ClassDoc findModel(Collection<ClassDoc> classes, String qualifiedClassName) {
		if (classes != null && qualifiedClassName != null) {
			// map primitives to their class equiv
			if (PRIMITIVE_TO_CLASS.containsKey(qualifiedClassName)) {
				qualifiedClassName = PRIMITIVE_TO_CLASS.get(qualifiedClassName);
			}
			for (ClassDoc cls : classes) {
				if (qualifiedClassName.equals(cls.qualifiedName())) {
					return cls;
				}
			}
		}
		return null;
	}

	/**
	 * This generates a file name to use for a resource with the given resource path.
	 * It ensures that any non filename safe characters are removed
	 * For example /api/test/{id:[0-9]+} will become api_test_id
	 * @param resourcePath The path to sanitize
	 * @return The resource path sanitized
	 */
	public static String generateResourceFilename(String resourcePath) {

		// first remove regex expressions so we end up for example
		// instead of /api/test/{id:[0-9]+} with /api/test/{id}
		String sanitized = sanitizePath(resourcePath)
		// now replace / and {} with underscores
				.replace("{", "_").replace("}", "").replace("/", "_")
				// and finally swap multiple underscores with a single one
				.replaceAll("[_]+", "_");

		// remove any trailing or leading underscores
		if (sanitized.endsWith("_")) {
			sanitized = sanitized.substring(0, sanitized.length() - 1);
		}
		if (sanitized.startsWith("_")) {
			sanitized = sanitized.substring(1);
		}

		return sanitized;

	}

	/**
	 * This sanitizes an API path. It handles removing regex path expressions
	 * e.g instead of /api/test/{id:[0-9]+} it will return /api/test/{id}
	 * @param apiPath The api path to sanitize
	 * @return The sanitized path
	 */
	public static String sanitizePath(String apiPath) {
		return apiPath.replaceAll("[: ]+.*?}", "}");
	}

	/**
	 * This trims specific characters from the start of the given string
	 * @param str The string to trim
	 * @param trimChars The characters to trim from the string
	 * @return The string with any of the trimable characters removed from the start of the string
	 */
	public static String trimLeadingChars(String str, char... trimChars) {
		if (str == null || str.trim().isEmpty()) {
			return str;
		}
		StringBuilder newStr = new StringBuilder();
		boolean foundNonTrimChar = false;
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			if (foundNonTrimChar) {
				newStr.append(c);
			} else {
				if (Character.isWhitespace(c)) {
					// trim
				} else {
					// see if a non trim char, if so add it and set flag
					boolean isTrimChar = false;
					for (char trimC : trimChars) {
						if (c == trimC) {
							isTrimChar = true;
							break;
						}
					}
					if (!isTrimChar) {
						foundNonTrimChar = true;
						newStr.append(c);
					}
				}
			}
		}
		return newStr.length() == 0 ? null : newStr.toString().trim();
	}

}

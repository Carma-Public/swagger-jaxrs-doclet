package com.carma.swagger.doclet.translator;

import static com.carma.swagger.doclet.translator.Translator.OptionalName.ignored;
import static com.carma.swagger.doclet.translator.Translator.OptionalName.presentOrMissing;

import java.util.HashMap;
import java.util.Map;

import com.carma.swagger.doclet.DocletOptions;
import com.carma.swagger.doclet.parser.AnnotationParser;
import com.carma.swagger.doclet.parser.ParserHelper;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.ProgramElementDoc;
import com.sun.javadoc.Type;

/**
 * The AnnotationAwareTranslator represents a translator that can source the names from various
 * annotations like jaxb and json ones.
 * @version $Id$
 */
public class AnnotationAwareTranslator implements Translator {

	private final Map<QualifiedType, OptionalName> typeNameCache;

	private String ignore;
	private String element;
	private String elementProperty;
	private String rootElement;
	private String rootElementProperty;
	private DocletOptions options;

	/**
	 * This creates a AnnotationAwareTranslator that uses the given doclet options
	 * @param options the doclet options
	 */
	public AnnotationAwareTranslator(DocletOptions options) {
		this.options = options;
		this.typeNameCache = new HashMap<QualifiedType, OptionalName>();
	}

	/**
	 * This adds an ignore annotation to this translator
	 * @param qualifiedAnnotationType The FQN of the annotation that if present means a field is ignored e.g. JsonIgnore, XmlTransient
	 * @return this
	 */
	public AnnotationAwareTranslator ignore(String qualifiedAnnotationType) {
		this.ignore = qualifiedAnnotationType;
		return this;
	}

	/**
	 * This adds an element annotation to this translator, these ones are used for field and method names
	 * @param qualifiedAnnotationType The FQN of the annotation that if present means a field/method uses this annotation's property for the name
	 * @param property The property name of the annotation to use for the field/method name
	 * @return this
	 */
	public AnnotationAwareTranslator element(String qualifiedAnnotationType, String property) {
		this.element = qualifiedAnnotationType;
		this.elementProperty = property;
		return this;
	}

	/**
	 * This adds an root annotation to this translator, these ones are used for class type names
	 * @param qualifiedAnnotationType The FQN of the annotation that if present means a class type uses uses this annotation's property for the name
	 * @param property The property name of the annotation to use for the class type name
	 * @return this
	 */
	public AnnotationAwareTranslator rootElement(String qualifiedAnnotationType, String property) {
		this.rootElement = qualifiedAnnotationType;
		this.rootElementProperty = property;
		return this;
	}

	/**
	 * {@inheritDoc}
	 * @see com.carma.swagger.doclet.translator.Translator#typeName(com.sun.javadoc.Type, com.sun.javadoc.ClassDoc[])
	 */
	public OptionalName typeName(Type type, ClassDoc[] views) {
		OptionalName name = typeName(type);

		if (views != null && views.length > 0 && name != null && name.isPresent() && !ParserHelper.isPrimitive(type, this.options)) {
			StringBuilder nameWithViews = new StringBuilder(name.value()).append("-");
			for (ClassDoc view : views) {
				nameWithViews.append(view.name());
			}
			name = presentOrMissing(nameWithViews.toString(), name.getFormat());
		}
		return name;
	}

	/**
	 * {@inheritDoc}
	 * @see com.carma.swagger.doclet.translator.Translator#parameterTypeName(boolean, com.sun.javadoc.Parameter, com.sun.javadoc.Type)
	 */
	public OptionalName parameterTypeName(boolean multipart, Parameter parameter, Type paramType) {
		if (paramType == null) {
			paramType = parameter.type();
		}
		QualifiedType cacheKey = new QualifiedType(String.valueOf(multipart), paramType);

		if (this.typeNameCache.containsKey(cacheKey)) {
			return this.typeNameCache.get(cacheKey);
		}

		// look for File data types
		if (multipart) {
			boolean isFileDataType = ParserHelper.isFileParameterDataType(parameter, this.options);
			if (isFileDataType) {
				OptionalName res = presentOrMissing("File");
				this.typeNameCache.put(cacheKey, res);
				return res;
			}
		}

		return typeName(cacheKey);
	}

	/**
	 * {@inheritDoc}
	 * @see com.carma.swagger.doclet.translator.Translator#typeName(com.sun.javadoc.Type)
	 */
	public OptionalName typeName(Type type) {
		return typeName(new QualifiedType("typeName", type));
	}

	private OptionalName typeName(QualifiedType type) {
		if (this.typeNameCache.containsKey(type)) {
			return this.typeNameCache.get(type);
		}

		if (ParserHelper.isPrimitive(type.getType(), this.options) || type.getType().asClassDoc() == null) {
			return null;
		}

		OptionalName name = null;
		if (ParserHelper.isArray(type.getType())) {
			name = presentOrMissing("array");
		} else {
			name = nameFor(this.rootElement, this.rootElementProperty, type.getType().asClassDoc(), false);
		}
		this.typeNameCache.put(type, name);
		return name;
	}

	/**
	 * {@inheritDoc}
	 * @see com.carma.swagger.doclet.translator.Translator#fieldName(com.sun.javadoc.FieldDoc)
	 */
	public OptionalName fieldName(FieldDoc field) {
		return nameFor(this.element, this.elementProperty, field, true);
	}

	/**
	 * {@inheritDoc}
	 * @see com.carma.swagger.doclet.translator.Translator#methodName(com.sun.javadoc.MethodDoc)
	 */
	public OptionalName methodName(MethodDoc method) {
		return nameFor(this.element, this.elementProperty, method, true);
	}

	private OptionalName nameFor(String annotation, String property, ProgramElementDoc doc, boolean processIgnore) {
		AnnotationParser element = new AnnotationParser(doc, this.options);
		if (processIgnore && element.isAnnotatedBy(this.ignore)) {
			return ignored();
		}
		String name = element.getAnnotationValue(annotation, property);
		return presentOrMissing(name);
	}

}

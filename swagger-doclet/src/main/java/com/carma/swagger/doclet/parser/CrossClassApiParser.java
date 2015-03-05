package com.carma.swagger.doclet.parser;

import static com.carma.swagger.doclet.parser.ParserHelper.parsePath;
import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.collect.Maps.uniqueIndex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.carma.swagger.doclet.DocletOptions;
import com.carma.swagger.doclet.model.Api;
import com.carma.swagger.doclet.model.ApiDeclaration;
import com.carma.swagger.doclet.model.Method;
import com.carma.swagger.doclet.model.Model;
import com.carma.swagger.doclet.model.Operation;
import com.google.common.base.Function;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.Type;

/**
 * The CrossClassApiParser represents an api class parser that supports ApiDeclaration being
 * spread across multiple resource classes.
 * @version $Id$
 * @author conor.roche
 */
public class CrossClassApiParser {

	private final DocletOptions options;
	private final ClassDoc classDoc;
	private final Collection<ClassDoc> classes;
	private final String rootPath;
	private final String swaggerVersion;
	private final String apiVersion;
	private final String basePath;

	private final Method parentMethod;
	private final Queue<CrossClassApiParser> subResourceParsers;
	private final Collection<ClassDoc> typeClasses;

	/**
	 * This creates a CrossClassApiParser for top level parsing
	 * @param options The options for parsing
	 * @param classDoc The class doc
	 * @param classes The doclet classes to document
	 * @param typeClasses Extra type classes that can be used as generic parameters
	 * @param subResourceClasses Sub resource doclet classes
	 * @param swaggerVersion Swagger version
	 * @param apiVersion Overall API version
	 * @param basePath Overall base path
	 */
	public CrossClassApiParser(DocletOptions options, ClassDoc classDoc, Collection<ClassDoc> classes, Queue<CrossClassApiParser> subResourceParsers,
			Collection<ClassDoc> typeClasses, String swaggerVersion, String apiVersion, String basePath) {
		this(options, classDoc, classes, subResourceParsers, typeClasses, swaggerVersion, apiVersion, basePath, null, null);
	}

	/**
	 * This creates a CrossClassApiParser for parsing a subresource
	 * @param options The options for parsing
	 * @param classDoc The class doc
	 * @param classes The doclet classes to document
	 * @param typeClasses Extra type classes that can be used as generic parameters
	 * @param subResourceClasses Sub resource doclet classes
	 * @param swaggerVersion Swagger version
	 * @param apiVersion Overall API version
	 * @param basePath Overall base path
	 * @param parentMethod The parent method that "owns" this sub resource
	 * @param parentResourcePath The parent resource path
	 */
	public CrossClassApiParser(DocletOptions options, ClassDoc classDoc, Collection<ClassDoc> classes, Queue<CrossClassApiParser> subResourceParsers,
			Collection<ClassDoc> typeClasses, String swaggerVersion, String apiVersion, String basePath, Method parentMethod, String parentResourcePath) {
		super();
		this.options = options;
		this.classDoc = classDoc;
		this.classes = classes;
		this.typeClasses = typeClasses;
		this.subResourceParsers = subResourceParsers;
		String path = parsePath(classDoc, options);
		this.rootPath = parentResourcePath == null ? path : parentResourcePath + firstNonNull(path, "");
		this.swaggerVersion = swaggerVersion;
		this.apiVersion = apiVersion;
		this.basePath = basePath;
		this.parentMethod = parentMethod;
	}

	/**
	 * This gets the root jaxrs path of the api resource class
	 * @return The root path
	 */
	public String getRootPath() {
		return this.rootPath;
	}
	
	public ClassDoc getClassDoc() {
	    return this.classDoc;
	}

	/**
	 * This parses the api declarations from the resource classes of the api
	 * @param declarations The map of resource name to declaration which will be added to
	 */
	public void parse(Map<String, ApiDeclaration> declarations) {

	    Queue<ClassDoc> classDocs = new LinkedList<ClassDoc>();
		for (ClassDoc currentClassDoc = this.classDoc; currentClassDoc != null; currentClassDoc = classDocs.poll()) {

			// read default error type for class
			String defaultErrorTypeClass = ParserHelper.getTagValue(currentClassDoc, this.options.getDefaultErrorTypeTags(), this.options);
			Type defaultErrorType = ParserHelper.findModel(this.classes, defaultErrorTypeClass);

			Set<Model> classModels = new HashSet<Model>();
			if (this.options.isParseModels() && defaultErrorType != null) {
				classModels.addAll(new ApiModelParser(this.options, this.options.getTranslator(), defaultErrorType).parse());
			}
			
			if (this.rootPath != null || this.parentMethod != null) {
				for (MethodDoc method : currentClassDoc.methods()) {
					ApiMethodParser methodParser = this.parentMethod == null ? new ApiMethodParser(this.options, this.rootPath, method, this.classes,
							this.typeClasses, defaultErrorTypeClass) : new ApiMethodParser(this.options, this.parentMethod, method, this.classes,
							this.typeClasses, defaultErrorTypeClass);

					Method parsedMethod = methodParser.parse();
					if (parsedMethod == null) {
					    continue;
					}

					// see which resource path to use for the method, if its got a resourceTag then use that
					// otherwise use the root path
					String resourcePath = parsedMethod.getPath();

					if (parsedMethod.isSubResource()) {
					    ClassDoc subResourceClassDoc = ParserHelper.lookUpClassDoc(method.returnType(), this.classes);
					    if (subResourceClassDoc != null) {
					        // recursively parse the sub-resource class
					        CrossClassApiParser subResourceParser = new CrossClassApiParser(this.options, subResourceClassDoc, this.classes,
					            this.subResourceParsers, this.typeClasses, this.swaggerVersion, this.apiVersion, this.basePath, parsedMethod, resourcePath);
					        this.subResourceParsers.add(subResourceParser);
					    }
					    continue;
					}

					ApiDeclaration declaration = declarations.get(resourcePath);
					if (declaration == null) {
					    declaration = new ApiDeclaration(this.swaggerVersion, this.apiVersion, this.basePath, resourcePath, null, null, Integer.MAX_VALUE, null);
					    declaration.setApis(new ArrayList<Api>());
					    declaration.setModels(new HashMap<String, Model>());
					    declarations.put(resourcePath, declaration);
					}

					// look for a priority tag for the resource listing and set on the resource if the resource hasn't had one set
					String classResourcePriority = ParserHelper.getTagValue(currentClassDoc, this.options.getResourcePriorityTags(), this.options);
					setApiPriority(classResourcePriority, method, currentClassDoc, declaration);

					// look for a method level description tag for the resource listing and set on the resource if the resource hasn't had one set
					String classResourceDescription = ParserHelper.getTagValue(currentClassDoc, this.options.getResourceDescriptionTags(), this.options);
					setApiDeclarationDescription(classResourceDescription, method, declaration);

					// find api this method should be added to
					addMethod(method, parsedMethod, declaration);

					// add models
					Set<Model> methodModels = methodParser.models();
					Map<String, Model> idToModels = addApiModels(classModels, methodModels, method);
					declaration.getModels().putAll(idToModels);
				}
			}
			for (ClassDoc classDoc : currentClassDoc.interfaces()) {
			    classDocs.add(classDoc);
			}
			currentClassDoc = currentClassDoc.superclass();
			// ignore parent object class
			if (ParserHelper.hasAncestor(currentClassDoc)) {
				classDocs.add(currentClassDoc);
			}
		}

	}

	private Map<String, Model> addApiModels(Set<Model> classModels, Set<Model> methodModels, MethodDoc method) {
		methodModels.addAll(classModels);
		Map<String, Model> idToModels = Collections.emptyMap();
		try {
			idToModels = uniqueIndex(methodModels, new Function<Model, String>() {

				public String apply(Model model) {
					return model.getId();
				}
			});
		} catch (Exception ex) {
			throw new IllegalStateException("dupe models, method : " + method + ", models: " + methodModels, ex);
		}
		return idToModels;
	}

	private void setApiPriority(String classResourcePriority, MethodDoc method, ClassDoc currentClassDoc, ApiDeclaration declaration) {
		int priorityVal = Integer.MAX_VALUE;
		String priority = ParserHelper.getTagValue(method, this.options.getResourcePriorityTags(), this.options);
		if (priority != null) {
			priorityVal = Integer.parseInt(priority);
		} else if (classResourcePriority != null) {
			// set from the class
			priorityVal = Integer.parseInt(classResourcePriority);
		}

		if (priorityVal != Integer.MAX_VALUE && declaration.getPriority() == Integer.MAX_VALUE) {
			declaration.setPriority(priorityVal);
		}
	}

	private void setApiDeclarationDescription(String classResourceDescription, MethodDoc method, ApiDeclaration declaration) {
		String description = ParserHelper.getTagValue(method, this.options.getResourceDescriptionTags(), this.options);
		if (description == null) {
			description = classResourceDescription;
		}
		if (description != null && declaration.getDescription() == null) {
			declaration.setDescription(this.options.replaceVars(description));
		}
	}

	private void addMethod(MethodDoc method, Method parsedMethod, ApiDeclaration declaration) {
		Api methodApi = null;
		for (Api api : declaration.getApis()) {
			if (parsedMethod.getPath().equals(api.getPath())) {
				methodApi = api;
				break;
			}
		}

		// read api level description
		String apiDescription = ParserHelper.getTagValue(method, this.options.getApiDescriptionTags(), this.options);

		if (methodApi == null) {
			methodApi = new Api(parsedMethod.getPath(), this.options.replaceVars(apiDescription), new ArrayList<Operation>());
			declaration.getApis().add(methodApi);
		} else if (methodApi.getDescription() == null && apiDescription != null) {
			methodApi.setDescription(apiDescription);
		}

		methodApi.getOperations().add(new Operation(parsedMethod));
	}

}

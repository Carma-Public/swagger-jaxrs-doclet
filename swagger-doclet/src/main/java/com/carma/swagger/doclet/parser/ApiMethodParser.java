package com.carma.swagger.doclet.parser;

import static com.google.common.base.Objects.firstNonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.MediaType;

import com.carma.swagger.doclet.DocletOptions;
import com.carma.swagger.doclet.model.ApiParameter;
import com.carma.swagger.doclet.model.ApiResponseMessage;
import com.carma.swagger.doclet.model.HttpMethod;
import com.carma.swagger.doclet.model.Method;
import com.carma.swagger.doclet.model.Model;
import com.carma.swagger.doclet.model.Oauth2Scope;
import com.carma.swagger.doclet.model.OperationAuthorizations;
import com.carma.swagger.doclet.model.Property;
import com.carma.swagger.doclet.translator.Translator;
import com.carma.swagger.doclet.translator.Translator.OptionalName;
import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.Type;
import com.sun.javadoc.TypeVariable;

/**
 * The ApiMethodParser represents a parser for resource methods
 * @version $Id$
 */
public class ApiMethodParser {

	private static final Pattern GENERIC_RESPONSE_PATTERN = Pattern.compile("(.*)<(.*)>");

	// pattern that can match a code, a description and an optional response model type
	private static final Pattern[] RESPONSE_MESSAGE_PATTERNS = new Pattern[] { Pattern.compile("(\\d+)([^\\d`]+)(`.*)?") };

	private Method parentMethod;
	private String parentPath;

	private final DocletOptions options;
	private final Translator translator;
	private final MethodDoc methodDoc;
	private final Set<Model> models;
	private final HttpMethod httpMethod;
	private final Collection<ClassDoc> classes;
	private final String classDefaultErrorType;
	private final String methodDefaultErrorType;

	/**
	 * This creates a ApiMethodParser
	 * @param options
	 * @param parentPath
	 * @param methodDoc
	 * @param classes
	 * @param classDefaultErrorType
	 */
	public ApiMethodParser(DocletOptions options, String parentPath, MethodDoc methodDoc, Collection<ClassDoc> classes, String classDefaultErrorType) {
		this.options = options;
		this.translator = options.getTranslator();
		this.parentPath = parentPath;
		this.methodDoc = methodDoc;
		this.models = new LinkedHashSet<Model>();
		this.httpMethod = ParserHelper.resolveMethodHttpMethod(methodDoc);
		this.parentMethod = null;
		this.classDefaultErrorType = classDefaultErrorType;
		this.methodDefaultErrorType = ParserHelper.getInheritableTagValue(methodDoc, options.getDefaultErrorTypeTags(), options);
		this.classes = classes;
	}

	/**
	 * This creates a ApiMethodParser
	 * @param options
	 * @param parentMethod
	 * @param methodDoc
	 * @param classes
	 * @param classDefaultErrorType
	 */
	public ApiMethodParser(DocletOptions options, Method parentMethod, MethodDoc methodDoc, Collection<ClassDoc> classes, String classDefaultErrorType) {
		this(options, parentMethod.getPath(), methodDoc, classes, classDefaultErrorType);

		this.parentPath = parentMethod.getPath();
		this.parentMethod = parentMethod;

	}

	/**
	 * This parses a javadoc method doc and builds a pojo representation of it.
	 * @return The method with appropriate data set
	 */
	public Method parse() {
		String methodPath = ParserHelper.resolveMethodPath(this.methodDoc, this.options);
		if (this.httpMethod == null && methodPath.isEmpty()) {
			if (this.options.isLogDebug()) {
				System.out.println("skipping method: " + this.methodDoc.name() + " as it has neither @Path nor a http method annotation");
			}
			return null;
		}

		// check if deprecated and exclude if set to do so
		boolean deprecated = false;
		if (ParserHelper.isInheritableDeprecated(this.methodDoc, this.options)) {
			if (this.options.isExcludeDeprecatedOperations()) {
				if (this.options.isLogDebug()) {
					System.out.println("skipping method: " + this.methodDoc.name() + " as it is deprecated and configuration excludes deprecated methods");
				}
				return null;
			}
			deprecated = true;
		}

		// exclude if it has exclusion tags/annotations
		if (ParserHelper.hasInheritableTag(this.methodDoc, this.options.getExcludeOperationTags())) {
			if (this.options.isLogDebug()) {
				System.out.println("skipping method: " + this.methodDoc.name() + " as it has an exclusion tag");
			}
			return null;
		}
		if (ParserHelper.hasInheritableAnnotation(this.methodDoc, this.options.getExcludeOperationAnnotations(), this.options)) {
			if (this.options.isLogDebug()) {
				System.out.println("skipping method: " + this.methodDoc.name() + " as it has an exclusion annotation");
			}
			return null;
		}

		String path = this.parentPath + methodPath;
                if ("".equals(path)) {
                    path = "/";
                }

		// build params
		List<ApiParameter> parameters = this.generateParameters();

		ClassDoc[] viewClasses = ParserHelper.getInheritableJsonViews(this.methodDoc, this.options);

		// build response messages
		List<ApiResponseMessage> responseMessages = generateResponseMessages(viewClasses);

		// ************************************
		// Return type
		// ************************************
		Type returnType = this.methodDoc.returnType();
		// first check if its a wrapper type and if so replace with the wrapped type
		returnType = firstNonNull(ApiModelParser.getReturnType(this.options, returnType), returnType);

		OptionalName returnTypeOName = this.translator.typeName(returnType, this.options.isUseFullModelIds());

		String returnTypeName = returnTypeOName.value();
		String returnTypeFormat = returnTypeOName.getFormat();

		Type modelType = returnType;

		// now see if it is a collection if so the return type will be array and the
		// containerOf will be added to the model

		String returnTypeItemsRef = null;
		String returnTypeItemsType = null;
		String returnTypeItemsFormat = null;
		List<String> returnTypeItemsAllowableValues = null;
		Type containerOf = ParserHelper.getContainerType(returnType, null, this.classes);

		Map<String, Type> varsToTypes = new HashMap<String, Type>();

		// look for a custom return type, this is useful where we return a jaxrs Response in the method signature
		// but typically return a different object in its entity (such as for a 201 created response)
		String customReturnTypeName = ParserHelper.getInheritableTagValue(this.methodDoc, this.options.getResponseTypeTags(), this.options);
		NameToType nameToType = readCustomReturnType(customReturnTypeName, viewClasses);
		if (nameToType != null) {
			returnTypeName = nameToType.returnTypeName;
			returnTypeFormat = nameToType.returnTypeFormat;
			returnType = nameToType.returnType;
			// set collection data
			if (nameToType.containerOf != null) {
				returnTypeName = "array";
				// its a model collection, add the container of type to the model
				modelType = nameToType.containerOf;
				returnTypeItemsRef = this.translator.typeName(nameToType.containerOf, this.options.isUseFullModelIds(), viewClasses).value();
			} else if (nameToType.containerOfPrimitiveType != null) {
				returnTypeName = "array";
				// its a primitive collection
				returnTypeItemsType = nameToType.containerOfPrimitiveType;
				returnTypeItemsFormat = nameToType.containerOfPrimitiveTypeFormat;
			} else {
				modelType = returnType;
				if (nameToType.varsToTypes != null) {
					varsToTypes.putAll(nameToType.varsToTypes);
				}
			}
		} else if (containerOf != null) {
			returnTypeName = "array";
			// its a collection, add the container of type to the model
			modelType = containerOf;
			returnTypeItemsAllowableValues = ParserHelper.getAllowableValues(containerOf.asClassDoc());
			if (returnTypeItemsAllowableValues != null) {
				returnTypeItemsType = "string";
			} else {
				// set the items type or ref
				if (ParserHelper.isPrimitive(containerOf, this.options)) {
					OptionalName oName = this.translator.typeName(containerOf, this.options.isUseFullModelIds());
					returnTypeItemsType = oName.value();
					returnTypeItemsFormat = oName.getFormat();
				} else {
					returnTypeItemsRef = this.translator.typeName(containerOf, this.options.isUseFullModelIds(), viewClasses).value();
				}
			}
		} else {
			// if its not a container then adjust the return type name for any views
			returnTypeOName = this.translator.typeName(returnType, this.options.isUseFullModelIds(), viewClasses);
			returnTypeName = returnTypeOName.value();
			returnTypeFormat = returnTypeOName.getFormat();

			// add parameterized types to the model
			// TODO: support variables e.g. for inherited or sub resources
			addParameterizedModelTypes(returnType, varsToTypes, viewClasses);
		}

		// read extra details for the return type
		FieldReader returnTypeReader = new FieldReader(this.options);

		// set enum values
		List<String> returnTypeAllowableValues = null;
		if (returnType != null) {
			returnTypeAllowableValues = ParserHelper.getAllowableValues(returnType.asClassDoc());
			if (returnTypeAllowableValues != null) {
				returnTypeName = "string";
			}
		}

		Boolean returnTypeUniqueItems = null;
		if (returnType != null && returnTypeName.equals("array")) {
			if (ParserHelper.isSet(returnType.qualifiedTypeName())) {
				returnTypeUniqueItems = Boolean.TRUE;
			}
		}

		String tagFormat = returnTypeReader.getFieldFormatValue(this.methodDoc, returnType);
		if (tagFormat != null) {
			returnTypeFormat = tagFormat;
		}

		String returnTypeMinimum = returnTypeReader.getFieldMin(this.methodDoc, returnType);
		String returnTypeMaximum = returnTypeReader.getFieldMax(this.methodDoc, returnType);
		String returnTypeDefaultValue = returnTypeReader.getFieldDefaultValue(this.methodDoc, returnType);

		if (modelType != null && this.options.isParseModels()) {
			this.models.addAll(new ApiModelParser(this.options, this.translator, modelType, viewClasses, this.classes).addVarsToTypes(varsToTypes).parse());
		}

		// ************************************
		// Summary and notes
		// ************************************
		// First Sentence of Javadoc method description
		String firstSentences = ParserHelper.getInheritableFirstSentenceTags(this.methodDoc);

		// default plugin behaviour
		String summary = firstSentences == null ? "" : firstSentences;
		String notes = ParserHelper.getInheritableCommentText(this.methodDoc);
		if (notes == null) {
			notes = "";
		}
		notes = notes.replace(summary, "").trim();

		// look for custom notes/summary tags to use instead
		String customNotes = ParserHelper.getInheritableTagValue(this.methodDoc, this.options.getOperationNotesTags(), this.options);
		if (customNotes != null) {
			notes = customNotes;
		}
		String customSummary = ParserHelper.getInheritableTagValue(this.methodDoc, this.options.getOperationSummaryTags(), this.options);
		if (customSummary != null) {
			summary = customSummary;
		}
		summary = this.options.replaceVars(summary);
		notes = this.options.replaceVars(notes);

		// Auth support
		OperationAuthorizations authorizations = generateAuthorizations();

		// ************************************
		// Produces & consumes
		// ************************************
		List<String> consumes = ParserHelper.getConsumes(this.methodDoc, this.options);
		List<String> produces = ParserHelper.getProduces(this.methodDoc, this.options);

		if (this.options.isLogDebug()) {
			System.out.println("finished parsing method: " + this.methodDoc.name());
		}

		// final result!
		return new Method(this.httpMethod, this.methodDoc.name(), path, parameters, responseMessages, summary, notes, returnTypeName, returnTypeFormat,
				returnTypeMinimum, returnTypeMaximum, returnTypeDefaultValue, returnTypeAllowableValues, returnTypeUniqueItems, returnTypeItemsRef,
				returnTypeItemsType, returnTypeItemsFormat, returnTypeItemsAllowableValues, consumes, produces, authorizations, deprecated);
	}

	private OperationAuthorizations generateAuthorizations() {
		OperationAuthorizations authorizations = null;

		// build map of scopes from the api auth
		Map<String, Oauth2Scope> apiScopes = new HashMap<String, Oauth2Scope>();
		if (this.options.getApiAuthorizations() != null && this.options.getApiAuthorizations().getOauth2() != null
				&& this.options.getApiAuthorizations().getOauth2().getScopes() != null) {
			List<Oauth2Scope> scopes = this.options.getApiAuthorizations().getOauth2().getScopes();
			if (scopes != null) {
				for (Oauth2Scope scope : scopes) {
					apiScopes.put(scope.getScope(), scope);
				}
			}
		}
		// see if method has a tag that implies there is no authentication
		// in this case set the authentication object to {} to indicate we override
		// at the operation level
		// a) if method has an explicit unauth tag
		if (ParserHelper.hasInheritableTag(this.methodDoc, this.options.getUnauthOperationTags())) {
			authorizations = new OperationAuthorizations();
		} else {

			// otherwise if method has scope tags then add those to indicate method requires scope
			List<String> scopeValues = ParserHelper.getInheritableTagValues(this.methodDoc, this.options.getOperationScopeTags(), this.options);
			if (scopeValues != null) {
				List<Oauth2Scope> oauth2Scopes = new ArrayList<Oauth2Scope>();
				for (String scopeVal : scopeValues) {
					Oauth2Scope apiScope = apiScopes.get(scopeVal);
					if (apiScope == null) {
						throw new IllegalStateException("The scope: " + scopeVal + " was referenced in the method: " + this.methodDoc
								+ " but this scope was not part of the API service.json level authorization object.");
					}
					oauth2Scopes.add(apiScope);
				}
				authorizations = new OperationAuthorizations(oauth2Scopes);
			}

			// if not scopes see if its auth and whether we need to add default scope to it
			if (scopeValues == null || scopeValues.isEmpty()) {
				// b) if method has an auth tag that starts with one of the known values that indicates whether auth required.
				String authSpec = ParserHelper.getInheritableTagValue(this.methodDoc, this.options.getAuthOperationTags(), this.options);
				if (authSpec != null) {

					boolean unauthFound = false;
					for (String unauthValue : this.options.getUnauthOperationTagValues()) {
						if (authSpec.toLowerCase().startsWith(unauthValue.toLowerCase())) {
							authorizations = new OperationAuthorizations();
							unauthFound = true;
							break;
						}
					}
					if (!unauthFound) {
						// its deemed to require authentication, however there is no explicit scope so we need to use
						// the default scopes
						List<String> defaultScopes = this.options.getAuthOperationScopes();
						if (defaultScopes != null && !defaultScopes.isEmpty()) {
							List<Oauth2Scope> oauth2Scopes = new ArrayList<Oauth2Scope>();
							for (String scopeVal : defaultScopes) {
								Oauth2Scope apiScope = apiScopes.get(scopeVal);
								if (apiScope == null) {
									throw new IllegalStateException("The default scope: " + scopeVal + " needed for the authorized method: " + this.methodDoc
											+ " was not part of the API service.json level authorization object.");
								}
								oauth2Scopes.add(apiScope);
							}
							authorizations = new OperationAuthorizations(oauth2Scopes);
						}
					}
				}
			}

		}
		return authorizations;
	}

	private List<ApiResponseMessage> generateResponseMessages(ClassDoc[] viewClasses) {
		List<ApiResponseMessage> responseMessages = new ArrayList<ApiResponseMessage>();

		Map<Integer, Integer> codeToMessageIdx = new HashMap<Integer, Integer>();

		List<String> tagValues = ParserHelper.getInheritableTagValues(this.methodDoc, this.options.getResponseMessageTags(), this.options);
		if (tagValues != null) {
			for (String tagValue : tagValues) {
				for (Pattern pattern : RESPONSE_MESSAGE_PATTERNS) {
					Matcher matcher = pattern.matcher(tagValue);
					if (matcher.find()) {
						int statusCode = Integer.parseInt(matcher.group(1).trim());
						// trim special chars the desc may start with
						String desc = ParserHelper.trimLeadingChars(matcher.group(2), '|', '-');

						// see if it has a custom response model
						String responseModelClass = null;
						if (matcher.groupCount() > 2) {
							responseModelClass = ParserHelper.trimLeadingChars(matcher.group(3), '`');
						}
						// for errors, if no custom one use the method level one if there is one
						if (statusCode >= 400) {
							if (responseModelClass == null) {
								responseModelClass = this.methodDefaultErrorType;
							}
							// for errors, if no custom one use the class level one if there is one
							if (responseModelClass == null) {
								responseModelClass = this.classDefaultErrorType;
							}
						}

						String responseModel = null;
						if (responseModelClass != null) {
							Type responseType = ParserHelper.findModel(this.classes, responseModelClass);
							if (responseType != null) {
								responseModel = this.translator.typeName(responseType, this.options.isUseFullModelIds()).value();
								if (this.options.isParseModels()) {
									this.models.addAll(new ApiModelParser(this.options, this.translator, responseType, viewClasses, this.classes).parse());
								}
							}
						}

						if (codeToMessageIdx.containsKey(statusCode)) {
							int idx = codeToMessageIdx.get(statusCode);
							responseMessages.get(idx).merge(desc, responseModel);
						} else {
							responseMessages.add(new ApiResponseMessage(statusCode, desc, responseModel));
							codeToMessageIdx.put(statusCode, responseMessages.size() - 1);
						}
						break;
					}
				}
			}
		}

		// sort the response messages as required
		if (!responseMessages.isEmpty() && this.options.getResponseMessageSortMode() != null) {
			switch (this.options.getResponseMessageSortMode()) {
				case CODE_ASC:
					Collections.sort(responseMessages, new Comparator<ApiResponseMessage>() {

						public int compare(ApiResponseMessage o1, ApiResponseMessage o2) {
							return Integer.compare(o1.getCode(), o2.getCode());
						}
					});
					break;
				case CODE_DESC:
					Collections.sort(responseMessages, new Comparator<ApiResponseMessage>() {

						public int compare(ApiResponseMessage o1, ApiResponseMessage o2) {
							return Integer.compare(o2.getCode(), o1.getCode());
						}
					});
					break;
				case AS_APPEARS:
					// noop
					break;
				default:
					throw new UnsupportedOperationException("Unknown ResponseMessageSortMode: " + this.options.getResponseMessageSortMode());

			}
		}

		return responseMessages;
	}

	private List<ApiParameter> generateParameters() {
		// parameters
		List<ApiParameter> parameters = new LinkedList<ApiParameter>();

		// read whether the method consumes multipart
		List<String> consumes = ParserHelper.getConsumes(this.methodDoc, this.options);
		boolean consumesMultipart = consumes != null && consumes.contains(MediaType.MULTIPART_FORM_DATA);

		// get raw parameter names from method signature
		Set<String> rawParamNames = ParserHelper.getParamNames(this.methodDoc);

		// get full list including any beanparam parameter names
		Set<String> allParamNames = new HashSet<String>(rawParamNames);
		for (int paramIndex = 0; paramIndex < this.methodDoc.parameters().length; paramIndex++) {
			final Parameter parameter = ParserHelper.getParameterWithAnnotations(this.methodDoc, paramIndex);
			String paramCategory = ParserHelper.paramTypeOf(consumesMultipart, parameter, this.options);
			// see if its a special composite type e.g. @BeanParam
			if ("composite".equals(paramCategory)) {
				Type paramType = parameter.type();
				ApiModelParser modelParser = new ApiModelParser(this.options, this.translator, paramType, consumesMultipart, true);
				Set<Model> models = modelParser.parse();
				String rootModelId = modelParser.getRootModelId();
				for (Model model : models) {
					if (model.getId().equals(rootModelId)) {
						Map<String, Property> modelProps = model.getProperties();
						for (Map.Entry<String, Property> entry : modelProps.entrySet()) {
							Property property = entry.getValue();
							String rawFieldName = property.getRawFieldName();
							allParamNames.add(rawFieldName);
						}
					}
				}
			}
		}

		// read exclude params
		List<String> excludeParams = ParserHelper.getCsvParams(this.methodDoc, allParamNames, this.options.getExcludeParamsTags(), this.options);

		ParameterReader paramReader = new ParameterReader(this.options, this.classes);
		paramReader.readClass(this.methodDoc.containingClass());

		Set<String> addedParamNames = new HashSet<String>();

		// build params from the method's params
		for (int paramIndex = 0; paramIndex < this.methodDoc.parameters().length; paramIndex++) {
			final Parameter parameter = ParserHelper.getParameterWithAnnotations(this.methodDoc, paramIndex);
			if (!shouldIncludeParameter(this.httpMethod, excludeParams, parameter)) {
				continue;
			}

			List<ApiParameter> apiParams = paramReader.buildApiParams(this.methodDoc, parameter, consumesMultipart, allParamNames, this.models);
			addUniqueParam(addedParamNames, apiParams, parameters);
		}

		// add any parent method parameters that are inherited
		if (this.parentMethod != null) {
			addUniqueParam(addedParamNames, this.parentMethod.getParameters(), parameters);
		}

		// add class level parameters
		List<ApiParameter> classLevelParams = paramReader.readClassLevelParameters(this.models);
		addUniqueParam(addedParamNames, classLevelParams, parameters);

		// add on any implicit params
		List<ApiParameter> implicitParams = paramReader.readImplicitParameters(this.methodDoc, consumesMultipart, this.models);
		addUniqueParam(addedParamNames, implicitParams, parameters);

		return parameters;
	}

	private void addUniqueParam(Set<String> addedParamNames, List<ApiParameter> paramsToAdd, List<ApiParameter> targetList) {
		if (paramsToAdd != null) {
			for (ApiParameter apiParam : paramsToAdd) {
				if (!addedParamNames.contains(apiParam.getName())) {
					addedParamNames.add(apiParam.getName());
					targetList.add(apiParam);
				}
			}
		}
	}

	/**
	 * This gets the parsed models found for this method
	 * @return the set of parsed models found for this method
	 */
	public Set<Model> models() {
		return this.models;
	}

	static class NameToType {

		Type returnType;
		Type containerOf;
		String containerOfPrimitiveType;
		String containerOfPrimitiveTypeFormat;
		String returnTypeName;
		String returnTypeFormat;
		Map<String, Type> varsToTypes;
	}

	// TODO refactor building type details from a string into a common class for reuse
	// across various parts of the doclet
	NameToType readCustomReturnType(String customTypeName, ClassDoc[] viewClasses) {
		if (customTypeName != null && customTypeName.trim().length() > 0) {
			customTypeName = customTypeName.trim();

			Type[] paramTypes = null;
			Type customType = null;

			boolean useFqn = this.options.isUseFullModelIds();

			// split it into container and container of, if its in the form X<Y>
			Matcher matcher = GENERIC_RESPONSE_PATTERN.matcher(customTypeName);
			if (matcher.find()) {
				customTypeName = matcher.group(1);
				if (ParserHelper.isCollection(customTypeName)) {
					String containerOfType = matcher.group(2);
					Type containerOf = null;
					String containerOfPrimitiveType = null;
					String containerOfPrimitiveTypeFormat = null;
					if (ParserHelper.isPrimitive(containerOfType, this.options)) {
						String[] typeFormat = ParserHelper.typeOf(containerOfType, useFqn, this.options);
						containerOfPrimitiveType = typeFormat[0];
						containerOfPrimitiveTypeFormat = typeFormat[1];
					} else {
						containerOf = ParserHelper.findModel(this.classes, containerOfType);
						if (containerOf == null) {
							raiseCustomTypeNotFoundError(containerOfType);
						}
					}

					NameToType res = new NameToType();
					String[] nameFormat = ParserHelper.typeOf(customTypeName, useFqn, this.options);
					res.returnTypeName = nameFormat[0];
					res.returnTypeFormat = nameFormat[1];
					res.returnType = null;
					res.containerOf = containerOf;
					res.containerOfPrimitiveType = containerOfPrimitiveType;
					res.containerOfPrimitiveTypeFormat = containerOfPrimitiveTypeFormat;
					return res;
				} else if (ParserHelper.isMap(customTypeName)) {
					NameToType res = new NameToType();
					String[] nameFormat = ParserHelper.typeOf(customTypeName, useFqn, this.options);
					res.returnTypeName = nameFormat[0];
					res.returnTypeFormat = nameFormat[1];
					res.returnType = null;
					return res;
				} else {
					// its a parameterized type, add the parameterized classes to the model
					String[] paramTypeNames = matcher.group(2).split(",");
					paramTypes = new Type[paramTypeNames.length];
					int i = 0;
					for (String paramTypeName : paramTypeNames) {
						paramTypes[i] = ParserHelper.findModel(this.classes, paramTypeName);
						i++;
					}
				}
			}

			// lookup the type from the doclet classes
			customType = ParserHelper.findModel(this.classes, customTypeName);
			if (customType == null) {
				raiseCustomTypeNotFoundError(customTypeName);
			} else {
				customType = firstNonNull(ApiModelParser.getReturnType(this.options, customType), customType);

				// build map of var names to parameters if applicable
				Map<String, Type> varsToTypes = null;
				if (paramTypes != null) {
					varsToTypes = new HashMap<String, Type>();
					TypeVariable[] vars = customType.asClassDoc().typeParameters();
					int i = 0;
					for (TypeVariable var : vars) {
						varsToTypes.put(var.qualifiedTypeName(), paramTypes[i]);
						i++;
					}
					// add param types to the model
					for (Type type : paramTypes) {
						if (this.classes.contains(type)) {
							if (this.options.isParseModels()) {
								this.models.addAll(new ApiModelParser(this.options, this.translator, type, viewClasses, this.classes).addVarsToTypes(
										varsToTypes).parse());
							}
						}
					}
				}

				OptionalName translated = this.translator.typeName(customType, this.options.isUseFullModelIds(), viewClasses);
				if (translated != null && translated.value() != null) {
					NameToType res = new NameToType();
					res.returnTypeName = translated.value();
					res.returnTypeFormat = translated.getFormat();
					res.returnType = customType;
					res.varsToTypes = varsToTypes;
					return res;
				}
			}
		}
		return null;
	}

	private void addParameterizedModelTypes(Type returnType, Map<String, Type> varsToTypes, ClassDoc[] viewClasses) {
		// TODO support variable types e.g. parameterize sub resources or inherited resources
		List<Type> parameterizedTypes = ParserHelper.getParameterizedTypes(returnType, varsToTypes);
		for (Type type : parameterizedTypes) {
			if (this.classes.contains(type)) {
				if (this.options.isParseModels()) {
					this.models.addAll(new ApiModelParser(this.options, this.translator, type, viewClasses, null).addVarsToTypes(varsToTypes).parse());
				}
			}
		}
	}

	private void raiseCustomTypeNotFoundError(String customType) {
		throw new IllegalStateException(
				"Could not find the source for the custom response class: "
						+ customType
						+ ". If it is not in the same project as the one you have added the doclet to, "
						+ "for example if it is in a dependent project then you should copy the source to the doclet calling project using the maven-dependency-plugin's unpack goal,"
						+ " and then add it to the source using the build-helper-maven-plugin's add-source goal.");
	}

	private boolean shouldIncludeParameter(HttpMethod httpMethod, List<String> excludeParams, Parameter parameter) {
		List<AnnotationDesc> allAnnotations = Arrays.asList(parameter.annotations());

		// remove any params annotated with exclude param annotations e.g. jaxrs Context
		if (ParserHelper.hasAnnotation(parameter, this.options.getExcludeParamAnnotations(), this.options)) {
			return false;
		}

		// remove any params with exclude param tags
		if (excludeParams != null && excludeParams.contains(parameter.name())) {
			return false;
		}

		// remove any deprecated params
		if (this.options.isExcludeDeprecatedParams() && ParserHelper.isDeprecated(parameter, this.options)) {
			return false;
		}

		// include if it has a jaxrs annotation
		if (ParserHelper.hasJaxRsAnnotation(parameter, this.options)) {
			return true;
		}

		// include if there are either no annotations or its a put/post/patch
		// this means for GET/HEAD/OPTIONS we don't include if it has some non jaxrs annotation on it
		return (allAnnotations.isEmpty() || httpMethod == HttpMethod.POST || httpMethod == HttpMethod.PUT || httpMethod == HttpMethod.PATCH);
	}

}

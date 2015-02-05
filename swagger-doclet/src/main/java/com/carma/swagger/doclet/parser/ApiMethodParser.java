package com.carma.swagger.doclet.parser;

import static com.carma.swagger.doclet.parser.ParserHelper.parsePath;
import static com.carma.swagger.doclet.parser.ParserHelper.ROOT_PATH;
import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.collect.Collections2.filter;

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
import com.sun.javadoc.ParamTag;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.Tag;
import com.sun.javadoc.Type;

/**
 * The ApiMethodParser represents a parser for resource methods
 * @version $Id$
 */
public class ApiMethodParser {

	private static final Pattern GENERIC_RESPONSE_PATTERN = Pattern.compile("(.*)<(.*)>");

	// pattern that can match a code, a description and an optional response model type
	private static final Pattern[] RESPONSE_MESSAGE_PATTERNS = new Pattern[] { Pattern.compile("(\\d+)([^`]+)(`.*)?") };

	private static String trimLeadingChars(String str, char... trimChars) {
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

	private static String commentForParameter(MethodDoc method, Parameter parameter) {
		for (ParamTag tag : method.paramTags()) {
			if (tag.parameterName().equals(parameter.name())) {
				return tag.parameterComment();
			}
		}
		return "";
	}

	private final DocletOptions options;
	private final Translator translator;
	private final String parentPath;
	private final MethodDoc methodDoc;
	private final Set<Model> models;
	private final HttpMethod httpMethod;
	private final Method parentMethod;
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
		this.httpMethod = HttpMethod.fromMethod(methodDoc);
		this.parentMethod = null;
		this.classes = classes;
		this.classDefaultErrorType = classDefaultErrorType;
		this.methodDefaultErrorType = ParserHelper.getTagValue(methodDoc, options.getDefaultErrorTypeTags(), options);
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
		this.options = options;
		this.translator = options.getTranslator();
		this.methodDoc = methodDoc;
		this.models = new LinkedHashSet<Model>();
		this.httpMethod = HttpMethod.fromMethod(methodDoc);
		this.parentPath = parentMethod.getPath();
		this.parentMethod = parentMethod;
		this.classes = classes;
		this.classDefaultErrorType = classDefaultErrorType;
		this.methodDefaultErrorType = ParserHelper.getTagValue(methodDoc, options.getDefaultErrorTypeTags(), options);
	}

	/**
	 * This parses a javadoc method doc and builds a pojo representation of it.
	 * @return The method with appropriate data set
	 */
	public Method parse() {
		String methodPath = firstNonNull(parsePath(this.methodDoc, this.options), "");
		if (this.httpMethod == null && methodPath.isEmpty()) {
			return null;
		}

		// check if deprecated and exclude if set to do so
		boolean deprecated = false;
		if (ParserHelper.isDeprecated(this.methodDoc)) {
			if (this.options.isExcludeDeprecatedOperations()) {
				return null;
			}
			deprecated = true;
		}

		// exclude if it has exclusion tags
		if (ParserHelper.hasTag(this.methodDoc, this.options.getExcludeOperationTags())) {
			return null;
		}

        String path = (ROOT_PATH.equals(this.parentPath) ? "" : this.parentPath) + methodPath;

		// build params
		List<ApiParameter> parameters = this.generateParameters();

		// build response messages
		List<ApiResponseMessage> responseMessages = generateResponseMessages();

		// ************************************
		// Return type
		// ************************************
		Type returnType = this.methodDoc.returnType();
		// first check if its a wrapper type and if so replace with the wrapped type
		returnType = firstNonNull(ApiModelParser.getReturnType(this.options, returnType), returnType);

		String returnTypeName = this.translator.typeName(returnType).value();
		Type modelType = returnType;

		ClassDoc[] viewClasses = ParserHelper.getJsonViews(this.methodDoc, this.options);

		// now see if it is a collection if so the return type will be array and the
		// containerOf will be added to the model

		String returnTypeItemsRef = null;
		String returnTypeItemsType = null;
		Type containerOf = ParserHelper.getContainerType(returnType, null);

		if (containerOf != null) {
			// its a collection, add the container of type to the model
			modelType = containerOf;
			// set the items type or ref
			if (ParserHelper.isPrimitive(containerOf, this.options)) {
				returnTypeItemsType = this.translator.typeName(containerOf).value();
			} else {
				returnTypeItemsRef = this.translator.typeName(containerOf, viewClasses).value();
			}

		} else {

			// look for a custom return type, this is useful where we return a jaxrs Response in the method signature
			// but typically return a different object in its entity (such as for a 201 created response)
			String customReturnTypeName = ParserHelper.getTagValue(this.methodDoc, this.options.getResponseTypeTags(), this.options);
			NameToType nameToType = readCustomReturnType(customReturnTypeName, viewClasses);
			if (nameToType != null) {
				returnTypeName = nameToType.returnTypeName;
				returnType = nameToType.returnType;
				// set collection data
				if (nameToType.containerOf != null) {
					// its a model collection, add the container of type to the model
					modelType = nameToType.containerOf;
					returnTypeItemsRef = this.translator.typeName(nameToType.containerOf, viewClasses).value();
				} else if (nameToType.containerOfPrimitive != null) {
					// its a primitive collection
					returnTypeItemsType = nameToType.containerOfPrimitive;
				} else {
					modelType = returnType;
				}
			} else {
				// if its not a container then adjust the return type name for any views
				returnTypeName = this.translator.typeName(returnType, viewClasses).value();
			}

		}

		if (this.options.isParseModels()) {
			this.models.addAll(new ApiModelParser(this.options, this.translator, modelType, viewClasses).parse());
		}

		// ************************************
		// Summary and notes
		// ************************************
		// First Sentence of Javadoc method description
		Tag[] fst = this.methodDoc.firstSentenceTags();
		StringBuilder sentences = new StringBuilder();
		for (Tag tag : fst) {
			sentences.append(tag.text());
		}
		String firstSentences = sentences.toString();

		// default plugin behaviour
		String summary = firstSentences;
		String notes = this.methodDoc.commentText();
		notes = notes.replace(summary, "");

		// look for custom notes/summary tags to use instead
		String customNotes = ParserHelper.getTagValue(this.methodDoc, this.options.getOperationNotesTags(), this.options);
		if (customNotes != null) {
			notes = customNotes;
		}
		String customSummary = ParserHelper.getTagValue(this.methodDoc, this.options.getOperationSummaryTags(), this.options);
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

		// final result!
		return new Method(this.httpMethod, this.methodDoc.name(), path, parameters, responseMessages, summary, notes, returnTypeName, returnTypeItemsRef,
				returnTypeItemsType, consumes, produces, authorizations, deprecated);
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
		if (ParserHelper.hasTag(this.methodDoc, this.options.getUnauthOperationTags())) {
			authorizations = new OperationAuthorizations();
		} else {

			// otherwise if method has scope tags then add those to indicate method requires scope
			List<String> scopeValues = ParserHelper.getTagValues(this.methodDoc, this.options.getOperationScopeTags(), this.options);
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
				String authSpec = ParserHelper.getTagValue(this.methodDoc, this.options.getAuthOperationTags(), this.options);
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

	private List<ApiResponseMessage> generateResponseMessages() {
		List<ApiResponseMessage> responseMessages = new LinkedList<ApiResponseMessage>();

		List<String> responseMessageTags = new ArrayList<String>(this.options.getResponseMessageTags());

		Tag[] tags = this.methodDoc.tags();
		for (Tag tag : tags) {

			if (responseMessageTags.contains(tag.name().substring(1))) {
				boolean matched = false;

				if (!matched) {
					for (Pattern pattern : RESPONSE_MESSAGE_PATTERNS) {
						Matcher matcher = pattern.matcher(tag.text());
						if (matcher.find()) {

							int statusCode = Integer.parseInt(matcher.group(1).trim());
							// trim special chars the desc may start with
							String desc = trimLeadingChars(matcher.group(2), '|', '-');

							// see if it has a custom response model
							String responseModelClass = null;
							if (matcher.groupCount() > 2) {
								responseModelClass = trimLeadingChars(matcher.group(3), '`');
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
									responseModel = this.translator.typeName(responseType).value();
									if (this.options.isParseModels()) {
										this.models.addAll(new ApiModelParser(this.options, this.translator, responseType).parse());
									}
								}
							}

							responseMessages.add(new ApiResponseMessage(statusCode, desc, responseModel));
							matched = true;
							break;
						}
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
							return Integer.valueOf(o1.getCode()).compareTo(Integer.valueOf(o2.getCode()));
						}
					});
					break;
				case CODE_DESC:
					Collections.sort(responseMessages, new Comparator<ApiResponseMessage>() {

						public int compare(ApiResponseMessage o1, ApiResponseMessage o2) {
							return Integer.valueOf(o2.getCode()).compareTo(Integer.valueOf(o1.getCode()));
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
		for (Parameter parameter : this.methodDoc.parameters()) {
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

		// read required and optional params
		List<String> optionalParams = ParserHelper.getCsvParams(this.methodDoc, allParamNames, this.options.getOptionalParamsTags(), this.options);
		List<String> requiredParams = ParserHelper.getCsvParams(this.methodDoc, allParamNames, this.options.getRequiredParamsTags(), this.options);

		// read exclude params
		List<String> excludeParams = ParserHelper.getCsvParams(this.methodDoc, allParamNames, this.options.getExcludeParamsTags(), this.options);

		// read csv params
		List<String> csvParams = ParserHelper.getCsvParams(this.methodDoc, allParamNames, this.options.getCsvParamsTags(), this.options);

		// read min and max values of params
		Map<String, String> paramMinVals = ParserHelper.getMethodParamNameValuePairs(this.methodDoc, allParamNames, this.options.getParamsMinValueTags(),
				this.options);
		Map<String, String> paramMaxVals = ParserHelper.getMethodParamNameValuePairs(this.methodDoc, allParamNames, this.options.getParamsMaxValueTags(),
				this.options);

		// read default values of params
		Map<String, String> paramDefaultVals = ParserHelper.getMethodParamNameValuePairs(this.methodDoc, allParamNames,
				this.options.getParamsDefaultValueTags(), this.options);

		// read override names of params
		Map<String, String> paramNames = ParserHelper.getMethodParamNameValuePairs(this.methodDoc, allParamNames, this.options.getParamsNameTags(),
				this.options);

		for (Parameter parameter : this.methodDoc.parameters()) {
			if (!shouldIncludeParameter(this.httpMethod, excludeParams, parameter)) {
				continue;
			}

			Type paramType = parameter.type();
			String paramCategory = ParserHelper.paramTypeOf(consumesMultipart, parameter, this.options);
			String paramName = parameter.name();

			// see if its a special composite type e.g. @BeanParam
			if ("composite".equals(paramCategory)) {

				ApiModelParser modelParser = new ApiModelParser(this.options, this.translator, paramType, consumesMultipart, true);
				Set<Model> models = modelParser.parse();
				String rootModelId = modelParser.getRootModelId();
				for (Model model : models) {
					if (model.getId().equals(rootModelId)) {
						List<String> requiredFields = model.getRequiredFields();
						List<String> optionalFields = model.getOptionalFields();
						Map<String, Property> modelProps = model.getProperties();
						for (Map.Entry<String, Property> entry : modelProps.entrySet()) {
							Property property = entry.getValue();
							String renderedParamName = entry.getKey();
							String rawFieldName = property.getRawFieldName();

							Boolean allowMultiple = getAllowMultiple(paramCategory, rawFieldName, csvParams);

							// see if there is a required javadoc tag directly on the bean param field, if so use that
							Boolean required = null;
							if (requiredFields != null && requiredFields.contains(renderedParamName)) {
								required = Boolean.TRUE;
							} else if (optionalFields != null && optionalFields.contains(renderedParamName)) {
								required = Boolean.FALSE;
							} else {
								required = getRequired(paramCategory, rawFieldName, property.getType(), optionalParams, requiredParams);
							}

							String itemsRef = property.getItems() == null ? null : property.getItems().getRef();
							String itemsType = property.getItems() == null ? null : property.getItems().getType();

							ApiParameter param = new ApiParameter(property.getParamCategory(), renderedParamName, required, allowMultiple, property.getType(),
									property.getFormat(), property.getDescription(), itemsRef, itemsType, property.getUniqueItems(),
									property.getAllowableValues(), property.getMinimum(), property.getMaximum(), property.getDefaultValue());

							parameters.add(param);
						}
						break;
					}
				}

				continue;
			}

			// look for a custom input type for body params
			if ("body".equals(paramCategory)) {
				String customParamType = ParserHelper.getTagValue(this.methodDoc, this.options.getInputTypeTags(), this.options);
				paramType = readCustomParamType(customParamType, paramType);
			}

			OptionalName paramTypeFormat = this.translator.parameterTypeName(consumesMultipart, parameter, paramType);
			String typeName = paramTypeFormat.value();
			String format = paramTypeFormat.getFormat();

			Boolean allowMultiple = null;
			List<String> allowableValues = null;
			String itemsRef = null;
			String itemsType = null;
			Boolean uniqueItems = null;
			String minimum = null;
			String maximum = null;
			String defaultVal = null;

			// set to form param type if data type is File
			if ("File".equals(typeName)) {
				paramCategory = "form";
			} else {

				if (this.options.isParseModels()) {
					this.models.addAll(new ApiModelParser(this.options, this.translator, paramType).parse());
				}

				// set enum values
				ClassDoc typeClassDoc = parameter.type().asClassDoc();
				allowableValues = ParserHelper.getAllowableValues(typeClassDoc);
				if (allowableValues != null) {
					typeName = "string";
				}

				// set whether its a csv param
				allowMultiple = getAllowMultiple(paramCategory, paramName, csvParams);

				// get min and max param values
				minimum = paramMinVals.get(paramName);
				maximum = paramMaxVals.get(paramName);

				String validationContext = " for the method: " + this.methodDoc.name() + " parameter: " + paramName;

				// verify min max are numbers
				ParserHelper.verifyNumericValue(validationContext + " min value.", typeName, format, minimum);
				ParserHelper.verifyNumericValue(validationContext + " max value.", typeName, format, maximum);

				// get a default value, prioritize the jaxrs annotation
				// otherwise look for the javadoc tag
				defaultVal = ParserHelper.getDefaultValue(parameter, this.options);
				if (defaultVal == null) {
					defaultVal = paramDefaultVals.get(paramName);
				}

				// verify default vs min, max and by itself
				if (defaultVal != null) {
					if (minimum == null && maximum == null) {
						// just validate the default
						ParserHelper.verifyValue(validationContext + " default value.", typeName, format, defaultVal);
					}
					// if min/max then default is validated as part of comparison
					if (minimum != null) {
						int comparison = ParserHelper.compareNumericValues(validationContext + " min value.", typeName, format, defaultVal, minimum);
						if (comparison < 0) {
							throw new IllegalStateException("Invalid value for the default value of the method: " + this.methodDoc.name() + " parameter: "
									+ paramName + " it should be >= the minimum: " + minimum);
						}
					}
					if (maximum != null) {
						int comparison = ParserHelper.compareNumericValues(validationContext + " max value.", typeName, format, defaultVal, maximum);
						if (comparison > 0) {
							throw new IllegalStateException("Invalid value for the default value of the method: " + this.methodDoc.name() + " parameter: "
									+ paramName + " it should be <= the maximum: " + maximum);
						}
					}
				}

				// if enum and default value check it matches the enum values
				if (allowableValues != null && defaultVal != null && !allowableValues.contains(defaultVal)) {
					throw new IllegalStateException("Invalid value: " + defaultVal + " for the default value of the method: " + this.methodDoc.name()
							+ " parameter: " + paramName + " it should be one of: " + allowableValues);
				}

				// set collection related fields
				// TODO: consider supporting parameterized collections as api parameters...
				Type containerOf = null;
				containerOf = ParserHelper.getContainerType(paramType, null);
				String containerTypeOf = containerOf == null ? null : this.translator.typeName(containerOf).value();
				if (containerOf != null) {
					if (ParserHelper.isPrimitive(containerOf, this.options)) {
						itemsType = containerTypeOf;
					} else {
						itemsRef = containerTypeOf;
					}
				}

				if (typeName.equals("array")) {
					if (ParserHelper.isSet(paramType.qualifiedTypeName())) {
						uniqueItems = Boolean.TRUE;
					}
				}
			}

			// get whether required
			Boolean required = getRequired(paramCategory, paramName, typeName, optionalParams, requiredParams);

			// get the parameter name to use for the documentation
			String renderedParamName = ParserHelper.paramNameOf(parameter, paramNames, this.options.getParameterNameAnnotations(), this.options);

			// get description
			String description = this.options.replaceVars(commentForParameter(this.methodDoc, parameter));

			// build parameter
			ApiParameter param = new ApiParameter(paramCategory, renderedParamName, required, allowMultiple, typeName, format, description, itemsRef,
					itemsType, uniqueItems, allowableValues, minimum, maximum, defaultVal);

			parameters.add(param);
		}

		// parent method parameters are inherited
		if (this.parentMethod != null) {
			parameters.addAll(this.parentMethod.getParameters());
		}

		return parameters;
	}

	private Boolean getAllowMultiple(String paramCategory, String paramName, List<String> csvParams) {
		Boolean allowMultiple = null;
		if ("query".equals(paramCategory) || "path".equals(paramCategory) || "header".equals(paramCategory)) {
			if (csvParams != null && csvParams.contains(paramName)) {
				allowMultiple = Boolean.TRUE;
			}
		}
		return allowMultiple;
	}

	private Boolean getRequired(String paramCategory, String paramName, String typeName, List<String> optionalParams, List<String> requiredParams) {
		// set whether the parameter is required or not
		Boolean required = null;
		// if its a path param then its required as per swagger spec
		if ("path".equals(paramCategory)) {
			required = Boolean.TRUE;
		}
		// if its in the required list then its required
		else if (requiredParams.contains(paramName)) {
			required = Boolean.TRUE;
		}
		// else if its in the optional list its optional
		else if (optionalParams.contains(paramName)) {
			// leave as null as this is equivalent to false but doesn't add to the json
		}
		// else if its a body or File param its required
		else if ("body".equals(paramCategory) || ("File".equals(typeName) && "form".equals(paramCategory))) {
			required = Boolean.TRUE;
		}
		// otherwise its optional
		else {
			// leave as null as this is equivalent to false but doesn't add to the json
		}
		return required;
	}

	/**
	 * This gets the parsed models found for this method
	 * @return the set of parsed models found for this method
	 */
	public Set<Model> models() {
		return this.models;
	}

	private Type readCustomParamType(String customTypeName, Type defaultType) {
		if (customTypeName != null) {
			// lookup the type from the doclet classes
			Type customType = ParserHelper.findModel(this.classes, customTypeName);
			if (customType != null) {
				// also add this custom return type to the models
				if (this.options.isParseModels()) {
					this.models.addAll(new ApiModelParser(this.options, this.translator, customType).parse());
				}
				return customType;
			}
		}
		return defaultType;
	}

	static class NameToType {

		Type returnType;
		Type containerOf;
		String containerOfPrimitive;
		String returnTypeName;
	}

	NameToType readCustomReturnType(String customTypeName, ClassDoc[] viewClasses) {
		if (customTypeName != null && customTypeName.trim().length() > 0) {
			customTypeName = customTypeName.trim();

			// split it into container and container of, if its in the form X<Y>
			Matcher matcher = GENERIC_RESPONSE_PATTERN.matcher(customTypeName);
			if (matcher.find()) {
				String collectionType = matcher.group(1);
				if (ParserHelper.isCollection(collectionType)) {
					String containerOfType = matcher.group(2);
					Type containerOf = null;
					String containerOfPrimitive = null;
					if (ParserHelper.isPrimitive(containerOfType, this.options)) {
						containerOfPrimitive = ParserHelper.typeOf(containerOfType, this.options)[0];
					} else {
						containerOf = ParserHelper.findModel(this.classes, containerOfType);
						if (containerOf == null) {
							raiseCustomTypeNotFoundError(containerOfType);
						}
					}

					NameToType res = new NameToType();
					res.returnTypeName = ParserHelper.typeOf(collectionType, this.options)[0];
					res.returnType = null;
					res.containerOf = containerOf;
					res.containerOfPrimitive = containerOfPrimitive;
					return res;
				} else {
					throw new UnsupportedOperationException(
							"Generic response types are not supported for custom @returnType, only collection types are allowed.");
				}
			}

			// lookup the type from the doclet classes
			Type customType = ParserHelper.findModel(this.classes, customTypeName);
			if (customType == null) {
				raiseCustomTypeNotFoundError(customTypeName);
			} else {
				customType = firstNonNull(ApiModelParser.getReturnType(this.options, customType), customType);
				String translated = this.translator.typeName(customType, viewClasses).value();
				if (translated != null) {
					NameToType res = new NameToType();
					res.returnTypeName = translated;
					res.returnType = customType;
					return res;
				}
			}
		}
		return null;
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
		Collection<AnnotationDesc> excluded = filter(allAnnotations, new ParserHelper.ExcludedAnnotations(this.options.getExcludeParamAnnotations()));
		if (!excluded.isEmpty()) {
			return false;
		}

		// remove any params with exclude param tags
		if (excludeParams != null && excludeParams.contains(parameter.name())) {
			return false;
		}

		// remove any deprecated params
		if (this.options.isExcludeDeprecatedParams() && ParserHelper.hasDeprecated(parameter.annotations())) {
			return false;
		}

		Collection<AnnotationDesc> jaxRsAnnotations = filter(allAnnotations, new ParserHelper.JaxRsAnnotations());
		if (!jaxRsAnnotations.isEmpty()) {
			return true;
		}

		return (allAnnotations.isEmpty() || httpMethod == HttpMethod.POST || httpMethod == HttpMethod.PUT);
	}

}

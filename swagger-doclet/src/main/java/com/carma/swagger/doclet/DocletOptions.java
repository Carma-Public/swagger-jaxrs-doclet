package com.carma.swagger.doclet;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Arrays.asList;
import static java.util.Arrays.copyOfRange;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.carma.swagger.doclet.model.ApiAuthorizations;
import com.carma.swagger.doclet.model.ApiDeclaration;
import com.carma.swagger.doclet.model.ApiInfo;
import com.carma.swagger.doclet.parser.NamingConvention;
import com.carma.swagger.doclet.parser.ParserHelper;
import com.carma.swagger.doclet.parser.ResponseMessageSortMode;
import com.carma.swagger.doclet.parser.VariableReplacer;
import com.carma.swagger.doclet.translator.AnnotationAwareTranslator;
import com.carma.swagger.doclet.translator.FirstNotNullTranslator;
import com.carma.swagger.doclet.translator.NameBasedTranslator;
import com.carma.swagger.doclet.translator.Translator;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The DocletOptions represents the supported options for this doclet.
 * @version $Id$
 */
@SuppressWarnings("javadoc")
public class DocletOptions {

	private static <T> T loadModelFromJson(String option, String path, Class<T> resourceClass) {
		File file = new File(path);
		checkArgument(file.isFile(), "Path for " + option + " (" + file.getAbsolutePath() + ") is expected to be an existing file!");
		// load it as json and build the object from it
		InputStream is = null;
		try {
			is = new BufferedInputStream(new FileInputStream(file));
			return new ObjectMapper().readValue(is, resourceClass);
		} catch (Exception ex) {
			throw new IllegalArgumentException("Failed to read model file: " + path + ", error : " + ex.getMessage(), ex);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (Exception ex) {
					// ignore
				}
			}
		}
	}

	/**
	 * This parses doclet options
	 * @param options The cmdline options
	 * @return The parse options
	 */
	public static DocletOptions parse(String[][] options) {

		DocletOptions parsedOptions = new DocletOptions();

		// Object mapper settings
		String serializationFeaturesCsv = null;
		String deserializationFeaturesCsv = null;
		String defaultTyping = null;
		String serializationInclusion = null;

		for (String[] option : options) {
			if (option[0].equals("-d")) {
				parsedOptions.outputDirectory = new File(option[1]);
				if (!parsedOptions.outputDirectory.exists()) {
					boolean created = parsedOptions.outputDirectory.mkdirs();
					if (!created) {
						throw new IllegalArgumentException("Path after -d is expected to be a directory!");
					}
				}
			} else if (option[0].equals("-apiAuthorizationsFile")) {
				parsedOptions.apiAuthorizations = loadModelFromJson("-apiAuthorizationsFile", option[1], ApiAuthorizations.class);
			} else if (option[0].equals("-apiInfoFile")) {
				parsedOptions.apiInfo = loadModelFromJson("-apiInfoFile", option[1], ApiInfo.class);

			} else if (option[0].equals("-extraApiDeclarations")) {
				List<ApiDeclaration> extraApiDeclarations = new ArrayList<ApiDeclaration>();
				String[] filePaths = option[1].split(",");
				for (String filePath : filePaths) {
					filePath = filePath.trim();
					ApiDeclaration api = loadModelFromJson("-extraApiDeclarations", filePath, ApiDeclaration.class);
					extraApiDeclarations.add(api);
				}
				if (!extraApiDeclarations.isEmpty()) {
					parsedOptions.extraApiDeclarations = extraApiDeclarations;
				}

			} else if (option[0].equals("-variablesPropertiesFile")) {

				File varFile = new File(option[1]);
				if (!varFile.exists() && !varFile.canRead()) {
					throw new IllegalStateException("Unable to read variables file: " + varFile.getAbsolutePath() + " check it exists and is readable.");
				}
				Properties props = new Properties();
				InputStream is = null;
				try {
					is = new FileInputStream(varFile);
					props.load(is);
					parsedOptions.variableReplacements = props;
				} catch (IOException ex) {
					throw new IllegalStateException("Failed to read variables file: " + varFile.getAbsolutePath(), ex);
				} finally {
					if (is != null) {
						try {
							is.close();
						} catch (IOException ex) {
							// ignore
							ex.printStackTrace();
						}
					}
				}
			} else if (option[0].equals("-docBasePath")) {
				parsedOptions.docBasePath = option[1];
			} else if (option[0].equals("-apiBasePath")) {
				parsedOptions.apiBasePath = option[1];
			} else if (option[0].equals("-apiVersion")) {
				parsedOptions.apiVersion = option[1];
			} else if (option[0].equals("-swaggerUiZipPath") || option[0].equals("-swaggerUiPath")) {
				parsedOptions.swaggerUiPath = option[1];
			} else if (option[0].equals("-resourceRootPath")) {
				parsedOptions.resourceRootPath = option[1];
			} else if (option[0].equals("-responseMessageSortMode")) {
				parsedOptions.responseMessageSortMode = ResponseMessageSortMode.valueOf(option[1]);
			} else if (option[0].equals("-excludeParamAnnotations")) {
				parsedOptions.excludeParamAnnotations.addAll(asList(copyOfRange(option, 1, option.length)));
			} else if (option[0].equals("-disableModels")) {
				parsedOptions.parseModels = false;
			} else if (option[0].equals("-useFullModelIds")) {
				parsedOptions.useFullModelIds = true;
			} else if (option[0].equals("-logDebug")) {
				parsedOptions.logDebug = true;
			} else if (option[0].equals("-modelFieldsRequiredByDefault")) {
				parsedOptions.modelFieldsRequiredByDefault = true;
			} else if (option[0].equals("-disableModelFieldsXmlAccessType")) {
				parsedOptions.modelFieldsXmlAccessTypeEnabled = false;
			} else if (option[0].equals("-defaultModelFieldsXmlAccessType")) {
				parsedOptions.modelFieldsDefaultXmlAccessTypeEnabled = true;
			} else if (option[0].equals("-modelFieldsNamingConvention")) {
				parsedOptions.modelFieldsNamingConvention = NamingConvention.forValue(option[1], NamingConvention.DEFAULT_NAME);
			} else if (option[0].equals("-disableCopySwaggerUi") || option[0].equals("-skipUiFiles")) {
				parsedOptions.includeSwaggerUi = false;
			} else if (option[0].equals("-disableSortApisByPath")) {
				parsedOptions.sortApisByPath = false;
			} else if (option[0].equals("-sortResourcesByPath")) {
				parsedOptions.sortResourcesByPath = true;
			} else if (option[0].equals("-sortResourcesByPriority")) {
				parsedOptions.sortResourcesByPriority = true;
			} else if (option[0].equals("-disableDeprecatedOperationExclusion")) {
				parsedOptions.excludeDeprecatedOperations = false;
			} else if (option[0].equals("-disableDeprecatedFieldExclusion")) {
				parsedOptions.excludeDeprecatedFields = false;
			} else if (option[0].equals("-disableDeprecatedParamExclusion")) {
				parsedOptions.excludeDeprecatedParams = false;
			} else if (option[0].equals("-disableDeprecatedResourceClassExclusion")) {
				parsedOptions.excludeDeprecatedResourceClasses = false;
			} else if (option[0].equals("-disableDeprecatedModelClassExclusion")) {
				parsedOptions.excludeDeprecatedModelClasses = false;
			} else if (option[0].equals("-customDeprecatedAnnotation")) {
				ParserHelper.addCustomDeprecatedAnnotation(asList(copyOfRange(option, 1, option.length)));
            }else if (option[0].equals("-profileMode")) {
				parsedOptions.profileMode = true;

			} else if (option[0].equals("-excludeModelPrefixes") || option[0].equals("-typesToTreatAsOpaque")) {
				parsedOptions.excludeModelPrefixes.addAll(asList(copyOfRange(option, 1, option.length)));
			} else if (option[0].equals("-excludeResourcePrefixes")) {
				parsedOptions.excludeResourcePrefixes.addAll(asList(copyOfRange(option, 1, option.length)));
			} else if (option[0].equals("-includeResourcePrefixes")) {
				parsedOptions.includeResourcePrefixes.addAll(asList(copyOfRange(option, 1, option.length)));
			} else if (option[0].equals("-genericWrapperTypes")) {
				parsedOptions.genericWrapperTypes.addAll(asList(copyOfRange(option, 1, option.length)));
			} else if (option[0].equals("-fileParameterAnnotations")) {
				parsedOptions.fileParameterAnnotations.addAll(asList(copyOfRange(option, 1, option.length)));
			} else if (option[0].equals("-fileParameterTypes")) {
				parsedOptions.fileParameterTypes.addAll(asList(copyOfRange(option, 1, option.length)));
			} else if (option[0].equals("-formParameterAnnotations")) {
				parsedOptions.formParameterAnnotations.addAll(asList(copyOfRange(option, 1, option.length)));
			} else if (option[0].equals("-formParameterTypes")) {
				parsedOptions.formParameterTypes.addAll(asList(copyOfRange(option, 1, option.length)));

			} else if (option[0].equals("-discriminatorAnnotations")) {
				parsedOptions.discriminatorAnnotations.addAll(asList(copyOfRange(option, 1, option.length)));
			} else if (option[0].equals("-subTypesAnnotations")) {
				parsedOptions.subTypesAnnotations.addAll(asList(copyOfRange(option, 1, option.length)));

			} else if (option[0].equals("-compositeParamAnnotations")) {
				parsedOptions.compositeParamAnnotations.addAll(asList(copyOfRange(option, 1, option.length)));
			} else if (option[0].equals("-compositeParamTypes")) {
				parsedOptions.compositeParamTypes.addAll(asList(copyOfRange(option, 1, option.length)));

			} else if (option[0].equals("-parameterNameAnnotations")) {
				parsedOptions.parameterNameAnnotations.addAll(asList(copyOfRange(option, 1, option.length)));

			} else if (option[0].equals("-longTypePrefixes")) {
				parsedOptions.longTypePrefixes.addAll(asList(copyOfRange(option, 1, option.length)));

			} else if (option[0].equals("-intTypePrefixes")) {
				parsedOptions.intTypePrefixes.addAll(asList(copyOfRange(option, 1, option.length)));

			} else if (option[0].equals("-floatTypePrefixes")) {
				parsedOptions.floatTypePrefixes.addAll(asList(copyOfRange(option, 1, option.length)));

			} else if (option[0].equals("-doubleTypePrefixes")) {
				parsedOptions.doubleTypePrefixes.addAll(asList(copyOfRange(option, 1, option.length)));

			} else if (option[0].equals("-stringTypePrefixes")) {
				parsedOptions.stringTypePrefixes.addAll(asList(copyOfRange(option, 1, option.length)));

			} else if (option[0].equals("-responseMessageTags")) {
				addTagsOption(parsedOptions.responseMessageTags, option);

			} else if (option[0].equals("-excludeClassTags")) {
				addTagsOption(parsedOptions.excludeClassTags, option);

			} else if (option[0].equals("-excludeOperationTags")) {
				addTagsOption(parsedOptions.excludeOperationTags, option);

			} else if (option[0].equals("-excludeFieldTags")) {
				addTagsOption(parsedOptions.excludeFieldTags, option);

			} else if (option[0].equals("-excludeParamsTags")) {
				addTagsOption(parsedOptions.excludeParamsTags, option);

			} else if (option[0].equals("-csvParamsTags")) {
				addTagsOption(parsedOptions.csvParamsTags, option);

			} else if (option[0].equals("-implicitParamTags")) {
				addTagsOption(parsedOptions.implicitParamTags, option);

			} else if (option[0].equals("-paramsFormatTags")) {
				addTagsOption(parsedOptions.paramsFormatTags, option);

			} else if (option[0].equals("-paramsMinValueTags")) {
				addTagsOption(parsedOptions.paramsMinValueTags, option);

			} else if (option[0].equals("-paramsMaxValueTags")) {
				addTagsOption(parsedOptions.paramsMaxValueTags, option);

			} else if (option[0].equals("-paramsDefaultValueTags")) {
				addTagsOption(parsedOptions.paramsDefaultValueTags, option);

			} else if (option[0].equals("-paramsAllowableValuesTags")) {
				addTagsOption(parsedOptions.paramsAllowableValuesTags, option);

			} else if (option[0].equals("-paramsNameTags")) {
				addTagsOption(parsedOptions.paramsNameTags, option);

			} else if (option[0].equals("-resourceTags")) {
				addTagsOption(parsedOptions.resourceTags, option);

			} else if (option[0].equals("-responseTypeTags")) {
				addTagsOption(parsedOptions.responseTypeTags, option);

			} else if (option[0].equals("-inputTypeTags")) {
				addTagsOption(parsedOptions.inputTypeTags, option);

			} else if (option[0].equals("-defaultErrorTypeTags")) {
				addTagsOption(parsedOptions.defaultErrorTypeTags, option);

			} else if (option[0].equals("-apiDescriptionTags")) {
				addTagsOption(parsedOptions.apiDescriptionTags, option);

			} else if (option[0].equals("-operationNotesTags")) {
				addTagsOption(parsedOptions.operationNotesTags, option);

			} else if (option[0].equals("-operationSummaryTags")) {
				addTagsOption(parsedOptions.operationSummaryTags, option);

			} else if (option[0].equals("-fieldDescriptionTags")) {
				addTagsOption(parsedOptions.fieldDescriptionTags, option);

			} else if (option[0].equals("-fieldFormatTags")) {
				addTagsOption(parsedOptions.fieldFormatTags, option);

			} else if (option[0].equals("-fieldMinTags")) {
				addTagsOption(parsedOptions.fieldMinTags, option);

			} else if (option[0].equals("-fieldMaxTags")) {
				addTagsOption(parsedOptions.fieldMaxTags, option);

			} else if (option[0].equals("-fieldDefaultTags")) {
				addTagsOption(parsedOptions.fieldDefaultTags, option);

			} else if (option[0].equals("-fieldAllowableValuesTags")) {
				addTagsOption(parsedOptions.fieldAllowableValuesTags, option);

			} else if (option[0].equals("-requiredParamsTags")) {
				addTagsOption(parsedOptions.requiredParamsTags, option);

			} else if (option[0].equals("-optionalParamsTags")) {
				addTagsOption(parsedOptions.optionalParamsTags, option);

			} else if (option[0].equals("-requiredFieldTags")) {
				addTagsOption(parsedOptions.requiredFieldTags, option);

			} else if (option[0].equals("-optionalFieldTags")) {
				addTagsOption(parsedOptions.optionalFieldTags, option);

				// JSR 303
			} else if (option[0].equals("-paramMinValueAnnotations")) {
				parsedOptions.paramMinValueAnnotations.addAll(asList(copyOfRange(option, 1, option.length)));
			} else if (option[0].equals("-paramMaxValueAnnotations")) {
				parsedOptions.paramMaxValueAnnotations.addAll(asList(copyOfRange(option, 1, option.length)));
			} else if (option[0].equals("-fieldMinAnnotations")) {
				parsedOptions.fieldMinAnnotations.addAll(asList(copyOfRange(option, 1, option.length)));
			} else if (option[0].equals("-fieldMaxAnnotations")) {
				parsedOptions.fieldMaxAnnotations.addAll(asList(copyOfRange(option, 1, option.length)));
			} else if (option[0].equals("-requiredParamAnnotations")) {
				parsedOptions.requiredParamAnnotations.addAll(asList(copyOfRange(option, 1, option.length)));
			} else if (option[0].equals("-optionalParamAnnotations")) {
				parsedOptions.optionalParamAnnotations.addAll(asList(copyOfRange(option, 1, option.length)));
			} else if (option[0].equals("-requiredFieldAnnotations")) {
				parsedOptions.requiredFieldAnnotations.addAll(asList(copyOfRange(option, 1, option.length)));
			} else if (option[0].equals("-optionalFieldAnnotations")) {
				parsedOptions.optionalFieldAnnotations.addAll(asList(copyOfRange(option, 1, option.length)));

			} else if (option[0].equals("-unauthOperationTags")) {
				addTagsOption(parsedOptions.unauthOperationTags, option);

			} else if (option[0].equals("-authOperationTags")) {
				addTagsOption(parsedOptions.authOperationTags, option);

			} else if (option[0].equals("-unauthOperationTagValues")) {
				parsedOptions.unauthOperationTagValues.addAll(asList(copyOfRange(option, 1, option.length)));

			} else if (option[0].equals("-authOperationScopes")) {
				parsedOptions.authOperationScopes.addAll(asList(copyOfRange(option, 1, option.length)));

			} else if (option[0].equals("-operationScopeTags")) {
				addTagsOption(parsedOptions.operationScopeTags, option);

			} else if (option[0].equals("-serializationFeatures")) {
				serializationFeaturesCsv = option[1];
			} else if (option[0].equals("-deserializationFeatures")) {
				deserializationFeaturesCsv = option[1];
			} else if (option[0].equals("-defaultTyping")) {
				defaultTyping = option[1];
			} else if (option[0].equals("-serializationInclusion")) {
				serializationInclusion = option[1];
			}
		}
		parsedOptions.recorder = new ObjectMapperRecorder(serializationFeaturesCsv, deserializationFeaturesCsv, defaultTyping, serializationInclusion);
		return parsedOptions;
	}

	private static void addTagsOption(List<String> list, String[] option) {
		List<String> tags = asList(copyOfRange(option, 1, option.length));
		for (String tag : tags) {
			if (tag.startsWith("@")) {
				tag = tag.substring(1);
			}
			list.add(tag);
		}
	}

	private File outputDirectory;
	private String docBasePath = null;
	private String apiBasePath = "http://localhost:8080";
	private String swaggerUiPath = null;
	private String apiVersion = "0";

	private String resourceRootPath = "/root";

	private boolean includeSwaggerUi = true;

	private boolean profileMode = false;

	private Properties variableReplacements;

	private List<String> excludeResourcePrefixes;
	private List<String> includeResourcePrefixes;
	private List<String> excludeModelPrefixes;
	private List<String> genericWrapperTypes;
	private List<String> responseMessageTags;
	private List<String> responseTypeTags;
	private List<String> inputTypeTags;
	private List<String> defaultErrorTypeTags;

	private List<String> compositeParamAnnotations;
	private List<String> compositeParamTypes;

	private List<String> discriminatorAnnotations;
	private List<String> subTypesAnnotations;

	private List<String> excludeParamAnnotations;
	private List<String> excludeClassTags;
	private List<String> excludeOperationTags;
	private List<String> excludeFieldTags;
	private List<String> excludeParamsTags;
	private List<String> csvParamsTags;
	private List<String> implicitParamTags;
	private List<String> paramsFormatTags;
	private List<String> paramsMinValueTags;
	private List<String> paramMinValueAnnotations;
	private List<String> paramsMaxValueTags;
	private List<String> paramMaxValueAnnotations;
	private List<String> paramsDefaultValueTags;
	private List<String> paramsAllowableValuesTags;
	private List<String> paramsNameTags;
	private List<String> resourceTags;
	private List<String> apiDescriptionTags;
	private List<String> operationNotesTags;
	private List<String> operationSummaryTags;
	private List<String> fieldDescriptionTags;

	private List<String> fieldFormatTags;
	private List<String> fieldMinTags;
	private List<String> fieldMinAnnotations;
	private List<String> fieldMaxTags;
	private List<String> fieldMaxAnnotations;
	private List<String> fieldDefaultTags;
	private List<String> fieldAllowableValuesTags;

	private List<String> requiredParamsTags;
	private List<String> requiredParamAnnotations;
	private List<String> optionalParamsTags;
	private List<String> optionalParamAnnotations;

	private List<String> requiredFieldTags;
	private List<String> requiredFieldAnnotations;
	private List<String> optionalFieldTags;
	private List<String> optionalFieldAnnotations;

	private List<String> unauthOperationTags; // tags that say a method does NOT require authorization
	private List<String> authOperationTags; // tags that indicate whether an operation requires auth or not, coupled with a value from unauthOperationTagValues
	private List<String> unauthOperationTagValues; // for tags in authOperationTags this is the value to look for to indicate method does NOT require
													// authorization
	private List<String> authOperationScopes; // default scopes to add if authOperationTags is present but no scopes
	private List<String> operationScopeTags; // explicit scopes that are required for authorization for a method

	private List<String> resourcePriorityTags;
	private List<String> resourceDescriptionTags;

	private List<String> fileParameterAnnotations; // FQN of annotations that if present denote a parameter as being a File data type
	private List<String> fileParameterTypes; // FQN of types of a parameter that are File data types

	private List<String> formParameterAnnotations; // FQN of annotations that if present denote a parameter as being a form parameter type
	private List<String> formParameterTypes; // FQN of types of a parameter that are form parameter types

	private List<String> parameterNameAnnotations;

	private List<String> longTypePrefixes; // list of type prefixes that are mapped to long data type
	private List<String> intTypePrefixes; // list of type prefixes that are mapped to int data type
	private List<String> floatTypePrefixes; // list of type prefixes that are mapped to float data type
	private List<String> doubleTypePrefixes; // list of type prefixes that are mapped to double data type
	private List<String> stringTypePrefixes; // list of type prefixes that are mapped to string data type, can be used for example to map header types to
												// strings

	private boolean excludeDeprecatedResourceClasses = true;
	private boolean excludeDeprecatedModelClasses = true;
	private boolean excludeDeprecatedOperations = true;
	private boolean excludeDeprecatedFields = true;
	private boolean excludeDeprecatedParams = true;

	private boolean logDebug = false;
	private boolean parseModels = true;
	private boolean useFullModelIds = false;
	private boolean modelFieldsRequiredByDefault = false;
	private boolean modelFieldsXmlAccessTypeEnabled = true;
	private boolean modelFieldsDefaultXmlAccessTypeEnabled = false;
	private NamingConvention modelFieldsNamingConvention = NamingConvention.DEFAULT_NAME;

	private boolean sortResourcesByPath = false;
	private boolean sortResourcesByPriority = false;
	private boolean sortApisByPath = true;

	private ResponseMessageSortMode responseMessageSortMode;

	private ApiAuthorizations apiAuthorizations;

	private ApiInfo apiInfo;

	private List<ApiDeclaration> extraApiDeclarations;

	private Recorder recorder;
	private Translator translator;

	/**
	 * This creates a DocletOptions
	 */
	public DocletOptions() {
		this.excludeParamAnnotations = new ArrayList<String>();
		this.excludeParamAnnotations.add("javax.ws.rs.core.Context");
		this.excludeParamAnnotations.add("javax.ws.rs.CookieParam");
		this.excludeParamAnnotations.add("javax.ws.rs.MatrixParam");
		this.excludeParamAnnotations.add("javax.ws.rs.container.Suspended");

		this.responseMessageTags = new ArrayList<String>();
		this.responseMessageTags.add("responseMessage");
		this.responseMessageTags.add("status");
		this.responseMessageTags.add("errorResponse");
		this.responseMessageTags.add("errorCode");
		this.responseMessageTags.add("successResponse");
		this.responseMessageTags.add("successCode");

		this.excludeModelPrefixes = new ArrayList<String>();
		this.excludeModelPrefixes.add("org.joda.time.DateTime");
		this.excludeModelPrefixes.add("java.util.UUID");
		this.excludeModelPrefixes.add("java.io.");
		this.excludeModelPrefixes.add("com.sun.jersey.core.header.");
		this.excludeModelPrefixes.add("org.springframework.web.multipart.");
		this.excludeModelPrefixes.add("org.jboss.resteasy.plugins.providers.multipart.");

		// custom types which are mapped to longs
		this.longTypePrefixes = new ArrayList<String>();

		// custom types which are mapped to ints
		this.intTypePrefixes = new ArrayList<String>();

		// custom types which are mapped to floats
		this.floatTypePrefixes = new ArrayList<String>();

		// custom types which are mapped to doubles
		this.doubleTypePrefixes = new ArrayList<String>();

		// custom types which are mapped to strings
		this.stringTypePrefixes = new ArrayList<String>();
		this.stringTypePrefixes.add("com.sun.jersey.core.header.");

		// types which simply wrap an entity
		this.genericWrapperTypes = new ArrayList<String>();
		this.genericWrapperTypes.add("com.sun.jersey.api.JResponse");
		this.genericWrapperTypes.add("com.google.common.base.Optional");
		this.genericWrapperTypes.add("jersey.repackaged.com.google.common.base.Optional");

		// annotations and types which are mapped to File data type,
		// NOTE these only apply for multipart resources
		this.fileParameterAnnotations = new ArrayList<String>();
		this.fileParameterAnnotations.add("org.jboss.resteasy.annotations.providers.multipart.MultipartForm");

		this.fileParameterTypes = new ArrayList<String>();
		this.fileParameterTypes.add("java.io.File");
		this.fileParameterTypes.add("java.io.InputStream");
		this.fileParameterTypes.add("byte[]");
		this.fileParameterTypes.add("org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput");

		// annotations and types which are mapped to form parameter type
		this.formParameterAnnotations = new ArrayList<String>();
		this.formParameterAnnotations.add("com.sun.jersey.multipart.FormDataParam");
		this.formParameterAnnotations.add("javax.ws.rs.FormParam");

		this.formParameterTypes = new ArrayList<String>();
		this.formParameterTypes.add("com.sun.jersey.core.header.FormDataContentDisposition");

		// overrides for parameter names
		this.paramsNameTags = new ArrayList<String>();
		this.paramsNameTags.add("paramsName");
		this.paramsNameTags.add("overrideParamsName");

		// annotations to use for parameter names
		this.parameterNameAnnotations = new ArrayList<String>();
		for (String annotation : ParserHelper.JAXRS_PARAM_ANNOTATIONS) {
			this.parameterNameAnnotations.add(annotation);
		}
		this.parameterNameAnnotations.add("com.sun.jersey.multipart.FormDataParam");

		// annotations/types to use for composite param objects
		this.compositeParamAnnotations = new ArrayList<String>();
		this.compositeParamAnnotations.add("javax.ws.rs.BeanParam");
		this.compositeParamTypes = new ArrayList<String>();

		this.discriminatorAnnotations = new ArrayList<String>();
		this.discriminatorAnnotations.add("com.fasterxml.jackson.annotation.JsonTypeInfo");

		this.subTypesAnnotations = new ArrayList<String>();
		this.subTypesAnnotations.add("com.fasterxml.jackson.annotation.JsonSubTypes");

		this.excludeResourcePrefixes = new ArrayList<String>();
		this.includeResourcePrefixes = new ArrayList<String>();

		this.excludeClassTags = new ArrayList<String>();
		this.excludeClassTags.add("hidden");
		this.excludeClassTags.add("hide");
		this.excludeClassTags.add("exclude");

		this.excludeOperationTags = new ArrayList<String>();
		this.excludeOperationTags.add("hidden");
		this.excludeOperationTags.add("hide");
		this.excludeOperationTags.add("exclude");

		this.excludeFieldTags = new ArrayList<String>();
		this.excludeFieldTags.add("hidden");
		this.excludeFieldTags.add("hide");
		this.excludeFieldTags.add("exclude");

		this.excludeParamsTags = new ArrayList<String>();
		this.excludeParamsTags.add("excludeParams");
		this.excludeParamsTags.add("hiddenParams");
		this.excludeParamsTags.add("hideParams");

		this.csvParamsTags = new ArrayList<String>();
		this.csvParamsTags.add("csvParams");

		this.implicitParamTags = new ArrayList<String>();
		this.implicitParamTags.add("implicitParam");
		this.implicitParamTags.add("additionalParam");
		this.implicitParamTags.add("extraParam");

		this.paramsFormatTags = new ArrayList<String>();
		this.paramsFormatTags.add("paramsFormat");
		this.paramsFormatTags.add("formats");

		this.paramsMinValueTags = new ArrayList<String>();
		this.paramsMinValueTags.add("paramsMinValue");
		this.paramsMinValueTags.add("paramsMinimumValue");
		this.paramsMinValueTags.add("minValues");

		this.paramsMaxValueTags = new ArrayList<String>();
		this.paramsMaxValueTags.add("paramsMaxValue");
		this.paramsMaxValueTags.add("paramsMaximumValue");
		this.paramsMaxValueTags.add("maxValues");

		this.paramsDefaultValueTags = new ArrayList<String>();
		this.paramsDefaultValueTags.add("paramsDefaultValue");
		this.paramsDefaultValueTags.add("defaultValues");

		this.paramsAllowableValuesTags = new ArrayList<String>();
		this.paramsAllowableValuesTags.add("paramsAllowableValues");
		this.paramsAllowableValuesTags.add("allowableValues");

		this.resourceTags = new ArrayList<String>();
		this.resourceTags.add("parentEndpointName");
		this.resourceTags.add("resourcePath");
		this.resourceTags.add("resource");

		this.responseTypeTags = new ArrayList<String>();
		this.responseTypeTags.add("responseType");
		this.responseTypeTags.add("outputType");
		this.responseTypeTags.add("returnType");

		this.inputTypeTags = new ArrayList<String>();
		this.inputTypeTags.add("inputType");
		this.inputTypeTags.add("bodyType");

		this.defaultErrorTypeTags = new ArrayList<String>();
		this.defaultErrorTypeTags.add("defaultErrorType");

		this.apiDescriptionTags = new ArrayList<String>();
		this.apiDescriptionTags.add("apiDescription");

		this.operationNotesTags = new ArrayList<String>();
		this.operationNotesTags.add("description");
		this.operationNotesTags.add("comment");
		this.operationNotesTags.add("notes");

		this.operationSummaryTags = new ArrayList<String>();
		this.operationSummaryTags.add("summary");
		this.operationSummaryTags.add("endpointName");

		this.fieldDescriptionTags = new ArrayList<String>();
		this.fieldDescriptionTags.add("description");
		this.fieldDescriptionTags.add("comment");
		this.fieldDescriptionTags.add("return");

		this.fieldFormatTags = new ArrayList<String>();
		this.fieldFormatTags.add("format");

		this.fieldMinTags = new ArrayList<String>();
		this.fieldMinTags.add("min");
		this.fieldMinTags.add("minimum");

		this.fieldMaxTags = new ArrayList<String>();
		this.fieldMaxTags.add("max");
		this.fieldMaxTags.add("maximum");

		this.fieldDefaultTags = new ArrayList<String>();
		this.fieldDefaultTags.add("default");
		this.fieldDefaultTags.add("defaultValue");

		this.fieldAllowableValuesTags = new ArrayList<String>();
		this.fieldAllowableValuesTags.add("allowableValues");
		this.fieldAllowableValuesTags.add("values");
		this.fieldAllowableValuesTags.add("enum");

		this.requiredParamsTags = new ArrayList<String>();
		this.requiredParamsTags.add("requiredParams");

		this.optionalParamsTags = new ArrayList<String>();
		this.optionalParamsTags.add("optionalParams");

		this.requiredFieldTags = new ArrayList<String>();
		this.requiredFieldTags.add("required");
		this.requiredFieldTags.add("requiredField");

		this.optionalFieldTags = new ArrayList<String>();
		this.optionalFieldTags.add("optional");
		this.optionalFieldTags.add("optionalField");

		// JSR 303

		this.paramMinValueAnnotations = new ArrayList<String>();
		this.paramMinValueAnnotations.add("javax.validation.constraints.Size");
		this.paramMinValueAnnotations.add("javax.validation.constraints.DecimalMin");

		this.paramMaxValueAnnotations = new ArrayList<String>();
		this.paramMaxValueAnnotations.add("javax.validation.constraints.Size");
		this.paramMaxValueAnnotations.add("javax.validation.constraints.DecimalMax");

		this.fieldMinAnnotations = new ArrayList<String>();
		this.fieldMinAnnotations.add("javax.validation.constraints.Size");
		this.fieldMinAnnotations.add("javax.validation.constraints.DecimalMin");

		this.fieldMaxAnnotations = new ArrayList<String>();
		this.fieldMaxAnnotations.add("javax.validation.constraints.Size");
		this.fieldMaxAnnotations.add("javax.validation.constraints.DecimalMax");

		this.requiredParamAnnotations = new ArrayList<String>();
		this.requiredParamAnnotations.add("javax.validation.constraints.NotNull");

		this.optionalParamAnnotations = new ArrayList<String>();
		this.optionalParamAnnotations.add("javax.validation.constraints.Null");

		this.requiredFieldAnnotations = new ArrayList<String>();
		this.requiredFieldAnnotations.add("javax.validation.constraints.NotNull");

		this.optionalFieldAnnotations = new ArrayList<String>();
		this.optionalFieldAnnotations.add("javax.validation.constraints.Null");

		this.unauthOperationTags = new ArrayList<String>();
		this.unauthOperationTags.add("noAuth");
		this.unauthOperationTags.add("unauthorized");

		this.authOperationTags = new ArrayList<String>();
		this.authOperationTags.add("authentication");
		this.authOperationTags.add("authorization");

		this.unauthOperationTagValues = new ArrayList<String>();
		this.unauthOperationTagValues.add("not required");
		this.unauthOperationTagValues.add("off");
		this.unauthOperationTagValues.add("false");
		this.unauthOperationTagValues.add("none");

		this.operationScopeTags = new ArrayList<String>();
		this.operationScopeTags.add("scope");
		this.operationScopeTags.add("oauth2Scope");

		this.authOperationScopes = new ArrayList<String>();

		this.resourcePriorityTags = new ArrayList<String>();
		this.resourcePriorityTags.add("resourcePriority");
		this.resourcePriorityTags.add("resourceOrder");
		this.resourcePriorityTags.add("priority");

		this.resourceDescriptionTags = new ArrayList<String>();
		this.resourceDescriptionTags.add("resourceDescription");

		this.responseMessageSortMode = ResponseMessageSortMode.CODE_ASC;

		FirstNotNullTranslator fnnTranslator =

		new FirstNotNullTranslator();
		for (String paramAnnotation : ParserHelper.JAXRS_PARAM_ANNOTATIONS) {
			fnnTranslator.addNext(new AnnotationAwareTranslator(this).element(paramAnnotation, "value"));
		}

		fnnTranslator
				.addNext(
						new AnnotationAwareTranslator(this).ignore("javax.xml.bind.annotation.XmlTransient")
								.element("javax.xml.bind.annotation.XmlElement", "name").rootElement("javax.xml.bind.annotation.XmlRootElement", "name"))
				.addNext(
						new AnnotationAwareTranslator(this).ignore("javax.xml.bind.annotation.XmlTransient").element("javax.xml.bind.annotation.XmlAttribute",
								"name"))
				.addNext(
						new AnnotationAwareTranslator(this).ignore("com.fasterxml.jackson.annotation.JsonIgnore")
								.element("com.fasterxml.jackson.annotation.JsonProperty", "value")
								.rootElement("com.fasterxml.jackson.annotation.JsonRootName", "value"))

				.addNext(
						new AnnotationAwareTranslator(this).ignore("org.codehaus.jackson.annotate.JsonIgnore")
								.element("org.codehaus.jackson.annotate.JsonProperty", "value")
								.rootElement("org.codehaus.jackson.map.annotate.JsonRootName", "value")).addNext(new NameBasedTranslator(this));

		fnnTranslator.addNext(new NameBasedTranslator(this));

		this.translator = fnnTranslator;
	}

	public File getOutputDirectory() {
		return this.outputDirectory;
	}

	public String getDocBasePath() {
		return this.docBasePath;
	}

	/**
	 * This sets the docBasePath
	 * @param docBasePath the docBasePath to set
	 * @return this
	 */
	public DocletOptions setDocBasePath(String docBasePath) {
		this.docBasePath = docBasePath;
		return this;
	}

	/**
	 * This gets the resourceRootPath
	 * @return the resourceRootPath
	 */
	public String getResourceRootPath() {
		return this.resourceRootPath;
	}

	/**
	 * This sets the resourceRootPath
	 * @param resourceRootPath the resourceRootPath to set
	 * @return this
	 */
	public DocletOptions setResourceRootPath(String resourceRootPath) {
		this.resourceRootPath = resourceRootPath;
		return this;
	}

	public String getApiBasePath() {
		return this.apiBasePath;
	}

	/**
	 * This sets the apiBasePath
	 * @param apiBasePath the apiBasePath to set
	 * @return this
	 */
	public DocletOptions setApiBasePath(String apiBasePath) {
		this.apiBasePath = apiBasePath;
		return this;
	}

	public String getApiVersion() {
		return this.apiVersion;
	}

	/**
	 * This sets the apiVersion
	 * @param apiVersion the apiVersion to set
	 * @return this
	 */
	public DocletOptions setApiVersion(String apiVersion) {
		this.apiVersion = apiVersion;
		return this;
	}

	/**
	 * This gets the swaggerUiPath
	 * @return the swaggerUiPath
	 */
	public String getSwaggerUiPath() {
		return this.swaggerUiPath;
	}

	/**
	 * This gets the responseMessageTags
	 * @return the responseMessageTags
	 */
	public List<String> getResponseMessageTags() {
		return this.responseMessageTags;
	}

	/**
	 * This gets the excludeOperationTags
	 * @return the excludeOperationTags
	 */
	public List<String> getExcludeOperationTags() {
		return this.excludeOperationTags;
	}

	/**
	 * This gets the excludeClassTags
	 * @return the excludeClassTags
	 */
	public List<String> getExcludeClassTags() {
		return this.excludeClassTags;
	}

	/**
	 * This gets the excludeFieldTags
	 * @return the excludeFieldTags
	 */
	public List<String> getExcludeFieldTags() {
		return this.excludeFieldTags;
	}

	/**
	 * This gets the excludeParamAnnotations
	 * @return the excludeParamAnnotations
	 */
	public List<String> getExcludeParamAnnotations() {
		return this.excludeParamAnnotations;
	}

	/**
	 * This gets the excludeParamsTags
	 * @return the excludeParamsTags
	 */
	public List<String> getExcludeParamsTags() {
		return this.excludeParamsTags;
	}

	/**
	 * This gets the csvParamsTags
	 * @return the csvParamsTags
	 */
	public List<String> getCsvParamsTags() {
		return this.csvParamsTags;
	}

	public List<String> getImplicitParamTags() {
		return this.implicitParamTags;
	}

	/**
	 * This gets the paramsFormatTags
	 * @return the paramsFormatTags
	 */
	public List<String> getParamsFormatTags() {
		return this.paramsFormatTags;
	}

	/**
	 * This gets the paramsMinValueTags
	 * @return the paramsMinValueTags
	 */
	public List<String> getParamsMinValueTags() {
		return this.paramsMinValueTags;
	}

	/**
	 * This gets the paramsMaxValueTags
	 * @return the paramsMaxValueTags
	 */
	public List<String> getParamsMaxValueTags() {
		return this.paramsMaxValueTags;
	}

	/**
	 * This gets the paramsDefaultValueTags
	 * @return the paramsDefaultValueTags
	 */
	public List<String> getParamsDefaultValueTags() {
		return this.paramsDefaultValueTags;
	}

	/**
	 * This gets the paramsAllowableValuesTags
	 * @return the paramsAllowableValuesTags
	 */
	public List<String> getParamsAllowableValuesTags() {
		return this.paramsAllowableValuesTags;
	}

	/**
	 * This gets the paramsNameTags
	 * @return the paramsNameTags
	 */
	public List<String> getParamsNameTags() {
		return this.paramsNameTags;
	}

	/**
	 * This gets the resourceTags
	 * @return the resourceTags
	 */
	public List<String> getResourceTags() {
		return this.resourceTags;
	}

	/**
	 * This gets prefixes of the FQN of resource classes to exclude
	 * @return the prefixes of the FQN of resource classes to exclude
	 */
	public List<String> getExcludeResourcePrefixes() {
		return this.excludeResourcePrefixes;
	}

	/**
	 * This sets the prefixes of the FQN of resource classes to exclude
	 * @param excludeResourcePrefixes the prefixes of the FQN of resource classes to exclude
	 * @return this
	 */
	public DocletOptions setExcludeResourcePrefixes(List<String> excludeResourcePrefixes) {
		this.excludeResourcePrefixes = excludeResourcePrefixes;
		return this;
	}

	/**
	 * This gets prefixes of the FQN of resource classes to include, if specified then resources must match these
	 * @return the prefixes of the FQN of resource classes to include, if specified then resources must match these
	 */
	public List<String> getIncludeResourcePrefixes() {
		return this.includeResourcePrefixes;
	}

	/**
	 * This sets the prefixes of the FQN of resource classes to include, if specified then resources must match these
	 * @param includeResourcePrefixes the prefixes of the FQN of resource classes to include, if specified then resources must match these
	 * @return this
	 */
	public DocletOptions setIncludeResourcePrefixes(List<String> includeResourcePrefixes) {
		this.includeResourcePrefixes = includeResourcePrefixes;
		return this;
	}

	/**
	 * This gets the discriminatorAnnotations
	 * @return the discriminatorAnnotations
	 */
	public List<String> getDiscriminatorAnnotations() {
		return this.discriminatorAnnotations;
	}

	/**
	 * This gets the subTypesAnnotations
	 * @return the subTypesAnnotations
	 */
	public List<String> getSubTypesAnnotations() {
		return this.subTypesAnnotations;
	}

	/**
	 * This gets prefixes of the FQN of model classes to exclude
	 * @return prefixes of the FQN of model classes to exclude
	 */
	public List<String> getExcludeModelPrefixes() {
		return this.excludeModelPrefixes;
	}

	/**
	 * This sets the prefixes of the FQN of model classes to exclude
	 * @param excludeModelPrefixes prefixes of the FQN of model classes to exclude
	 * @return this
	 */
	public DocletOptions setExcludeModelPrefixes(List<String> excludeModelPrefixes) {
		this.excludeModelPrefixes = excludeModelPrefixes;
		return this;
	}

	/**
	 * This gets a list of FQN of types which simply wrap the
	 * real underlying data type
	 * @return The type
	 */
	public List<String> getGenericWrapperTypes() {
		return this.genericWrapperTypes;
	}

	/**
	 * This gets the fileParameterAnnotations
	 * @return the fileParameterAnnotations
	 */
	public List<String> getFileParameterAnnotations() {
		return this.fileParameterAnnotations;
	}

	/**
	 * This gets the fileParameterTypes
	 * @return the fileParameterTypes
	 */
	public List<String> getFileParameterTypes() {
		return this.fileParameterTypes;
	}

	/**
	 * This gets the formParameterAnnotations
	 * @return the formParameterAnnotations
	 */
	public List<String> getFormParameterAnnotations() {
		return this.formParameterAnnotations;
	}

	/**
	 * This gets the formParameterTypes
	 * @return the formParameterTypes
	 */
	public List<String> getFormParameterTypes() {
		return this.formParameterTypes;
	}

	/**
	 * This gets the compositeParamAnnotations
	 * @return the compositeParamAnnotations
	 */
	public List<String> getCompositeParamAnnotations() {
		return this.compositeParamAnnotations;
	}

	/**
	 * This gets the compositeParamTypes
	 * @return the compositeParamTypes
	 */
	public List<String> getCompositeParamTypes() {
		return this.compositeParamTypes;
	}

	/**
	 * This gets the parameterNameAnnotations
	 * @return the parameterNameAnnotations
	 */
	public List<String> getParameterNameAnnotations() {
		return this.parameterNameAnnotations;
	}

	/**
	 * This gets the stringTypePrefixes
	 * @return the stringTypePrefixes
	 */
	public List<String> getStringTypePrefixes() {
		return this.stringTypePrefixes;
	}

	/**
	 * This gets the longTypePrefixes
	 * @return the longTypePrefixes
	 */
	public List<String> getLongTypePrefixes() {
		return this.longTypePrefixes;
	}

	/**
	 * This gets the intTypePrefixes
	 * @return the intTypePrefixes
	 */
	public List<String> getIntTypePrefixes() {
		return this.intTypePrefixes;
	}

	/**
	 * This gets the floatTypePrefixes
	 * @return the floatTypePrefixes
	 */
	public List<String> getFloatTypePrefixes() {
		return this.floatTypePrefixes;
	}

	/**
	 * This gets the doubleTypePrefixes
	 * @return the doubleTypePrefixes
	 */
	public List<String> getDoubleTypePrefixes() {
		return this.doubleTypePrefixes;
	}

	/**
	 * This is whether model parsing is enabled
	 * @return Whether model parsing is enabled
	 */
	public boolean isParseModels() {
		return this.parseModels;
	}

	/**
	 * This is whether to use FQN of classes for model ids, only needed if your model uses
	 * same name classes from different packages.
	 * @return the useFullModelIds
	 */
	public boolean isUseFullModelIds() {
		return this.useFullModelIds;
	}

	/**
	 * This sets the useFullModelIds
	 * @param useFullModelIds the useFullModelIds to set
	 */
	public void setUseFullModelIds(boolean useFullModelIds) {
		this.useFullModelIds = useFullModelIds;
	}

	/**
	 * This is whether model fields are required by default e.g. if it is not specified whether a field is optional or not
	 * @return whether model fields are required by default e.g. if it is not specified whether a field is optional or not
	 */
	public boolean isModelFieldsRequiredByDefault() {
		return this.modelFieldsRequiredByDefault;
	}

	/**
	 * This sets the modelFieldsRequiredByDefault
	 * @param modelFieldsRequiredByDefault the modelFieldsRequiredByDefault to set
	 * @return this
	 */
	public DocletOptions setModelFieldsRequiredByDefault(boolean modelFieldsRequiredByDefault) {
		this.modelFieldsRequiredByDefault = modelFieldsRequiredByDefault;
		return this;
	}

	/**
	 * This gets the modelFieldsXmlAccessTypeEnabled
	 * @return the modelFieldsXmlAccessTypeEnabled
	 */
	public boolean isModelFieldsXmlAccessTypeEnabled() {
		return this.modelFieldsXmlAccessTypeEnabled;
	}

	/**
	 * This sets the modelFieldsXmlAccessTypeEnabled
	 * @param modelFieldsXmlAccessTypeEnabled the modelFieldsXmlAccessTypeEnabled to set
	 * @return this
	 */
	public DocletOptions setModelFieldsXmlAccessTypeEnabled(boolean modelFieldsXmlAccessTypeEnabled) {
		this.modelFieldsXmlAccessTypeEnabled = modelFieldsXmlAccessTypeEnabled;
		return this;
	}

	/**
	 * This gets the modelFieldsDefaultXmlAccessTypeEnabled
	 * @return the modelFieldsDefaultXmlAccessTypeEnabled
	 */
	public boolean isModelFieldsDefaultXmlAccessTypeEnabled() {
		return this.modelFieldsDefaultXmlAccessTypeEnabled;
	}

	/**
	 * This sets the modelFieldsDefaultXmlAccessTypeEnabled
	 * @param modelFieldsDefaultXmlAccessTypeEnabled the modelFieldsDefaultXmlAccessTypeEnabled to set
	 * @return this
	 */
	public DocletOptions setModelFieldsDefaultXmlAccessTypeEnabled(boolean modelFieldsDefaultXmlAccessTypeEnabled) {
		this.modelFieldsDefaultXmlAccessTypeEnabled = modelFieldsDefaultXmlAccessTypeEnabled;
		return this;
	}

	/**
	 * This gets the modelFieldsNamingConvention
	 * @return the modelFieldsNamingConvention
	 */
	public NamingConvention getModelFieldsNamingConvention() {
		return this.modelFieldsNamingConvention;
	}

	/**
	 * This sets the modelFieldsNamingConvention
	 * @param modelFieldsNamingConvention the modelFieldsNamingConvention to set
	 * @return this
	 */
	public DocletOptions setModelFieldsNamingConvention(NamingConvention modelFieldsNamingConvention) {
		this.modelFieldsNamingConvention = modelFieldsNamingConvention;
		return this;
	}

	/**
	 * This gets the responseTypeTags
	 * @return the responseTypeTags
	 */
	public List<String> getResponseTypeTags() {
		return this.responseTypeTags;
	}

	/**
	 * This gets tags that can customize the type for input body params
	 * @return The list of tags that can customize input body params
	 */
	public List<String> getInputTypeTags() {
		return this.inputTypeTags;
	}

	/**
	 * This gets the defaultErrorTypeTags
	 * @return the defaultErrorTypeTags
	 */
	public List<String> getDefaultErrorTypeTags() {
		return this.defaultErrorTypeTags;
	}

	/**
	 * This gets a list of javadoc tag names that can be used for the api description
	 * @return list of javadoc tag names that can be used for the api description
	 */
	public List<String> getApiDescriptionTags() {
		return this.apiDescriptionTags;
	}

	/**
	 * This gets a list of javadoc tag names that can be used for the operation notes
	 * @return list of javadoc tag names that can be used for the operation notes
	 */
	public List<String> getOperationNotesTags() {
		return this.operationNotesTags;
	}

	/**
	 * This gets a list of javadoc tag names that can be used for the operation summary
	 * @return a list of javadoc tag names that can be used for the operation summary
	 */
	public List<String> getOperationSummaryTags() {
		return this.operationSummaryTags;
	}

	/**
	 * This gets list of javadoc tag names that can be used for the model field/method descriptions
	 * @return list of javadoc tag names that can be used for the model field/method descriptions
	 */
	public List<String> getFieldDescriptionTags() {
		return this.fieldDescriptionTags;
	}

	/**
	 * This gets list of javadoc tag names that can be used for ordering resources in the resource listing
	 * @return the list of javadoc tag names that can be used for ordering resources in the resource listing
	 */
	public List<String> getResourcePriorityTags() {
		return this.resourcePriorityTags;
	}

	/**
	 * This gets list of javadoc tag names that can be used for the description of resources
	 * @return the list of javadoc tag names that can be used for the description of resources
	 */
	public List<String> getResourceDescriptionTags() {
		return this.resourceDescriptionTags;
	}

	/**
	 * This gets the fieldFormatTags
	 * @return the fieldFormatTags
	 */
	public List<String> getFieldFormatTags() {
		return this.fieldFormatTags;
	}

	/**
	 * This gets the fieldMinTags
	 * @return the fieldMinTags
	 */
	public List<String> getFieldMinTags() {
		return this.fieldMinTags;
	}

	/**
	 * This gets the fieldMaxTags
	 * @return the fieldMaxTags
	 */
	public List<String> getFieldMaxTags() {
		return this.fieldMaxTags;
	}

	/**
	 * This gets the fieldDefaultTags
	 * @return the fieldDefaultTags
	 */
	public List<String> getFieldDefaultTags() {
		return this.fieldDefaultTags;
	}

	/**
	 * This gets the fieldAllowableValuesTags
	 * @return the fieldAllowableValuesTags
	 */
	public List<String> getFieldAllowableValuesTags() {
		return this.fieldAllowableValuesTags;
	}

	/**
	 * This gets the requiredParamsTags
	 * @return the requiredParamsTags
	 */
	public List<String> getRequiredParamsTags() {
		return this.requiredParamsTags;
	}

	/**
	 * This gets the optionalParamsTags
	 * @return the optionalParamsTags
	 */
	public List<String> getOptionalParamsTags() {
		return this.optionalParamsTags;
	}

	/**
	 * This gets the requiredFieldTags
	 * @return the requiredFieldTags
	 */
	public List<String> getRequiredFieldTags() {
		return this.requiredFieldTags;
	}

	/**
	 * This gets the optionalFieldTags
	 * @return the optionalFieldTags
	 */
	public List<String> getOptionalFieldTags() {
		return this.optionalFieldTags;
	}

	public List<String> getParamMinValueAnnotations() {
		return this.paramMinValueAnnotations;
	}

	public List<String> getParamMaxValueAnnotations() {
		return this.paramMaxValueAnnotations;
	}

	public List<String> getFieldMinAnnotations() {
		return this.fieldMinAnnotations;
	}

	public List<String> getFieldMaxAnnotations() {
		return this.fieldMaxAnnotations;
	}

	public List<String> getRequiredParamAnnotations() {
		return this.requiredParamAnnotations;
	}

	public List<String> getOptionalParamAnnotations() {
		return this.optionalParamAnnotations;
	}

	public List<String> getRequiredFieldAnnotations() {
		return this.requiredFieldAnnotations;
	}

	public List<String> getOptionalFieldAnnotations() {
		return this.optionalFieldAnnotations;
	}

	/**
	 * This gets the unauthOperationTags
	 * @return the unauthOperationTags
	 */
	public List<String> getUnauthOperationTags() {
		return this.unauthOperationTags;
	}

	/**
	 * This gets the authOperationTags
	 * @return the authOperationTags
	 */
	public List<String> getAuthOperationTags() {
		return this.authOperationTags;
	}

	/**
	 * This gets the unauthOperationTagValues
	 * @return the unauthOperationTagValues
	 */
	public List<String> getUnauthOperationTagValues() {
		return this.unauthOperationTagValues;
	}

	/**
	 * This gets the operationScopeTags
	 * @return the operationScopeTags
	 */
	public List<String> getOperationScopeTags() {
		return this.operationScopeTags;
	}

	/**
	 * This gets the authOperationScopes
	 * @return the authOperationScopes
	 */
	public List<String> getAuthOperationScopes() {
		return this.authOperationScopes;
	}

	public Recorder getRecorder() {
		return this.recorder;
	}

	/**
	 * This sets the recorder to use
	 * @param recorder The recorder
	 * @return this
	 */
	public DocletOptions setRecorder(Recorder recorder) {
		this.recorder = recorder;
		return this;
	}

	public Translator getTranslator() {
		return this.translator;
	}

	/**
	 * This sets the translator to use
	 * @param translator The translator
	 * @return this
	 */
	public DocletOptions setTranslator(Translator translator) {
		this.translator = translator;
		return this;
	}

	/**
	 * This gets the sortResourcesByPath
	 * @return the sortResourcesByPath
	 */
	public boolean isSortResourcesByPath() {
		return this.sortResourcesByPath;
	}

	/**
	 * This sets the sortResourcesByPath
	 * @param sortResourcesByPath the sortResourcesByPath to set
	 * @return this
	 */
	public DocletOptions setSortResourcesByPath(boolean sortResourcesByPath) {
		this.sortResourcesByPath = sortResourcesByPath;
		return this;
	}

	/**
	 * This gets the sortResourcesByPriority
	 * @return the sortResourcesByPriority
	 */
	public boolean isSortResourcesByPriority() {
		return this.sortResourcesByPriority;
	}

	/**
	 * This sets the sortResourcesByPriority
	 * @param sortResourcesByPriority the sortResourcesByPriority to set
	 * @return this
	 */
	public DocletOptions setSortResourcesByPriority(boolean sortResourcesByPriority) {
		this.sortResourcesByPriority = sortResourcesByPriority;
		return this;
	}

	/**
	 * This gets the sortOperationsByPath
	 * @return the sortOperationsByPath
	 */
	public boolean isSortApisByPath() {
		return this.sortApisByPath;
	}

	/**
	 * This sets the sortApisByPath
	 * @param sortApisByPath the sortApisByPath to set
	 * @return this
	 */
	public DocletOptions setSortApisByPath(boolean sortApisByPath) {
		this.sortApisByPath = sortApisByPath;
		return this;
	}

	/**
	 * This gets the includeSwaggerUi
	 * @return the includeSwaggerUi
	 */
	public boolean isIncludeSwaggerUi() {
		return this.includeSwaggerUi;
	}

	/**
	 * This sets the includeSwaggerUi
	 * @param includeSwaggerUi the includeSwaggerUi to set
	 * @return this
	 */
	public DocletOptions setIncludeSwaggerUi(boolean includeSwaggerUi) {
		this.includeSwaggerUi = includeSwaggerUi;
		return this;
	}

	/**
	 * This gets the excludeDeprecatedResourceClasses
	 * @return the excludeDeprecatedResourceClasses
	 */
	public boolean isExcludeDeprecatedResourceClasses() {
		return this.excludeDeprecatedResourceClasses;
	}

	/**
	 * This gets the excludeDeprecatedModelClasses
	 * @return the excludeDeprecatedModelClasses
	 */
	public boolean isExcludeDeprecatedModelClasses() {
		return this.excludeDeprecatedModelClasses;
	}

	/**
	 * This gets the excludeDeprecatedOperations
	 * @return the excludeDeprecatedOperations
	 */
	public boolean isExcludeDeprecatedOperations() {
		return this.excludeDeprecatedOperations;
	}

	/**
	 * This sets the excludeDeprecatedOperations
	 * @param excludeDeprecatedOperations the excludeDeprecatedOperations to set
	 * @return this
	 */
	public DocletOptions setExcludeDeprecatedOperations(boolean excludeDeprecatedOperations) {
		this.excludeDeprecatedOperations = excludeDeprecatedOperations;
		return this;
	}

	/**
	 * This gets the excludeDeprecatedFields
	 * @return the excludeDeprecatedFields
	 */
	public boolean isExcludeDeprecatedFields() {
		return this.excludeDeprecatedFields;
	}

	/**
	 * This sets the excludeDeprecatedFields
	 * @param excludeDeprecatedFields the excludeDeprecatedFields to set
	 * @return this
	 */
	public DocletOptions setExcludeDeprecatedFields(boolean excludeDeprecatedFields) {
		this.excludeDeprecatedFields = excludeDeprecatedFields;
		return this;
	}

	/**
	 * This gets the excludeDeprecatedParams
	 * @return the excludeDeprecatedParams
	 */
	public boolean isExcludeDeprecatedParams() {
		return this.excludeDeprecatedParams;
	}

	/**
	 * This sets the excludeDeprecatedParams
	 * @param excludeDeprecatedParams the excludeDeprecatedParams to set
	 * @return this
	 */
	public DocletOptions setExcludeDeprecatedParams(boolean excludeDeprecatedParams) {
		this.excludeDeprecatedParams = excludeDeprecatedParams;
		return this;
	}

	/**
	 * This gets the responseMessageSortMode
	 * @return the responseMessageSortMode
	 */
	public ResponseMessageSortMode getResponseMessageSortMode() {
		return this.responseMessageSortMode;
	}

	/**
	 * This sets the responseMessageSortMode
	 * @param responseMessageSortMode the responseMessageSortMode to set
	 * @return this
	 */
	public DocletOptions setResponseMessageSortMode(ResponseMessageSortMode responseMessageSortMode) {
		this.responseMessageSortMode = responseMessageSortMode;
		return this;
	}

	/**
	 * This gets the apiAuthorizations
	 * @return the apiAuthorizations
	 */
	public ApiAuthorizations getApiAuthorizations() {
		return this.apiAuthorizations;
	}

	/**
	 * This sets the apiAuthorizations
	 * @param apiAuthorizations the apiAuthorizations to set
	 * @return this
	 */
	public DocletOptions setApiAuthorizations(ApiAuthorizations apiAuthorizations) {
		this.apiAuthorizations = apiAuthorizations;
		return this;
	}

	/**
	 * This gets the apiInfo
	 * @return the apiInfo
	 */
	public ApiInfo getApiInfo() {
		return this.apiInfo;
	}

	/**
	 * This sets the apiInfo
	 * @param apiInfo the apiInfo to set
	 * @return this
	 */
	public DocletOptions setApiInfo(ApiInfo apiInfo) {
		this.apiInfo = apiInfo;
		return this;
	}

	/**
	 * This gets the extraApiDeclarations
	 * @return the extraApiDeclarations
	 */
	public List<ApiDeclaration> getExtraApiDeclarations() {
		return this.extraApiDeclarations;
	}

	/**
	 * This sets the extraApiDeclarations
	 * @param extraApiDeclarations the extraApiDeclarations to set
	 */
	public DocletOptions setExtraApiDeclarations(List<ApiDeclaration> extraApiDeclarations) {
		this.extraApiDeclarations = extraApiDeclarations;
		return this;
	}

	/**
	 * This sets the variable replacements to use
	 * @param variableReplacements properties to use for variable replacements
	 * @return this
	 */
	public DocletOptions setVariableReplacements(Properties variableReplacements) {
		this.variableReplacements = variableReplacements;
		return this;
	}

	/**
	 * This gets the profileMode
	 * @return the profileMode
	 */
	public boolean isProfileMode() {
		return this.profileMode;
	}

	/**
	 * This sets the profileMode
	 * @param profileMode the profileMode to set
	 */
	public void setProfileMode(boolean profileMode) {
		this.profileMode = profileMode;
	}

	/**
	 * This gets the logDebug
	 * @return the logDebug
	 */
	public boolean isLogDebug() {
		return this.logDebug;
	}

	/**
	 * This replaces any variables in the given value with replacements defined in the doclets variable replacements file
	 * @param value The value to replace variables in
	 * @return The value with any variable references replaced
	 */
	public String replaceVars(String value) {
		if (value != null && this.variableReplacements != null && !this.variableReplacements.isEmpty()) {
			return VariableReplacer.replaceVariables(this.variableReplacements, value);
		}
		return value;
	}

}

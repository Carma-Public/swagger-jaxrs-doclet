package com.carma.swagger.doclet;

import java.util.HashMap;
import java.util.Map;

import com.carma.swagger.doclet.parser.JaxRsAnnotationParser;
import com.sun.javadoc.LanguageVersion;
import com.sun.javadoc.RootDoc;

@SuppressWarnings("javadoc")
public class ServiceDoclet {

	/**
	 * Generate documentation here.
	 * This method is required for all doclets.
	 * @param doc The root doc
	 * @return true on success.
	 */
	public static boolean start(RootDoc doc) {

		long start = System.currentTimeMillis();

		String[][] additionalParams = doc.options();
		sanitizeAdditionalParams(additionalParams);
		DocletOptions options = DocletOptions.parse(additionalParams);
		// delay before
		if (options.isProfileMode()) {
			try {
				Thread.sleep(30000);
			} catch (InterruptedException ex) {
				// ignore
			}
		}

		// exec
		boolean result = new JaxRsAnnotationParser(options, doc).run();

		// delay after
		if (options.isProfileMode()) {
			try {
				Thread.sleep(30000);
			} catch (InterruptedException ex) {
				// ignore
			}
		}

		long end = System.currentTimeMillis();

		System.out.println("Completed doclet api generation in " + (end - start) + " ms.");

		return result;
	}

	private static void sanitizeAdditionalParams(String[][] additionalParams) {
		// if given paths as uris e.g. with file: prefix then we
		// convert to unix style path
		for (String[] nvp : additionalParams) {
			if (nvp.length > 1) {
				String val = nvp[1];
				if (val.startsWith("file:")) {
					String replacedPath = val.replace("file:", "");
					replacedPath = replacedPath.replace("//", "/");
					replacedPath = replacedPath.replace("%20", " ");
					nvp[1] = replacedPath;
				}
			}
		}
	}

	/**
	 * Check for doclet-added options. Returns the number of
	 * arguments you must specify on the command line for the
	 * given option. For example, "-d docs" would return 2. <br>
	 * This method is required if the doclet contains any options. If this method is missing, Javadoc will print an invalid flag error for every option.
	 * @param option The option to check
	 * @return number of arguments on the command line for an option
	 *         including the option name itself. Zero return means
	 *         option not known. Negative value means error occurred.
	 */
	public static int optionLength(String option) {
		Map<String, Integer> options = new HashMap<String, Integer>();
		options.put("-d", 2);
		options.put("-docBasePath", 2);
		options.put("-apiBasePath", 2);
		options.put("-apiVersion", 2);
		options.put("-resourceRootPath", 2);

		options.put("-genericWrapperTypes", 2);

		options.put("-fileParameterAnnotations", 2);
		options.put("-fileParameterTypes", 2);
		options.put("-formParameterAnnotations", 2);
		options.put("-formParameterTypes", 2);
		options.put("-stringTypePrefixes", 2);
		options.put("-floatTypePrefixes", 2);
		options.put("-doubleTypePrefixes", 2);
		options.put("-intTypePrefixes", 2);
		options.put("-longTypePrefixes", 2);

		options.put("-compositeParamAnnotations", 2);
		options.put("-compositeParamTypes", 2);
		options.put("-subTypesAnnotations", 2);

		options.put("-responseTypeTags", 2);
		options.put("-inputTypeTags", 2);

		options.put("-defaultErrorTypeTags", 2);
		options.put("-responseMessageTags", 2);

		options.put("-resourceTags", 2);
		options.put("-apiDescriptionTags", 2);
		options.put("-operationNotesTags", 2);
		options.put("-operationSummaryTags", 2);
		options.put("-fieldDescriptionTags", 2);
		options.put("-fieldFormatTags", 2);
		options.put("-fieldMinTags", 2);
		options.put("-fieldMaxTags", 2);
		options.put("-fieldDefaultTags", 2);
		options.put("-fieldAllowableValuesTags", 2);

		options.put("-requiredParamsTags", 2);
		options.put("-optionalParamsTags", 2);
		options.put("-paramsFormatTags", 2);
		options.put("-paramsMinValueTags", 2);
		options.put("-paramsMaxValueTags", 2);
		options.put("-paramsDefaultValueTags", 2);
		options.put("-paramsAllowableValuesTags", 2);
		options.put("-paramsNameTags", 2);
		options.put("-parameterNameAnnotations", 2);

		options.put("-requiredFieldTags", 2);
		options.put("-optionalFieldTags", 2);

		// JSR 303
		options.put("-paramMinValueAnnotations", 2);
		options.put("-paramMaxValueAnnotations", 2);
		options.put("-fieldMinAnnotations", 2);
		options.put("-fieldMaxAnnotations", 2);
		options.put("-requiredParamAnnotations", 2);
		options.put("-optionalParamAnnotations", 2);
		options.put("-requiredFieldAnnotations", 2);
		options.put("-optionalFieldAnnotations", 2);

		// file inclusions
		options.put("-apiAuthorizationsFile", 2);
		options.put("-apiInfoFile", 2);
		options.put("-extraApiDeclarations", 2);

		options.put("-unauthOperationTags", 2);
		options.put("-authOperationTags", 2);
		options.put("-unauthOperationTagValues", 2);
		options.put("-authOperationScopes", 2);
		options.put("-operationScopeTags", 2);

		// options for configuring the generated json
		options.put("-serializationFeatures", 2);
		options.put("-deserializationFeatures", 2);
		options.put("-defaultTyping", 2);
		options.put("-serializationInclusion", 2);

		options.put("-variablesPropertiesFile", 2);
		options.put("-responseMessageSortMode", 2);

		options.put("-swaggerUiZipPath", 2); // kept for backward compatibility
		options.put("-swaggerUiPath", 2);
		// supports turning off copy of swagger ui, useful for tests and also for
		// people that use their own swagger ui files
		options.put("-disableCopySwaggerUi", 1);
		options.put("-skipUiFiles", 1);

		options.put("-logDebug", 1);
		options.put("-disableModels", 1);
		options.put("-useFullModelIds", 1);
		options.put("-modelFieldsRequiredByDefault", 1);
		options.put("-modelFieldsNamingConvention", 2);
		options.put("-disableModelFieldsXmlAccessType", 1);
		options.put("-defaultModelFieldsXmlAccessType", 1);

		// supports removing certain methods from the docs, e.g. for hidden/private
		// endpoints
		options.put("-typesToTreatAsOpaque", 2); // this is kept for backward compatibility
		options.put("-excludeModelPrefixes", 2);
		options.put("-excludeResourcePrefixes", 2);
		options.put("-includeResourcePrefixes", 2);

		options.put("-excludeParamAnnotations", 2);
		options.put("-excludeClassTags", 2);
		options.put("-excludeOperationTags", 2);
		options.put("-excludeFieldTags", 2);
		options.put("-excludeParamsTags", 2);

		// flags params as multiple
		options.put("-csvParamsTags", 2);

		// allows extra params on method
		options.put("-implicitParamTags", 2);

		// legacy no longer needed
		options.put("-crossClassResources", 1);

		// control deprecation exclusion
		options.put("-disableDeprecatedResourceClassExclusion", 1);
		options.put("-disableDeprecatedModelClassExclusion", 1);
		options.put("-disableDeprecatedOperationExclusion", 1);
		options.put("-disableDeprecatedFieldExclusion", 1);
		options.put("-disableDeprecatedParamExclusion", 1);
		options.put("-customDeprecatedAnnotation", 2);

		// sort options
		options.put("-sortResourcesByPath", 1);
		options.put("-sortResourcesByPriority", 1);
		options.put("-sortApisByPath", 1);

		// profile mode - adds extra delay at start and end to help capture profile stats
		options.put("-profileMode", 1);

		// standard doclet options that we don't use but have here to avoid errors with tools like gradle
		// that auto pass them in
		options.put("-doctitle", 2);
		options.put("-windowtitle", 2);

		Integer value = options.get(option);
		if (value != null) {
			return value;
		} else {
			return 0;
		}
	}

	/**
	 * Return the version of the Java Programming Language supported
	 * by this doclet. <br>
	 * This method is required by any doclet supporting a language version newer than 1.1.
	 * @return the language version supported by this doclet.
	 * @since 1.5
	 */
	public static LanguageVersion languageVersion() {
		return LanguageVersion.JAVA_1_5;
	}

}

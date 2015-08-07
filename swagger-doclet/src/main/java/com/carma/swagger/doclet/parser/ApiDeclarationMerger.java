package com.carma.swagger.doclet.parser;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.carma.swagger.doclet.model.Api;
import com.carma.swagger.doclet.model.ApiDeclaration;
import com.carma.swagger.doclet.model.HttpMethod;
import com.carma.swagger.doclet.model.Model;
import com.carma.swagger.doclet.model.Operation;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;

/**
 * The ApiDeclarationMerger represents a util that can merge api declarations together based on the resource path
 * @version $Id$
 * @author conor.roche
 */
public class ApiDeclarationMerger {

	private final String swaggerVersion;
	private final String apiVersion;
	private final String basePath;

	/**
	 * This creates a ApiDeclarationMerger
	 * @param swaggerVersion
	 * @param apiVersion
	 * @param basePath
	 */
	public ApiDeclarationMerger(String swaggerVersion, String apiVersion, String basePath) {
		super();
		this.swaggerVersion = swaggerVersion;
		this.apiVersion = apiVersion;
		this.basePath = basePath;
	}

	/**
	 * This merges the declarations in the given collection together as needed
	 * @param declarations The declarations to merge
	 * @return A collection of merged API declarations
	 */
	public Collection<ApiDeclaration> merge(Collection<ApiDeclaration> declarations) {
		return mergeApiDeclarations(declarations);
	}

	private Collection<ApiDeclaration> mergeApiDeclarations(Collection<ApiDeclaration> apiDeclarations) {
		List<ApiDeclaration> mergedApiDeclarations = Lists.newArrayList();
		// Guava ImmutableMultimaps for preserving ordering
		Multimaps.index(apiDeclarations, a -> a.getResourcePath()).asMap().forEach( (path, apiDecl) -> {
			mergedApiDeclarations.add(mergeResourceApiDeclarations(path, apiDecl));
		});
		return mergedApiDeclarations;
	}

	private ApiDeclaration mergeResourceApiDeclarations(String resourcePath, Collection<ApiDeclaration> apiDeclarations) {
		String apiVersion = this.apiVersion;
		String swaggerVersion = this.swaggerVersion;
		String basePath = this.basePath;
		int priority = Integer.MAX_VALUE;
		String description = null;

		List<Api> apis = Lists.newArrayList();
		Map<String, Model> models = Maps.newLinkedHashMap();
		for (ApiDeclaration apiDeclaration : apiDeclarations) {
			apiVersion = getFirstNonNull(apiVersion, apiDeclaration.getApiVersion());
			swaggerVersion = getFirstNonNull(swaggerVersion, apiDeclaration.getSwaggerVersion());
			basePath = getFirstNonNull(this.basePath, apiDeclaration.getBasePath());
			priority = getFirstNonNullNorVal(Integer.MAX_VALUE, Integer.MAX_VALUE, apiDeclaration.getPriority());
			description = getFirstNonNull(description, apiDeclaration.getDescription());

			apis.addAll(apiDeclaration.getApis());
			for (Map.Entry<String, Model> modelEntry : apiDeclaration.getModels().entrySet()) {
				// only add new models
				if (!models.containsKey(modelEntry.getKey())) {
					models.put(modelEntry.getKey(), modelEntry.getValue());
				}
			}
		}
		List<Api> mergedApis = mergeSameResourceApis(apis);
		ApiDeclaration newApi = new ApiDeclaration(swaggerVersion, apiVersion, basePath, resourcePath,
				mergedApis, models, priority, description);
		return newApi;
	}

	private List<Api> mergeSameResourceApis(Collection<Api> apis) {
		List<Api> mergedApis = Lists.newArrayList();
		Multimaps.index(apis, a -> a.getPath()).asMap().forEach((path, pathApis) -> {
			List<Operation> operations = pathApis.stream()
					.flatMap(a -> nullToEmpty(a.getOperations()).stream())
					.collect(Collectors.toList());
			operations = mergeSameResourcePathOperations(operations);
			Api firstApi = pathApis.iterator().next();
			Api mergedApi = new Api(path, firstApi.getDescription(), operations);
			mergedApis.add(mergedApi);
		});
		return mergedApis;
	}

	private List<Operation> mergeSameResourcePathOperations(Collection<Operation> operations) {
		// Merge Operations with same parameters and returntype but differing produces-mimetypes.
		// It seems Swagger currently does not support methods having multiple differing return types, only different
		// produces-mimetypes. See https://github.com/swagger-api/swagger-spec/issues/146 or
		// https://github.com/swagger-api/swagger-core/issues/521 . But doclet can at least support multiple java-methods
		// (one java-method per mimetype) for one Swagger-Operation.
		List<Operation> mergedOperations = Lists.newArrayList();
		Multimaps.index(operations, o -> operationSignature(o)).asMap().forEach((signature, signatureOps) -> {
			Operation firstOp = signatureOps.iterator().next();
			List<String> consumes = signatureOps.stream()
					.flatMap(o -> nullToEmpty(o.getConsumes()).stream())
					.distinct()
					.collect(Collectors.toList());
			Collection<String> produces = signatureOps.stream()
					.flatMap(o -> nullToEmpty(o.getProduces()).stream())
					.distinct()
					.collect(Collectors.toList());
			Operation mergedOperation = firstOp.consumes(consumes).produces(produces);
			mergedOperations.add(mergedOperation);
		});
		return mergedOperations;
	}

	private static String operationSignature(Operation o) {
		StringBuilder sb = new StringBuilder();
		sb.append(o.getType()).append(" ");
		sb.append(o.getMethod()).append(" (");
		Joiner.on(",").appendTo(sb, nullToEmpty(o.getParameters()).stream().map((p) -> p.getType() + " " + p.getName()).iterator());
		sb.append(")");
		return sb.toString();
	}

	private static <T> T getFirstNonNull(T defaultValue, T val) {
		if (val != null) {
			return val;
		}
		return defaultValue;
	}

	private static <T> T getFirstNonNullNorVal(T defaultValue, T excludeVal, T val) {
		if (val != null && !val.equals(excludeVal)) {
			return val;
		}
		return defaultValue;
	}

	private static <T> Collection<T> nullToEmpty(Collection<T> c) {
		return c != null ? c : Collections.emptyList();
	}
}

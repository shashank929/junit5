/*
 * Copyright 2015-2016 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.platform.launcher.core;

import static org.junit.platform.commons.meta.API.Usage.Experimental;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.platform.commons.meta.API;
import org.junit.platform.commons.util.PreconditionViolationException;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.DiscoveryFilter;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.Filter;
import org.junit.platform.launcher.EngineFilter;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.PostDiscoveryFilter;

/**
 * The {@code LauncherDiscoveryRequestBuilder} provides a light-weight DSL for
 * generating a {@link LauncherDiscoveryRequest}.
 *
 * <h4>Example</h4>
 *
 * <pre style="code">
 * import static org.junit.platform.engine.discovery.DiscoverySelectors.*;
 * import static org.junit.platform.engine.discovery.ClassFilter.*;
 * import static org.junit.platform.launcher.EngineFilter.*;
 * import static org.junit.platform.launcher.TagFilter.*;
 *
 * // ...
 *
 *   LauncherDiscoveryRequestBuilder.request()
 *     .selectors(
 *        selectJavaPackage("org.example.user"),
 *        selectJavaClass("org.example.payment.PaymentTests"),
 *        selectJavaClass(ShippingTests.class),
 *        selectJavaMethod("org.example.order.OrderTests#test1"),
 *        selectJavaMethod("org.example.order.OrderTests#test2()"),
 *        selectJavaMethod("org.example.order.OrderTests#test3(java.lang.String)"),
 *        selectJavaMethod("org.example.order.OrderTests", "test4"),
 *        selectJavaMethod(OrderTests.class, "test5"),
 *        selectJavaMethod(OrderTests.class, testMethod),
 *        selectClasspathRoots(Collections.singleton(new File("/my/local/path1"))),
 *        selectUniqueId("unique-id-1"),
 *        selectUniqueId("unique-id-2")
 *     )
 *     .filters(
 *        includeEngines("junit-jupiter", "spek"),
 *        // excludeEngines("junit-vintage"),
 *        includeTags("fast"),
 *        // excludeTags("slow"),
 *        includeClassNamePattern(".*Test[s]?")
 *        // includeClassNamePattern("org\.example\.tests.*")
 *     )
 *     .configurationParameter("key1", "value1")
 *     .configurationParameters(configParameterMap)
 *     .build();
 * </pre>
 *
 * @since 1.0
 * @see org.junit.platform.engine.discovery.DiscoverySelectors
 * @see org.junit.platform.engine.discovery.ClassFilter
 * @see org.junit.platform.launcher.EngineFilter
 * @see org.junit.platform.launcher.TagFilter
 */
@API(Experimental)
public final class LauncherDiscoveryRequestBuilder {

	private List<DiscoverySelector> selectors = new LinkedList<>();
	private List<EngineFilter> engineFilters = new LinkedList<>();
	private List<DiscoveryFilter<?>> discoveryFilters = new LinkedList<>();
	private List<PostDiscoveryFilter> postDiscoveryFilters = new LinkedList<>();
	private Map<String, String> configurationParameters = new HashMap<>();

	/**
	 * Create a new {@code LauncherDiscoveryRequestBuilder}.
	 */
	public static LauncherDiscoveryRequestBuilder request() {
		return new LauncherDiscoveryRequestBuilder();
	}

	/**
	 * Add all of the supplied {@code selectors} to the request.
	 *
	 * @param selectors the {@code DiscoverySelectors} to add
	 */
	public LauncherDiscoveryRequestBuilder selectors(DiscoverySelector... selectors) {
		if (selectors != null) {
			selectors(Arrays.asList(selectors));
		}
		return this;
	}

	/**
	 * Add all of the supplied {@code selectors} to the request.
	 *
	 * @param selectors the {@code DiscoverySelectors} to add
	 */
	public LauncherDiscoveryRequestBuilder selectors(List<? extends DiscoverySelector> selectors) {
		if (selectors != null) {
			this.selectors.addAll(selectors);
		}
		return this;
	}

	/**
	 * Add all of the supplied {@code filters} to the request.
	 *
	 * <p><strong>Warning</strong>: be cautious when registering multiple competing
	 * {@link EngineFilter#includeEngines include} {@code EngineFilters} or multiple
	 * competing {@link EngineFilter#excludeEngines exclude} {@code EngineFilters}
	 * for the same discovery request since doing so will likely lead to
	 * undesirable results (i.e., zero engines being active).
	 *
	 * @param filters the {@code Filter}s to add
	 */
	public LauncherDiscoveryRequestBuilder filters(Filter<?>... filters) {
		if (filters != null) {
			Arrays.stream(filters).forEach(this::storeFilter);
		}
		return this;
	}

	/**
	 * Add the supplied <em>configuration parameter</em> to the request.
	 */
	public LauncherDiscoveryRequestBuilder configurationParameter(String key, String value) {
		Preconditions.notBlank(key, "configuration parameter key must not be null or blank");
		this.configurationParameters.put(key, value);
		return this;
	}

	/**
	 * Add all of the supplied {@code configurationParameters} to the request.
	 *
	 * @param configurationParameters the map of configuration parameters to add
	 */
	public LauncherDiscoveryRequestBuilder configurationParameters(Map<String, String> configurationParameters) {
		if (configurationParameters != null) {
			configurationParameters.forEach(this::configurationParameter);
		}
		return this;
	}

	private void storeFilter(Filter<?> filter) {
		if (filter instanceof EngineFilter) {
			this.engineFilters.add((EngineFilter) filter);
		}
		else if (filter instanceof PostDiscoveryFilter) {
			this.postDiscoveryFilters.add((PostDiscoveryFilter) filter);
		}
		else if (filter instanceof DiscoveryFilter<?>) {
			this.discoveryFilters.add((DiscoveryFilter<?>) filter);
		}
		else {
			throw new PreconditionViolationException(
				String.format("Filter [%s] must implement %s, %s, or %s.", filter, EngineFilter.class.getSimpleName(),
					PostDiscoveryFilter.class.getSimpleName(), DiscoveryFilter.class.getSimpleName()));
		}
	}

	/**
	 * Build the {@link LauncherDiscoveryRequest} that has been configured via
	 * this builder.
	 */
	public LauncherDiscoveryRequest build() {
		LauncherConfigurationParameters launcherConfigurationParameters = new LauncherConfigurationParameters(
			this.configurationParameters);
		return new DefaultDiscoveryRequest(this.selectors, this.engineFilters, this.discoveryFilters,
			this.postDiscoveryFilters, launcherConfigurationParameters);
	}

}

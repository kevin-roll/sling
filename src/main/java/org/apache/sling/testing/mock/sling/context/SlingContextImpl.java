/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.testing.mock.sling.context;

import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.models.impl.FirstImplementationPicker;
import org.apache.sling.models.impl.injectors.BindingsInjector;
import org.apache.sling.models.impl.injectors.ChildResourceInjector;
import org.apache.sling.models.impl.injectors.OSGiServiceInjector;
import org.apache.sling.models.impl.injectors.RequestAttributeInjector;
import org.apache.sling.models.impl.injectors.ResourcePathInjector;
import org.apache.sling.models.impl.injectors.ResourceResolverInjector;
import org.apache.sling.models.impl.injectors.SelfInjector;
import org.apache.sling.models.impl.injectors.SlingObjectInjector;
import org.apache.sling.models.impl.injectors.ValueMapInjector;
import org.apache.sling.models.spi.ImplementationPicker;
import org.apache.sling.models.spi.Injector;
import org.apache.sling.settings.SlingSettingsService;
import org.apache.sling.testing.mock.osgi.context.OsgiContextImpl;
import org.apache.sling.testing.mock.sling.MockSling;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.builder.ContentBuilder;
import org.apache.sling.testing.mock.sling.loader.ContentLoader;
import org.apache.sling.testing.mock.sling.services.MockMimeTypeService;
import org.apache.sling.testing.mock.sling.services.MockModelAdapterFactory;
import org.apache.sling.testing.mock.sling.services.MockSlingSettingService;
import org.apache.sling.testing.mock.sling.servlet.MockRequestPathInfo;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.osgi.framework.ServiceReference;

import aQute.bnd.annotation.ConsumerType;

import com.google.common.collect.ImmutableSet;

/**
 * Defines Sling context objects with lazy initialization. Should not be used
 * directly but via the {@link org.apache.sling.testing.mock.sling.junit.SlingContext} JUnit
 * rule.
 */
@ConsumerType
public class SlingContextImpl extends OsgiContextImpl {

    // default to publish instance run mode
    static final Set<String> DEFAULT_RUN_MODES = ImmutableSet.<String> builder().add("publish").build();

    protected ResourceResolverFactory resourceResolverFactory;
    protected MockModelAdapterFactory modelAdapterFactory;
    protected ResourceResolverType resourceResolverType;
    protected ResourceResolver resourceResolver;
    protected MockSlingHttpServletRequest request;
    protected MockSlingHttpServletResponse response;
    protected SlingScriptHelper slingScriptHelper;
    protected ContentLoader contentLoader;
    protected ContentBuilder contentBuilder;

    /**
     * @param resourceResolverType Resource resolver type
     */
    protected void setResourceResolverType(final ResourceResolverType resourceResolverType) {
        this.resourceResolverType = resourceResolverType;
    }

    /**
     * Setup actions before test method execution
     */
    protected void setUp() {
        super.setUp();
        MockSling.setAdapterManagerBundleContext(bundleContext());
        this.resourceResolverFactory = newResourceResolverFactory();
        registerDefaultServices();
    }
    
    /**
     * Initialize mocked resource resolver factory.
     * @return Resource resolver factory
     */
    protected ResourceResolverFactory newResourceResolverFactory() {
        return ContextResourceResolverFactory.get(this.resourceResolverType);
    }

    /**
     * Default services that should be available for every unit test
     */
    protected void registerDefaultServices() {

        // resource resolver factory
        registerService(ResourceResolverFactory.class, this.resourceResolverFactory);
        
        // adapter factories
        modelAdapterFactory = new MockModelAdapterFactory(componentContext());
        registerService(AdapterFactory.class, modelAdapterFactory);

        // sling models injectors
        registerService(Injector.class, new BindingsInjector());
        registerService(Injector.class, new ChildResourceInjector());
        OSGiServiceInjector osgiServiceInjector = new OSGiServiceInjector();
        osgiServiceInjector.activate(componentContext());
        registerService(Injector.class, osgiServiceInjector);
        registerService(Injector.class, new RequestAttributeInjector());
        registerService(Injector.class, new ResourcePathInjector());
        registerService(Injector.class, new ResourceResolverInjector());
        registerService(Injector.class, new SelfInjector());
        registerService(Injector.class, new SlingObjectInjector());
        registerService(Injector.class, new ValueMapInjector());

        // sling models implementation pickers
        registerService(ImplementationPicker.class, new FirstImplementationPicker());

        // other services
        registerService(SlingSettingsService.class, new MockSlingSettingService(DEFAULT_RUN_MODES));
        registerService(MimeTypeService.class, new MockMimeTypeService());
    }

    /**
     * Teardown actions after test method execution
     */
    protected void tearDown() {

        if (this.resourceResolver != null) {
            // revert potential unsaved changes in resource resolver/JCR session
            this.resourceResolver.revert();
            Session session = this.resourceResolver.adaptTo(Session.class);
            if (session != null) {
                try {
                    session.refresh(false);
                } catch (RepositoryException ex) {
                    // ignore
                }
            }
        }

        this.modelAdapterFactory = null;
        this.componentContext = null;
        this.resourceResolver = null;
        this.request = null;
        this.response = null;
        this.slingScriptHelper = null;
        this.contentLoader = null;
        this.contentBuilder = null;

        MockSling.clearAdapterManagerBundleContext();
        
        super.tearDown();
    }

    /**
     * @return Resource resolver type
     */
    public final ResourceResolverType resourceResolverType() {
        return this.resourceResolverType;
    }

    /**
     * @return Resource resolver
     */
    public final ResourceResolver resourceResolver() {
        if (this.resourceResolver == null) {
            try {
                this.resourceResolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);
            } catch (LoginException ex) {
                throw new RuntimeException("Creating resource resolver failed.", ex);
            }
        }
        return this.resourceResolver;
    }
    
    /**
     * @return Sling request
     */
    public final MockSlingHttpServletRequest request() {
        if (this.request == null) {
            this.request = new MockSlingHttpServletRequest(this.resourceResolver(), this.bundleContext());

            // initialize sling bindings
            SlingBindings bindings = new SlingBindings();
            bindings.put(SlingBindings.REQUEST, this.request);
            bindings.put(SlingBindings.RESPONSE, response());
            bindings.put(SlingBindings.SLING, slingScriptHelper());
            this.request.setAttribute(SlingBindings.class.getName(), bindings);
        }
        return this.request;
    }

    /**
     * @return Request path info
     */
    public final MockRequestPathInfo requestPathInfo() {
        return (MockRequestPathInfo) request().getRequestPathInfo();
    }

    /**
     * @return Sling response
     */
    public final MockSlingHttpServletResponse response() {
        if (this.response == null) {
            this.response = new MockSlingHttpServletResponse();
        }
        return this.response;
    }

    /**
     * @return Sling script helper
     */
    public final SlingScriptHelper slingScriptHelper() {
        if (this.slingScriptHelper == null) {
            this.slingScriptHelper = MockSling.newSlingScriptHelper(this.request(), this.response(),
                    this.bundleContext());
        }
        return this.slingScriptHelper;
    }

    /**
     * @return Content loader
     */
    public ContentLoader load() {
        if (this.contentLoader == null) {
            this.contentLoader = new ContentLoader(resourceResolver(), bundleContext());
        }
        return this.contentLoader;
    }

    /**
     * @return Content builder for building test content
     */
    public ContentBuilder create() {
        if (this.contentBuilder == null) {
            this.contentBuilder = new ContentBuilder(resourceResolver());
        }
        return this.contentBuilder;
    }

    /**
     * @return Current resource
     */
    public final Resource currentResource() {
        return request().getResource();
    }

    /**
     * Set current resource in request.
     * @param resourcePath Resource path
     * @return Current resource
     */
    public final Resource currentResource(String resourcePath) {
        if (resourcePath != null) {
            Resource resource = resourceResolver().getResource(resourcePath);
            if (resource == null) {
                throw new IllegalArgumentException("Resource does not exist: " + resourcePath);
            }
            return currentResource(resource);
        } else {
            return currentResource((Resource) null);
        }
    }

    /**
     * Set current resource in request.
     * @param resource Resource
     * @return Current resource
     */
    public final Resource currentResource(Resource resource) {
        request().setResource(resource);
        return resource;
    }

    /**
     * Scan classpaths for given package name (and sub packages) to scan for and
     * register all classes with @Model annotation.
     * @param packageName Java package name
     */
    public final void addModelsForPackage(String packageName) {
        this.modelAdapterFactory.addModelsForPackage(packageName);
    }

    /**
     * Set current run mode(s).
     * @param runModes Run mode(s).
     */
    public final void runMode(String... runModes) {
        Set<String> newRunModes = ImmutableSet.<String> builder().add(runModes).build();
        ServiceReference ref = bundleContext().getServiceReference(SlingSettingsService.class.getName());
        if (ref != null) {
            MockSlingSettingService slingSettings = (MockSlingSettingService) bundleContext().getService(ref);
            slingSettings.setRunModes(newRunModes);
        }
    }

}

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
package org.apache.sling.api.security;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * The <code>ResourceAccessSecurity</code> defines a service API which might be
 * used in implementations of resource providers where the underlaying
 * persistence layer does not have any ACLs. The service should it make easy to
 * implement a lightweight access control in such sort of providers.
 * 
 * - Expected to only be implemented once in the framework/application (much
 * like the OSGi LogService or Configuration Admin Service) - ResourceProvider
 * implementations are encouraged to use this service for access control unless
 * the underlying storage already has it.
 * 
 */

public interface ResourceAccessSecurity {

    public Resource getReadableResource(Resource resource);

    public boolean canCreate(String absPathName, ResourceResolver resourceResolver);

    public boolean canUpdate(Resource resource);

    public boolean canDelete(Resource resource);

    public boolean canExecute(Resource resource);

    public boolean canReadValue(Resource resource, String valueName);

    public boolean canSetValue(Resource resource, String valueName);

    public boolean canDeleteValue(Resource resource, String valueName);

    /**
     * Allows to transform the query based on the current
     * user's credentials. Can be used to narrow down queries to omit results
     * that the current user is not allowed to see anyway, speeding up
     * downstream access control.
     * 
     * Query transformations are not critical with respect to access control as results
     * are checked using the canRead.. methods anyway. 
     * 
     * @param query the query
     * @param language the language in which the query is expressed
     * @param resourceResolver the resource resolver which resolves the query
     * @return the transformed query
     * @throws AccessSecurityException 
     */
    public String transformQuery(String query, String language, ResourceResolver resourceResolver)
            throws AccessSecurityException;

}

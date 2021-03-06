/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package info.wso2.scim2.compliance.protocol;


import info.wso2.scim2.compliance.objects.SCIMResourceType;
import info.wso2.scim2.compliance.objects.SCIMSchema;
import info.wso2.scim2.compliance.objects.SCIMServiceProviderConfig;

/**
 * This contains the meta data that need to configure the test suite.
 */
public class ComplianceTestMetaDataHolder {

    private String url;
    private String username;
    private String password;
    private String client_id;
    private String client_secret;
    private String authorization_server;
    private String authorization_header;
    private String authorization_method;
    private SCIMServiceProviderConfig scimServiceProviderConfig;
    private SCIMResourceType scimResourceType;
    private SCIMSchema scimSchema;

    public SCIMServiceProviderConfig getScimServiceProviderConfig() {
        return scimServiceProviderConfig;
    }

    public void setScimServiceProviderConfig(SCIMServiceProviderConfig scimServiceProviderConfig) {
        this.scimServiceProviderConfig = scimServiceProviderConfig;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getClient_id() {
        return client_id;
    }

    public void setClient_id(String client_id) {
        this.client_id = client_id;
    }

    public String getClient_secret() {
        return client_secret;
    }

    public void setClient_secret(String client_secret) {
        this.client_secret = client_secret;
    }

    public String getAuthorization_server() {
        return authorization_server;
    }

    public void setAuthorization_server(String authorization_server) {
        this.authorization_server = authorization_server;
    }

    public String getAuthorization_header() {
        return authorization_header;
    }

    public void setAuthorization_header(String authorization_header) {
        this.authorization_header = authorization_header;
    }

    public String getAuthorization_method() {
        return authorization_method;
    }

    public void setAuthorization_method(String authorization_method) {
        this.authorization_method = authorization_method;
    }

    public void setScimResourceType(SCIMResourceType scimResourceType) {
        this.scimResourceType = scimResourceType;
    }

    public SCIMResourceType getScimResourceType() {
        return scimResourceType;
    }

    public SCIMSchema getScimSchema() {
        return scimSchema;
    }

    public void setScimSchema(SCIMSchema scimSchema) {
        this.scimSchema = scimSchema;
    }
}


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

package info.wso2.scim2.compliance.tests;


import info.wso2.scim2.compliance.entities.TestResult;
import info.wso2.scim2.compliance.exception.ComplianceException;
import info.wso2.scim2.compliance.exception.CriticalComplianceException;
import info.wso2.scim2.compliance.exception.GeneralComplianceException;
import info.wso2.scim2.compliance.httpclient.HTTPClient;
import info.wso2.scim2.compliance.protocol.ComplianceTestMetaDataHolder;
import info.wso2.scim2.compliance.protocol.ComplianceUtils;
import info.wso2.scim2.compliance.utils.ComplianceConstants;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 *  This class performs the tests related to /Bulk endpoint.
 */
public class BulkTest extends BaseTest {

    private String url;
    private ArrayList<String> createdUserLocations = new ArrayList<>();

    /**
     * Initializer.
     * @param complianceTestMetaDataHolder
     */
    public BulkTest(ComplianceTestMetaDataHolder complianceTestMetaDataHolder) {
        super(complianceTestMetaDataHolder);
        url =  complianceTestMetaDataHolder.getUrl() +
                ComplianceConstants.TestConstants.BULK_ENDPOINT;
    }

    /**
     * Service Provider clean up method.
     * @throws ComplianceException
     * @throws GeneralComplianceException
     */
    public void RunCleanUpTask() throws ComplianceException, GeneralComplianceException {
        for(String location : createdUserLocations) {
            DeleteUser(location);
        }
    }

    /**
     * Post test on /Bulk endpoint.
     * @return
     * @throws GeneralComplianceException
     * @throws ComplianceException
     */
    @TestCase
    public TestResult BulkPostTest() throws GeneralComplianceException, ComplianceException {

        String definedRequest = ComplianceConstants.DefinedInstances.DEFINED_BULK_REQUEST;

        HttpPost method = new HttpPost(url);
        //create group test
        HttpClient client = HTTPClient.getHttpClient();

        method = (HttpPost) HTTPClient.setAuthorizationHeader(complianceTestMetaDataHolder, method);
        method.setHeader("Accept", "application/json");
        method.setHeader("Content-Type", "application/json");

        HttpResponse response = null;
        String responseString = "";
        String headerString = "";
        String responseStatus = "";
        ArrayList<String> subTests =  new ArrayList<>();
        try {
            HttpEntity entity = new ByteArrayEntity(definedRequest.getBytes("UTF-8"));
            method.setEntity(entity);
            response = client.execute(method);
            // Read the response body.
            responseString = new BasicResponseHandler().handleResponse(response);
            //get all headers
            Header[] headers = response.getAllHeaders();
            for (Header header : headers) {
                headerString += header.getName() + " : " + header.getValue() + "\n";
            }
            responseStatus = response.getStatusLine().getStatusCode() + " " +
                    response.getStatusLine().getReasonPhrase();

            //get the created user locations
            createdUserLocations = getLocations(responseString);

        } catch (Exception e) {
            // Read the response body.
            //get all headers
            Header[] headers = response.getAllHeaders();
            for (Header header : headers) {
                headerString += header.getName() + " : " + header.getValue() + "\n";
            }
            responseStatus = response.getStatusLine().getStatusCode() + " "
                    + response.getStatusLine().getReasonPhrase();
            throw new GeneralComplianceException(new TestResult(TestResult.ERROR, "Bulk Test",
                    "Could not perform bulk request at " + url,
                    ComplianceUtils.getWire(method, responseString, headerString, responseStatus, subTests)));
        }

        if (response.getStatusLine().getStatusCode() == 200) {
            //run clean up task
            RunCleanUpTask();
            return new TestResult
                    (TestResult.SUCCESS, "Bulk Test",
                            "", ComplianceUtils.getWire(method, responseString,
                            headerString, responseStatus, subTests));
        } else {
            return new TestResult
                    (TestResult.ERROR, "Bulk Test",
                            "", ComplianceUtils.getWire(method, responseString,
                            headerString, responseStatus, subTests));
        }
    }

    /**
     * Delete the users created by the bulk request.
     * @param url
     * @return
     * @throws GeneralComplianceException
     * @throws ComplianceException
     */
    public TestResult DeleteUser (String url) throws GeneralComplianceException, ComplianceException {

        String deleteUserURL = url;

        HttpDelete method = new HttpDelete(deleteUserURL);

        HttpClient client = HTTPClient.getHttpClient();

        method = (HttpDelete) HTTPClient.setAuthorizationHeader(complianceTestMetaDataHolder, method);
        method.setHeader("Accept", "application/json");

        HttpResponse response = null;
        String responseString = "";
        String headerString = "";
        String responseStatus = "";
        ArrayList<String> subTests =  new ArrayList<>();
        try {

            response = client.execute(method);
            // Read the response body.
            responseString = new BasicResponseHandler().handleResponse(response);
            //get all headers
            Header[] headers = response.getAllHeaders();
            for (Header header : headers) {
                headerString += header.getName() + " : " + header.getValue() + "\n";
            }
            responseStatus = response.getStatusLine().getStatusCode() + " "
                    + response.getStatusLine().getReasonPhrase();

        } catch (Exception e) {
            // Read the response body.
            //get all headers
            Header[] headers = response.getAllHeaders();
            for (Header header : headers) {
                headerString += header.getName() + " : " + header.getValue() + "\n";
            }
            responseStatus = response.getStatusLine().getStatusCode() + " "
                    + response.getStatusLine().getReasonPhrase();
            throw new GeneralComplianceException(new TestResult(TestResult.ERROR, "Bulk Test",
                    "Could not delete the created user at url response location : " + url,
                    ComplianceUtils.getWire(method, responseString, headerString, responseStatus, subTests)));
        }

        if (response.getStatusLine().getStatusCode() == 204) {
            return new TestResult
                    (TestResult.SUCCESS, "Bulk Test",
                            "", ComplianceUtils.getWire(method, responseString, headerString,
                            responseStatus, subTests));
        } else {
            return new TestResult
                    (TestResult.ERROR, "Bulk Test",
                            "", ComplianceUtils.getWire(method, responseString, headerString,
                            responseStatus, subTests));
        }
    }

    /**
     * Extract created user locations from the bulk response.
     * @param response
     * @return
     * @throws JSONException
     */
    public ArrayList<String> getLocations (String response) throws JSONException {

        ArrayList<String> UserLocations = new ArrayList<>();

        JSONObject jsonObject = new JSONObject(response);
        JSONArray jsonarray = jsonObject.optJSONArray("Operations");
        for (int i = 0; i < jsonarray.length(); i++) {
            JSONObject innerJsonobject = jsonarray.getJSONObject(i);
            String location = innerJsonobject.getString("location");
            UserLocations.add(location);
        }

        return UserLocations;
    }
}

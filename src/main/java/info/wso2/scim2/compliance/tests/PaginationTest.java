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
import info.wso2.scim2.compliance.exception.GeneralComplianceException;
import info.wso2.scim2.compliance.httpclient.HTTPClient;
import info.wso2.scim2.compliance.protocol.ComplianceTestMetaDataHolder;
import info.wso2.scim2.compliance.protocol.ComplianceUtils;
import info.wso2.scim2.compliance.tests.common.ResponseValidateTests;
import info.wso2.scim2.compliance.utils.ComplianceConstants;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.charon3.core.encoder.JSONDecoder;
import org.wso2.charon3.core.exceptions.BadRequestException;
import org.wso2.charon3.core.exceptions.CharonException;
import org.wso2.charon3.core.exceptions.InternalErrorException;
import org.wso2.charon3.core.objects.Group;
import org.wso2.charon3.core.objects.User;
import org.wso2.charon3.core.schema.SCIMResourceSchemaManager;
import org.wso2.charon3.core.schema.SCIMResourceTypeSchema;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * This class is for pagination related test cases.
 */
public class PaginationTest extends BaseTest{

    private String usersURL  = null;
    private String groupURL  = null;
    private ArrayList<String> groupIDs = new ArrayList<>();
    private ArrayList<String> userIDs = new ArrayList<>();

    /**
     * Initialize.
     * @param complianceTestMetaDataHolder
     */
    public PaginationTest(ComplianceTestMetaDataHolder complianceTestMetaDataHolder) {

        super(complianceTestMetaDataHolder);

        usersURL =  complianceTestMetaDataHolder.getUrl() +
                ComplianceConstants.TestConstants.USERS_ENDPOINT;

        groupURL =  complianceTestMetaDataHolder.getUrl() +
                ComplianceConstants.TestConstants.GROUPS_ENDPOINT;
    }

    /**
     * Pagination test main method.
     * @return
     * @throws ComplianceException
     */
    @TestCase
    public ArrayList<TestResult> GetPaginationTest() throws ComplianceException {
        ArrayList<TestResult> testResults = new ArrayList<>();
        try {
            CreateTestsUsers();
            testResults.add(PaginateUsers());
        } catch (GeneralComplianceException e){
            testResults.add(e.getResult());
        }
        try {
            CreateTestsGroups();
            testResults.add(PaginateGroups());
        } catch (GeneralComplianceException e){
            testResults.add(e.getResult());
        }
        return testResults;
    }

    /**
     * Pagination users test case.
     * @return
     * @throws ComplianceException
     * @throws GeneralComplianceException
     */
    private TestResult PaginateUsers() throws ComplianceException, GeneralComplianceException {

        HttpGet method = new HttpGet(usersURL + "?startIndex=1&count=2");

        HttpClient client = HTTPClient.getHttpClient();

        method = (HttpGet) HTTPClient.setAuthorizationHeader(complianceTestMetaDataHolder, method);
        method.setHeader("Accept", "application/json");

        HttpResponse response = null;
        String responseString = "";
        String headerString = "";
        String responseStatus = "";
        ArrayList<String> subTests = new ArrayList<>();
        try {
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

        } catch (Exception e) {
            // Read the response body.
            //get all headers
            Header[] headers = response.getAllHeaders();
            for (Header header : headers) {
                headerString += header.getName() + " : " + header.getValue() + "\n";
            }
            responseStatus = response.getStatusLine().getStatusCode() + " "
                    + response.getStatusLine().getReasonPhrase();
            //clean up task
            for (String id : userIDs) {
                CleanUpUser(id);
            }
            throw new GeneralComplianceException(new TestResult(TestResult.ERROR, "Pagination Users",
                    "Could not paginate the users at url " + usersURL,
                    ComplianceUtils.getWire(method, responseString, headerString, responseStatus, subTests)));
        }

        if (response.getStatusLine().getStatusCode() == 200) {

            //obtain the schema corresponding to user
            SCIMResourceTypeSchema schema = SCIMResourceSchemaManager.getInstance().getUserResourceSchema();
            JSONDecoder jsonDecoder = new JSONDecoder();
            ArrayList<User> userList = new ArrayList<>();
            try {
                JSONObject jsonObj = new JSONObject(responseString);
                JSONArray usersArray = jsonObj.getJSONArray("Resources");
                JSONObject tmp;
                for (int i = 0; i < usersArray.length(); i++) {
                    tmp = usersArray.getJSONObject(i);
                    userList.add((User) jsonDecoder.decodeResource(tmp.toString(), schema, new User()));
                    try {
                        ResponseValidateTests.runValidateTests(userList.get(i), schema,
                                null, null, method,
                                responseString, headerString, responseStatus, subTests);

                    } catch (BadRequestException | CharonException e) {
                        //clean up task
                        for (String id : userIDs) {
                            CleanUpUser(id);
                        }
                        throw new GeneralComplianceException(new TestResult(TestResult.ERROR, "Pagination Users",
                                "Response Validation Error",
                                ComplianceUtils.getWire(method, responseString, headerString,
                                        responseStatus, subTests)));
                    }
                }
            } catch (JSONException e) {
                //clean up task
                for (String id : userIDs) {
                    CleanUpUser(id);
                }
                throw new ComplianceException(500, "Error in decoding the returned paginated resource.");

            } catch (BadRequestException | CharonException | InternalErrorException e) {
                //clean up task
                for (String id : userIDs) {
                    CleanUpUser(id);
                }
                throw new GeneralComplianceException(new TestResult(TestResult.ERROR, "Pagination Users",
                        "Could not decode the server response",
                        ComplianceUtils.getWire(method, responseString, headerString, responseStatus, subTests)));
            }
            // check for all created groups
            try {
                CheckForListOfUsersReturned(userList, method, responseString, headerString,
                        responseStatus, subTests);
            } catch (CharonException e) {
                //clean up task
                for (String id : userIDs) {
                    CleanUpUser(id);
                }
                throw new ComplianceException(500, "Could not get the created user id");
            }
            //clean up task
            for (String id : userIDs) {
                CleanUpUser(id);
            }
            return new TestResult
                    (TestResult.SUCCESS, "Pagination Users",
                            "", ComplianceUtils.getWire(method, responseString,
                            headerString, responseStatus, subTests));
        } else {
            return new TestResult
                    (TestResult.ERROR, "Pagination Users",
                            "", ComplianceUtils.getWire(method, responseString,
                            headerString, responseStatus, subTests));
        }
    }

    /**
     * Clean up task for users.
     * @param id
     * @throws ComplianceException
     * @throws GeneralComplianceException
     */
    private void CleanUpUser(String id) throws ComplianceException, GeneralComplianceException {

        String deleteUserURL = usersURL + "/" + id;

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

            throw new GeneralComplianceException(new TestResult(TestResult.ERROR, "Pagination Users",
                    "Could not delete the default user at url " + deleteUserURL,
                    ComplianceUtils.getWire(method, responseString, headerString, responseStatus, subTests)));
        }

        if (response.getStatusLine().getStatusCode() != 204) {
            throw new GeneralComplianceException(new TestResult(TestResult.ERROR, "Pagination Users",
                    "Could not delete the default user at url " + deleteUserURL,
                    ComplianceUtils.getWire(method, responseString, headerString, responseStatus, subTests)));
        }
    }

    private void CheckForListOfUsersReturned(ArrayList<User> userList,
                                             HttpGet method, String responseString,
                                             String headerString, String responseStatus,
                                             ArrayList<String> subTests) throws CharonException,
            ComplianceException, GeneralComplianceException {

        subTests.add(ComplianceConstants.TestConstants.PAGINATION_USER_TEST);
        if (userList.size() != 2){
            //clean up task
            for (String id : userIDs) {
                CleanUpUser(id);
            }
            throw new GeneralComplianceException(new TestResult(TestResult.ERROR, "Pagination Users",
                    "Response does not contain right number of pagination.",
                    ComplianceUtils.getWire(method, responseString, headerString, responseStatus, subTests)));
        }
    }

    /**
     * Create users for testing purpose.
     * @return
     * @throws ComplianceException
     * @throws GeneralComplianceException
     */
    private ArrayList<String> CreateTestsUsers() throws ComplianceException, GeneralComplianceException {

        ArrayList<String> definedUsers = new ArrayList<>();
        definedUsers.add("{\"password\": \"7019asd81\",\"userName\": \"AbrTkAA11\"}");
        definedUsers.add("{\"password\": \"7019asd82\",\"userName\": \"UttEdHt12\"}");
        definedUsers.add("{\"password\": \"7019asd83\",\"userName\": \"KKTQwhr13\"}");

        HttpPost method = new HttpPost(usersURL);
        //create users
        HttpClient client = HTTPClient.getHttpClient();

        method = (HttpPost) HTTPClient.setAuthorizationHeader(complianceTestMetaDataHolder, method);
        method.setHeader("Accept", "application/json");
        method.setHeader("Content-Type", "application/json");

        HttpResponse response = null;
        String responseString = "";
        String headerString = "";
        String responseStatus = "";
        ArrayList<String> subTests =  new ArrayList<>();
        for (int i = 0 ; i < 3 ; i++) {
            try {
                //create the group
                HttpEntity entity = new ByteArrayEntity(definedUsers.get(i).getBytes("UTF-8"));
                method.setEntity(entity);
                response = client.execute(method);
                // Read the response body.
                responseString = new BasicResponseHandler().handleResponse(response);
                responseStatus = String.valueOf(response.getStatusLine().getStatusCode());
                if (responseStatus.equals("201")) {
                    //obtain the schema corresponding to group
                    SCIMResourceTypeSchema schema = SCIMResourceSchemaManager.getInstance().getUserResourceSchema();

                    JSONDecoder jsonDecoder = new JSONDecoder();
                    User user = null;
                    try {
                        user = (User) jsonDecoder.decodeResource(responseString, schema, new User());
                    } catch (BadRequestException | CharonException | InternalErrorException e) {
                        throw new GeneralComplianceException(new TestResult(TestResult.ERROR, "Pagination Users",
                                "Could not decode the server response of users create.",
                                ComplianceUtils.getWire(method, responseString, headerString, responseStatus, subTests)));
                    }
                    userIDs.add(user.getId());
                }

            } catch (Exception e) {
                // Read the response body.
                Header[] headers = response.getAllHeaders();
                for (Header header : headers) {
                    headerString += header.getName() + " : " + header.getValue() + "\n";
                }
                responseStatus = response.getStatusLine().getStatusCode() + " "
                        + response.getStatusLine().getReasonPhrase();
                throw new GeneralComplianceException(new TestResult(TestResult.ERROR, "Pagination Users",
                        "Could not create default users at url " + usersURL,
                        ComplianceUtils.getWire(method, responseString, headerString, responseStatus, subTests)));
            }
        }
        return userIDs;
    }

    /**
     * Create groups fro testing purpose.
     * @return
     * @throws ComplianceException
     * @throws GeneralComplianceException
     */
    private ArrayList<String> CreateTestsGroups () throws ComplianceException, GeneralComplianceException {

        ArrayList<String> definedGroups = new ArrayList<>();
        definedGroups.add("{\"displayName\": \"EYtXcD11\"}");
        definedGroups.add("{\"displayName\": \"BktqER12\"}");
        definedGroups.add("{\"displayName\": \"ZwLtOP13\"}");

        HttpPost method = new HttpPost(groupURL);
        //create groups
        HttpClient client = HTTPClient.getHttpClient();

        method = (HttpPost) HTTPClient.setAuthorizationHeader(complianceTestMetaDataHolder, method);
        method.setHeader("Accept", "application/json");
        method.setHeader("Content-Type", "application/json");

        HttpResponse response = null;
        String responseString = "";
        String headerString = "";
        String responseStatus = "";
        ArrayList<String> subTests =  new ArrayList<>();
        for (int i = 0 ; i < 3 ; i++) {
            try {
                //create the group
                HttpEntity entity = new ByteArrayEntity(definedGroups.get(i).getBytes("UTF-8"));
                method.setEntity(entity);
                response = client.execute(method);
                // Read the response body.
                responseString = new BasicResponseHandler().handleResponse(response);
                responseStatus = String.valueOf(response.getStatusLine().getStatusCode());
                if (responseStatus.equals("201")) {
                    //obtain the schema corresponding to group
                    SCIMResourceTypeSchema schema = SCIMResourceSchemaManager.getInstance().getGroupResourceSchema();

                    JSONDecoder jsonDecoder = new JSONDecoder();
                    Group group = null;
                    try {
                        group = (Group) jsonDecoder.decodeResource(responseString, schema, new Group());
                    } catch (BadRequestException | CharonException | InternalErrorException e) {
                        throw new GeneralComplianceException(new TestResult(TestResult.ERROR, "Paginate Groups",
                                "Could not decode the server response of groups create.",
                                ComplianceUtils.getWire(method, responseString, headerString, responseStatus, subTests)));
                    }
                    groupIDs.add(group.getId());
                }

            } catch (Exception e) {
                // Read the response body.
                Header[] headers = response.getAllHeaders();
                for (Header header : headers) {
                    headerString += header.getName() + " : " + header.getValue() + "\n";
                }
                responseStatus = response.getStatusLine().getStatusCode() + " "
                        + response.getStatusLine().getReasonPhrase();
                throw new GeneralComplianceException(new TestResult(TestResult.ERROR, "Paginate Groups",
                        "Could not create default groups at url " + groupURL,
                        ComplianceUtils.getWire(method, responseString, headerString, responseStatus, subTests)));
            }
        }
        return groupIDs;
    }

    /**
     * Pagination test for groups.
     * @return
     * @throws ComplianceException
     * @throws GeneralComplianceException
     */
    private TestResult PaginateGroups()
            throws ComplianceException, GeneralComplianceException {

        HttpGet method = new HttpGet(groupURL + "?startIndex=1&count=2");

        HttpClient client = HTTPClient.getHttpClient();

        method = (HttpGet) HTTPClient.setAuthorizationHeader(complianceTestMetaDataHolder, method);
        method.setHeader("Accept", "application/json");

        HttpResponse response = null;
        String responseString = "";
        String headerString = "";
        String responseStatus = "";
        ArrayList<String> subTests = new ArrayList<>();
        try {
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

        } catch (Exception e) {
            // Read the response body.
            //get all headers
            Header[] headers = response.getAllHeaders();
            for (Header header : headers) {
                headerString += header.getName() + " : " + header.getValue() + "\n";
            }
            responseStatus = response.getStatusLine().getStatusCode() + " "
                    + response.getStatusLine().getReasonPhrase();
            //clean up task
            for (String id : groupIDs) {
                CleanUpGroup(id);
            }
            throw new GeneralComplianceException(new TestResult(TestResult.ERROR, "Paginate Groups",
                    "Could not paginate the groups at url " + groupURL,
                    ComplianceUtils.getWire(method, responseString, headerString, responseStatus, subTests)));
        }

        if (response.getStatusLine().getStatusCode() == 200) {

            //obtain the schema corresponding to group
            SCIMResourceTypeSchema schema = SCIMResourceSchemaManager.getInstance().getGroupResourceSchema();
            JSONDecoder jsonDecoder = new JSONDecoder();
            ArrayList<Group> groupList = new ArrayList<>();
            try {
                JSONObject jsonObj = new JSONObject(responseString);
                JSONArray groupsArray = jsonObj.getJSONArray("Resources");
                JSONObject tmp;
                for (int i = 0; i < groupsArray.length(); i++) {
                    tmp = groupsArray.getJSONObject(i);
                    groupList.add((Group) jsonDecoder.decodeResource(tmp.toString(), schema, new Group()));
                    try {
                        ResponseValidateTests.runValidateTests(groupList.get(i), schema,
                                null, null, method,
                                responseString, headerString, responseStatus, subTests);

                    } catch (BadRequestException | CharonException e) {
                        //clean up task
                        for (String id : groupIDs) {
                            CleanUpGroup(id);
                        }
                        throw new GeneralComplianceException(new TestResult(TestResult.ERROR, "Paginate Groups",
                                "Response Validation Error",
                                ComplianceUtils.getWire(method, responseString, headerString,
                                        responseStatus, subTests)));
                    }
                }
            } catch (JSONException e) {
                //clean up task
                for (String id : groupIDs) {
                    CleanUpGroup(id);
                }
                throw new ComplianceException(500, "Error in decoding the returned paginated resource.");

            } catch (BadRequestException | CharonException | InternalErrorException e) {
                //clean up task
                for (String id : groupIDs) {
                    CleanUpGroup(id);
                }
                throw new GeneralComplianceException(new TestResult(TestResult.ERROR, "Paginate Groups",
                        "Could not decode the server response",
                        ComplianceUtils.getWire(method, responseString, headerString, responseStatus, subTests)));
            }
            // check for all created groups
            try {
                CheckForListOfGroupsReturned(groupList, method, responseString, headerString, responseStatus, subTests);
            } catch (CharonException e) {
                //clean up task
                for (String id : groupIDs) {
                    CleanUpGroup(id);
                }
                throw new ComplianceException(500, "Could not get the created group id");
            }
            //clean up task
            for (String id : groupIDs) {
                CleanUpGroup(id);
            }
            return new TestResult
                    (TestResult.SUCCESS, "Paginate Groups",
                            "", ComplianceUtils.getWire(method, responseString,
                            headerString, responseStatus, subTests));
        } else {
            return new TestResult
                    (TestResult.ERROR, "Paginate Groups",
                            "", ComplianceUtils.getWire(method, responseString,
                            headerString, responseStatus, subTests));
        }
    }

    /**
     * Validation test for groups returned in response.
     * @param returnedGroups
     * @param method
     * @param responseString
     * @param headerString
     * @param responseStatus
     * @param subTests
     * @throws CharonException
     * @throws ComplianceException
     * @throws GeneralComplianceException
     */
    private void CheckForListOfGroupsReturned(ArrayList<Group> returnedGroups,
                                              HttpGet method, String responseString,
                                              String headerString, String responseStatus,
                                              ArrayList<String> subTests)
            throws CharonException, ComplianceException, GeneralComplianceException {
        subTests.add(ComplianceConstants.TestConstants.PAGINATION_GROUP_TEST);
        if(returnedGroups.size() != 2) {
            //clean up task
            for (String id : groupIDs) {
                CleanUpGroup(id);
            }
            throw new GeneralComplianceException(new TestResult(TestResult.ERROR, "Paginate Groups",
                    "Response does not contain right number of paginated groups",
                    ComplianceUtils.getWire(method, responseString, headerString, responseStatus, subTests)));
        }

    }

    /**
     * Clean up task for groups.
     * @param id
     * @throws GeneralComplianceException
     * @throws ComplianceException
     */
    private void CleanUpGroup (String id) throws GeneralComplianceException, ComplianceException {

        String deleteGroupURL = groupURL + "/" + id;

        HttpDelete method = new HttpDelete(deleteGroupURL);

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

            throw new GeneralComplianceException(new TestResult(TestResult.ERROR, "Paginate Groups",
                    "Could not delete the default group at url " + deleteGroupURL,
                    ComplianceUtils.getWire(method, responseString, headerString, responseStatus, subTests)));
        }

        if (response.getStatusLine().getStatusCode() != 204) {
            throw new GeneralComplianceException(new TestResult(TestResult.ERROR, "Paginate Groups",
                    "Could not delete the default group at url " + deleteGroupURL,
                    ComplianceUtils.getWire(method, responseString, headerString, responseStatus, subTests)));
        }
    }
}

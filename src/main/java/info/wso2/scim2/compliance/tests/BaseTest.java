package info.wso2.scim2.compliance.tests;

import info.wso2.scim2.compliance.entities.TestResult;
import info.wso2.scim2.compliance.exception.ComplianceException;
import info.wso2.scim2.compliance.exception.CriticalComplianceException;
import info.wso2.scim2.compliance.httpclient.HTTPClient;
import info.wso2.scim2.compliance.objects.SCIMSchema;
import info.wso2.scim2.compliance.protocol.ComplianceTestMetaDataHolder;
import info.wso2.scim2.compliance.protocol.ComplianceUtils;
import info.wso2.scim2.compliance.utils.ComplianceConstants;
import info.wso2.scim2.compliance.utils.SchemaBuilder;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * This class performs the /Schemas test.
 */
public class BaseTest {

    public ComplianceTestMetaDataHolder complianceTestMetaDataHolder;
    public BaseTest(ComplianceTestMetaDataHolder complianceTestMetaDataHolder){
        this.complianceTestMetaDataHolder = complianceTestMetaDataHolder;
    }

    public ArrayList<TestResult> performTest() throws CriticalComplianceException, ComplianceException {
        ArrayList<TestResult> testResults = new ArrayList<>();
        Method[] methods = this.getClass().getMethods();
        for (Method method : methods) {
            TestCase annos = method.getAnnotation(TestCase.class);
            if (annos != null) {
                try {
                    Object results = method.invoke(this);
                    if(results instanceof TestResult){
                        testResults.add((TestResult)results);    
                    }else if(results instanceof ArrayList<?>){
                        testResults.addAll(((ArrayList<TestResult>)results));
                    }
                } catch (InvocationTargetException e) {
                    try{
                        throw  e.getCause();
                    } catch (ComplianceException e1) {
                        throw e1;
                    } catch (CriticalComplianceException e1){
                        testResults.add(e1.getResult());
                    } catch (Throwable throwable) {
                        throw new ComplianceException("Error occurred in " + this.getClass().getName() + ". " + throwable.getMessage());
                    }
                } catch (IllegalAccessException e) {
                    throw new ComplianceException("Error occurred in " + this.getClass().getName() + ". " + e.getMessage());
                }
            }
        }
        return testResults;
    }
}
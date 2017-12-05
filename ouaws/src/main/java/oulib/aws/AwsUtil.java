/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oulib.aws;

import com.amazonaws.AmazonClientException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Tao Zhao
 */
public class AwsUtil {
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(AwsUtil.class);
    
    private static final String USER_AGENT = "Mozilla/5.0";
    private static final String CREDENTIALS_REQUEST_URL = "http://169.254.169.254/latest/meta-data/iam/security-credentials/lib-amz-default";
    
    public static String getAwsCredentialsByRequest(){
        
        BufferedReader in = null;
                
        try{
            URL obj = null;
            try {
                obj = new URL(CREDENTIALS_REQUEST_URL);
            } catch (MalformedURLException ex) {
                logger.error("Cannot get credential URL", ex);
            }
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            try {
                con.setRequestMethod("GET");
            } catch (ProtocolException ex) {
                logger.error(ex);
            }

            con.setRequestProperty("User-Agent", USER_AGENT);

            int responseCode = con.getResponseCode();
            System.out.println("\nSending 'GET' request to URL : " + CREDENTIALS_REQUEST_URL);
            System.out.println("Response Code : " + responseCode);

            in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
            }
            return response.toString();
        }
        catch(IOException ex){
            logger.error("Cannot get credential info by request", ex);
        }
        finally{
            if(null != in){
                try{
                    in.close();
                }
                catch(IOException ex){
                    ex.printStackTrace();
                }
            }
        }
        return null;
    }
    
    public static AWSCredentials getAwsCredentials(){
        AWSCredentials credentials = null;            

        try {
            credentials = new ProfileCredentialsProvider("default").getCredentials();                   
        } catch (Exception e) {
            logger.error("Cannot interpret the credential info response!", e);
            String access_key_id = null;
            String secret_key_id = null;
            String credentialInfo = AwsUtil.getAwsCredentialsByRequest();                
            ObjectMapper mapper = new ObjectMapper();
            Map<String,String> credentialInfoMap = new HashMap<>();
            try{
                credentialInfoMap = mapper.readValue(credentialInfo, HashMap.class);
            }
            catch(IOException ex){
                logger.error("Cannot interpret the credential info response!", ex);
            }
            for(String key : credentialInfoMap.keySet()){

                if("AccessKeyId".equals(key)){
                    access_key_id = credentialInfoMap.get(key);
                }
                else if("SecretAccessKey".equals(key)){
                    secret_key_id = credentialInfoMap.get(key);
                }
            }
//                System.out.println("access_key_id = "+access_key_id+" access_key_id = "+access_key_id);
            if(null != access_key_id && null != secret_key_id){
                credentials = new BasicAWSCredentials(access_key_id, secret_key_id);
            }
            else{
                throw new AmazonClientException(
                    "Cannot load the credentials from the credential information. " +
                            "Please make sure that your credentials file is at the correct, and is in valid format.",
                    e);
            }
        }
        
        return credentials;
    }
    
    public static AmazonS3Client getS3ClientByCredentialInfo(AWSCredentials credentials){

        ClientConfiguration config = new ClientConfiguration();
        config.setConnectionTimeout(250000);
        config.setSocketTimeout(50000);     
        AmazonS3Client s3Client = new AmazonS3Client(credentials, config);
        Region usEast = Region.getRegion(Regions.US_EAST_1);
        s3Client.setRegion(usEast);
        
        return s3Client;
    }
}

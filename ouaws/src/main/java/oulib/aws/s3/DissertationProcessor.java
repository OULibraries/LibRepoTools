/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oulib.aws.s3;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.shareok.data.documentProcessor.DocumentProcessorUtil;
import org.shareok.data.dspacemanager.DspaceDataUtil;
import oulib.aws.AwsUtil;
import oulib.aws.exceptions.IncompleteFunctionArgumentsException;
import oulib.aws.exceptions.InvalidDissertationInfoException;
import oulib.aws.exceptions.InvalidS3ClientException;
import oulib.aws.exceptions.InvalidS3ObjectKeyException;

/**
 *
 * @author zhao0677
 */
public class DissertationProcessor {
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(DissertationProcessor.class);
    
    private AmazonS3Client s3Client;
    private String endPoint;
    private String collection;
    private Dissertation[] dissertations;

    public DissertationProcessor() {
        AWSCredentials credentials = AwsUtil.getAwsCredentials();
        AmazonS3Client client = AwsUtil.getS3ClientByCredentialInfo(credentials);
        setS3Client(client);
    }

    public AmazonS3Client getS3Client() {
        return s3Client;
    }

    public String getCollection() {
        return collection;
    }

    public String getEndPoint() {
        return endPoint;
    }

    public Dissertation[] getDissertations() {
        return dissertations;
    }

    public final void setS3Client(AmazonS3Client s3Client) {
        this.s3Client = s3Client;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public void setEndPoint(String endPoint) {
        this.endPoint = endPoint;
    }

    public void setDissertations(Dissertation[] dissertations) {
        this.dissertations = dissertations;
    }
    
    @Override
    public String toString(){
        String output = "Parsing the recipe file gets the following dissertation information:\n";
        output += "The DSpace REST API end point: " + getEndPoint() + ";\n";
        output += "Collection to be imported into: " + getCollection() + ";\n";
        output += "The recipte contains " + getDissertations().length + " dissertaions;\n";
        output += "The included dissertations are: \n";
        int index = 1;
        for(Dissertation dis : getDissertations()){
            output += "Dissertation " + index + ":\n";
            output += dis.toString() + "\n";
            index++;
        }
        return output;
    }
    
    public void parseRecipeFile(String recipeJson){
        try{
            if(null == s3Client){
                throw new InvalidS3ClientException("Dissertation processor has null s3 client!");
            }
            JSONObject json = new JSONObject(recipeJson);
            
            String endPointStr = json.getString("rest endpoint");            
            if(StringUtils.isBlank(endPointStr)){
                throw new InvalidDissertationInfoException("The end point is empty for the dissertation!");
            }
            setEndPoint(endPointStr);
            
            String collectionStr = json.getString("collection");
            if(StringUtils.isBlank(collectionStr)){
                throw new InvalidDissertationInfoException("The collection is empty for the dissertation!");
            }
            setCollection(collectionStr);
            
            JSONArray items = json.getJSONArray("items");
            List<Dissertation> dissertationList = new ArrayList<>();
            for(int i = 0; i < items.length(); i++){
                JSONObject item = items.getJSONObject(i);                
                if(null != item){
                    Dissertation dis = new Dissertation();
                    Iterator<String> keys = item.keys();
                    while(keys.hasNext()){
                        String key = keys.next();
//                        dis.setBucket(bucketName);
                        dis.setName(key);
                        JSONObject itemInfo = item.getJSONObject(key);
                        
                        String metadataTxt = itemInfo.getString("metadata");
                        if(StringUtils.isBlank(metadataTxt)){
                            throw new InvalidDissertationInfoException("The metadata file key is empty for the dissertation!");
                        }
                        dis.setMetadata(metadataTxt);
                        
                        JSONArray filesArray = itemInfo.getJSONArray("files");
                        if(null == filesArray || filesArray.length() == 0){
                            throw new InvalidDissertationInfoException("The files information is empty for the dissertation!");
                        }
                        String[] files = new String[filesArray.length()];
                        for(int j = 0; j < files.length; j++){
                            String filePath = filesArray.getString(j);
                            String[] filePathArr = filePath.split("/");
                            String bucketName = filePathArr[0];
                            if(j == 0){
                                dis.setBucket(filePathArr[0]);
                            }
                            files[j] = filePath.replaceFirst(bucketName+"/", "");
                        }
                        dis.setFiles(files);
                        
                        dissertationList.add(dis);
                        break;
                    }
                    
                }
            }
            
            Dissertation[] dissertationsLocal = dissertationList.toArray(new Dissertation[dissertationList.size()]);
            setDissertations(dissertationsLocal);
            
        } catch (InvalidS3ClientException ex) {
            logger.error(ex);
        } catch (Exception ex){
            logger.error(ex);
            ex.printStackTrace();
        }
    }
    
    public String generateSafPackage(){
        Date now = new Date();
        String uploadPath = DspaceDataUtil.getDspaceUploadPath("dissertation", now);
        String safPackageFullPath = uploadPath + File.separator + getSafPackageFolderName();
        FileWriter fw = null;
        BufferedWriter bw = null;
        PrintWriter out = null;

        try{
            File safPackageFullPathFile = new File(safPackageFullPath);
            if(!safPackageFullPathFile.exists()){
                safPackageFullPathFile.mkdirs();
            }
            for(Dissertation dis : dissertations){
                if(null != dis){
                    String name = dis.getName();
                    String bucket = dis.getBucket();
                    String metadata = dis.getMetadata();
                    String dissertationPath = safPackageFullPath + File.separator + "output_dissertation_" + bucket + "_" + name;
                    File dissertationPathFile = new File(dissertationPath);
                    if(!dissertationPathFile.exists()){
                        dissertationPathFile.mkdir();
                    }
                    
                    String dissertationDublinPath = dissertationPath + File.separator + "dublin_core.xml";
//                    S3Util.downloadS3ObjectToFile(s3Client, bucket, metadata, dissertationDublinPath);
                    DocumentProcessorUtil.outputStringToFile(metadata, dissertationDublinPath);
                    
                    String dissertationContentsPath = dissertationPath + File.separator + "contents";
                    File dissertationContentsFile = new File(dissertationContentsPath);
                    if(!dissertationContentsFile.exists()){
                        dissertationContentsFile.createNewFile();
                    }
                    if(null == fw){
                        fw = new FileWriter(dissertationContentsFile);
                    }
                    if(null == bw){
                        bw = new BufferedWriter(fw);
                    }
                    if(null == out){
                        out = new PrintWriter(bw);
                    }
                    
                    for(String file : dis.getFiles()){
                        String fileName = getFileNameFromS3ObjectKey(file);
                        out.println(fileName);
                        S3Util.downloadS3ObjectToFile(s3Client, bucket, file, dissertationPath + File.separator + fileName);
                    }
                }
            }
        }
        catch(IOException | IncompleteFunctionArgumentsException | InvalidS3ClientException ex){
            logger.error("Something is wrong when generating the saf package: " + ex.getMessage());
            ex.printStackTrace();
        } catch (InvalidS3ObjectKeyException ex) {
            logger.error("S3 object key is wrong: " + ex.getMessage());
        } finally {
            try{
                if(null != out){
                    out.close();
                }
                if(null != bw){
                    bw.close();
                }  
                if(null != fw){
                    fw.close();
                }        
            }
            catch(IOException ioex){
                logger.error("Cannot close the file streams: "+ioex.getMessage());
                ioex.printStackTrace();
            }
        }
        return safPackageFullPath;
    }
    
    private String getSafPackageFolderName(){
        return "output_dissertation";
    }
    
    private String getFileNameFromS3ObjectKey(String key) throws InvalidS3ObjectKeyException{
        try{
            String[] keyArr = key.split("/");
            if(null == keyArr || keyArr.length == 0){
                throw new InvalidS3ObjectKeyException("This object key cannot be split to get file name: "+key);
            }
            return keyArr[keyArr.length-1];
        }
        catch(InvalidS3ObjectKeyException ex){
            throw new InvalidS3ObjectKeyException("DissertationProcess cannot process this object key: "+key);
        }
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shareok.data.kernel.api.services;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import org.shareok.data.config.DataUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.shareok.data.config.ShareokdataManager;
import org.shareok.data.datahandlers.DataHandlersUtil;
import org.shareok.data.datahandlers.exceptions.InvalidDoiException;
import org.shareok.data.documentProcessor.DocumentProcessorUtil;
import org.shareok.data.dspacemanager.exceptions.ErrorDspaceApiResponseException;
import org.shareok.data.kernel.api.exceptions.AwsDissertationProcessorParseRecipeJsonException;
import org.shareok.data.kernel.api.exceptions.InvalidCommandLineArgumentsException;
import org.shareok.data.kernel.api.exceptions.NoServiceProcessDoiException;
import org.shareok.data.kernel.api.exceptions.NotFoundServiceBeanException;
import org.shareok.data.kernel.api.services.dspace.DspaceJournalDataService;
import org.shareok.data.kernel.api.services.dspace.DspaceRestServiceImpl;
import org.shareok.data.kernel.api.services.job.DspaceApiJobServiceImpl;
import org.shareok.data.kernel.api.services.job.RedisJobService;
import org.shareok.data.kernel.api.services.ouaws.AwsDissertationService;
import org.shareok.data.kernel.api.services.ouaws.AwsDissertationServiceImpl;
import org.shareok.data.ouhistory.exceptions.NonCsvFileException;
import org.shareok.data.redis.job.RedisJob;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.web.multipart.MultipartFile;
import safbuilder.SAFPackage;

/**
 *
 * @author Tao Zhao
 */
public class ServiceUtil {
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ServiceUtil.class);
    
    public static final Map<String, String> serviceBeanMap = new HashMap<>();
    static {
        for(String job : DataUtil.JOB_TYPES){
            switch(job){
                case "ssh-import-dspace":
                case "ssh-upload-dspace":
                case "ssh-importloaded-dspace":
                    serviceBeanMap.put(job, "dspaceSshServiceImpl");
                    break;
                case "ssh-import-islandora":
                    serviceBeanMap.put(job, "islandoraSshServiceImpl");
                    break;
                case "rest-import-dspace":
                    serviceBeanMap.put(job, "dspaceRestServiceImpl");
                    break;
                default:
                    break; 
            }
        }
        serviceBeanMap.put("Public Library of Science (PLoS)", "plosDspaceServiceImpl");
        serviceBeanMap.put("plos", "plosDspaceServiceImpl");
        serviceBeanMap.put("sage", "sageDspaceServiceImpl");
        serviceBeanMap.put("SAGE Publications", "sageDspaceServiceImpl");
    }
    
    public static String getServiceBean(int repoType, int jobType){
        return serviceBeanMap.get(DataUtil.REPO_TYPES[repoType] + "-" + DataUtil.JOB_TYPES[jobType]);
    }
    
    /**
     * 
     * @param repoType : e.g. "dspace" or "islandora", lower case only
     * @param jobType : e.g. "ssh-import" or "rest-import", lower case only
     * @return bean name
     */
    public static String getServiceBean(String repoType, String jobType){
        return serviceBeanMap.get(repoType + "-" + jobType);
    }
    
    public static DataService getDataService(String jobType){
        String bean = serviceBeanMap.get(jobType);  
        ApplicationContext context = new ClassPathXmlApplicationContext("kernelApiContext.xml");
        DataService ds = (DataService) context.getBean(bean);
        return ds;
    }
    
    public static DataService getDataService(int jobType){
        return getDataService(DataUtil.JOB_TYPES[jobType]);
    }
    
    public static String saveUploadedFile(MultipartFile file, String jobFilePath){
        String uploadedFilePath = null;
        try{
            String fileName = file.getOriginalFilename();
            File path = new File(jobFilePath);
            if(!path.exists()){
                path.mkdir();
            }
            uploadedFilePath = jobFilePath + File.separator + fileName;
            File uploadedFile = new File(uploadedFilePath);
            file.transferTo(uploadedFile);
        }
        catch(IOException | IllegalStateException ex){
            logger.error("Cannot save the uploaded file", ex);
        }
        return uploadedFilePath;
    }

    public static boolean downloadRemoteFile(String link, String targetFile) {
        ByteArrayOutputStream out = null;
        InputStream in = null;
        FileOutputStream fos = null;
        try {
            URL linkUrl = new URL(link);
            in = new BufferedInputStream(linkUrl.openStream());
            out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int n = 0;
            while (-1 != (n = in.read(buffer))) {
                out.write(buffer, 0, n);
            }
            byte[] response = out.toByteArray();
            fos = new FileOutputStream(targetFile);
            fos.write(response);
            return true;
        } catch (MalformedURLException mex) {
            logger.error("Cannot set up the url for file download.", mex);
        } catch (IOException ioex) {
            logger.error("Cannot open the stream for the link URL.", ioex);
        } finally {
            try {
                if (null != out) {
                    out.close();
                }
                if (null != in) {
                    in.close();
                }
                if (null != fos) {
                    fos.close();
                }
            } catch (IOException ioex) {
                logger.error("Cannot close the outputstream when downloading file online.", ioex);
            }
        }
        return false;
    }
    
    public static String getReportFilePathByJob(RedisJob job){
        return ShareokdataManager.getShareokdataPath() + File.separator + DataUtil.REPO_TYPES[job.getRepoType()] 
                + File.separator + DataUtil.JOB_TYPES[job.getType()] + File.separator + String.valueOf(job.getJobId()) 
                + File.separator + String.valueOf(job.getJobId()) + "-report.txt";
    }
    
    public static String getRepoTypeByJob(RedisJob job){
        String jobType = DataUtil.JOB_TYPES[job.getType()];
        if(null != jobType && !"unknown".equals(jobType)){
            String repoTypeStr = jobType.split("-")[2];
            if(null != repoTypeStr){
                if(repoTypeStr.equals("dspace")){
                    return "DSpace";
                }
                if(repoTypeStr.equals("islandora")){
                    return "Islandora";
                }
                if(repoTypeStr.equals("hydra")){
                    return "Hydra";
                }
            }
        }
        return "";
    }
    
    public static Thread getThreadByName(String name){
        for(Thread th : Thread.getAllStackTraces().keySet()){
            String threadName = th.getName();
            if(null != threadName && threadName.equals(name)){
                return th;
            }
        }
        return null;
    }
    
    public static RedisJobService getJobService(){
        ApplicationContext context = new ClassPathXmlApplicationContext("kernelApiContext.xml");
        return (RedisJobService) context.getBean("redisJobServiceImpl");
    }
    
    public static void processJobReturnValue(String jobReturnValue, RedisJob job){
        RedisJobService redisJobServ = ServiceUtil.getJobService();
        long jobId = job.getJobId();
        int jobType = job.getType();
        if(null != jobReturnValue && !jobReturnValue.equals("")){
//                redisJobServ.updateJob(jobId, "status", "2");  
                Map values = new HashMap();
                switch(jobType){
                    case 1:
                    case 3:
                        values.put("uploadedPackagePath", jobReturnValue);
                        redisJobServ.updateJobInfoByJobType(jobId, DataUtil.JOB_TYPES[jobType], values);
                        if(jobType == 3){
                            redisJobServ.updateJob(jobId, "uploadedPackagePath", jobReturnValue);
                            redisJobServ.updateJob(jobId, "status", "6");
                        }
                        else if(jobType == 1){
                            redisJobServ.updateJob(jobId, "status", "2");
                        }
                        break;
                    case 2:
                    case 4:
                    case 5:
                    default:
                        redisJobServ.updateJob(jobId, "status", "2");
                        break;
                }
            }
            else{
                redisJobServ.updateJob(jobId, "status", "3");
            }
        redisJobServ.updateJob(jobId, "endTime", ShareokdataManager.getSimpleDateFormat().format(new Date()));
    }
    
    public static String getThreadNameByJob(RedisJob job){
        long uid = job.getUserId();
        String jobType = DataUtil.JOB_TYPES[job.getType()];
        int serverId = job.getServerId();
        long jobId = job.getJobId();
        return String.valueOf(uid)+"--"+jobType+"--"+String.valueOf(serverId)+"--"+String.valueOf(jobId);
    }
    
    public static RedisJobService getJobServiceByJobType(RedisJob job){
        ApplicationContext context = new ClassPathXmlApplicationContext("kernelApiContext.xml");
        switch(job.getType()){
            case 2:
                return (DspaceApiJobServiceImpl)context.getBean("dspaceApiJobServiceImpl");
            default:
                return (RedisJobService) context.getBean("redisJobServiceImpl");
        }
    }
    
    public static DspaceJournalDataService getDspaceJournalDataServInstanceByPublisher(String publisher){
        DspaceJournalDataService obj = null;
        try {
            String bean = serviceBeanMap.get(publisher);
            if(null == bean){
                throw new NotFoundServiceBeanException("No beans found for publisher "+publisher);
            }
            ApplicationContext context = new ClassPathXmlApplicationContext("kernelApiContext.xml");
            return (DspaceJournalDataService)context.getBean(bean);            
        } catch (NotFoundServiceBeanException ex) {
            logger.error("Cannot create the instance to get item information by DOI! ", ex);
        }
        return obj;
    }
    
    public static String generateDspaceSafPackagesByDois(String[] dois){
        return generateDspaceSafPackagesByDois(dois, null, null);
    }
    
    public static String generateDspaceSafPackagesByDois(String[] dois, String startDate, String endDate){
        String safDownloadPaths = null;
        Map<String, List<String>>doisMap = new HashMap<>();
        for(String doi : dois){
            try{
                String[] doiInfo = DataHandlersUtil.getItemInfoByDoi(doi).split("\\n");
                if(null == doiInfo || doiInfo.length != 2){
                    throw new InvalidDoiException("Cannot get the correct response from crossref for DOI: "+doi);
                }
                else if(!doiInfo[0].equals("200")){
                    throw new InvalidDoiException("The response code is "+ doiInfo[0] + " from crossref for DOI: "+doi);
                }
                else{
                    JSONObject obj = new JSONObject(doiInfo[1]);
                    String url = obj.getJSONObject("message").getString("URL");
                    String publisher = obj.getJSONObject("message").getString("publisher");
                    String key = ServiceUtil.serviceBeanMap.get(publisher);
                    List list = doisMap.get(key);
                    if(null == list){
                        list = new ArrayList<>();
                    }
                    list.add(doi);
                    doisMap.put(key, list);                                
                }
            } catch (InvalidDoiException ex) {
                String error = "error: cannot get correct response from crossref by doi="+doi;
                logger.error(error, ex);
                return error;
            }
        }
        List<String> pathList = new ArrayList<>();
        if(!doisMap.isEmpty()){
            Date now = new Date();            
            ApplicationContext context = new ClassPathXmlApplicationContext("kernelApiContext.xml");        
            for(String key : doisMap.keySet()){
                try{
                    DspaceJournalDataService serviceObj = (DspaceJournalDataService)context.getBean(key);
                    if(null == serviceObj){
                        throw new NoServiceProcessDoiException("Cannot find the DOI processing service for bean " + key + "!");
                    }
                    String[] doisByPubliser = doisMap.get(key).toArray(new String[doisMap.get(key).size()]);
                    String safFilePath = serviceObj.getDspaceJournalLoadingFilesByDoi(doisByPubliser, now);
                    pathList.add(safFilePath);
                }
                catch(Exception ex){
                    logger.error("Cannot get DspaceJournalDataService object to generate SAF package with bean = "+key, ex);
                }
            }                
        }
        
        // Now reset the SAF package file name based on start date and end date:
        if(null != startDate && null != endDate){    
            for(int i = 0; i < pathList.size(); i++){
                String safPath = pathList.get(i);
                File saf = new File(safPath);
                String safFilePath = saf.getAbsolutePath();
                String safFileName = saf.getName();
                String safFileNameBase = DocumentProcessorUtil.getFileNameWithoutExtension(safFileName);
                String safFileNameExtension = DocumentProcessorUtil.getFileExtension(safFileName);
                String safFileContainer = DocumentProcessorUtil.getFileContainerPath(safFilePath);
                safFileNameBase += "_" + startDate + "_" + endDate;
                String newFileName = safFileContainer+safFileNameBase+"."+safFileNameExtension;
                try{
                    boolean rename = DocumentProcessorUtil.renameFile(safFilePath, newFileName);
                    if(rename){
                    pathList.set(i, newFileName);
                    }
                    else{
                        pathList.set(i, safFileName+" cannot be renamed to "+newFileName);
                    }
                }
                catch(IOException ex){
                    pathList.set(i, safFileName+" cannot be renamed to "+newFileName+": "+ex.getMessage());
                }
            }
            
        }
        
        safDownloadPaths = DataUtil.getJsonFromStringList(pathList);
        
        return safDownloadPaths;
    }
    
    public static AwsDissertationService getAwsDissertationProcessorServiceInstance(){
        ApplicationContext context = new ClassPathXmlApplicationContext("kernelApiContext.xml");
        AwsDissertationService awsServ = (AwsDissertationServiceImpl)context.getBean("awsDissertationServiceImpl");
        return awsServ;
    }
    
    public static String executeCommandLineTask(String taskId, String taskType, String data) throws InvalidCommandLineArgumentsException, JSONException, IOException{
        
        BufferedWriter loggingForUserFileInfoFileWr = null;
        BufferedWriter onputFileInfoFileWr = null;
        String message;
        String startDate;
        String endDate;
        String outputFilePath = null;
        Integer compare;
        
        try{
            loggingForUserFileInfoFileWr = new BufferedWriter(new FileWriter(DataHandlersUtil.getLoggingForUserFilePath(taskId, taskType), true));
            if(DocumentProcessorUtil.isEmptyString(taskId)){
                message = "The data argument is null or empty task ID!";                
                throw new InvalidCommandLineArgumentsException(message);
            }
            if(DocumentProcessorUtil.isEmptyString(taskType)){
                message = "The data argument does not specify task type!";
                throw new InvalidCommandLineArgumentsException(message);
            }
            if(DocumentProcessorUtil.isEmptyString(data)){
                message = "The data provided for execution of the task is empty!\n";                
                loggingForUserFileInfoFileWr.write(message);
                throw new InvalidCommandLineArgumentsException(message);
            }            
            
            DataHandlersUtil.CURRENT_TASK_TYPE = taskType;            
            JSONObject dataObj = new JSONObject(data);

            switch(taskType){
                case "journal-search":                
                    String publisher = dataObj.getString("publisher");
                    DataHandlersUtil.CURRENT_TASK_ID = taskId;
                    startDate = dataObj.getString("startDate");
                    endDate = dataObj.getString("endDate");
                    String affiliate = dataObj.getString("affiliate");
                    loggingForUserFileInfoFileWr.write("Task information:\n");
                    loggingForUserFileInfoFileWr.write("Search publications at "+publisher+" between "+startDate+" and "+endDate+" by authors at "+affiliate+".\n");
                    loggingForUserFileInfoFileWr.flush();
                    
                    if(DocumentProcessorUtil.isEmptyString(publisher) || DocumentProcessorUtil.isEmptyString(taskId) || DocumentProcessorUtil.isEmptyString(startDate) || 
                        DocumentProcessorUtil.isEmptyString(endDate) || DocumentProcessorUtil.isEmptyString(affiliate)){
                        message = "Cannot get specific information such as publisher, start date, end date, and/or author affiliation to execute the task.\n";
                        loggingForUserFileInfoFileWr.write(message);
                        throw new InvalidCommandLineArgumentsException(message);
                    }
                    compare = DataHandlersUtil.datesCompare(startDate, endDate);
                    if(compare == null){
                        message = "Cannot parse the start date or the end date!\n";
                        loggingForUserFileInfoFileWr.write(message);
                        throw new InvalidCommandLineArgumentsException(message);
                    }
                    else if(compare > 0){
                        message = "The start date is later than the end date!\n";
                        loggingForUserFileInfoFileWr.write(message);
                        throw new InvalidCommandLineArgumentsException(message);
                    }
                    try{                    
                        DspaceJournalDataService serviceObj = ServiceUtil.getDspaceJournalDataServInstanceByPublisher(publisher);
                        if(null == serviceObj){
                            loggingForUserFileInfoFileWr.write("The program has internal error, please contact relative personnel.\n");
                            onputFileInfoFileWr.write("Cannot get the service bean from task type: "+taskType);
                            return null;
                        }
                        String articlesData = serviceObj.getApiResponseByDatesAffiliate(startDate, endDate, affiliate);                    
                        if(!DocumentProcessorUtil.isEmptyString(articlesData)){
                            articlesData = articlesData.replace("’", "'");
                            outputFilePath = DataHandlersUtil.getTaskFileFolderPath(taskId, taskType) + File.separator + startDate + "_" + endDate + ".json";
                            File outputFile = new File(outputFilePath);
                            if(!outputFile.exists()){
                                outputFile.createNewFile();
                            }
                            DocumentProcessorUtil.outputStringToFile(articlesData, outputFilePath);
                            System.out.println("article data = "+articlesData);
                            loggingForUserFileInfoFileWr.write("The journal search task has been completed sucessfully.\n");
                        }
                        else{
                            loggingForUserFileInfoFileWr.write("The program has internal error, please contact relative personnel.\n");
                            System.out.println("The "+taskType+" task id="+taskId+" cannot retrieve the article data!");
                        }
                    }
                    catch(Exception ex){
                        loggingForUserFileInfoFileWr.write("The program has internal error, please contact relative personnel.\n");
                        logger.error("Cannot complete the "+taskType+" with id="+taskId, ex);
                    }                
    //                articlesData = articlesData.replaceAll("'", "\\\\\\'");                
                    break;
                case "journal-saf":         
                    String[] dois;
                    DataHandlersUtil.CURRENT_TASK_ID = taskId;
                    startDate = dataObj.getString("startDate");
                    endDate = dataObj.getString("endDate");
                    dois = dataObj.getString("dois").split(";");                          
                    if(DocumentProcessorUtil.isEmptyString(startDate) || DocumentProcessorUtil.isEmptyString(endDate) || null == dois || dois.length == 0){
                        message = "Cannot get specific information such as publication DOI, start date, and/or end date to execute the task.\n";
                        loggingForUserFileInfoFileWr.write(message);
                        throw new InvalidCommandLineArgumentsException(message);
                    }
                    loggingForUserFileInfoFileWr.write("Task information:\n");
                    loggingForUserFileInfoFileWr.write("Generate DSpace SAF packge for publications with DOIs in "+Arrays.toString(dois)+" which are published between "+startDate+" and "+endDate+".\n");
                    loggingForUserFileInfoFileWr.flush();
                    
                    compare = DataHandlersUtil.datesCompare(startDate, endDate);
                    if(compare == null){
                        message = "Cannot parse the start date or the end date!\n";
                        loggingForUserFileInfoFileWr.write(message);
                        throw new InvalidCommandLineArgumentsException(message);
                    }
                    else if(compare > 0){
                        message = "The start date is later than the end date!\n";
                        loggingForUserFileInfoFileWr.write(message);
                        throw new InvalidCommandLineArgumentsException(message);                            
                    }
    
                    try{                    
                        outputFilePath = ServiceUtil.generateDspaceSafPackagesByDois(dois, startDate, endDate);
                        if(outputFilePath.startsWith("error")){
                            message = "The DOI provided is not valid: "+outputFilePath+"\n";
                            System.out.println(message);
                            loggingForUserFileInfoFileWr.write(message);
                            throw new ErrorDspaceApiResponseException(message);
                        }
                        else if(null == outputFilePath){
                            loggingForUserFileInfoFileWr.write("The program has internal error, please contact relative personnel.\n");
                            throw new ErrorDspaceApiResponseException("Cannot get null saf path!");
                        }
                        else{
                            loggingForUserFileInfoFileWr.write("safPath="+outputFilePath+"\n");
                            loggingForUserFileInfoFileWr.write("The SAF package has been prepared sucessfully.\n");
                            System.out.println("The SAF package has been stored at path="+outputFilePath);
                        }
                    }
                    catch(Exception ex){
                        logger.error(ex);
                        loggingForUserFileInfoFileWr.write("The program has internal error, please contact relative personnel.\n");
                        ex.printStackTrace();
                    }
                    break;
                case "journal-import":
                    try{                        
                        DataHandlersUtil.CURRENT_TASK_ID = taskId;
                        outputFilePath = ShareokdataManager.getDspaceCommandLineTaskOutputPath() + "_" + DataHandlersUtil.getCurrentTimeString()+"_"+taskType+"_"+taskId+".txt";
                        String safPath = dataObj.getString("safPath");
                        String collectionHandle = dataObj.getString("collectionHandle");
                        String dspaceApiUrl = dataObj.getString("dspaceApiUrl");
                        loggingForUserFileInfoFileWr.write("Importing into DSpace repo with safPath="+safPath+" collection ID="+collectionHandle+" and rest end point="+dspaceApiUrl);
                        if(DocumentProcessorUtil.isEmptyString(safPath) || DocumentProcessorUtil.isEmptyString(collectionHandle) || DocumentProcessorUtil.isEmptyString(dspaceApiUrl)){
                            message = "Cannot get specific information such as SAF package path, collection handle, and/or DSpace REST API url to execute the task.\n";
                            loggingForUserFileInfoFileWr.write(message);
                            throw new InvalidCommandLineArgumentsException(message);
                        }
                        loggingForUserFileInfoFileWr.write("Task information:\n");
                        loggingForUserFileInfoFileWr.write("Import DSpace SAF packge into collection: "+ collectionHandle + " with DSPace REST API URL=" + dspaceApiUrl + "\n");
                        loggingForUserFileInfoFileWr.flush();
                        
                        DspaceRestServiceImpl ds = (DspaceRestServiceImpl)getDataService("rest-import-dspace");
                        ds.getHandler().setReportFilePath(DataHandlersUtil.getJobReportPath("cli-import-dspace-"+taskType, taskId)+ File.separator + taskId + "-report.txt");
                        ds.loadItemsFromSafPackage(safPath, collectionHandle, dspaceApiUrl);                    
                    }
                    catch(Exception ex){
                        logger.error(ex);
                        ex.printStackTrace();
                    }
                    break;
                case "saf-build":
                    try{
                        loggingForUserFileInfoFileWr.write("Task information:\n");
                        String csvPath = dataObj.getString("csvPath");
                        String zipped = dataObj.getString("zipped");
                        if(DocumentProcessorUtil.isEmptyString(csvPath) || DocumentProcessorUtil.isEmptyString(zipped)){
                            message = "Cannot get specific information such as csv file path and/or zipped value to execute the task.\n";
                            loggingForUserFileInfoFileWr.write(message);
                            throw new InvalidCommandLineArgumentsException(message);
                        }
                        String extension = DocumentProcessorUtil.getFileExtension(csvPath);
                        if(null == extension || !extension.contains("csv")){
                            throw new NonCsvFileException("The uploaded file is not a CSV file!");
                        }
                        loggingForUserFileInfoFileWr.write("Generate a SAF package with metadata file at "+csvPath+".\n");
                        loggingForUserFileInfoFileWr.write("Start generating...\n");
                        SAFPackage safPackageInstance = new SAFPackage();
                        safPackageInstance.processMetaPack(csvPath, true);
                        String csvDirectoryPath = DocumentProcessorUtil.getFileContainerPath(csvPath);
                        File safPackage = new File(csvDirectoryPath + File.separator + "SimpleArchiveFormat");
                        File safPackageZipped = new File(csvDirectoryPath + File.separator + "SimpleArchiveFormat.zip");
                        if(zipped.equals("true")){
                            FileUtils.deleteDirectory(safPackage);
                            outputFilePath = safPackageZipped.getAbsolutePath();
                            message = "The new SAF package path is: \nsafPath=[\""+outputFilePath+"\"]\n";
                            System.out.println(message);
                            loggingForUserFileInfoFileWr.write(message);
                            return outputFilePath;
                        }
                        else{
                            FileUtils.deleteQuietly(safPackageZipped);
                            outputFilePath = safPackage.getAbsolutePath();
                            message = "The new SAF package path is: \nsafPath=[\""+outputFilePath+"\"]\n";
                            System.out.println(message);
                            loggingForUserFileInfoFileWr.write(message);
                            return outputFilePath;
                        }                        

                    } catch (IOException | NonCsvFileException ex) {
                        logger.error(ex.getMessage());
                        loggingForUserFileInfoFileWr.write("Error:"+ex.getMessage()+"\n");
                        loggingForUserFileInfoFileWr.flush();
                    }
                    break;
                case "aws-dissertation":
                    try{
                        String jsonPath = dataObj.getString("json");
                        String json = DocumentProcessorUtil.readFileIntoString(jsonPath);
                        loggingForUserFileInfoFileWr.write("Starting to create a processor to handle the generation of SAF package...");
                        AwsDissertationService awsServ = getAwsDissertationProcessorServiceInstance();
                        String processorInfo = awsServ.parseRecipeFile(json);
                        if(StringUtils.isBlank(processorInfo)){
                            throw new AwsDissertationProcessorParseRecipeJsonException("Empty string was returned after dissertation processor parsed recipe JSON!");
                        }
                        loggingForUserFileInfoFileWr.write(processorInfo);
                        String path = awsServ.generateDissertationSafPackage();
                        message = "The SAF package has been successfully created:\n safPath=[\""+path+"\"]\n";
                        loggingForUserFileInfoFileWr.write(message);
                        System.out.println(message);
                    }
                    catch(Exception ex){
                        logger.error(ex.getMessage());
                        loggingForUserFileInfoFileWr.write("Error:"+ex.getMessage()+"\n");
                        loggingForUserFileInfoFileWr.flush();
                    }
                    break;
                default:
                    throw new InvalidCommandLineArgumentsException("The command line task type is valid!");
            }
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
        finally{
            if(null != loggingForUserFileInfoFileWr){
                try{
                    loggingForUserFileInfoFileWr.flush();
                    loggingForUserFileInfoFileWr.close();                    
                }
                catch(IOException ex){
                    ex.printStackTrace();
                }
            }
        }
        return outputFilePath;
    }

}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shareok.data.dspacemanager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringEscapeUtils;
import org.shareok.data.config.DataUtil;
import org.shareok.data.datahandlers.DataHandlersUtil;
import org.shareok.data.datahandlers.exceptions.ErrorUnzipSafPackageException;
import org.shareok.data.datahandlers.exceptions.IncompleteServerInfoException;
import org.shareok.data.datahandlers.exceptions.NoExistingApiTokenException;
import org.shareok.data.datahandlers.exceptions.SecurityFileDoesNotExistException;
import org.shareok.data.documentProcessor.DocumentProcessorUtil;
import org.shareok.data.documentProcessor.FileZipper;
import org.shareok.data.dspacemanager.exceptions.EmptyDspaceCredentialInfoException;
import org.shareok.data.dspacemanager.exceptions.ErrorDspaceApiResponseException;
import org.shareok.data.dspacemanager.exceptions.SafPackageMissingFileException;
import org.shareok.data.dspacemanager.exceptions.SafPackagePathErrorException;
import org.shareok.data.htmlrequest.HttpRequestHandler;
import org.shareok.data.htmlrequest.exceptions.ErrorConnectionOutputStreamException;
import org.shareok.data.htmlrequest.exceptions.ErrorHandlingResponseException;
import org.shareok.data.htmlrequest.exceptions.ErrorOpenConnectionException;
import org.shareok.data.htmlrequest.exceptions.ErrorResponseCodeException;
import org.shareok.data.htmlrequest.exceptions.ReadResponseInputStreamException;
import org.shareok.data.redis.RedisUtil;
import org.shareok.data.redis.job.DspaceApiJob;
import org.shareok.data.redis.job.RedisJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Tao Zhao
 */
public class DspaceApiHandlerImpl implements DspaceApiHandler{
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(DspaceApiHandlerImpl.class);
    
    public static final List<String> METADATA_FILE_NAMES_LIST;
    static{
        METADATA_FILE_NAMES_LIST = new ArrayList<>();
        METADATA_FILE_NAMES_LIST.add("dcterms");
        METADATA_FILE_NAMES_LIST.add("dublin_core");
        METADATA_FILE_NAMES_LIST.add("metadata_ou");
    }
    
    public static final Map <String, String> DSPACE_FILE_MIME_TYPE_LIST;
    static{
        DSPACE_FILE_MIME_TYPE_LIST = new HashMap<>();
        DSPACE_FILE_MIME_TYPE_LIST.put("pdf", "application/pdf");
        DSPACE_FILE_MIME_TYPE_LIST.put("swf", "application/x-shockwave-flash");
        DSPACE_FILE_MIME_TYPE_LIST.put("apk", "application/vnd.android.package-archive");
        DSPACE_FILE_MIME_TYPE_LIST.put("avi", "video/x-msvideo");
        DSPACE_FILE_MIME_TYPE_LIST.put("bmp", "image/bmp");
        DSPACE_FILE_MIME_TYPE_LIST.put("torrent", "application/x-bittorrent");
        DSPACE_FILE_MIME_TYPE_LIST.put("sh", "application/x-sh");
        DSPACE_FILE_MIME_TYPE_LIST.put("c", "text/x-c");
        DSPACE_FILE_MIME_TYPE_LIST.put("css", "text/css");
        DSPACE_FILE_MIME_TYPE_LIST.put("csv", "text/csv");
        DSPACE_FILE_MIME_TYPE_LIST.put("curl", "text/vnd.curl");
        DSPACE_FILE_MIME_TYPE_LIST.put("gif", "image/gif");
        DSPACE_FILE_MIME_TYPE_LIST.put("rip", "audio/vnd.rip");
        DSPACE_FILE_MIME_TYPE_LIST.put("var", "application/java-archive");
        DSPACE_FILE_MIME_TYPE_LIST.put("java", "text/x-java-source,java");
        DSPACE_FILE_MIME_TYPE_LIST.put("js", "application/javascript");
        DSPACE_FILE_MIME_TYPE_LIST.put("json", "application/json");
        DSPACE_FILE_MIME_TYPE_LIST.put("jpm", "video/jpm");
        DSPACE_FILE_MIME_TYPE_LIST.put("jpeg", "image/jpeg");
        DSPACE_FILE_MIME_TYPE_LIST.put("jpg", "image/jpeg");
        DSPACE_FILE_MIME_TYPE_LIST.put("xls", "application/vnd.ms-excel");
        DSPACE_FILE_MIME_TYPE_LIST.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        DSPACE_FILE_MIME_TYPE_LIST.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        DSPACE_FILE_MIME_TYPE_LIST.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        DSPACE_FILE_MIME_TYPE_LIST.put("ppt", "application/vnd.ms-powerpoint");
        DSPACE_FILE_MIME_TYPE_LIST.put("wma", "audio/x-ms-wma");
        DSPACE_FILE_MIME_TYPE_LIST.put("doc", "application/msword");
        DSPACE_FILE_MIME_TYPE_LIST.put("mpeg", "video/mpeg");
        DSPACE_FILE_MIME_TYPE_LIST.put("mp4", "video/mp4");
        DSPACE_FILE_MIME_TYPE_LIST.put("png", "image/png");
        DSPACE_FILE_MIME_TYPE_LIST.put("rar", "application/x-rar-compressed");
        DSPACE_FILE_MIME_TYPE_LIST.put("rtx", "text/richtext");
        DSPACE_FILE_MIME_TYPE_LIST.put("rm", "application/vnd.rn-realmedia");
        DSPACE_FILE_MIME_TYPE_LIST.put("movie", "video/x-sgi-movie");
        DSPACE_FILE_MIME_TYPE_LIST.put("tiff", "image/tiff");
        DSPACE_FILE_MIME_TYPE_LIST.put("txt", "text/plain");
        DSPACE_FILE_MIME_TYPE_LIST.put("xml", "application/xml");
        DSPACE_FILE_MIME_TYPE_LIST.put("zip", "application/zip");
        DSPACE_FILE_MIME_TYPE_LIST.put("tif", "image/tiff");
    }
    
    private String token;
    private DspaceApiJob job;
    private String reportFilePath;
    private String mapping;
    private String output;
    private String dspaceApiUrl;
    private HttpRequestHandler httpRequestHandler;

    @Override
    public DspaceApiJob getJob() {
        return job;
    }

    public String getReportFilePath() {
        return reportFilePath;
    }

    public String getToken() {
        return token;
    }

    public HttpRequestHandler getHttpRequestHandler() {
        return httpRequestHandler;
    }

    public String getMapping() {
        return mapping;
    }

    public String getOutput() {
        return output;
    }

    public String getDspaceApiUrl() {
        return dspaceApiUrl;
    }

    @Autowired
    @Qualifier("dspaceApiJob")
    public void setJob(RedisJob job) {
        this.job = (DspaceApiJob)job;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setMapping(String mapping) {
        this.mapping = mapping;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public void setDspaceApiUrl(String dspaceApiUrl) {
        this.dspaceApiUrl = dspaceApiUrl;
    }

    @Autowired
    public void setHttpRequestHandler(HttpRequestHandler httpRequestHandler) {
        this.httpRequestHandler = httpRequestHandler;
    }
    
    @Override
    public String getTokenFromServer(){
        
        String dspaceUserName = null;
        String dspacePassword = null;
        if(DocumentProcessorUtil.isEmptyString(dspaceApiUrl)){
            dspaceApiUrl = RedisUtil.getServerDaoInstance().findServerById(job.getServerId()).getAddress();
        }
        
	String[] credential = null;
	
	try {

	    if(null == dspaceApiUrl || "".equals(dspaceApiUrl)){
                throw new IncompleteServerInfoException("empty or null url.");
            }

            String domain = DataHandlersUtil.getDomainNameFromUrl(dspaceApiUrl);
            credential = DataHandlersUtil.getRepoCredentials(domain);
            dspaceUserName = credential[0];
            dspacePassword = credential[1];
        } catch (SecurityFileDoesNotExistException | IOException ex) {
            logger.error(ex.getMessage());
            return null;
        } catch (URISyntaxException ex) {
            logger.error("Repository URL cannot be parsed: " + ex.getMessage());
            return null;
        } catch (IncompleteServerInfoException ex) {
            logger.error("Incomplete server information." + ex.getMessage());
        }    
        
        try{
            if(null == dspaceUserName || "".equals(dspaceUserName) || null == dspacePassword || "".equals(dspacePassword)){
                throw new EmptyDspaceCredentialInfoException("The DSpace username or DSpace password information is missing!");
            }
        
            Map<String, String> headerInfo = new HashMap<>();
            headerInfo.put("Content-Type", "application/json");            
            String response = httpRequestHandler.requestWithHeaderInfo(dspaceApiUrl+"/login", "POST", headerInfo, "{\"email\":\""+dspaceUserName+"\",\"password\":\""+dspacePassword+"\"}").toString();
            String[]responseInfo = response.split("\\n");
            if(null != responseInfo[0]){
                if(responseInfo[0].equals("code:200")){
                    token = responseInfo[1];
                    return token;
                }
                else{
                    throw new ErrorDspaceApiResponseException("Got response code "+responseInfo[0]);
                }
            }
        }
        catch(EmptyDspaceCredentialInfoException empEx){
            logger.error("Missing DSpace log in information", empEx);
        } catch (ErrorDspaceApiResponseException ex) {
            logger.error("Missing DSpace logging in information", ex);
        }
        return null;
    }
    
    public String getTokenByUserCredentials(String dspaceApiUrl, String userName, String password){
        String token = null;
        try{
            if(null == userName || "".equals(userName) || null == password || "".equals(password)){
                throw new EmptyDspaceCredentialInfoException("The DSpace username or DSpace password information is missing!");
            }
        
            Map<String, String> headerInfo = new HashMap<>();
            headerInfo.put("Content-Type", "application/json");            
            String response = httpRequestHandler.requestWithHeaderInfo(dspaceApiUrl+"/login", "POST", headerInfo, "{\"email\":\""+userName+"\",\"password\":\""+password+"\"}").toString();
            String[]responseInfo = response.split("\\n");
            if(null != responseInfo[0]){
                if(responseInfo[0].equals("code:200")){
                    token = responseInfo[1];
                    return token;
                }
                else{
                    throw new ErrorDspaceApiResponseException("Got response code "+responseInfo[0]);
                }
            }
        }
        catch(EmptyDspaceCredentialInfoException empEx){
            logger.error("Missing DSpace log in information", empEx);
        } catch (ErrorDspaceApiResponseException ex) {
            logger.error("Missing DSpace logging in information", ex);
        }
        return token;
    }
    
    @Override
    public boolean isAuthorizedUser(){
        Map<String, Object> userInfo = getUserInfoByToken();
        
        if(null != userInfo){
            Object auth = userInfo.get("authenticated");
            if(null != auth && (Boolean)auth == true){
                return true;
            }
        }
        return false;
    }
    
    @Override
    public Map<String, Object> getUserInfoByToken(){
        
        if(null == token || "".equals(token)){
            getTokenFromServer();
        }
        
        Map<String, Object> userInfo = new HashMap<>();
        Map<String, String>header = new HashMap<>();
        header.put("Content-Type", "application/json");
        header.put("Accept", "application/json");
        header.put("rest-dspace-token", token);
        
        try{
            String dspaceApiUrl = RedisUtil.getServerDaoInstance().findServerById(job.getServerId()).getAddress();
            String response = httpRequestHandler.requestWithHeaderInfo(dspaceApiUrl + "/status", "GET", header, null).toString();
            String[] userInfoResponseArr = response.split("\\n");
            if(null != userInfoResponseArr[0] && userInfoResponseArr[0].equals("code:200")){
                response = userInfoResponseArr[1];
            }
            else{
                throw new ErrorDspaceApiResponseException("Got the response code "+userInfoResponseArr[0]);
            }
            return DataUtil.getMapFromJson(response);
        }
        catch(ErrorDspaceApiResponseException ex){
            logger.error("Cannot get the user information by token!", ex);
        }
        return null;
    }
    
    @Override
    public String getItemsInfoByCollectionId(String id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Map<String, Object>> getItemsListByCollectionHandler(String handle, String dspaceApiUrl) {
        try{            
            String collectionId = getObjectIdByHandler(handle, dspaceApiUrl);
            String itemCount = String.valueOf(getItemCountByCollectionHandler(handle, dspaceApiUrl));
            String itemsInfo = httpRequestHandler.sendGet(dspaceApiUrl + "/collections/" + collectionId + "/items?limit="+itemCount);
            String[] itemsInfoArr = itemsInfo.split("\\n");
            if(null != itemsInfoArr[0] && itemsInfoArr[0].equals("200")){
                    itemsInfo = itemsInfoArr[1];
            }
            else{
                throw new ErrorDspaceApiResponseException("Got the response code "+itemsInfoArr[0]);
            }
            List<Map<String, Object>> itemsList = DataUtil.getListFromJson(itemsInfo);   
            return itemsList;
        }
        catch(ErrorDspaceApiResponseException ex){
            logger.error("Cannot get object infor by its handle" , ex);
        }
        return null;
    }

    @Override
    public String getObjectIdByHandler(String handle) {
        Map map = DataUtil.getMapFromJson(getObjectInfoByHandler(handle));
        if(null != map.get("id")){
            return String.valueOf(map.get("id"));
        }
        return null;
    }
    
    @Override
    public String getObjectIdByHandler(String handle, String dspaceApiUrl) {
        try{
            Map map = DataUtil.getMapFromJson(getObjectInfoByHandler(handle, dspaceApiUrl));
            if(null != map.get("id")){
                return String.valueOf(map.get("id"));
            }
        }
        catch(ErrorDspaceApiResponseException ex){
            logger.error(ex);
        }
        return null;
    }
    
    @Override
    public String getObjectInfoByHandler(String handle){
        try{
            String dspaceApiUrl = RedisUtil.getServerDaoInstance().findServerById(job.getServerId()).getAddress();
            return getObjectInfoByHandler(handle, dspaceApiUrl);
        }
        catch(Exception ex){
            logger.error("Cannot get object infor by its handle" , ex);
        }
        return null;
    }
    
    @Override
    public String getObjectInfoByHandler(String handle, String dspaceApiUrl) throws ErrorDspaceApiResponseException{
        String objectInfo = httpRequestHandler.sendGet(dspaceApiUrl + "/handle/" + handle);
        String[] objectInfoArr = objectInfo.split("\\n");
        if(null != objectInfoArr[0] && objectInfoArr[0].equals("200")){
            return objectInfoArr[1];
        }
        else{
            throw new ErrorDspaceApiResponseException("Got the response code "+objectInfoArr[0]);
        }
    }

    @Override
    public String[] getItemIdsByCollectionId(String id) {
        try{
            String dspaceApiUrl = RedisUtil.getServerDaoInstance().findServerById(job.getServerId()).getAddress();
            return getItemIdsByCollectionId(id, dspaceApiUrl);
        }
        catch(Exception ex){
            logger.error("Cannot get object infor by its handle" , ex);
        }
        return null;
    }
    
    @Override
    public String[] getItemIdsByCollectionId(String id, String dspaceApiUrl) {
        try{            
            String itemCount = String.valueOf(getItemCountByCollectionId(id, dspaceApiUrl));
            String itemsInfo = httpRequestHandler.sendGet(dspaceApiUrl + "/collections/" + id + "/items?limit="+itemCount);
            String[] itemsInfoArr = itemsInfo.split("\\n");
            if(null != itemsInfoArr[0] && itemsInfoArr[0].equals("200")){
                    itemsInfo = itemsInfoArr[1];
            }
            else{
                throw new ErrorDspaceApiResponseException("Got the response code "+itemsInfoArr[0]);
            }
            List<Map<String, Object>> itemsList = DataUtil.getListFromJson(itemsInfo);
            if(itemsList.size() > 0){
                List<String> itemIdList = new ArrayList();
                for(Map<String, Object> item : itemsList){
                    Object idObj = item.get("id");
                    if(null != idObj && !"".equals(String.valueOf(idObj))){
                        itemIdList.add(String.valueOf(idObj));
                    }
                    else{
                        continue;
                    }
                }
                if(itemIdList.size() > 0){
                    return itemIdList.toArray(new String[itemIdList.size()]);
                }
            }
        }
        catch(ErrorDspaceApiResponseException ex){
            logger.error("Cannot get object infor by its handle" , ex);
        }
        return null;
    }

    @Override
    public String[] getItemIdsByCollectionHandler(String handle) {
        return getItemIdsByCollectionId(getObjectIdByHandler(handle));
    }
    
    @Override
    public String[] getItemIdsByCollectionHandler(String handle, String dspaceApiUrl) {
        return getItemIdsByCollectionId(getObjectIdByHandler(handle, dspaceApiUrl), dspaceApiUrl);
    }
    
    @Override
    public Map<String, String> getItemDoisByCollectionHandler(String handle, String dspaceApiUrl){
        Map<String, String> doiMap = new HashMap<>();
        String[] ids = getItemIdsByCollectionHandler(handle, dspaceApiUrl);
        if(null != ids && ids.length > 0){
            for(String id : ids){
                String[] doi = getMetadataValuesByKey(id, "dc.identifier.doi", dspaceApiUrl);
                if(null != doi && doi.length > 0){
                    doiMap.put(doi[0], doi[0]);
                }
            }
        }
        return doiMap;
    }
    
    @Override
    public List<Map<String, Object>> getItemMetadataById(String id){
        List<Map<String, Object>> metadataInfo = null;
        try{
            String dspaceApiUrl = RedisUtil.getServerDaoInstance().findServerById(job.getServerId()).getAddress();
            metadataInfo = getItemMetadataById(id, dspaceApiUrl);
        }catch(Exception ex){
            logger.error("Cannot get the item metadata!", ex);
        }
        return metadataInfo;
    }
    
    @Override
    public List<Map<String, Object>> getItemMetadataById(String id, String dspaceApiUrl){
        String metadataInfo;
        try{            
            String url = dspaceApiUrl + "/items/" + id + "/metadata";
            String metadataJson = httpRequestHandler.sendGet(url);
            String[] metadataJsonArr = metadataJson.split("\\n");
            if(null != metadataJsonArr[0] && metadataJsonArr[0].equals("200")){
                    metadataInfo = metadataJsonArr[1];
            }
            else{
                throw new ErrorDspaceApiResponseException("Got the response code "+metadataJsonArr[0]);
            }
            return DataUtil.getListFromJson(metadataInfo);
        }catch(ErrorDspaceApiResponseException ex){
            logger.error("Cannot get the item metadata!", ex);
        }
        return null;
    }
    
    @Override
    public String[] getMetadataValuesByKey(String itemId, String key){
        List<String> values = new ArrayList<>();
        try{
            List<Map<String, Object>> metadata = getItemMetadataById(itemId);
            ListIterator it = metadata.listIterator();
            while(it.hasNext()){
                Map itemMap = (HashMap)it.next();
                if(key.equals((String)itemMap.get("key"))){
                    values.add((String)itemMap.get("value"));
                }
            }
        }
        catch(Exception ex){
            logger.error("Cannot get the item metadata values by key!", ex);
        }
        return (values.toArray(new String[values.size()]));
    }
    
    @Override
    public String[] getMetadataValuesByKey(String itemId, String key, String dspaceApiUrl){
        List<String> values = new ArrayList<>();
        try{
            List<Map<String, Object>> metadata = getItemMetadataById(itemId, dspaceApiUrl);
            ListIterator it = metadata.listIterator();
            while(it.hasNext()){
                Map itemMap = (HashMap)it.next();
                if(key.equals((String)itemMap.get("key"))){
                    values.add((String)itemMap.get("value"));
                }
            }
        }
        catch(Exception ex){
            logger.error("Cannot get the item metadata values by key!", ex);
        }
        return (values.toArray(new String[values.size()]));
    }
    
    @Override
    public int getItemCountByCollectionHandler(String handle){
        return getItemCountByCollectionId(getObjectIdByHandler(handle));
    }
    
    @Override
    public int getItemCountByCollectionHandler(String handle, String dspaceApiUrl){
        return getItemCountByCollectionId(getObjectIdByHandler(handle, dspaceApiUrl), dspaceApiUrl);
    }
    
    @Override
    public int getItemCountByCollectionId(String id){
        try{
            String dspaceApiUrl = RedisUtil.getServerDaoInstance().findServerById(job.getServerId()).getAddress();
            String itemsInfo = httpRequestHandler.sendGet(dspaceApiUrl + "/collections/" + id);
            String[] itemsInfoArr = itemsInfo.split("\\n");
            if(null != itemsInfoArr[0] && itemsInfoArr[0].equals("200")){
                    itemsInfo = itemsInfoArr[1];
            }
            else{
                throw new ErrorDspaceApiResponseException("Got the response code "+itemsInfoArr[0]);
            }
            Map<String, Object> itemsList = DataUtil.getMapFromJson(itemsInfo);
            return (int) itemsList.get("numberItems");
        }
        catch(ErrorDspaceApiResponseException ex){
            logger.error("Cannot get object infor by its handle" , ex);
        }
        return -1;
    }
    
    @Override
    public int getItemCountByCollectionId(String id, String dspaceApiUrl){
        try{            
            String itemsInfo = httpRequestHandler.sendGet(dspaceApiUrl + "/collections/" + id);
            String[] itemsInfoArr = itemsInfo.split("\\n");
            if(null != itemsInfoArr[0] && itemsInfoArr[0].equals("200")){
                    itemsInfo = itemsInfoArr[1];
            }
            else{
                throw new ErrorDspaceApiResponseException("Got the response code "+itemsInfoArr[0]);
            }
            Map<String, Object> itemsList = DataUtil.getMapFromJson(itemsInfo);
            return (int) itemsList.get("numberItems");
        }
        catch(ErrorDspaceApiResponseException ex){
            logger.error("Cannot get object infor by its handle" , ex);
        }
        return -1;
    }
    
    /**
     * 
     * @param id : item internal id
     * @param data : is a list of maps of metadata entries: every entry should have three keys, i.e., "key", "value", and "language"
     */
    @Override
    public void updateItemMetadata(String id, List<Map<String, String>> data){
        if(null == data || data.isEmpty()){
            return;
        }
        String json = "[";
        for(Map entry : data){
            if(null != entry){
                json += "{";
                json += "\"key\":\"" + (String)entry.get("key") + "\",";
                json += "\"value\":\"" + (String)entry.get("value") + "\",";
                json += "\"language\":\"" + (String)entry.get("language") + "\"";
                json += "},";
            }
        }
        json = json.substring(0, json.length()-1) + "]";
        
        Map<String, String>header = new HashMap<>();
        header.put("Content-Type", "application/json");
        header.put("Accept", "application/json");
        header.put("rest-dspace-token", token);
        String dspaceApiUrl = RedisUtil.getServerDaoInstance().findServerById(job.getServerId()).getAddress();
        httpRequestHandler.requestWithHeaderInfo(dspaceApiUrl + "/items/" + id +"/metadata", "PUT", header, json);
    }
    
    @Override
    public Map<String, Object> createEmptyItem(String collectionId){
        String json = "{\"type\":\"item\",\"expand\":[\"metadata\",\"parentCollection\",\"parentCollectionList\"," +
                      "\"parentCommunityList\",\"bitstreams\",\"all\"],\"parentCollection\":null,\"parentCollectionList\":null," +
                      "\"parentCommunityList\":null,\"bitstreams\":null,\"archived\":\"true\",\"withdrawn\":\"false\"}";       
        
        Map<String, String>header = new HashMap<>();
        header.put("Content-Type", "application/json");
        header.put("Accept", "application/json");
        header.put("rest-dspace-token", token);
        
        if(DocumentProcessorUtil.isEmptyString(dspaceApiUrl)){
            dspaceApiUrl = RedisUtil.getServerDaoInstance().findServerById(job.getServerId()).getAddress();
        }
        
        String response = httpRequestHandler.requestWithHeaderInfo(dspaceApiUrl + "/collections/" + collectionId +"/items", "POST", header, json).toString();
        String[] userInfoResponseArr = response.split("\\n");
        if(null != userInfoResponseArr[0] && userInfoResponseArr[0].equals("code:200")){
            response = userInfoResponseArr[1];
        }
        else{
            try {
                throw new ErrorDspaceApiResponseException("Got the response code "+userInfoResponseArr[0]);
            } catch (ErrorDspaceApiResponseException ex) {
                logger.error("Cannot create a new empty item for collection " + collectionId, ex);
            }
        }
        return DataUtil.getMapFromJson(response);
    }
    
    @Override
    public void deleteItemById(String id){
        Map<String, String>header = new HashMap<>();
        header.put("Content-Type", "application/json");
        header.put("Accept", "application/json");
        header.put("rest-dspace-token", token);
        String dspaceApiUrl = RedisUtil.getServerDaoInstance().findServerById(job.getServerId()).getAddress();
        httpRequestHandler.requestWithHeaderInfo(dspaceApiUrl + "/items/" + id, "DELETE", header, null).toString();
    }
    
    @Override
    public String addItemMetadata(String id, String data){
        
        Map<String, String>header = new HashMap<>();
        header.put("Content-Type", "application/json");
        header.put("Accept", "application/json");
        header.put("rest-dspace-token", token);
        
        if(DocumentProcessorUtil.isEmptyString(dspaceApiUrl)){
            dspaceApiUrl = RedisUtil.getServerDaoInstance().findServerById(job.getServerId()).getAddress();
        }
        
        String response = httpRequestHandler.requestWithHeaderInfo(dspaceApiUrl + "/items/" + id +"/metadata", "POST", header, data).toString();
        String[] userInfoResponseArr = response.split("\\n");
        if(null != userInfoResponseArr[0] && userInfoResponseArr[0].equals("code:200")){
            if(userInfoResponseArr.length == 2){
                response = userInfoResponseArr[1];
            }
            else{
                response = userInfoResponseArr[0];
            }
        }
        else{
            try {
                throw new ErrorDspaceApiResponseException("Got the response code "+userInfoResponseArr[0]);
            } catch (ErrorDspaceApiResponseException ex) {
                logger.error("Cannot add a new metadata for the item " + id, ex);
                return null;
            }
        }
        return response;   
    }
    
    /**
     * Suppose ONLY two metadata file names for importing: dublin_core.xml and metadata_dcterms.xml
     * 
     * @param paths : paths of the metadata files
     * @return : string array of metadata information
     */
    @Override
    public Map<String, String> getMetadataFromXmlFiles(String[] paths){
        Map<String, String> data = new HashMap<>();
        Document doc = null;
        try{
            for(String path : paths){
                String json = "";
                String dcType = "dc";
                doc = DocumentProcessorUtil.loadXMLFromString(path);
                if(null != doc){
                    doc.getDocumentElement().normalize();
                    Node dcNode = doc.getDocumentElement();
                    Node schema = dcNode.getAttributes().getNamedItem("schema");
                    if(null != schema){
                        dcType = schema.getNodeValue();
                    }
                    NodeList nList = doc.getElementsByTagName("dcvalue");
                    for (int temp = 0; temp < nList.getLength(); temp++) {
                            Node nNode = nList.item(temp);
                            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                                    Element eElement = (Element) nNode;
                                    String dcElement = "";
                                    String qualifier = "";
                                    String lang = "en_US";
                                    String value = "";
                                    
                                    if(eElement.hasAttribute("element")){
                                        dcElement = eElement.getAttribute("element");
                                    }
                                    if(eElement.hasAttribute("qualifier")){
                                        qualifier = eElement.getAttribute("qualifier");
                                        if("none".equals(qualifier)){
                                            qualifier = "";
                                        }
                                    }
                                    if(eElement.hasAttribute("language")){
                                        lang = eElement.getAttribute("language");
                                    }
                                    value = StringEscapeUtils.escapeJava(eElement.getTextContent());
                                    
                                    String key = dcType + "." + dcElement + (!"".equals(qualifier) ? "."+qualifier : "");
                                    json += "{\"key\":\"" + key + "\",\"value\":\"" + value + "\",\"language\":\"" + lang + "\"},";
                            }
                    }
                }
                if(!"".equals(json)){
                    json = "[" + json.substring(0, json.length()-1) + "]";
                    data.put(path, json);
                }
            }
        }
        catch(Exception ex){
            logger.error("Cannot read metadata information from xml files" , ex);
        }
        return data;
    }
    
    /**
     * 
     * @param id : item id
     * @param filePath : path of the bitstream file
     * @param fileName : designed file name for the added bitstream
     * @param description : description of the bitstream
     * @return : bitstream metadata
     */
    @Override
    public Map<String, String> addItemBitstream(String id, String filePath, String fileName, String description) {
        
        String fileType = DocumentProcessorUtil.getFileExtension(filePath);
        
        Map<String, String>header = new HashMap<>();
        
        header.put("Content-Type", (String)DSPACE_FILE_MIME_TYPE_LIST.get(fileType));
        header.put("Accept", "application/json");
        header.put("rest-dspace-token", token);
        
        String response = null;
        try {
            if(DocumentProcessorUtil.isEmptyString(dspaceApiUrl)){
                dspaceApiUrl = RedisUtil.getServerDaoInstance().findServerById(job.getServerId()).getAddress();
            }
            
            response = httpRequestHandler.uploadFileByHttpClient(dspaceApiUrl + "/items/" + id +"/bitstreams?name="
                    +fileName+"&description="+description, filePath, header).toString();
        } catch (ErrorOpenConnectionException | ErrorConnectionOutputStreamException | ErrorResponseCodeException | ErrorHandlingResponseException | ReadResponseInputStreamException ex) {
            ex.printStackTrace();
            logger.error("Cannot create a new bitstream "+ filePath +" for item " + id, ex);
            return null;
        }
        
        if(null != response){
            try{
                String[] userInfoResponseArr = response.split("\\n");
                if(null != userInfoResponseArr[0] && userInfoResponseArr[0].equals("code:200")){
                    response = userInfoResponseArr[1];
                }
                else{
                    throw new ErrorDspaceApiResponseException("Got the response code "+userInfoResponseArr[0]);
                }
                Map responseMap = DataUtil.getMapFromJson(response);
                if(null == responseMap){
                    throw new ErrorDspaceApiResponseException("Cannot convert the response JSON into a Map!");
                }
                return responseMap;
            }
            catch(ErrorDspaceApiResponseException ex){
                logger.error("Cannot get correct response when creating a new bitstream "+ filePath +" for item " + id, ex);
            }
        }
        return null;
    }
    
    /**
     * Load saf package information and import it into DSpace repository
     * 
     * @return : the mapping of the imported information to DSpace items
     */
    @Override
    public Map<String, List<String>> loadItemsFromSafPackage(){
        
        String collectionHandle = job.getCollectionId();
        String safPath = job.getFilePath();
        if(DocumentProcessorUtil.isEmptyString(dspaceApiUrl)){
            dspaceApiUrl = RedisUtil.getServerDaoInstance().findServerById(job.getServerId()).getAddress();
        }
        
        Map<String, List<String>> importResults = new HashMap<>();
        
        if(DocumentProcessorUtil.isEmptyString(token)){
            getTokenFromServer();
            if(DocumentProcessorUtil.isEmptyString(token)){
                try {
                    throw new NoExistingApiTokenException("Cannot get useful token!");
                } catch (NoExistingApiTokenException ex) {
                    logger.error(ex.getMessage());
                    return null;
                }
            }
        }
        
        try{            
            File safFile = new File(safPath);
            if(safPath.endsWith(".zip")){
                String newPath = FileZipper.unzipToDirectory(safPath);
                if(null == newPath){
                    throw new ErrorUnzipSafPackageException("Cannot unzip the saf package at "+safPath);
                }
                // Change the path to be the unzipped folder
//                safPath = DocumentProcessorUtil.getFileNameWithoutExtension(safPath);
                safFile = new File(newPath);
            }
            if(safFile.isDirectory()){
                mainLoop:
                for(File file : safFile.listFiles()){
                    if(null != file && file.isDirectory()){
                        
                        File[] fileList = file.listFiles();
                        boolean containsContentsFile = false;                        
                        boolean containsMetadataFile = false;
                        
                        List<File> metadataFileList = new ArrayList<>();
                        File contentFile = null;
                        
                        for(File itemFile : fileList){    
                            
                            if(null == itemFile){
                                continue;
                            }
                            
                            String fileName = itemFile.getName();
                            if("contents".equals(fileName)){
                                containsContentsFile = true;
                                contentFile = itemFile;
                            }
                            else if(METADATA_FILE_NAMES_LIST.contains(fileName.replaceAll(".xml", ""))){
                                // Check if there are some duplicates based on doi
                                if(fileName.contains("dublin")){
                                    String doi = findDoiFromDublin(itemFile.getAbsolutePath());
                                    if(null != doi && checkDuplicatesByDoi(doi, collectionHandle, dspaceApiUrl)){
                                        logger.debug("Duplication detected:in collection "+collectionHandle+" there has already been an item with doi="+doi);
                                        output += "Duplication detected:in collection \"+collectionHandle+\" there has already been an item with doi="+doi+".\n\n";
                                        continue mainLoop;
                                    }
                                }
                                containsMetadataFile = true;
                                metadataFileList.add(itemFile);
                            }
                        }
                        if(containsContentsFile == true && containsMetadataFile == true){
                            // Create the new item now:
                            Map newItemInfo = createEmptyItem(getObjectIdByHandler(collectionHandle, dspaceApiUrl));
                            String newItemId = String.valueOf(newItemInfo.get("id"));
                            String newItemHandle = (String)newItemInfo.get("handle");
                            logger.debug("A new item with handle = "+newItemHandle+" has been added to collection "+collectionHandle+".");
                            output += "A new item with handle = "+newItemHandle+" has been added to collection "+collectionHandle+".\n\n";
                            mapping += file.getName() + "   " + newItemHandle+"\n";
                            
                            // Add the metadata to the new item:
                            String[] paths = new String[metadataFileList.size()];
                            ListIterator metadataIt = metadataFileList.listIterator();
                            while(metadataIt.hasNext()){
                                File metadataFile = (File)metadataIt.next();
                                paths[metadataIt.nextIndex()-1] = metadataFile.getAbsolutePath();
                            }
                            Map<String, String> metadataStrings = getMetadataFromXmlFiles(paths);
                            for(String path : metadataStrings.keySet()){
                                String metadata = metadataStrings.get(path);                                
                                logger.debug(" adding metadata file " + metadata +" now with file name : "+file.getName());
                                output += " adding metadata file " + metadata +" now with file name : "+file.getName()+"\n\n";
                                try{
                                String metadataInfo = addItemMetadata(newItemId, metadata);
                                if(null == metadataInfo || metadataInfo.equals("")){
                                    if(null == importResults.get("metadata-imported")){
                                        importResults.put("metadata-imported", new ArrayList<String>());
                                    }
                                    List metadataUnimportedList = (ArrayList)importResults.get("metadata-imported");
                                    metadataUnimportedList.add(newItemId + "---" + path);
                                    logger.debug("Failed to add the metadata into item "+newItemHandle+".\n");
                                    output += "Failed to add the metadata into item "+newItemHandle+".\n\n";
                                }
                                else{
                                    if(null == importResults.get("metadata-imported")){
                                        importResults.put("metadata-imported", new ArrayList<String>());
                                    }
                                    List metadataImportedList = (ArrayList)importResults.get("metadata-imported");
                                    metadataImportedList.add(newItemId + "---" + path);
                                    logger.debug("A new set of metadata entries have been added to the item "+newItemHandle+". \n");
                                    output += "A new set of metadata entries have been added to the item "+newItemHandle+". \n\n";
                                }
                                }
                                catch(Exception ex){
                                    if(null == importResults.get("metadata-imported")){
                                        importResults.put("metadata-imported", new ArrayList<String>());
                                    }
                                    List metadataUnimportedList = (ArrayList)importResults.get("metadata-imported");
                                    metadataUnimportedList.add(newItemId + "---" + path);
                                    logger.debug("Failed to add metadata into item "+newItemHandle+"\n"+ex.getMessage());
                                    output += "Failed to add metadata into item "+newItemHandle+"\n"+ex.getMessage()+"\n\n";
                                }
                            }
                            
                            List bitstreamFileList = DocumentProcessorUtil.readTextFileIntoList(contentFile.getAbsolutePath());
                            ListIterator it = bitstreamFileList.listIterator();
                            while(it.hasNext()){
                                String bitstreamFileName = (String)it.next();
                                File bitstreamFile = new File(file.getAbsoluteFile() + File.separator + bitstreamFileName);
                                if(!bitstreamFile.exists()){
                                    logger.debug("The bitstream file "+bitstreamFileName+" does not exist in the saf package "+safPath+"!\n");
                                    output += "The bitstream file "+bitstreamFileName+" does not exist in the saf package "+safPath+"!\n\n";
                                }
                                else{
                                    String newName = bitstreamFile.getName().replace(" ", "_");
                                    try{
                                        Map bitstreamInfo = addItemBitstream(newItemId, bitstreamFile.getAbsolutePath(), newName, newName);
                                        if(null != bitstreamInfo){
                                            if(null == importResults.get("bitstream-imported")){
                                                importResults.put("bitstream-imported", new ArrayList<String>());
                                            }
                                            List bitstreamImportedList = (ArrayList)importResults.get("bitstream-imported");
                                            bitstreamImportedList.add(newItemId + "---" + bitstreamFile.getAbsoluteFile());
                                            logger.debug("A new bitstream file "+bitstreamFileName+" with link "+((String)bitstreamInfo.get("retrieveLink"))+" has been added to the item "+newItemHandle+". \n");
                                            output += "A new bitstream file "+bitstreamFileName+" with link "+((String)bitstreamInfo.get("retrieveLink"))+" has been added to the item "+newItemHandle+". \n\n";
                                        }
                                        else{
                                            if(null == importResults.get("bitstream-unimported")){
                                                importResults.put("bitstream-unimported", new ArrayList<String>());
                                            }
                                            List bitstreamUnimportedList = (ArrayList)importResults.get("bitstream-unimported");
                                            bitstreamUnimportedList.add(newItemId + "---" + bitstreamFile.getAbsoluteFile());
                                            logger.debug("Failed to add the bitstream file "+bitstreamFileName+" into item "+newItemHandle+".\n");
                                            output += "Failed to add the bitstream file "+bitstreamFileName+" into item "+newItemHandle+".\n\n";
                                        }
                                    }
                                    catch(Exception ex){
                                        if(null == importResults.get("bitstream-unimported")){
                                            importResults.put("bitstream-unimported", new ArrayList<String>());
                                        }
                                        List bitstreamUnimportedList = (ArrayList)importResults.get("bitstream-unimported");
                                        bitstreamUnimportedList.add(newItemId + "---" + bitstreamFile.getAbsoluteFile());
                                        logger.debug( "Failed to add the bitstream file "+bitstreamFileName+" into item "+newItemHandle+".\n"+ex.getMessage());
                                        output += "Failed to add the bitstream file "+bitstreamFileName+" into item "+newItemHandle+".\n"+ex.getMessage()+"\n\n";
                                    }
                                }
                            }                            
                        }
                        else{
                            logger.debug( "This saf package is missing either the contents file or the metadata files.\n");
                            output += "This saf package is missing either the contents file or the metadata files.\n\n";
                            throw new SafPackageMissingFileException("Saf package at " + safPath + " either the contents file or the metadata files are missing!");
                        }
                    }
                }
            }
            else {
                throw new SafPackagePathErrorException("Saf package path is not a directory");
            }
            
            // Due to various reasons, some metadata and/or bitstreams cannot be added and are given second chance here:
            List metadataUnimportedList = (ArrayList)importResults.get("metadata-unimported");
            if(null != metadataUnimportedList && metadataUnimportedList.size() > 0){
                for (Iterator<String> iterator = metadataUnimportedList.iterator(); iterator.hasNext();) {
                    String[] values = ((String) iterator.next()).split("---");
                    logger.debug("Second try to add metadata into "+values[0]+" with data "+values[1]);
                    output += "Second try to add metadata into "+values[0]+" with data "+values[1]+"\n\n";
                    try{
                        String metadataInfo = addItemMetadata(values[0], values[1]);
                        if(null != metadataInfo){
                            iterator.remove();
                            logger.debug("Second try: sucessfully added metadata into item "+values[0]+" with data "+values[1]);
                            output += "Second try: sucessfully added metadata into item "+values[0]+" with data "+values[1]+"\n\n";
                        }
                        else{
                            logger.debug("Second try: failed to add metadata into item "+values[0]+" with data "+values[1]);
                            output += "Second try: failed to add metadata into item "+values[0]+" with data "+values[1]+"\n\n";
                        }
                    }
                    catch(Exception ex){
                        logger.debug("Second try: failed to add metadata into item "+values[0]+" with data "+values[1]+"\n"+ex.getMessage());
                        output += "Second try: failed to add metadata into item "+values[0]+" with data "+values[1]+"\n"+ex.getMessage()+"\n\n";
                    }
                }
            }
            
            List bitstreamUnimportedList = (ArrayList)importResults.get("bitstream-unimported");
            if(null != bitstreamUnimportedList && bitstreamUnimportedList.size() > 0){
//                for (Object bitstreamUnimportedList1 : bitstreamUnimportedList) {
                for (Iterator<String> iterator = metadataUnimportedList.iterator(); iterator.hasNext();) {
                    String[] values = ((String) iterator.next()).split("---");
                    String name = new File(values[1]).getName().replace(" ", "_");
                    logger.debug("Second try to add bitstream into "+values[0]+" with path "+values[1]);
                    output += "Second try to add bitstream into "+values[0]+" with path "+values[1]+"\n\n";
                    try{
                        Map bitstreamInfo = addItemBitstream(values[0], values[1], name, name);
                        if(null != bitstreamInfo){
                            iterator.remove();
                            logger.debug("Second try: sucessfully added bitstream into item "+values[0]+" with path "+values[1]);
                            output += "Second try: sucessfully added bitstream into item "+values[0]+" with path "+values[1]+"\n\n";
                        }
                        else{
                            logger.debug("Second try: failed to add bitstream into item "+values[0]+" with path "+values[1]);
                            output += "Second try: failed to add bitstream into item "+values[0]+" with path "+values[1]+"\n\n";
                        }
                    }
                    catch(Exception ex){
                        logger.debug("Second try: failed to add bitstream into item "+values[0]+" with path "+values[1]+"\n"+ex.getMessage());
                        output += "Second try: failed to add bitstream into item "+values[0]+" with path "+values[1]+"\n"+ex.getMessage()+"\n\n";
                    }
                }
            }
            
        }
        catch(SafPackagePathErrorException | SafPackageMissingFileException ex){
            output += "Saf package is not valid!\n"+ex.getMessage()+"\n\n";
            logger.error("Cannot create new items with saf package path: " + safPath, ex);
        } catch (ErrorUnzipSafPackageException ex) {
            Logger.getLogger(DspaceApiHandlerImpl.class.getName()).log(Level.SEVERE, null, ex);
        } 
        finally{
            DocumentProcessorUtil.outputStringToFile(mapping, DocumentProcessorUtil.getFileContainerPath(reportFilePath)+File.separator+"mapfile");
            DocumentProcessorUtil.outputStringToFile(output, reportFilePath);
        }
        
        return importResults;
    }
    
    @Override
    public Map<String, List<String>> loadItemsFromSafPackage(String safPath, String collectionHandle, String dspaceApiUrl){
        Map<String, List<String>> importResults = new HashMap<>();
        this.dspaceApiUrl = dspaceApiUrl;
        BufferedWriter loggingForUserWriter = null;
        BufferedWriter mapWriter = null;
        String urlBase = dspaceApiUrl.split("rest")[0];
        String message;
        
        if(DocumentProcessorUtil.isEmptyString(token)){
            getTokenFromServer();
            if(DocumentProcessorUtil.isEmptyString(token)){
                try {
                    throw new NoExistingApiTokenException("Cannot get useful token!");
                } catch (NoExistingApiTokenException ex) {
                    logger.error(ex.getMessage());
                    return null;
                }
            }
        }
        
        try{            
            try {
                loggingForUserWriter = DataHandlersUtil.getWriterLoggingForUserFile(DataHandlersUtil.CURRENT_TASK_ID, DataHandlersUtil.CURRENT_TASK_TYPE);
            } catch (IOException ex) {
                logger.error("Cannot get the output file writer", ex);
                return null;
            }
            
            loggingForUserWriter.write("\n\n==================================================\n\nStart to import the items in the SAF package.\n\n\n");
            
            try{
                File mapFile = new File(DocumentProcessorUtil.getFileContainerPath(reportFilePath)+File.separator+"mapfile");
                if(!mapFile.exists()){
                    mapFile.createNewFile();
                }
                mapWriter = new BufferedWriter(new FileWriter(mapFile));
            }
            catch (IOException ex) {
                logger.error("Cannot get the output file writer", ex);
                loggingForUserWriter.write("Cannot generate the map file, task dismissed. Please contact the relative personnel.\n");
                return null;
            }
            File safFile = new File(safPath);
            if(safPath.endsWith(".zip")){
                String newPath = FileZipper.unzipToDirectory(safPath);
                if(null == newPath){
                    loggingForUserWriter.write("The SAF package has problem, please contact the relative personnel.\n");
                    throw new ErrorUnzipSafPackageException("Cannot unzip the saf package at "+safPath);
                }
                // Change the path to be the unzipped folder
//                safPath = DocumentProcessorUtil.getFileNameWithoutExtension(safPath);
                safFile = new File(newPath);
            }
            if(safFile.isDirectory()){
                mainLoop:
                for(File file : safFile.listFiles()){
                    if(null != file && file.isDirectory()){
                        String newItemHandle = "";
                        String itemName = file.getName();
                        File[] fileList = file.listFiles();
                        boolean containsContentsFile = false;                        
                        boolean containsMetadataFile = false;
                        
                        List<File> metadataFileList = new ArrayList<>();
                        File contentFile = null;
                        String doi = "";
                        
                        for(File itemFile : fileList){    
                            
                            if(null == itemFile){
                                continue;
                            }
                            
                            String fileName = itemFile.getName();
                            if("contents".equals(fileName)){
                                containsContentsFile = true;
                                contentFile = itemFile;
                            }
                            else if(METADATA_FILE_NAMES_LIST.contains(fileName.replaceAll(".xml", ""))){
                                // Check if there are some duplicates based on doi
                                if(fileName.contains("dublin")){
                                    doi = findDoiFromDublin(itemFile.getAbsolutePath());
                                    if(null != doi && checkDuplicatesByDoi(doi, collectionHandle, dspaceApiUrl)){
                                        message = "Duplication detected: collection "+collectionHandle+" has already had an item with doi="+doi;
                                        logger.debug(message);
                                        System.out.println(message+".\n\n\n");
                                        loggingForUserWriter.write(message+".\n\n\n");
                                        loggingForUserWriter.write("Item with doi:"+doi+" has been processed.\n\n==================================================\n\n");
                                        continue mainLoop;
                                    }
                                }
                                containsMetadataFile = true;
                                metadataFileList.add(itemFile);
                            }
                        }
                        if(containsContentsFile == true && containsMetadataFile == true){
                            String newItemId;                            
                            // Create the new item now:
                            try{
                                String collectionId = getObjectIdByHandler(collectionHandle, dspaceApiUrl);
                                if(DocumentProcessorUtil.isEmptyString(collectionId)){
                                    throw new Exception("Cannot get collection ID from handler "+collectionHandle+"!");
                                }
                                Map newItemInfo = createEmptyItem(getObjectIdByHandler(collectionHandle, dspaceApiUrl));
                                newItemId = String.valueOf(newItemInfo.get("id"));
                                newItemHandle = (String)newItemInfo.get("handle");
                                message = "A new item with handle = "+newItemHandle+" has been added to collection "+collectionHandle+".\n\n";
                                logger.debug(message);
                                System.out.println(message);
                                loggingForUserWriter.write(message);
                                mapWriter.write(file.getName() + "   " + newItemHandle+"\n");
                            }
                            catch(Exception ex){
                                message = "Cannot create the new item with collection handler "+collectionHandle+".\nItem with doi="+doi+" is not added.";
                                logger.error(message, ex);
                                System.out.println(message+"\n\n\n");
                                loggingForUserWriter.write(message+"\n\n\n");
                                continue;
                            }
                            
                            // Add the metadata to the new item:
                            String[] paths = new String[metadataFileList.size()];
                            ListIterator metadataIt = metadataFileList.listIterator();
                            while(metadataIt.hasNext()){
                                File metadataFile = (File)metadataIt.next();
                                paths[metadataIt.nextIndex()-1] = metadataFile.getAbsolutePath();
                            }
                            Map<String, String> metadataStrings = getMetadataFromXmlFiles(paths);
                            for(String path : metadataStrings.keySet()){
                                String metadata = metadataStrings.get(path);      
                                message = "Adding metadata file " + metadata +" now with file name : "+file.getName();
                                logger.debug(message);
                                System.out.println(message+"\n\n");
                                loggingForUserWriter.write("Start to add metadata for item doi:"+doi+"\n");
                                
                                try{
                                    String metadataInfo = addItemMetadata(newItemId, metadata);
                                    if(null == metadataInfo || metadataInfo.equals("")){
                                        if(null == importResults.get("metadata-imported")){
                                            importResults.put("metadata-imported", new ArrayList<String>());
                                        }
                                        List metadataUnimportedList = (ArrayList)importResults.get("metadata-imported");
                                        metadataUnimportedList.add(newItemId + "---" + path);
                                        message = "Failed to add the metadata into item "+newItemHandle+".\n\n";
                                        logger.debug(message);
                                        System.out.println(message);
                                        loggingForUserWriter.write(message);
                                    }
                                    else{
                                        if(null == importResults.get("metadata-imported")){
                                            importResults.put("metadata-imported", new ArrayList<String>());
                                        }
                                        List metadataImportedList = (ArrayList)importResults.get("metadata-imported");
                                        metadataImportedList.add(newItemId + "---" + path);
                                        message = "A new set of metadata entries have been added to the item "+newItemHandle+". \n\n";
                                        logger.debug(message);
                                        System.out.println(message);
                                        loggingForUserWriter.write(message);
                                    }
                                }
                                catch(Exception ex){
                                    if(null == importResults.get("metadata-imported")){
                                        importResults.put("metadata-imported", new ArrayList<String>());
                                    }
                                    List metadataUnimportedList = (ArrayList)importResults.get("metadata-imported");
                                    metadataUnimportedList.add(newItemId + "---" + path);
                                    message = "Failed to add metadata into item "+newItemHandle+"\n"+ex.getMessage();
                                    logger.debug(message);
                                    System.out.println(message+"\n\n");
                                    loggingForUserWriter.write(message+"\n\n");
                                }
                            }
                            
                            List bitstreamFileList = DocumentProcessorUtil.readTextFileIntoList(contentFile.getAbsolutePath());
                            ListIterator it = bitstreamFileList.listIterator();
                            while(it.hasNext()){
                                String bitstreamFileName = (String)it.next();
                                File bitstreamFile = new File(file.getAbsoluteFile() + File.separator + bitstreamFileName);
                                if(!bitstreamFile.exists()){
                                    message = "The bitstream file "+bitstreamFileName+" does not exist in the saf package "+safPath+"!\n";
                                    logger.debug(message);
                                    System.out.println(message+"!\n\n");
                                    loggingForUserWriter.write(message+"!\n\n");
                                }
                                else{
                                    String newName = bitstreamFile.getName().replace(" ", "_");
                                    try{
                                        Map bitstreamInfo = addItemBitstream(newItemId, bitstreamFile.getAbsolutePath(), newName, newName);
                                        if(null != bitstreamInfo){
                                            if(null == importResults.get("bitstream-imported")){
                                                importResults.put("bitstream-imported", new ArrayList<String>());
                                            }
                                            List bitstreamImportedList = (ArrayList)importResults.get("bitstream-imported");
                                            bitstreamImportedList.add(newItemId + "---" + bitstreamFile.getAbsoluteFile());
                                            message = "A new bitstream file "+bitstreamFileName+" has been added to the item "+newItemHandle+".\n\n";
                                            logger.debug(message);
                                            System.out.println(message);
                                            loggingForUserWriter.write(message);
                                        }
                                        else{
                                            if(null == importResults.get("bitstream-unimported")){
                                                importResults.put("bitstream-unimported", new ArrayList<String>());
                                            }
                                            List bitstreamUnimportedList = (ArrayList)importResults.get("bitstream-unimported");
                                            bitstreamUnimportedList.add(newItemId + "---" + bitstreamFile.getAbsoluteFile());
                                            message = "Cannot add the bitstream!";
                                            throw new Exception(message);
                                        }
                                    }
                                    catch(Exception ex){
                                        if(null == importResults.get("bitstream-unimported")){
                                            importResults.put("bitstream-unimported", new ArrayList<String>());
                                        }
                                        List bitstreamUnimportedList = (ArrayList)importResults.get("bitstream-unimported");
                                        bitstreamUnimportedList.add(newItemId + "---" + bitstreamFile.getAbsoluteFile());
                                        message = "Failed to add the bitstream file "+bitstreamFileName+" into item "+newItemHandle+".\n";
                                        logger.debug(message, ex);
                                        ex.printStackTrace();
                                        loggingForUserWriter.write(message);
                                    }
                                }
                            }                            
                        }
                        else{
                            message = "This saf package is missing either the contents file or the metadata files.\n\n";
                            logger.debug(message);
                            System.out.println(message);
                            loggingForUserWriter.write(message);
                            //throw new SafPackageMissingFileException("Saf package at " + safPath + " either the contents file or the metadata files are missing!");
                        }
                        loggingForUserWriter.write("url__" + itemName+"=" + urlBase + newItemHandle + "\n");                        
                        loggingForUserWriter.write("Item with doi:"+doi+" has been processed.\n\n==================================================\n\n");
                    }
                }
            }
            else {
                message = "This SAF package is not a directory.\n\n\n";
                logger.debug(message);
                System.out.println(message);
                loggingForUserWriter.write(message);
                throw new SafPackagePathErrorException(message);
            }
            
            // Due to various reasons, some metadata and/or bitstreams cannot be added and are given second chance here:
            List metadataUnimportedList = (ArrayList)importResults.get("metadata-unimported");
            if(null != metadataUnimportedList && metadataUnimportedList.size() > 0){
                for (Iterator<String> iterator = metadataUnimportedList.iterator(); iterator.hasNext();) {
                    String[] values = ((String) iterator.next()).split("---");
                    message = "Second try to add metadata into "+values[0]+" with data "+values[1];
                    logger.debug(message);
                    System.out.println(message+"\n\n");
                    loggingForUserWriter.write(message+"\n\n");
                    try{
                        String metadataInfo = addItemMetadata(values[0], values[1]);
                        if(null != metadataInfo){
                            iterator.remove();
                            message = "Second try: sucessfully added metadata into item "+values[0]+" with data "+values[1];
                            logger.debug(message);
                            System.out.println(message+"\n\n");
                            loggingForUserWriter.write(message+"\n\n");
                        }
                        else{
                            throw new Exception();
                        }
                    }
                    catch(Exception ex){
                        message = "Second try: failed to add metadata into item "+values[0]+" with data "+values[1]+"\n"+ex.getMessage();
                        logger.debug(message);
                        System.out.println(message+"\n\n");
                        loggingForUserWriter.write(message+"\n\n");
                    }
                }
            }
            
            List bitstreamUnimportedList = (ArrayList)importResults.get("bitstream-unimported");
            if(null != bitstreamUnimportedList && bitstreamUnimportedList.size() > 0){
//                for (Object bitstreamUnimportedList1 : bitstreamUnimportedList) {
                for (Iterator<String> iterator = bitstreamUnimportedList.iterator(); iterator.hasNext();) {
                    String[] values = ((String) iterator.next()).split("---");
                    String name = new File(values[1]).getName().replace(" ", "_");
                    message = "Second try to add bitstream into "+values[0]+" with path "+values[1];
                    logger.debug(message);
                    System.out.println(message+"\n\n");
                    loggingForUserWriter.write(message+"\n\n");
                    try{
                        Map bitstreamInfo = addItemBitstream(values[0], values[1], name, name);
                        if(null != bitstreamInfo){
                            iterator.remove();
                            message = "Second try: sucessfully added bitstream into item "+values[0]+" with path "+values[1];
                            logger.debug(message);
                            System.out.println(message+"\n\n");
                            loggingForUserWriter.write(message+"\n\n");
                        }
                        else{
                            message = "Second try: failed to add bitstream into item "+values[0]+" with path "+values[1];
                            logger.debug(message);
                            System.out.println(message+"\n\n");
                            loggingForUserWriter.write(message+"\n\n");
                            throw new Exception(message);
                        }
                    }
                    catch(Exception ex){
                        logger.error(ex);
                    }
                }
            }
            
        }
        catch(SafPackagePathErrorException ex){
            message = "Saf package is not valid!\n"+ex.getMessage();
            System.out.println(message+"\n\n");
            logger.error("Cannot create new items with saf package path: " + safPath, ex);
        } 
        catch(NullPointerException | ErrorUnzipSafPackageException | IOException ex){
            logger.error(ex);
        }
        finally{
            if(null != loggingForUserWriter){
                try{
                    loggingForUserWriter.flush();
                }
                catch(IOException ex){
                    logger.error("Cannot close the output file writer!");
                }
            }
            if(null != mapWriter){
                try{
                    mapWriter.flush();
                    mapWriter.close();
                }
                catch(IOException ex){
                    logger.error("Cannot close the map file writer!");
                }
            }
        }
        return importResults;
    }
    
    /**
     * 
     * Deletes all items in a collection
     * 
     * @param id : collection Id
     */
    @Override
    public void deleteItemsByCollectionId(String id){
        String[] ids = getItemIdsByCollectionId(id);
        for(String itemId : ids){
            try{
                deleteItemById(itemId);
            }
            catch(Exception ex){
                logger.error("Cannot delete item : " + itemId, ex);
                continue;
            }
        }
    }

    @Override
    public void setFilePath(String filePath) {
        job.setFilePath(filePath);
    }

    @Override
    public void setReportFilePath(String reportFilePath) {
        this.reportFilePath = reportFilePath;
    }

    @Override
    public int getJobType() {
        return job.getType();
    }

    @Override
    public Map<String, String> outputJobDataByJobType() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getServerName() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getRepoType() {
        return DataUtil.REPO_TYPES[job.getRepoType()];
    }

    @Override
    public boolean checkDuplicatesByDoi(String doi, String collectionHandle, String dspaceApiUrl){
        Map<String, String> dois = getItemDoisByCollectionHandler(collectionHandle, dspaceApiUrl);
        return dois.containsKey(doi);
    }
    
    /**
     * Dig out the doi from dublin.xml file
     * @param dublin : file path of dublin.xml. 
     *  an example: <dcvalue element="identifier" language="en_US" qualifier="doi">10.1177/0194599816689660</dcvalue> 
     * @return : doi value
     */
    private String findDoiFromDublin(String dublinPath){
        Map<String, String>attrMap = new HashMap<>();
        attrMap.put("qualifier", "doi");
        String doi[] = DocumentProcessorUtil.getDataFromXmlByTagNameAndAttributes(dublinPath, "dcvalue", attrMap);
        if(null != doi && doi.length > 0){
            return doi[0];
        }
        return null;
    }

    @Override
    public String getItemsInfoByCollectionHandler(String handle) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}

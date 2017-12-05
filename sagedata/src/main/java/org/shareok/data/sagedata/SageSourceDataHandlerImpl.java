/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shareok.data.sagedata;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.io.FileUtils;
import org.apache.poi.util.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import org.shareok.data.documentProcessor.FileHandler;
import org.shareok.data.documentProcessor.FileHandlerFactory;
import org.shareok.data.documentProcessor.DocumentProcessorUtil;
import org.shareok.data.sagedata.exceptions.EmptyFilePathException;
import org.shareok.data.config.ShareokdataManager;
import org.shareok.data.datahandlers.DataHandlersUtil;
import org.shareok.data.datahandlers.exceptions.NoHtmlComponentsFoundException;
import org.shareok.data.dspacemanager.DspaceDataUtil;
import org.shareok.data.htmlrequest.HttpRequestHandler;
import org.shareok.data.datahandlers.exceptions.NoFullTextAccessException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;

/**
 *
 * @author Tao Zhao
 */
public class SageSourceDataHandlerImpl implements SageSourceDataHandler {
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(SageSourceDataHandlerImpl.class);

    private String sourceFilePath;
    private String outputFilePath;
    private HashMap data;
    private SageJournalDataProcessorFactory factory;
    private ArrayList<HashMap> itemData;
    private HttpRequestHandler httpRequestHandler;

    /**
     *
     * @return
     */
    @Override
    public HashMap getData() {
        return data;
    }

    /**
     *
     * @param data
     */
    public void setData(HashMap data) {
        this.data = data;
    }

    /**
     *
     * @return
     */
    public ArrayList<HashMap> getItemData() {
        return itemData;
    }

    /**
     *
     * @param itemData
     */
    public void setItemData(ArrayList<HashMap> itemData) {
        this.itemData = itemData;
    }

    public String getSourceFilePath() {
        return sourceFilePath;
    }

    public String getOutputFilePath() {
        return outputFilePath;
    }

    public SageJournalDataProcessorFactory getFactory() {
        return factory;
    }

    public void setSourceFilePath(String sourceFilePath) {
        this.sourceFilePath = sourceFilePath;
    }

    public void setOutputFilePath(String outputFilePath) {
        this.outputFilePath = outputFilePath;
    }

    public void setFactory(SageJournalDataProcessorFactory factory) {
        this.factory = factory;
    }
    
    @Autowired
    public void setHttpRequestHandler(HttpRequestHandler httpRequestHandler){
        this.httpRequestHandler = httpRequestHandler;
    }

    /**
     *
     * @param filePath
     */
    @Override
    public void readSourceData() {

        String filePath = sourceFilePath;
        try {
            String fileExtension = DocumentProcessorUtil.getFileExtension(filePath);
            FileHandler fh = FileHandlerFactory.getFileHandlerByFileExtension(fileExtension);
            if (null == fh) {
                return;
            }
            fh.setFileName(filePath);
            fh.readData();
            data = fh.getData();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Organize the raw data in order to retrieve the necessary information to
     * request the metadata Note: this method is closely depending on the excel
     * file format
     */
    @Override
    public void processSourceData() {

        if (null == data || data.isEmpty()) {
            readSourceData();
            if (null == data || data.isEmpty()) {
                return;
            }
        }

        try {
            Set keys = data.keySet();
            Iterator it = keys.iterator();
            int rowPre = 0;

            HashMap articleData = new HashMap();

            while (it.hasNext()) {
                String key = (String) it.next();
                String value = (String) data.get(key);
                // the values is composed of "val--datatype": for example, Tom--Str or 0.50--num
                String[] values = value.split("--");
                if (null == values || values.length != 2) {
                    continue;
                }

                value = values[0];
                String[] rowCol = key.split("-");
                if (null == rowCol || rowCol.length != 2) {
                    throw new Exception("The row and column are not specifid!");
                }
                int row = Integer.parseInt(rowCol[0]);
                int col = Integer.parseInt(rowCol[1]);

                if (row != rowPre) {
                    rowPre = row;
                    if (null != articleData && !articleData.isEmpty()) {
                        if (null == itemData) {
                            itemData = new ArrayList<HashMap>();
                        }
                        Object articleDataCopy = articleData.clone();
                        itemData.add((HashMap) articleDataCopy);
                        articleData.clear();
                    }
                }

                if (0 != row) {
                    switch (col) {
                        case 0:
                            articleData.put("journal", value);
                            break;
                        case 2:
                            articleData.put("title", value);
                            break;
                        case 3:
                            articleData.put("volume", value);
                            break;
                        case 4:
                            articleData.put("issue", value);
                            break;
                        case 5:
                            articleData.put("pages", value);
                            break;
                        case 6:
                            articleData.put("year", value);
                            break;
                        case 7:
                            articleData.put("citation", value);
                            break;
                        case 8:
                            articleData.put("pubdate", value);
                            break;
                        case 9:
                            articleData.put("doi", value);
                            break;
                        case 10:
                            articleData.put("url", value);
                            break;
                        default:
                            break;
                    }
                }

            }
            
            // Put the last article into itemData:
            if (null != articleData && !articleData.isEmpty()) {
                if (null == itemData) {
                    itemData = new ArrayList<HashMap>();
                }
                Object articleDataCopy = articleData.clone();
                itemData.add((HashMap) articleDataCopy);
                articleData.clear();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void outputMetaData() {

        String filePath = outputFilePath;

        try {
            if (null == filePath || "".equals(filePath)) {
                throw new EmptyFilePathException("File path is NOT set!");
            }
            if (null == itemData || itemData.isEmpty()) {
                processSourceData();
                if (null == itemData || itemData.isEmpty()) {
                    return;
                }
            }
            File outputFolder = new File(filePath);
            if (!outputFolder.exists()) {
                if (outputFolder.mkdir()) {
                    System.out.print("The folder for data loading has been created.\n");
                }
            }

            int size = itemData.size();
            for (int i = 0; i < size; i++) {
                Map journalData = itemData.get(i);
                String journal = (String) journalData.get("journal");
                Map journalMap = SageDataUtil.getJournalListWithBeans();
                SageJournalDataProcessor sjdp = SageJournalDataProcessorFactory.getSageJournalDataProcessorByName(journalMap, journal);

                if (null == sjdp) {
                    System.out.print("The No. " + i + " article from journal \" " + journal + " \" has no metadata ...\n");
                    continue;
                } else {
                    sjdp.setData(journalData);
                    sjdp.getOutput(filePath);
                    System.out.print("The No. " + i + " article metadata has been prepared...\n");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public String getDspaceLoadingData(String filePath){
        MultipartFile multipartFile = null;
        try{
            File file = new File(filePath);
            FileInputStream input = new FileInputStream(file);
            multipartFile = new MockMultipartFile("file", file.getName(), "text/plain", IOUtils.toByteArray(input));
        }
        catch(IOException ioex){
            Logger.getLogger(SageSourceDataHandlerImpl.class.getName()).log(Level.SEVERE, null, ioex);
        }
        return getDspaceLoadingData(multipartFile);
    }
    
    /**
     * 
     * @param file : the uploaded file
     * @return : the path to the saved uploaded file
     */
    @Override
    public String saveUploadedData(MultipartFile file){
        String uploadedFilePath = null;
        try{
            String oldFileName = file.getOriginalFilename();
            String extension = DocumentProcessorUtil.getFileExtension(oldFileName);
            oldFileName = DocumentProcessorUtil.getFileNameWithoutExtension(oldFileName);
            //In the future the new file name will also has the user name
            String time = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
            String newFileName = oldFileName + "--" + time + "." + extension;
            String uploadPath = ShareokdataManager.getSageUploadPath();
            if(null != uploadPath){
                File uploadFolder = new File(uploadPath);
                if(!uploadFolder.exists()){
                    uploadFolder.mkdir();
                }
                File uploadTimeFolder = new File(uploadPath + File.separator + time);
                if(!uploadTimeFolder.exists()){
                    uploadTimeFolder.mkdir();
                }
            }
            uploadedFilePath = uploadPath + File.separator + time + File.separator + newFileName;
            File uploadedFile = new File(uploadedFilePath);
            file.transferTo(uploadedFile);
        }
        catch(Exception ex){
            Logger.getLogger(SageSourceDataHandlerImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        return uploadedFilePath;
    }
    
    /**
     * 
     * @param file : uploaded file
     * @return filePath : the path to the folder where the uploading data are saved
     */
    @Override
    public String getDspaceLoadingData(MultipartFile file){
        String filePath = null;
        try {
            filePath = DspaceDataUtil.saveUploadedData(file, "sage");
            if(null != filePath){
                setSourceFilePath(filePath);
                setOutputFilePath(DocumentProcessorUtil.getFileContainerPath(filePath) + "output");
                readSourceData();
                processSourceData();
                outputMetaData();
                DspaceDataUtil.packLoadingData(getOutputFilePath(), "sage");
            }            
        } catch (Exception ex) {
            Logger.getLogger(SageSourceDataHandlerImpl.class.getName()).log(Level.SEVERE, null, ex);
        }        
        return filePath;
    }

    @Override
    public String getDspaceJournalLoadingFilesByDoi(String[] dois, Date time) {
        String uploadPath = null;
        uploadPath = DspaceDataUtil.getDspaceUploadPath("sage", time);
        String outputPath = uploadPath+File.separator+"output_sage";
        File output = new File(outputPath);
        if(!output.exists()){
            output.mkdirs();
        }
        setOutputFilePath(outputPath);
        List<SageJournalData> journalDataList = new ArrayList<>();
        for(String doi : dois){
            SageJournalData data = getDspaceJournalLoadingFilesBySingleDoi(doi);
            if(null != data){
                journalDataList.add(data);
            }
        }
        if(journalDataList.size() > 0){
            for(SageJournalData journalData : journalDataList){
                String doi = journalData.getDoi();
                exportXmlByJournalData(journalData, outputPath);
                String pdfFileName = downloadPdfFiles(doi, outputPath);
                generateDspaceContentFile(pdfFileName, doi, outputPath);
            }
        }
                
        DspaceDataUtil.packLoadingData(outputPath, "sage");
        try {
            FileUtils.deleteDirectory(new File(outputPath));
        } catch (IOException ex) {
            logger.error("Cannot delete the saf folder after being zipped", ex);
        }
        return uploadPath+File.separator+"output_sage.zip";
    }

    @Override
    public SageJournalData getDspaceJournalLoadingFilesBySingleDoi(String doi) {
        SageJournalData journalData = null;
        String fullTextUrl = SageDataUtil.getArticleUrlByDoi(doi);
        try {            
            Document doc = Jsoup.connect(fullTextUrl)
                    .data("query", "Java")
                    .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/33.0.1750.152 Safari/537.36")
                    .cookie("auth", "token")
                    .timeout(300000)
                    .get();
            // Check full text access:
            boolean fullTextAccess = SageDataUtil.hasFullAccess(doc);
            if(fullTextAccess == false){
                throw new NoFullTextAccessException("Do not have full text access to sage article doi="+doi);
            }
            
            ApplicationContext context = new ClassPathXmlApplicationContext("sageDataContext.xml");
            journalData = (SageJournalData)context.getBean("sageJournalData");
            
            journalData.setDoi(doi);
            journalData.setRelationUri(fullTextUrl);
            
            String publisher = getPublisherFromFullTextDoc(doc);
            if(null != publisher){
                journalData.setPublisher(publisher);
            }
            
            /**
             * Research Article; Book Review; Case Report; Review Article; Other; Editorial; Brief Report
             */
            String type = getArticleTypeFromFullTextDoc(doc);
            if(null != type){
                journalData.setType(type);
                if(type.equals("Research Article") || type.equals("Review Article") || type.equals("Case Report")){
                    String ab = getArticleAbstractFromFullTextDoc(doc);
                    if(null != ab){
                        journalData.setAbstractText(ab);
                    }
                }
            }
            
            String title = getTitleFromFullTextDoc(doc);
            if(null != title){
                journalData.setTitle(title);
            }
            
            String issueDate = getArticleIssueDateFromFullTextDoc(doc);
            if(null != issueDate){
                SimpleDateFormat output = new SimpleDateFormat("yyyy-MM-dd");
                Date date;
                try {
                    date = output.parse(issueDate);
                } catch (ParseException ex) {
                    logger.error("Cannot conver the date string = "+ issueDate + " to be date object!", ex);
                    date = null;
                }
                journalData.setDateIssued(date);
            }
            
            String[] keys = getArticleKeyWordsFromFullTextDoc(doc);
            if(null != keys){
                journalData.setSubjects(keys);
            }
            
            String[] authors = getArticleAuthorsFromFullTextDoc(doc);
            if(null != authors){
                journalData.setAuthors(authors);
            }
                        
        } catch (IOException ex) {
            logger.error("Cannot get response from "+fullTextUrl, ex);
        } catch (NoFullTextAccessException ex){
            logger.error("Cannot access the full text and PDF file", ex);
        } catch (NoHtmlComponentsFoundException ex) {
            logger.error(ex);
        }        
        return journalData;
    }
    
    private String getPublisherFromFullTextDoc(Document doc) throws NoHtmlComponentsFoundException{
        String publisher = null;
        
        Elements headerTitleContainerElements = doc.select("div#headerTitleContainer");
        if(null == headerTitleContainerElements || headerTitleContainerElements.isEmpty()){
            throw new NoHtmlComponentsFoundException("Cannot find headerTitleContainer");
        }
        Element headerTitleContainer = headerTitleContainerElements.get(0);
        String pub = headerTitleContainer.text();
        if(null != pub){
            publisher = pub;
        }
        
        return publisher;
    }
    
    private String getArticleTypeFromFullTextDoc(Document doc) throws NoHtmlComponentsFoundException{
        String type = null;
        
        Elements typeElements = doc.select("span.ArticleType");
        if(null == typeElements || typeElements.isEmpty()){
            throw new NoHtmlComponentsFoundException("Cannot find the article type!");
        }
        String typeSpan = typeElements.get(0).select("span").get(0).text();
        if(null != typeSpan && typeSpan.contains("-")){
            typeSpan = typeSpan.replace("-", " ");
            String[] typeSpanInfo = typeSpan.split(" ");
            typeSpan = "";
            for(String str : typeSpanInfo){
                typeSpan += str.substring(0,1).toUpperCase() + str.substring(1) + " ";
            }
            type = typeSpan.split(":")[1].trim();
        }
        
        return type;
    }
    
    private String getTitleFromFullTextDoc(Document doc) throws NoHtmlComponentsFoundException{
        String title = null;
        
        Elements titleElements = doc.select("div.publicationContentTitle");
        if(null == titleElements || titleElements.isEmpty()){
            throw new NoHtmlComponentsFoundException("Cannot find the article title!");
        }
        String titleStr = titleElements.get(0).text();
        if(null != titleStr){
            title = titleStr;
        }
        
        return title;
    }
    
    private String getArticleAbstractFromFullTextDoc(Document doc) throws NoHtmlComponentsFoundException{
        String abs = null;
        
        Elements absElements = doc.select("div.abstractSection");
        if(null == absElements || absElements.isEmpty()){
            return null; //throw new NoHtmlComponentsFoundException("Cannot find the article type!");
        }
        String absStr = absElements.get(0).text();
        if(null != absStr){            
            abs = absStr;
        }
        
        return abs;
    }
    
    private String getArticleIssueDateFromFullTextDoc(Document doc) throws NoHtmlComponentsFoundException{
        String date = null;
        
        Elements dateElements = doc.select("span.publicationContentEpubDate");
        if(null == dateElements || dateElements.isEmpty()){
            return null; //throw new NoHtmlComponentsFoundException("Cannot find the article type!");
        }
        String dateStr = dateElements.get(0).text().split("Published ")[1].trim();
        if(null != dateStr){            
            try {
                dateStr = DataHandlersUtil.convertFullMonthDateStringFormat(dateStr);
            } catch (ParseException ex) {
                logger.error("Cannot parse the date = "+dateStr);
                return null;
            }
            date = dateStr;
        }
        
        return date;
    }
    
    private String[] getArticleKeyWordsFromFullTextDoc(Document doc) throws NoHtmlComponentsFoundException{
        String[] keys = null;

        Elements keyElements = doc.select("div.hlFld-KeywordText");
        if(null == keyElements || keyElements.isEmpty()){
            return null; 
        }
        Elements keyLinkElements = keyElements.get(0).select("a");
        if(null == keyLinkElements || keyLinkElements.isEmpty()){
            return null; 
        }
        
        List<String> keyList = new ArrayList<>();
        for(Element link : keyLinkElements){
            keyList.add(link.text());
        }

        if(keyList.size() > 0){            
            keys = keyList.toArray(new String[keyList.size()]);
        }
        
        return keys;
    }
    
    private String[] getArticleAuthorsFromFullTextDoc(Document doc) throws NoHtmlComponentsFoundException{
        String[] authors = null;
        List<String> auList = new ArrayList<>();
        
        try{
            Elements authorElements = doc.select("div.authors").get(0).select("span.contribDegrees");
            for(Element authSpan : authorElements){
                String author = authSpan.children().get(0).text();
                if(null != author && !author.equals("")){
                    auList.add(author);
                }
            }
        }
        catch(Exception ex){
            logger.error("Cannot get the authors for SAGE article!", ex);
            return null;
        }

        if(auList.size() > 0){            
            authors = auList.toArray(new String[auList.size()]);
        }
        
        return authors;
    }
    
    /** 
     * Convert the article data to dublin core xml metadata and save the the file
     * 
     * @param journalData : the SageJournalData
     * @param fileName : the root folder contains all the uploading article data
     */
    public void exportXmlByJournalData(SageJournalData journalData, String outputPath) {
                    
        try{
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            org.w3c.dom.Document doc = docBuilder.newDocument();
            org.w3c.dom.Element rootElement = doc.createElement("dublin_core");
            doc.appendChild(rootElement);

            // Add the type node:
            org.w3c.dom.Element element = doc.createElement("dcvalue");
            element.appendChild(doc.createTextNode(journalData.getType()));
            rootElement.appendChild(element);
            
            Attr attr = doc.createAttribute("element");
            attr.setValue("type");
            element.setAttributeNode(attr);
            
            attr = doc.createAttribute("language");
            attr.setValue("en_US");
            element.setAttributeNode(attr);
            
            attr = doc.createAttribute("qualifier");
            attr.setValue("none");
            element.setAttributeNode(attr);
            
            // Add the abstract node:
            String abs = journalData.getAbstractText();
            if(null != abs){
                org.w3c.dom.Element elementAbs = doc.createElement("dcvalue");
                elementAbs.appendChild(doc.createTextNode(abs));
                rootElement.appendChild(elementAbs);
                
                attr = doc.createAttribute("element");
                attr.setValue("description");
                elementAbs.setAttributeNode(attr);

                attr = doc.createAttribute("language");
                attr.setValue("en_US");
                elementAbs.setAttributeNode(attr);

                attr = doc.createAttribute("qualifier");
                attr.setValue("abstract");
                elementAbs.setAttributeNode(attr);
            }
            
            // Add the language node:
            String lang = journalData.getLanguage();
            if(null != lang){
                org.w3c.dom.Element elementLang = doc.createElement("dcvalue");
                elementLang.appendChild(doc.createTextNode(lang));
                rootElement.appendChild(elementLang);

                attr = doc.createAttribute("element");
                attr.setValue("language");
                elementLang.setAttributeNode(attr);

                attr = doc.createAttribute("language");
                attr.setValue("en_US");
                elementLang.setAttributeNode(attr);

                attr = doc.createAttribute("qualifier");
                attr.setValue("iso");
                elementLang.setAttributeNode(attr);
            }
            
            // Add the title node:
            String tit = journalData.getTitle();
            if(null != tit){
                org.w3c.dom.Element elementTitle = doc.createElement("dcvalue");
                elementTitle.appendChild(doc.createTextNode(tit));
                rootElement.appendChild(elementTitle);

                attr = doc.createAttribute("element");
                attr.setValue("title");
                elementTitle.setAttributeNode(attr);

                attr = doc.createAttribute("language");
                attr.setValue("en_US");
                elementTitle.setAttributeNode(attr);

                attr = doc.createAttribute("qualifier");
                attr.setValue("none");
                elementTitle.setAttributeNode(attr);
            }
            
            // Add the available date node:
//            Element elementAvailable = doc.createElement("dcvalue");
//            elementAvailable.appendChild(doc.createTextNode(getDateAvailable().toString()));
//            rootElement.appendChild(elementAvailable);
//            
//            attr = doc.createAttribute("element");
//            attr.setValue("date");
//            elementAvailable.setAttributeNode(attr);
//            
//            attr = doc.createAttribute("qualifier");
//            attr.setValue("available");
//            elementAvailable.setAttributeNode(attr);
            
            // Add the issued date node:
            Date issueDate = journalData.getDateIssued();
            if(null != issueDate){
                SimpleDateFormat format_issuedDate = new SimpleDateFormat ("yyyy-MM-dd"); 
                org.w3c.dom.Element elementIssued = doc.createElement("dcvalue");
                elementIssued.appendChild(doc.createTextNode(format_issuedDate.format(issueDate)));
                rootElement.appendChild(elementIssued);

                attr = doc.createAttribute("element");
                attr.setValue("date");
                elementIssued.setAttributeNode(attr);

                attr = doc.createAttribute("qualifier");
                attr.setValue("issued");
                elementIssued.setAttributeNode(attr);
            }
            
            // Add the author nodes:
            String[] authorSet = journalData.getAuthors();
            if(null != authorSet && authorSet.length > 0){
                for(String author : authorSet){
                    org.w3c.dom.Element elementAuthor = doc.createElement("dcvalue");
                    elementAuthor.appendChild(doc.createTextNode(author));
                    rootElement.appendChild(elementAuthor);

                    attr = doc.createAttribute("element");
                    attr.setValue("contributor");
                    elementAuthor.setAttributeNode(attr);

                    attr = doc.createAttribute("qualifier");
                    attr.setValue("author");
                    elementAuthor.setAttributeNode(attr);
                }
            }
            
            // Add the acknowledgements node:
            String ack = journalData.getAcknowledgements();
            if(null != ack){
                org.w3c.dom.Element elementAck = doc.createElement("dcvalue");
                elementAck.appendChild(doc.createTextNode(ack));
                rootElement.appendChild(elementAck);

                attr = doc.createAttribute("element");
                attr.setValue("description");
                elementAck.setAttributeNode(attr);

                attr = doc.createAttribute("language");
                attr.setValue("en_US");
                elementAck.setAttributeNode(attr);

                attr = doc.createAttribute("qualifier");
                attr.setValue("none");
                elementAck.setAttributeNode(attr);
            }
            
            // Add the author contributions node:
            String contrib = journalData.getAuthorContributions();
            if(null != contrib){
                org.w3c.dom.Element elementContribution = doc.createElement("dcvalue");
                elementContribution.appendChild(doc.createTextNode(contrib));
                rootElement.appendChild(elementContribution);

                attr = doc.createAttribute("element");
                attr.setValue("description");
                elementContribution.setAttributeNode(attr);

                attr = doc.createAttribute("language");
                attr.setValue("en_US");
                elementContribution.setAttributeNode(attr);

                attr = doc.createAttribute("qualifier");
                attr.setValue("none");
                elementContribution.setAttributeNode(attr);
            }
            
            // Add the publisher node:
            String puber = journalData.getPublisher();
            if(null != puber){
                org.w3c.dom.Element elementPublisher = doc.createElement("dcvalue");
                elementPublisher.appendChild(doc.createTextNode(puber));
                rootElement.appendChild(elementPublisher);

                attr = doc.createAttribute("element");
                attr.setValue("publisher");
                elementPublisher.setAttributeNode(attr);

                attr = doc.createAttribute("qualifier");
                attr.setValue("none");
                elementPublisher.setAttributeNode(attr);
            }
            
            // Add the citation node:
            String cit = journalData.getCitation();
            if(null != cit){
                org.w3c.dom.Element elementCitation = doc.createElement("dcvalue");
                elementCitation.appendChild(doc.createTextNode(cit));
                rootElement.appendChild(elementCitation);

                attr = doc.createAttribute("element");
                attr.setValue("identifier");
                elementCitation.setAttributeNode(attr);

                attr = doc.createAttribute("language");
                attr.setValue("en_US");
                elementCitation.setAttributeNode(attr);

                attr = doc.createAttribute("qualifier");
                attr.setValue("citation");
                elementCitation.setAttributeNode(attr);
            }
            
            // Add the rights node:
            String rit = journalData.getRights();
            if(null != rit){
                org.w3c.dom.Element elementRights = doc.createElement("dcvalue");
                elementRights.appendChild(doc.createTextNode(rit));
                rootElement.appendChild(elementRights);

                attr = doc.createAttribute("element");
                attr.setValue("rights");
                elementRights.setAttributeNode(attr);

                attr = doc.createAttribute("qualifier");
                attr.setValue("none");
                elementRights.setAttributeNode(attr);
            }
            
            // Add the rights URI node:
            String ritUri = journalData.getRightsUri();
            if(null != ritUri){
                org.w3c.dom.Element elementRightsUri = doc.createElement("dcvalue");
                elementRightsUri.appendChild(doc.createTextNode(ritUri));
                rootElement.appendChild(elementRightsUri);

                attr = doc.createAttribute("element");
                attr.setValue("rights");
                elementRightsUri.setAttributeNode(attr);

                attr = doc.createAttribute("qualifier");
                attr.setValue("uri");
                elementRightsUri.setAttributeNode(attr);
            }
            
            // Add the rights requestable node:
            org.w3c.dom.Element elementRightsRequestable = doc.createElement("dcvalue");
            elementRightsRequestable.appendChild(doc.createTextNode(Boolean.toString(journalData.isRightsRequestable())));
            rootElement.appendChild(elementRightsRequestable);
            
            attr = doc.createAttribute("element");
            attr.setValue("rights");
            elementRightsRequestable.setAttributeNode(attr);
            
            attr = doc.createAttribute("language");
            attr.setValue("en_US");
            elementRightsRequestable.setAttributeNode(attr);
            
            attr = doc.createAttribute("qualifier");
            attr.setValue("requestable");
            elementRightsRequestable.setAttributeNode(attr);
            
            // Add the is part of node:
            String partOf = journalData.getIsPartOfSeries();
            if(null != partOf){
                org.w3c.dom.Element elementIsPartOf = doc.createElement("dcvalue");
                elementIsPartOf.appendChild(doc.createTextNode(partOf));
                rootElement.appendChild(elementIsPartOf);

                attr = doc.createAttribute("element");
                attr.setValue("relation");
                elementIsPartOf.setAttributeNode(attr);

                attr = doc.createAttribute("qualifier");
                attr.setValue("ispartofseries");
                elementIsPartOf.setAttributeNode(attr);
            }
            
            // Add the relation uri node:
            String reUri = journalData.getRelationUri();
            if(null != reUri){
                org.w3c.dom.Element elementRelationUri = doc.createElement("dcvalue");
                elementRelationUri.appendChild(doc.createTextNode(reUri));
                rootElement.appendChild(elementRelationUri);

                attr = doc.createAttribute("element");
                attr.setValue("relation");
                elementRelationUri.setAttributeNode(attr);

                attr = doc.createAttribute("qualifier");
                attr.setValue("uri");
                elementRelationUri.setAttributeNode(attr);
            }
            
            // Add the subject nodes:
            String[] subjectSet = journalData.getSubjects();
            if(null != subjectSet && subjectSet.length > 0){
                for(String subject : subjectSet){
                    org.w3c.dom.Element elementSubject = doc.createElement("dcvalue");
                    elementSubject.appendChild(doc.createTextNode(subject));
                    rootElement.appendChild(elementSubject);

                    attr = doc.createAttribute("element");
                    attr.setValue("subject");
                    elementSubject.setAttributeNode(attr);

                    attr = doc.createAttribute("language");
                    attr.setValue("en_US");
                    elementSubject.setAttributeNode(attr);

                    attr = doc.createAttribute("qualifier");
                    attr.setValue("none");
                    elementSubject.setAttributeNode(attr);
                }
            }
            
            // Add the peerReview node:
            String review = journalData.getPeerReview();
            if(null != review){
                org.w3c.dom.Element elementPeerReview = doc.createElement("dcvalue");
                elementPeerReview.appendChild(doc.createTextNode(review));
                rootElement.appendChild(elementPeerReview);

                attr = doc.createAttribute("element");
                attr.setValue("description");
                elementPeerReview.setAttributeNode(attr);

                attr = doc.createAttribute("language");
                attr.setValue("en_US");
                elementPeerReview.setAttributeNode(attr);

                attr = doc.createAttribute("qualifier");
                attr.setValue("peerreview");
                elementPeerReview.setAttributeNode(attr);
            }
            
            // Add the peer review notes node:
            String peer = journalData.getPeerReviewNotes();
            if(null != peer){
                org.w3c.dom.Element elementPeerReviewNotes = doc.createElement("dcvalue");
                elementPeerReviewNotes.appendChild(doc.createTextNode(peer));
                rootElement.appendChild(elementPeerReviewNotes);

                attr = doc.createAttribute("element");
                attr.setValue("description");
                elementPeerReviewNotes.setAttributeNode(attr);

                attr = doc.createAttribute("language");
                attr.setValue("en_US");
                elementPeerReviewNotes.setAttributeNode(attr);

                attr = doc.createAttribute("qualifier");
                attr.setValue("peerreviewnotes");
                elementPeerReviewNotes.setAttributeNode(attr);
            }
            
            // Add the doi node:
            String doi = journalData.getDoi();
            if(null != doi){
                org.w3c.dom.Element elementDoi = doc.createElement("dcvalue");
                elementDoi.appendChild(doc.createTextNode(doi));
                rootElement.appendChild(elementDoi);

                attr = doc.createAttribute("element");
                attr.setValue("identifier");
                elementDoi.setAttributeNode(attr);

                attr = doc.createAttribute("language");
                attr.setValue("en_US");
                elementDoi.setAttributeNode(attr);

                attr = doc.createAttribute("qualifier");
                attr.setValue("doi");
                elementDoi.setAttributeNode(attr);
            }
            
            File outputFolder = new File(outputPath + File.separator + journalData.getDoi().replaceAll("/", "."));
            if(!outputFolder.exists()){
                outputFolder.mkdirs();
            }
            String filePath = outputFolder + File.separator + "dublin_core.xml";
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(filePath));

            transformer.transform(source, result);

        }       
        catch (ParserConfigurationException | DOMException | BeansException pce) {
            pce.printStackTrace();
        } catch (TransformerException ex) {
            Logger.getLogger(SageSourceDataHandlerImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private String downloadPdfFiles(String doi, String outputPath){
        File outputFolder = new File(outputPath + File.separator + doi.replaceAll("/", "."));
        if(!outputFolder.exists()){
            outputFolder.mkdirs();
        }
        String pdfPath = outputPath + File.separator + doi.replaceAll("/", ".") + File.separator + doi.replaceAll("/", ".") + ".pdf";
        httpRequestHandler.getPdfWithJsoupByUrl(SageDataUtil.getPdfLinkFromDoi(doi), pdfPath);
        return doi.replaceAll("/", ".") + ".pdf";
    }
    
    private void generateDspaceContentFile(String pdfFileName, String doi, String outputPath){
        File outputFolder = new File(outputPath + File.separator + doi.replaceAll("/", "."));
        if(!outputFolder.exists()){
            outputFolder.mkdirs();
        }
        String contentFilePath = outputPath + File.separator + doi.replaceAll("/", ".") + File.separator + "contents";
        File file = new File(contentFilePath);
        if(!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException ex) {
                logger.error("Cannot generate the contents file at "+contentFilePath, ex);
                return;
            }
        }
        try (PrintWriter writer = new PrintWriter(file, "UTF-8")) {
            writer.println(pdfFileName);
        } catch (FileNotFoundException | UnsupportedEncodingException ex) {
            logger.error("Cannot write into the contents file at "+contentFilePath, ex);
        }
    }
}

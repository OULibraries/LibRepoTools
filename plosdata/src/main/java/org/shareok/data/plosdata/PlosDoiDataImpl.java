/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.shareok.data.plosdata;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.shareok.data.datahandlers.exceptions.NoMatchingRegularExpressionException;
import org.shareok.data.datahandlers.exceptions.NotFoundPublisherException;
import org.shareok.data.htmlrequest.HtmlParser;
import org.shareok.data.documentProcessor.ExcelHandler;
import org.shareok.data.documentProcessor.DocumentProcessorUtil;
import org.shareok.data.dspacemanager.DspaceDataUtil;
import org.shareok.data.plosdata.PlosUtil.JournalType;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author Tao Zhao
 */
public class PlosDoiDataImpl implements ExcelData, PlosDoiData {
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PlosDoiDataImpl.class);
    
    private HashMap<String,String[]> data;
    private ExcelHandler excelHandler;
    private ArrayList<String> doiData;
    private ArrayList<PlosData> plosDataList;
    private String outputPath;

    public HashMap<String, String[]> getData() {
        return data;
    }

    public ExcelHandler getExcelHandler() {
        return excelHandler;
    }

    /**
     *
     * @param data
     */
    public void setData(HashMap<String, String[]> data) {
        this.data = data;
    }

    public void setExcelHandler(ExcelHandler excelHandler) {
        this.excelHandler = excelHandler;
    }

    public ArrayList<String> getDoiData() {
        return doiData;
    }

    public ArrayList<PlosData> getPlosDataList() {
        return plosDataList;
    }

    public void setPlosDataList(ArrayList<PlosData> plosDataList) {
        this.plosDataList = plosDataList;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }
    
    @Override
    public void addData(int row, int col, Object data){
        String key = Integer.toString(row) + "-" + Integer.toString(col);
        HashMap dataVal = this.getData();
        dataVal.put(key, data);
    }

    public void setDoiData(ArrayList<String> doiData) {
        this.doiData = doiData;
    }
    
    @Override
    public void printData() {
        
    }
    
    @Override
    public void convertHashMapDataToDoi(HashMap mapData) throws Exception {
        Set keys = mapData.keySet();
        Iterator it = keys.iterator();

        ArrayList <String> doiList = new ArrayList<>();
        while(it.hasNext()){
            String key = (String)it.next();
            String value = (String)mapData.get(key);
            // the values is composed of "val--datatype": for example, Tom--Str or 0.50--num
            String[] values = value.split("--");
            if(null == values || values.length != 2)
                continue;
            String type = values[1];
            value = values[0];
            String[] rowCol = key.split("-");
            if(null == rowCol || rowCol.length != 2)
                throw new Exception("The row and column are not specifid!");
            int row = Integer.parseInt(rowCol[0]);
            int col = Integer.parseInt(rowCol[1]);
            if(col == 3 && value.contains("/journal.p")){
                int index = value.toLowerCase().indexOf("plos ");
                if(index == -1)
                    continue;
                String doiVal = value.substring(index);
                Pattern pattern = Pattern.compile("(e)(\\d{1,10})(.)(\\s*)(doi:)");
                Matcher matcher = pattern.matcher(doiVal);
                if (matcher.find())
                {
                    String[] doiInfo = doiVal.split(":");
                    if(doiInfo.length != 3)
                        continue;
                    String isPartOfSeries = doiInfo[0]+":"+matcher.group(1)+matcher.group(2);
                    doiVal = isPartOfSeries + "---" + doiInfo[2];
                    doiList.add(doiVal);
                    //System.out.println("Matcher find the string for "+doiInfo[0]+"!!!  \n");//System.out.println(matcher.group(1) + " - " + matcher.group(2));
                }
                else{
                    System.out.println("Matcher cannot find the string for "+doiVal+"!!!  \n");
                }
            }
        }
        setDoiData(doiList);
    }
    
    @Override
    public void getDspaceLoadingData(String fileName) throws Exception {
        importData(fileName);
        ArrayList<String> doiList = getDoiData();
        if(!doiList.isEmpty()) {
            
            PlosRequest req = (PlosRequest)PlosUtil.getPlosContext().getBean("plosRequest");
            PlosData plosData = (PlosData)PlosUtil.getPlosContext().getBean("plosData");
            
            for(String doi : doiList){
                String[] valArray = doi.split("---");
                String isPartOfSeries = valArray[0];
                String[] doiArr = valArray[1].split(":");
                doi = doiArr[0];
                plosData = getDspaceJournalLoadingFilesBySingleDoi(doi);
                plosDataList.add(plosData);
            }
        }
    }
    
    /**
     * 
     * @param fileName : file path
     */
    @Override
    public void importData(String fileName) {

        //String path = DocumentProcessorUtil.getFilePathFromResources(fileName);
        excelHandler.setFileName(fileName);
        try {
            excelHandler.readData();
            HashMap mapData = excelHandler.getData();
            convertHashMapDataToDoi(mapData);
        } catch (Exception ex) {
            Logger.getLogger(PlosDoiDataImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public void exportXmlData(String filePath){
        
    }
    
    @Override
    public String getDspaceLoadingData(MultipartFile file){
        String filePath = null;
        filePath = DspaceDataUtil.saveUploadedData(file, "plos");
        setOutputPath(DocumentProcessorUtil.getFileContainerPath(filePath)+File.separator+"output");
        try {
            getDspaceLoadingData(filePath);
        } catch (Exception ex) {
            Logger.getLogger(PlosDoiDataImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        return filePath;
    }

    private String getArticleOutputFolderPath(String doi){
        return outputPath + File.separator + doi.split("/")[1];
    }
    
    private void matchCitationDate(String dateString, PlosData plosData) throws Exception{

        Date date;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        Pattern pattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})|(\\d{4}/\\d{2}/\\d{2})");
        Matcher matcher = pattern.matcher(dateString);
        if (matcher.find()){
            dateString = matcher.group(0);
            dateString = dateString.replace("-", "/");
            date = sdf.parse(dateString);
        }
        // the date can be in a form of 'MMM dd, yyyy'
        else if(!dateString.equals("")){
            SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy"); // 3-letter month name & 2-char day of month
            Date date2 = formatter.parse(dateString);
            String s3 = sdf.format(date2);
            date = sdf.parse(s3);
        }
        else {
            throw new Exception("Date match not found\n");
        }
        plosData.setDateIssued(date);
    }

    @Override
    public String getDspaceJournalLoadingFilesByDoi(String[] dois, Date time) {
        String uploadPath = null;
        plosDataList.clear();
        uploadPath = DspaceDataUtil.getDspaceUploadPath("plos", time);
        setOutputPath(uploadPath+File.separator+"output_plos");
        for(String doi : dois){
            getDspaceJournalLoadingFilesBySingleDoi(doi);
        }
        try {
            FileUtils.deleteDirectory(new File(uploadPath+File.separator+"output_plos"));
        } catch (IOException ex) {
            logger.error("Cannot delete the saf folder after being zipped", ex);
        }
        return uploadPath+File.separator+"output_plos.zip";
    }
    
    private String getIsPartOfSeriesByCitation(String citation) throws NoMatchingRegularExpressionException{
        String isPartOfSeries = null;
        String pattern = "\\.(\\s?)PLoS(\\s?)(.*)(\\d+)(\\((\\d+)\\):(\\s?)e(\\d+))";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(citation);
        if (m.find()) {
           isPartOfSeries = m.group(0);
        }
        else{
            throw new NoMatchingRegularExpressionException("Cannot find match strings at all!");
        }
        return isPartOfSeries.substring(2);
    }

    @Override
    public PlosData getDspaceJournalLoadingFilesBySingleDoi(String doi) {

        PlosRequest req = (PlosRequest)PlosUtil.getPlosContext().getBean("plosRequest");
        PlosData plosData = (PlosData)PlosUtil.getPlosContext().getBean("plosData");
        
        String doiDataVal = req.getFullData(doi);
        String[] tagNames = {"property"};
        HashMap<String,ArrayList<String>> metaData = HtmlParser.metaDataParserWithTagNames(doiDataVal, tagNames);

        String acknowledgement = PlosUtil.getPlosAck(doiDataVal);
        String citation = PlosUtil.getPlosCitation(doiDataVal);
        String contributions = PlosUtil.getAuthorContributions(doiDataVal);
        String journalTypeString = doi.split("journal.")[1].split("\\.")[0];

        try {
            plosData.setPlosJournalType(journalTypeString);
        } catch (Exception ex) {
            logger.error(ex);
        }
        plosData.setDoi(doi);
        plosData.setRelationUri(req.getRelationUriByDoi(doi));
        plosData.setUri(PlosUtil.DOI_PREFIX + doi);
        plosData.setAcknowledgements(acknowledgement);
        plosData.setAuthorContributions(contributions);    
        if(!DocumentProcessorUtil.isEmptyString(citation)){
            try {
                plosData.setIsPartOfSeries(getIsPartOfSeriesByCitation(citation));
            } catch (NoMatchingRegularExpressionException ex) {
                logger.error("Cannot get the string of IsPartOfSeries!", ex);
            }
        }
        plosData.setCitation(citation);
        JournalType type = plosData.getJournalType();

        switch(type){
            case PLOSONE: 
                plosData.setPeerReviewNotes(PlosUtil.PEERREVIEWNOTES_PONE);
                plosData.setPublisher("PLos One");
                break;
            case PLOSBIO: 
                plosData.setPeerReviewNotes(PlosUtil.PEERREVIEWNOTES_PBIO);
                plosData.setPublisher("PLos Biology");
                break;
            case PLOSGEN: 
                plosData.setPeerReviewNotes(PlosUtil.PEERREVIEWNOTES_PGEN);
                plosData.setPublisher("PLos Genetics");
                break;
            case PLOSMED: 
                plosData.setPeerReviewNotes(PlosUtil.PEERREVIEWNOTES_PMED);
                plosData.setPublisher("PLOS Medicine");
                break;
            case PLOSCBI: 
                plosData.setPeerReviewNotes(PlosUtil.PEERREVIEWNOTES_PCBI);
                plosData.setPublisher("PLOS Computational Biology");
                break;
            case PLOSPAT: 
                plosData.setPeerReviewNotes(PlosUtil.PEERREVIEWNOTES_PPAT);
                plosData.setPublisher("PLoS Pathogens");
                break;
            case PLOSNTD: 
                plosData.setPeerReviewNotes(PlosUtil.PEERREVIEWNOTES_PNTD);
                plosData.setPublisher("PLoS Neglected Tropical Diseases");
                break;
            default:
                {
                    try {
                        throw new NotFoundPublisherException("Journal type is undefined!");
                    } catch (NotFoundPublisherException ex) {
                        logger.error(ex);
                    }
                }
                break;
        }

        Iterator it = metaData.entrySet().iterator();

        try{
            while(it.hasNext()){
                Map.Entry pairs = (Map.Entry)it.next();
                if(pairs.getKey().equals("citation_title") || pairs.getKey().equals("og:title")){
                    plosData.setTitle(pairs.getValue().toString().replaceAll("(\\[|\\])*", ""));
                }
                else if(pairs.getKey().equals("twitter:description") || pairs.getKey().equals("og:description")){
                    plosData.setAbstractText(pairs.getValue().toString().replaceAll("(\\[|\\])*", ""));
                }
                else if(pairs.getKey().equals("citation_date")){
                    Date date = null;
                    String dateString = pairs.getValue().toString().replaceAll("(\\[|\\])*", "");
                    matchCitationDate(dateString, plosData);
                }
                else if(pairs.getKey().equals("citation_author")){
                    plosData.setAuthors(pairs.getValue().toString().replaceAll("(\\[|\\])*", "").split(", "));
                }
                else if(pairs.getKey().equals("keywords")){
                    plosData.setSubjects(pairs.getValue().toString().replaceAll("(\\[|\\])*", "").split(", "));
                }
                it.remove(); // avoids a ConcurrentModificationException
            }
            if(null == plosData.getSubjects()){
                String[] subjects = PlosUtil.getSubjects(doiDataVal);
                plosData.setSubjects(subjects);
            }
            if(null == plosData.getTitle() || "".equals(plosData.getTitle())){
                plosData.setTitle(PlosUtil.getTitleFromHtml(doiDataVal));
            }
            // download the PDF full text
            String articleOutputFolderPath = getArticleOutputFolderPath(doi);
            req.downloadPlosOnePdfByDoi(doi, articleOutputFolderPath);
            PlosUtil.createContentFile(articleOutputFolderPath+File.separator+"contents", doi.split("/")[1]+".pdf");
            plosData.exportXmlByDoiData(articleOutputFolderPath+File.separator+"dublin_core.xml");
            String outputFolderPath = DocumentProcessorUtil.getFileContainerPath(articleOutputFolderPath);
            DspaceDataUtil.packLoadingData(outputFolderPath, "plos");
        }
        catch(Exception ex){
            System.out.print("The data processing from doiData to plosData is wrong!\n");
            ex.printStackTrace();
        }
        return plosData;
    }
}

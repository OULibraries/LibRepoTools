/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shareok.data.documentProcessor;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

/**
 *
 * @author Tao Zhao
 */
public class CsvHandler implements FileHandler {
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(CsvHandler.class);
    private static final char DEFAULT_SEPARATOR = ',';
    
    private String fileName;
    private HashMap data;
    private int recordCount;
    private String [] fileHeadMapping;
    private CSVFormat csvFormat;

    public String getFileName() {
        return fileName;
    }

    @Override
    public HashMap getData() {
        return data;
    }

    @Override
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setData(HashMap data) {
        this.data = data;
    }

    public String[] getFileHeadMapping() {
        return fileHeadMapping;
    }

    public void setFileHeadMapping(String[] fileHeadMapping) {
        this.fileHeadMapping = fileHeadMapping;
    }

    public int getRecordCount() {
        return recordCount;
    }

    public void setRecordCount(int recordCount) {
        this.recordCount = recordCount;
    }

    public CSVFormat getCsvFormat() {
        return csvFormat;
    }

    public void setCsvFormat(CSVFormat csvFormat) {
        this.csvFormat = csvFormat;
    }
    
    
    /**
     * Reads out the data in an excel file and stores data in a hashmap
     * <p>Also sets the total record number and file heading</p>
     * 
     * @throws Exception
     */
    @Override
    public void readData() {
        FileReader fileReader = null;
        CSVParser csvFileParser = null;
        String[] headMapping = null;
        //CSVFormat csvFileFormat = CSVFormat.DEFAULT.withHeader(FILE_HEADER_MAPPING);
        
        try {            
            //initialize FileReader object
            fileReader = new FileReader(fileName);

            //initialize CSVParser object
            if(null == csvFormat){
                csvFormat = CSVFormat.DEFAULT;
            }
            csvFileParser = new CSVParser(fileReader, csvFormat);
            
            //Get a list of CSV file records
            List csvRecords = csvFileParser.getRecords(); 
            
            int size = csvRecords.size();
            
            setRecordCount(size);
            
            data = new HashMap();

            //Read the CSV file records starting from the second record to skip the header
            for (int i = 0; i < size; i++) {
                CSVRecord record = (CSVRecord)csvRecords.get(i);   
                if(null != record){
                    if(i == 0){
                        List headMappingList = new ArrayList();
                        Iterator it = record.iterator();
                        while(it.hasNext()){
                            String value = (String)it.next();
                            headMappingList.add(value);
                        }                    
                        headMapping = new String[headMappingList.size()];
                        headMapping = (String[]) headMappingList.toArray(headMapping);
                        setFileHeadMapping(headMapping);                        
                    }
                    else{
                        for(int j = 0; j < fileHeadMapping.length; j++){
                            String colName = fileHeadMapping[j].trim();
                            String key = colName + "-" + i;
                            data.put(key, record.get(j));
                        }
                    }
                }
            }

        } 
        catch (Exception e) {
            System.out.println("Error in CsvFileReader !!!");
            e.printStackTrace();
        } finally {
            try {
                fileReader.close();
                csvFileParser.close();
            } catch (IOException e) {
                System.out.println("Error while closing fileReader/csvFileParser !!!");
                e.printStackTrace();
            }
        }
    }

    /**
     * Output the data into a new CSV file
     * 
     * If the newFileName is not specified, a "-copy" will be attached to the old file name for the new CSV file
     * 
     * @return the path of the new CSV file
     */
    public String outputData() {
        return outputData(""); 
    }
    
    /**
     * Output the data into a new CSV file
     * <p>Uses "\n" as the line separator</P>
     * 
     * @param newFileName : String. 
     * If the newFileName is not specified, a "-copy" will be attached to the old file name for the new CSV file
     * 
     * @return the path of the new CSV file
     */
    public String outputData(String newFileName) {
        if(null == data){
            readData();
        }
        
        if(null != data){
            FileWriter fileWriter = null;
            CSVPrinter csvFilePrinter = null;
            CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator("\n");
            try{
                if(null == newFileName || newFileName.equals("")){                    
                    newFileName = fileName.substring(0,fileName.indexOf(".csv"))+"-copy.csv";
                }
                fileWriter = new FileWriter(newFileName);
                
                csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat);
                csvFilePrinter.printRecord( Arrays.asList(fileHeadMapping));
                Map<String, String[]> records = new HashMap<>();                
                List<String> headingList = Arrays.asList(fileHeadMapping);
                
                Iterator it = data.keySet().iterator();
                while(it.hasNext()){
                    String key = (String)it.next();
                    String value = (String)data.get(key);
                    String[] keyInfo = key.split("-");
                    String row = keyInfo[keyInfo.length-1];
                    String column = key.replace("-"+row, "");

                    if(null == records.get(row)){
                        String[] dataRecord = new String[fileHeadMapping.length];
                        dataRecord[headingList.indexOf(column)] = value;
                        records.put(row, dataRecord);
                    }
                    else{
                        String[] dataRecord = records.get(row);
                        dataRecord[headingList.indexOf(column)] = value;
                    }
                }
                
                Iterator it2 = records.keySet().iterator();
                while(it2.hasNext()){             
                    String key = (String)it2.next();
                    String[] value = (String[])records.get(key);
                    csvFilePrinter.printRecord(Arrays.asList(value));
                }
            }
            catch(Exception e){
                System.out.println("Error in CsvFileWriter!\n");
                e.printStackTrace();
            }
            finally{
                try{
                    fileWriter.flush();
                    fileWriter.close();
                    csvFilePrinter.close();
                }
                catch(IOException e){
                    System.out.println("Error while flushing/closing fileWriter/csvPrinter\n");
                    e.printStackTrace();
                }
            }
        }
        return newFileName;
    }
    
    /**
     * Adds a new column to the csv file
     * @param columnName
     * @param position 
     */
    public void addColumn(String columnName, int position) {
        addColumn(columnName, position, null);
    }
    
    /**
     * Adds a new column to the csv file. Note: empty file causes a bug!
     * <p>Adds the column data to the new column and save the data</p>
     * <p>The column data cannot have more number than the total row counts</p>
     * <p>Column data set should be in the form of "i" => "value" </p>
     * 
     * @param columnName
     * @param position
     * @param colData 
     */
    public void addColumn(String columnName, int position, HashMap colData) {
        
        try{
            if(null == data || data.isEmpty())
                readData();
            
            HashMap csvData = data;        

            // insert the new column name into the file heading:
            String[] oldFileHeading = fileHeadMapping;
            ArrayList<String> mapList = new ArrayList();
            for(String col : oldFileHeading){
                mapList.add(col.trim());
            }
            mapList.add(position, columnName);
            String[] newFileHeading = mapList.toArray(new String[mapList.size()]);
            setFileHeadMapping(newFileHeading);
            
            boolean emptyData = null == colData;
            
            for(int i = 0; i < recordCount; i++) {
                String index = String.valueOf(i);
                if(emptyData == true || null == colData.get(index)) {
                    csvData.put(columnName+"-"+index, "");
                }
                else{
                    csvData.put(columnName+"-"+index, colData.get(index));
                }
            }
            
            setData(csvData);
            
            File file = new File(fileName);
        	
            if(file.delete()){
                    System.out.println(file.getName() + " is deleted!");
            }else{
                    System.out.println("Delete operation is failed.");
            }
            
            outputData(fileName);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
    
    public void deleteColumnByColumnName(String[] columnNames){
        
        if(null == data || data.isEmpty()){
            readData();
        }
        List<String> headingList = new LinkedList<String>(Arrays.asList(fileHeadMapping));
        List<String> columnList = new LinkedList<String>(Arrays.asList(columnNames));
        
        for(String column : columnNames){
            if(headingList.contains(column)){   
                headingList.remove(column);
            }
        }
        
        fileHeadMapping = headingList.toArray(new String[headingList.size()]);
        
        if(recordCount > 0){
            for(int i = 0; i < recordCount; i++){
                for(String column : columnList){
                    String key = column + "-" + String.valueOf(i);
                    if(data.containsKey(key)){
                        data.remove(key);
                    }
                }
            }
        }
        //outputData("");
    }
    
    /**
     * Returns a mapped data uses values from one column as key and values from another column as values, for data searching, mapping and comparison
     * <p>Note: the data from keyColumn are used as the key for the Map so the data have to be composed of unique values</P>
     * 
     * @param keyColumn
     * @param valueColumn
     * @return 
     */
    public Map getDataMapByColumns(String keyColumn, String valueColumn) {
        Map colData = null;
        
        if(null == data){
            readData();            
        }
        
        if(null != data && recordCount > 0){
            colData = new HashMap();
            colData.put("keyColumn", keyColumn);
            colData.put("valueColumn", valueColumn);
            for(int i = 0; i < recordCount; i++) {
                String key = (String)data.get(keyColumn+"-"+i);
                String value = (String)data.get(valueColumn+"-"+i);
                if(null != key && !key.equals("")){
                    colData.put(key, value);
                }
            }
        }
        
        return colData;
    }
    
    @Override
    public void exportMapDataToXml(HashMap map, String filePath) {
        
    }
    
    public void writeCsvLine(FileWriter w, List<String> values) throws IOException {

        boolean first = true;

        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            if (!first) {
                sb.append(DEFAULT_SEPARATOR);
            }
            sb.append(followCVSformat(value));

            first = false;
        }
        sb.append("\n");
        w.append(sb.toString());
    }
    
    private String followCVSformat(String value) {

        String result = value;
        if (result.contains("\"")) {
            result = result.replace("\"", "\"\"");
        }
        if(result.contains(",")){
            result = "\"" + result + "\"";
        }
        return result;

    }
}

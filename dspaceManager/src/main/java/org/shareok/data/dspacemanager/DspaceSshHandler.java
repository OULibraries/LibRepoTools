/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shareok.data.dspacemanager;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.shareok.data.config.DataHandler;
import org.shareok.data.documentProcessor.FileUtil;
import org.shareok.data.redis.RedisUtil;
import org.shareok.data.ssh.SshExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author Tao Zhao
 */
@Service
public class DspaceSshHandler implements DataHandler {
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(DspaceSshHandler.class);
    
    private String serverId;
    private String reportFilePath;
    private String uploadDst;
    private String uploadFile; // *** suppose the uploaded file is a ZIP file ***
    private String dspaceUser;
    private String dspaceDirectory; // the DSpace installation directory
    private String collectionId;
    private SshExecutor sshExec;
    //private String mapfilePath;
    
    public String getReportFilePath(){
        return reportFilePath;
    }

    public String getUploadDst() {
        return uploadDst;
    }

    public String getUploadFile() {
        return uploadFile;
    }

    public String getDspaceUser() {
        return dspaceUser;
    }

    public String getDspaceDirectory() {
        return dspaceDirectory;
    }

    public String getCollectionId() {
        return collectionId;
    }

    public String getServerId() {
        return serverId;
    }
    
    @Override
    public void setReportFilePath(String reportFilePath){
        this.reportFilePath = reportFilePath;
    }

    public void setUploadDst(String uploadDst) {
        this.uploadDst = uploadDst;
    }

    @Override
    public void setUploadFile(String uploadFile) {
        this.uploadFile = uploadFile;
    }

    public void setDspaceUser(String dspaceUser) {
        this.dspaceUser = dspaceUser;
    }

    public void setDspaceDirectory(String dspaceDirectory) {
        this.dspaceDirectory = dspaceDirectory;
    }

    public void setCollectionId(String collectionId) {
        this.collectionId = collectionId;
    }

    public SshExecutor getSshExec() {
        return sshExec;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    @Autowired
    public void setSshExec(SshExecutor sshExec) {
        this.sshExec = sshExec;
        if((null == sshExec.getServer() || sshExec.getServer().getServerName().equals("")) && null != serverId && !"".equals(serverId)){
            this.sshExec.setServer(RedisUtil.getServerDaoInstance().findServerById(Integer.parseInt(serverId)));
        }
    }
    
    public String importDspace(){
        try{     
            String time = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
            String uploadFileName = new File(uploadFile).getName();
            String uploadFileNameWithoutExtension = uploadFileName.split("\\.")[0];
            
            String dspaceTargetFilePath = uploadDst + File.separator + time + File.separator + uploadFileName;
            
            // Build up the commands:
            String newDirCommand = " bash -c \"mkdir " + uploadDst + File.separator + time + "\"";
            String unzipCommand = " bash -c \"unzip -o " + dspaceTargetFilePath + " -d " + uploadDst + File.separator + time + "\"";
            //String unzipCommand = "sudo tar -xvf " + uploadDst + File.separator + uploadFileName;
            String importCommand = "sudo " + dspaceDirectory + File.separator + "bin" + File.separator +
                                   "dspace import --add " + "--eperson=" + dspaceUser + " --collection=" + collectionId +
                                   " --source=" + uploadDst + File.separator + time + File.separator + uploadFileNameWithoutExtension + " --mapfile=" + uploadDst +
                                   File.separator + time + File.separator + "mapfile ";
            sshExec.addReporter("Three commands to be executed:");
            sshExec.addReporter("Build up a new directory for the new importing: "+newDirCommand);
            sshExec.addReporter("Unzip the uploaded SAF package: "+unzipCommand);
            sshExec.addReporter("Import the SAF package into DSpace: "+importCommand);
            sshExec.execCmd(newDirCommand);
            sshExec.upload(uploadDst + File.separator + time, uploadFile);  
            sshExec.addReporter("The SAF package has been uploaded to the DSpace server: " + dspaceTargetFilePath + "\n");
            String[] commands = {unzipCommand, importCommand};
            sshExec.execCmd(commands);
            sshExec.addReporter("The SAF package has been imported into the DSpace repository.\n");
////            sshExec.execCmd(unzipCommand);
////            sshExec.execCmd(importCommand);
            String savedReportFilePath =  saveLoggerToFile();
            sshExec.addReporter("The importing logging information has been saved to file : " + reportFilePath);
            
            return dspaceTargetFilePath;
        }
        catch(Exception ex){
            logger.error("Cannot import the SAF package into DSapce", ex);
            return null; 
        }
    }
    
    public String uploadSafDspace(){
        try{     
            String time = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
            String uploadFileName = new File(uploadFile).getName();
            String uploadFileNameWithoutExtension = uploadFileName.split("\\.")[0];
            
            String dspaceTargetFilePath = uploadDst + File.separator + time + File.separator + uploadFileName;
            
            // Build up the commands:
            String newDirCommand = " bash -c \"mkdir " + uploadDst + File.separator + time + "\"";
            
            sshExec.addReporter("Two steps to be completed:");
            sshExec.addReporter("Build up a new directory for the SAF package: "+newDirCommand);
            sshExec.addReporter("Upload the SAF package zip file to the DSpace server at  "+uploadDst);
   
            sshExec.execCmd(newDirCommand);
            sshExec.upload(uploadDst + File.separator + time, uploadFile);  System.out.println(" ****** ");
            sshExec.addReporter("The SAF package has been uploaded to the DSpace server: " + dspaceTargetFilePath + "\n");
            
            String savedReportFilePath =  saveLoggerToFile();
            sshExec.addReporter("The importing logging information has been saved to file : " + reportFilePath);
            
            return dspaceTargetFilePath;
        }
        catch(Exception ex){
            logger.error("Cannot import the SAF package into DSapce", ex);
            return null; 
        }
    }
    
    public String importUploadedSafDspace(){
        try{                
            String mapFilePath = FileUtil.getFileContainerPath(uploadFile) + "mapfile";
            File uploadFileObj = new File(uploadFile);
            String uploadFileName = uploadFileObj.getName();
            String upzippedFilePath = FileUtil.getFileContainerPath(uploadFile) + FileUtil.getFileNameWithoutExtension(uploadFileName);//.getFileNameWithoutExtension(uploadFile);
            String unzipCommand = " bash -c \"unzip -o " + uploadFile + " -d " + FileUtil.getFileContainerPath(uploadFile) + "\"";
            String importCommand = "sudo " + dspaceDirectory + File.separator + "bin" + File.separator +
                                   "dspace import --add " + "--eperson=" + dspaceUser + " --collection=" + collectionId +
                                   " --source=" + upzippedFilePath + " --mapfile=" + mapFilePath;
            sshExec.addReporter("Two commands to be executed:");
            sshExec.addReporter("Unzip the uploaded SAF package: "+unzipCommand);
            sshExec.addReporter("Import the SAF package into DSpace: "+importCommand);
           
            String[] commands = {unzipCommand, importCommand};
            sshExec.execCmd(commands);
            sshExec.addReporter("The SAF package has been imported into the DSpace repository.\n");
////            sshExec.execCmd(unzipCommand);
////            sshExec.execCmd(importCommand);
            String savedReportFilePath =  saveLoggerToFile();
            sshExec.addReporter("The importing logging information has been saved to file : " + reportFilePath);
            
            return uploadFile;
        }
        catch(Exception ex){
            logger.error("Cannot import the SAF package into DSapce", ex);
            return null; 
        }
    }
    
    private String saveLoggerToFile(){
        try{
        File reportFile = new File(reportFilePath);
        if(!reportFile.exists()){
            reportFile.createNewFile();
        }
            FileUtil.outputStringToFile(sshExec.getReporter(), reportFilePath);
        }
        catch(IOException ioex){
            logger.error("Cannot save importing report!");
        }
        return reportFilePath;
    }
}
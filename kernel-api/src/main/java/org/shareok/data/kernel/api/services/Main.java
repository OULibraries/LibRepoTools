/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shareok.data.kernel.api.services;

/**
 * main function argument data is a JSON String
 * Action type: 
 *       journal-search <journal articles search> : java -jar target/kernel-api-1.1.0-SNAPSHOT-jar-with-dependencies.jar id-0 journal-search '{"publisher" : "sage", "startDate": "2017-01-02", "endDate" : "2017-04-05", "affiliate" : "University of Oklahoma"}'
 *       journal-saf <journal articles SAF package> : java -jar target/kernel-api-1.1.0-SNAPSHOT-jar-with-dependencies.jar id-1 journal-saf '{"dois": "10.1371/journal.pone.0171910;10.1371/journal.pone.0171683;10.1371/journal.pone.0171193", "startDate" : "2017-03-03", "endDate" : "2017-03-28"}'
                                                      java -jar target/kernel-api-1.1.0-SNAPSHOT-jar-with-dependencies.jar id-1 journal-saf '{"dois": "10.1177/8755123315593325;10.1177/1461444815606121", "startDate" : "2017-03-03", "endDate" : "2017-03-28"}'
 *       journal-import <journal articles SAF import> : java -jar target/kernel-api-1.1.0-SNAPSHOT-jar-with-dependencies.jar sdfx7 journal-import '{"safPath" : "/var/local/librepotools/librepotools-data/uploads/2017.07.21.16.42.16/sage/output_sage_2017-03-03_2017-03-28.zip", "collectionHandle" : "11244/37263", "dspaceApiUrl" : "https://test.shareok.org/rest"}'
 *       saf-build <Build up a SAF package for DSpace ingest> : java -jar target/kernel-api-1.1.0-SNAPSHOT-jar-with-dependencies.jar id-saf-1 saf-build '{"csvPath" : "/Users/zhao0677/Projects/ouhj/OUHJ-1/ouhj2.csv"}'
 *       aws-dissertation <Build up a SAF package for AWS dissertations> : java -jar target/kernel-api-1.1.0-SNAPSHOT-jar-with-dependencies.jar id-aws-4 aws-dissertation '{"json" : "{\"rest endpoint\": \"https://test.shareok.org/rest\",\"collection\": \"11244/23528\",\"items\": [{\"2002_Eoff_Jennifer_99356001102042\": {\"files\": [\"ul-bagit/private/shareok/2002_Eoff_Jennifer_99356001102042/data/2002_Eoff_Jennifer_Thesis.pdf\",\"ul-bagit/private/shareok/2002_Eoff_Jennifer_99356001102042/data/Abstract.txt\",\"ul-bagit/private/shareok/2002_Eoff_Jennifer_99356001102042/data/Committee.txt\"],\"metadata\": \"<xml> metadata in dublin core format\"}}]}"}'
 * 
 * @author Tao Zhao
 */
public class Main {
    public static void main(String[] args){        
        try {
            String taskId = args[0];
            String taskType = args[1];
            String data = args[2];
            ServiceUtil.executeCommandLineTask(taskId, taskType, data);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
    }
}

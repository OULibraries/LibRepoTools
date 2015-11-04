/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.shareok.data.documentProcessor;

import java.util.HashMap;

/**
 *
 * @author Tao Zhao
 */
public interface FileHandler {
    public void setFileName(String fileName);
    public HashMap getData();
    public void readData();
    public void exportMapDataToXml(HashMap map, String filePath);
}

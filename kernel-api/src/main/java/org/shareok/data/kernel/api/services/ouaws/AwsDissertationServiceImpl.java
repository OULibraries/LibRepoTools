/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shareok.data.kernel.api.services.ouaws;

import org.springframework.beans.factory.annotation.Autowired;
import oulib.aws.s3.DissertationProcessor;

/**
 *
 * @author zhao0677
 */
public class AwsDissertationServiceImpl implements AwsDissertationService {
    
    private DissertationProcessor processor;

    public DissertationProcessor getProcessor() {
        return processor;
    }

    @Autowired
    public void setProcessor(DissertationProcessor processor) {
        this.processor = processor;
    }
    
    @Override
    public String parseRecipeFile(String json){
        processor.parseRecipeFile(json);
        return processor.toString();
    }
    
    @Override
    public String generateDissertationSafPackage() {
        return processor.generateSafPackage();
    }
    
}

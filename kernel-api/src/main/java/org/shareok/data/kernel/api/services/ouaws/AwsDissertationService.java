/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shareok.data.kernel.api.services.ouaws;

/**
 *
 * @author zhao0677
 */
public interface AwsDissertationService {
    public String parseRecipeFile(String json);
    public String generateDissertationSafPackage();
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shareok.data.plosdata.exception;

/**
 *
 * @author Tao Zhao
 */
public class ErrorPlosApiResponseException extends Exception {
    /**
     *
     * @param message
     */
    public ErrorPlosApiResponseException(String message){
        super(message);
    }
}

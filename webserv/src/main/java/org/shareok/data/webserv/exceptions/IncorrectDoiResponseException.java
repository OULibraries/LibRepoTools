/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shareok.data.webserv.exceptions;

/**
 *
 * @author Tao Zhao
 */
public class IncorrectDoiResponseException extends Exception {
    
    /**
     *
     * @param message
     */
    public IncorrectDoiResponseException(String message){
        super(message);
    }
}
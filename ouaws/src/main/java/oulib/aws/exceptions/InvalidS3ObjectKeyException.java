/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oulib.aws.exceptions;

/**
 *
 * @author zhao0677
 */
public class InvalidS3ObjectKeyException extends Exception {
    /**
     *
     * @param message
     */
    public InvalidS3ObjectKeyException(String message){
        super(message);
    }
}

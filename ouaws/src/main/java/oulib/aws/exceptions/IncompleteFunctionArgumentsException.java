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
public class IncompleteFunctionArgumentsException extends Exception {
    /**
     *
     * @param message
     */
    public IncompleteFunctionArgumentsException(String message){
        super(message);
    }
}

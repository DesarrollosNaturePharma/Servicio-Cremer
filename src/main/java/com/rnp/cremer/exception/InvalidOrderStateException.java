package com.rnp.cremer.exception;

/**
 * Excepción lanzada cuando se intenta realizar una operación 
 * sobre una orden que no está en el estado adecuado.
 * 
 * @author RNP Team
 * @version 1.0
 * @since 2024-11-25
 */
public class InvalidOrderStateException extends RuntimeException {
    
    /**
     * Construye una nueva excepción con el mensaje especificado.
     * 
     * @param message mensaje descriptivo del error
     */
    public InvalidOrderStateException(String message) {
        super(message);
    }
    
    /**
     * Construye una nueva excepción con mensaje y causa.
     * 
     * @param message mensaje descriptivo del error
     * @param cause causa de la excepción
     */
    public InvalidOrderStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
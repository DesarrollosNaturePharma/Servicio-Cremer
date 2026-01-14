package com.rnp.cremer.exception;

/**
 * Excepción lanzada cuando se intenta crear una orden que ya existe.
 * 
 * @author RNP Team
 * @version 1.0
 * @since 2024-11-25
 */
public class OrderAlreadyExistsException extends RuntimeException {
    
    /**
     * Construye una nueva excepción con el mensaje especificado.
     * 
     * @param message mensaje descriptivo del error
     */
    public OrderAlreadyExistsException(String message) {
        super(message);
    }
    
    /**
     * Construye una nueva excepción con mensaje y causa.
     * 
     * @param message mensaje descriptivo del error
     * @param cause causa de la excepción
     */
    public OrderAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
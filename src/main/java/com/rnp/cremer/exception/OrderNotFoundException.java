package com.rnp.cremer.exception;

/**
 * Excepción lanzada cuando no se encuentra una orden en el sistema.
 * 
 * @author RNP Team
 * @version 1.0
 * @since 2024-11-25
 */
public class OrderNotFoundException extends RuntimeException {
    
    /**
     * Construye una nueva excepción con el mensaje especificado.
     * 
     * @param message mensaje descriptivo del error
     */
    public OrderNotFoundException(String message) {
        super(message);
    }
    
    /**
     * Construye una nueva excepción con mensaje y causa.
     * 
     * @param message mensaje descriptivo del error
     * @param cause causa de la excepción
     */
    public OrderNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
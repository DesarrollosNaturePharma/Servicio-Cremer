package com.rnp.cremer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Respuesta estándar de la API.
 *
 * <p>Envuelve todas las respuestas en un formato consistente
 * con información de éxito, mensaje y datos.</p>
 *
 * @param <T> Tipo de datos en la respuesta
 * @author RNP Team
 * @version 1.0
 * @since 2024-11-25
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {

    /**
     * Indica si la operación fue exitosa.
     */
    private Boolean success;

    /**
     * Mensaje descriptivo de la operación.
     */
    private String message;

    /**
     * Datos de respuesta (puede ser null).
     */
    private T data;

    /**
     * Timestamp de la respuesta.
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * Crea una respuesta exitosa con datos.
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Crea una respuesta de error.
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
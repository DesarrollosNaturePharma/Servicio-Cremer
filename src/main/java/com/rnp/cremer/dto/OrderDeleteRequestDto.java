package com.rnp.cremer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para solicitar la eliminación de una orden.
 *
 * <p>Contiene la información necesaria para registrar quién elimina
 * la orden y por qué motivo.</p>
 *
 * @author RNP Team
 * @version 1.0
 * @since 2024-11-26
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDeleteRequestDto {

    /**
     * Usuario que solicita la eliminación.
     * Campo obligatorio para auditoría.
     */
    @NotBlank(message = "El usuario que elimina es obligatorio")
    @Size(max = 100, message = "El nombre de usuario no puede exceder 100 caracteres")
    private String deletedBy;

    /**
     * Motivo de la eliminación.
     * Campo obligatorio para trazabilidad.
     */
    @NotBlank(message = "El motivo de eliminación es obligatorio")
    @Size(max = 500, message = "El motivo no puede exceder 500 caracteres")
    private String motivo;
}

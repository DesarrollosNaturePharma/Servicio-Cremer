package com.rnp.cremer.dto;

import com.rnp.cremer.model.TipoPausa;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para finalizar una pausa.
 *
 * <p>Si la pausa se creó sin tipo/descripción, se deben proporcionar aquí.
 * Si la pausa ya tenía tipo/descripción, estos campos son opcionales.</p>
 *
 * @author RNP Team
 * @version 1.0
 * @since 2024-11-25
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PauseFinishDto {

    /**
     * Tipo de pausa (OBLIGATORIO si no se proporcionó al crear).
     */
    private TipoPausa tipo;

    /**
     * Descripción/motivo de la pausa (OPCIONAL).
     * Se puede agregar o complementar la descripción inicial.
     */
    @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
    private String descripcion;

    /**
     * Operario (OPCIONAL).
     * Se puede agregar si no se proporcionó al crear.
     */
    @Size(max = 100, message = "El operario no puede superar 100 caracteres")
    private String operario;
}
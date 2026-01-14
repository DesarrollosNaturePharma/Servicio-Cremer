package com.rnp.cremer.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para finalizar una orden de producción.
 *
 * <p>Contiene los datos de cierre de producción y la decisión
 * sobre si la orden requiere proceso manual adicional.</p>
 *
 * @author RNP Team
 * @version 1.1
 * @since 2024-11-26
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderFinishDto {

    /**
     * Cantidad de botes buenos producidos.
     */
    @NotNull(message = "Los botes buenos son obligatorios")
    @Min(value = 0, message = "Los botes buenos no pueden ser negativos")
    private Integer botesBuenos;

    /**
     * Cantidad de botes malos/defectuosos.
     */
    @NotNull(message = "Los botes malos son obligatorios")
    @Min(value = 0, message = "Los botes malos no pueden ser negativos")
    private Integer botesMalos;

    /**
     * Total de cajas producidas al cierre.
     */
    @Min(value = 0, message = "Las cajas no pueden ser negativas")
    private Integer totalCajasCierre;

    /**
     * Indica si la orden requiere proceso manual adicional.
     * <p>
     * <ul>
     *   <li><b>true:</b> La orden pasa a ESPERA_MANUAL después de finalizar</li>
     *   <li><b>false:</b> La orden pasa directamente a FINALIZADA</li>
     * </ul>
     * </p>
     * <p>Valor por defecto: false</p>
     */
    @Builder.Default
    private Boolean acumula = false;
}
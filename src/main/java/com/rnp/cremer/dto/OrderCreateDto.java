package com.rnp.cremer.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para crear una nueva orden de producción.
 *
 * <p>Contiene solo los campos que el usuario debe proporcionar.
 * Los campos calculados y automáticos se generan en el backend.</p>
 *
 * @author RNP Team
 * @version 1.0
 * @since 2024-11-25
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreateDto {

    /**
     * Nombre del operario responsable.
     * <p><b>Ejemplo:</b> "Juan Pérez"</p>
     */
    @NotBlank(message = "El operario es obligatorio")
    @Size(max = 100, message = "El operario no puede superar 100 caracteres")
    private String operario;

    /**
     * Código único de la orden.
     * <p><b>Ejemplo:</b> "OF-2024-001"</p>
     */
    @NotBlank(message = "El código de orden es obligatorio")
    @Size(max = 50, message = "El código de orden no puede superar 50 caracteres")
    private String codOrder;

    /**
     * Número de lote.
     * <p><b>Ejemplo:</b> "LOTE-A-123"</p>
     */
    @NotBlank(message = "El lote es obligatorio")
    @Size(max = 50, message = "El lote no puede superar 50 caracteres")
    private String lote;

    /**
     * Código del artículo a producir.
     * <p><b>Ejemplo:</b> "BOTE-500ML"</p>
     */
    @NotBlank(message = "El artículo es obligatorio")
    @Size(max = 100, message = "El artículo no puede superar 100 caracteres")
    private String articulo;

    /**
     * Descripción opcional del producto.
     * <p><b>Ejemplo:</b> "Botes de conserva de tomate 500ml"</p>
     */
    @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
    private String descripcion;

    /**
     * Cantidad total de botes a producir.
     * <p><b>Ejemplo:</b> 1000</p>
     */
    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer cantidad;

    /**
     * Número de botes por caja.
     * <p><b>Ejemplo:</b> 24</p>
     */
    @NotNull(message = "Los botes por caja son obligatorios")
    @Min(value = 1, message = "Los botes por caja deben ser al menos 1")
    private Integer botesCaja;

    /**
     * Estándar de referencia: tiempo en minutos por unidad.
     * <p><b>Ejemplo:</b> 0.12 (7.2 segundos por bote)</p>
     */
    @NotNull(message = "El estándar de referencia es obligatorio")
    @DecimalMin(value = "0.001", message = "El estándar debe ser mayor a 0")
    private Float stdReferencia;

    /**
     * Formato del bote.
     * <p><b>Ejemplo:</b> "500ml"</p>
     */
    @NotBlank(message = "El formato del bote es obligatorio")
    @Size(max = 100, message = "El formato del bote no puede superar 100 caracteres")
    private String formatoBote;

    /**
     * Tipo de producto.
     * <p><b>Ejemplo:</b> "Conserva"</p>
     */
    @NotBlank(message = "El tipo es obligatorio")
    @Size(max = 100, message = "El tipo no puede superar 100 caracteres")
    private String tipo;

    /**
     * Unidades por bote (en ml, gramos, etc).
     * <p><b>Ejemplo:</b> 500</p>
     */
    @NotNull(message = "Las unidades por bote son obligatorias")
    @Min(value = 1, message = "Las unidades por bote deben ser al menos 1")
    private Integer udsBote;
}
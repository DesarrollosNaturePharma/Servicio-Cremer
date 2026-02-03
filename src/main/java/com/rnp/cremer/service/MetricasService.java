package com.rnp.cremer.service;

import com.rnp.cremer.model.EstadoOrder;
import com.rnp.cremer.model.Metricas;
import com.rnp.cremer.model.Order;
import com.rnp.cremer.repository.MetricasRepository;
import com.rnp.cremer.repository.OrderRepository;
import com.rnp.cremer.repository.PauseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio para cálculo de métricas de producción.
 *
 * <p>Calcula automáticamente los siguientes KPIs:</p>
 * <ul>
 *   <li><b>Tiempo Total:</b> (hora fin - hora inicio) - pausas NO computables</li>
 *   <li><b>Tiempo Pausado:</b> Suma de pausas computables</li>
 *   <li><b>Tiempo Activo:</b> Tiempo total - tiempo pausado</li>
 *   <li><b>Disponibilidad:</b> Tiempo activo / tiempo total</li>
 *   <li><b>Rendimiento:</b> Producción real vs esperada</li>
 *   <li><b>Calidad:</b> Botes buenos / total producido</li>
 *   <li><b>OEE:</b> Disponibilidad × Rendimiento × Calidad</li>
 *   <li><b>STD Real:</b> Tiempo real por unidad producida</li>
 * </ul>
 *
 * @author RNP Team
 * @version 1.0
 * @since 2024-11-26
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MetricasService {

    private final MetricasRepository metricasRepository;
    private final OrderRepository orderRepository;
    private final PauseRepository pauseRepository;

    /**
     * Calcula y guarda las métricas de una orden.
     *
     * <p><b>Condiciones para calcular:</b></p>
     * <ul>
     *   <li>La orden debe tener hora de inicio</li>
     *   <li>La orden debe estar en estado FINALIZADA o ESPERA_MANUAL</li>
     *   <li>NO debe existir un registro previo de métricas para esta orden</li>
     * </ul>
     *
     * @param order Orden finalizada
     * @return Metricas calculadas y guardadas, o null si ya existían
     */
    @Transactional
    public Metricas calcularYGuardarMetricas(Order order) {
        log.info("Verificando si se deben calcular métricas para orden ID: {}", order.getIdOrder());

        // Verificar si ya existen métricas (NO recalcular)
        if (metricasRepository.existsByIdOrder(order.getIdOrder())) {
            log.info("Las métricas ya existen para orden {}. NO se recalculan.", order.getCodOrder());
            return null;
        }

        // Validar que tenga hora de inicio
        if (order.getHoraInicio() == null) {
            log.warn("Orden {} no tiene hora de inicio. No se pueden calcular métricas.", order.getCodOrder());
            return null;
        }

        // Usar hora fin de la orden, o actual si no está definida (fallback)
        LocalDateTime horaFin = order.getHoraFin() != null ? order.getHoraFin() : LocalDateTime.now();

        log.info("Calculando métricas para orden {} - Inicio: {} - Fin: {}",
                order.getCodOrder(), order.getHoraInicio(), horaFin);

        // ===================================
        // CÁLCULO 1: TIEMPO BRUTO (minutos)
        // ===================================
        Duration duracionBruta = Duration.between(order.getHoraInicio(), horaFin);
        float tiempoBruto = duracionBruta.toSeconds() / 60.0f;

        // ===================================
        // CÁLCULO 2: PAUSAS NO COMPUTABLES (minutos)
        // ===================================
        // Suma de todas las pausas que NO computan (descansos, etc.)
        Float tiempoPausasNoComputables = pauseRepository.getNonComputedPauseTimeByOrder(order.getIdOrder());
        if (tiempoPausasNoComputables == null) {
            tiempoPausasNoComputables = 0.0f;
        }

        // ===================================
        // CÁLCULO 3: TIEMPO TOTAL (minutos)
        // ===================================
        // Tiempo total = Tiempo bruto - Pausas NO computables
        float tiempoTotal = tiempoBruto - tiempoPausasNoComputables;

        // ===================================
        // CÁLCULO 4: TIEMPO PAUSADO COMPUTABLE (minutos)
        // ===================================
        // Suma de todas las pausas computables (averías, mantenimiento, etc.)
        Float tiempoPausado = pauseRepository.getComputedPauseTimeByOrder(order.getIdOrder());
        if (tiempoPausado == null) {
            tiempoPausado = 0.0f;
        }

        // ===================================
        // CÁLCULO 5: TIEMPO ACTIVO (minutos)
        // ===================================
        float tiempoActivo = tiempoTotal - tiempoPausado;

        // Validar que tiempo activo sea positivo
        if (tiempoActivo <= 0) {
            log.warn("Tiempo activo es <= 0 para orden {}. Ajustando a 1 minuto.", order.getCodOrder());
            tiempoActivo = 1.0f; // Evitar división por cero
        }

        // ===================================
        // CÁLCULO 6: DISPONIBILIDAD (0-1)
        // ===================================
        // Porcentaje de tiempo que la máquina estuvo activa
        float disponibilidad = tiempoActivo / tiempoTotal;

        // ===================================
        // CÁLCULO 7: RENDIMIENTO (0-1+)
        // ===================================
        // (botes buenos + botes malos) / (tiempo activo * std referencia)
        int totalProducido = (order.getBotesBuenos() != null ? order.getBotesBuenos() : 0) +
                (order.getBotesMalos() != null ? order.getBotesMalos() : 0);

        float produccionEsperada = tiempoActivo * order.getStdReferencia();
        float rendimiento = produccionEsperada > 0 ? totalProducido / produccionEsperada : 0.0f;

        // ===================================
        // CÁLCULO 8: CALIDAD (0-1)
        // ===================================
        // botes buenos / total producido
        float calidad = totalProducido > 0 ?
                (float) (order.getBotesBuenos() != null ? order.getBotesBuenos() : 0) / totalProducido :
                0.0f;

        // ===================================
        // CÁLCULO 9: OEE (0-1)
        // ===================================
        // Disponibilidad × Rendimiento × Calidad
        float oee = disponibilidad * rendimiento * calidad;

        // ===================================
        // CÁLCULO 10: STD REAL (minutos/unidad)
        // ===================================
        // Tiempo real que tomó producir cada unidad
float stdReal = tiempoActivo > 0 ? totalProducido / tiempoActivo : 0.0f;

        // ===================================
        // CÁLCULO 11: PORCENTAJE CUMPLIMIENTO PEDIDO (0-1+)
        // ===================================
        // ⬅️ NUEVO: Calcula botes buenos / cantidad solicitada
        int botesBuenos = order.getBotesBuenos() != null ? order.getBotesBuenos() : 0;
        int cantidadSolicitada = order.getCantidad() != null ? order.getCantidad() : 1; // Evitar división por cero

        float porCumpPedido = (float) botesBuenos / cantidadSolicitada;

        log.info("Orden {} - Cumplimiento del pedido: {:.2%} ({} botes buenos de {} solicitados)",
                order.getCodOrder(), porCumpPedido, botesBuenos, cantidadSolicitada);


        // ===================================
        // GUARDAR MÉTRICAS
        // ===================================
        Metricas metricas = Metricas.builder()
                .idOrder(order.getIdOrder())
                .tiempoTotal(tiempoTotal)
                .tiempoPausado(tiempoPausado)
                .tiempoActivo(tiempoActivo)
                .disponibilidad(disponibilidad)
                .rendimiento(rendimiento)
                .calidad(calidad)
                .oee(oee)
                .stdReal(stdReal)
                .porCumpPedido(porCumpPedido)  // ⬅️ NUEVO CAMPO
                .build();

        Metricas savedMetricas = metricasRepository.save(metricas);

        log.info("Métricas calculadas para orden {} - Tiempo Bruto: {:.2f}min - Pausas No Computables: {:.2f}min - Tiempo Total: {:.2f}min",
                order.getCodOrder(), tiempoBruto, tiempoPausasNoComputables, tiempoTotal);
        log.info("Orden {} - OEE: {:.2%} - Disponibilidad: {:.2%} - Rendimiento: {:.2%} - Calidad: {:.2%} - Cumplimiento: {:.2%}",
                order.getCodOrder(), oee, disponibilidad, rendimiento, calidad, porCumpPedido);

        return savedMetricas;
    }


    @Transactional
public Map<String, Object> recalcularTodasMetricas() {
    log.info("Recalculando métricas para todas las órdenes cerradas (FINALIZADA, ESPERA_MANUAL, PROCESO_MANUAL)");

    List<Order> orders = orderRepository.findAll();

    int total = 0;
    int recalculadas = 0;
    int saltadas = 0;
    List<Map<String, Object>> detalles = new ArrayList<>();

    for (Order order : orders) {
        // Solo órdenes aptas
        if (order.getEstado() != EstadoOrder.FINALIZADA
                && order.getEstado() != EstadoOrder.ESPERA_MANUAL
                && order.getEstado() != EstadoOrder.PROCESO_MANUAL) {
            continue;
        }

        total++;

        // Si no tiene inicio, no se puede
        if (order.getHoraInicio() == null) {
            saltadas++;
            Map<String, Object> det = new HashMap<>();
            det.put("idOrder", order.getIdOrder());
            det.put("codOrder", order.getCodOrder());
            det.put("status", "SKIPPED_NO_START_TIME");
            detalles.add(det);
            continue;
        }

        try {
            recalcularMetricas(order.getIdOrder());
            recalculadas++;

            Map<String, Object> det = new HashMap<>();
            det.put("idOrder", order.getIdOrder());
            det.put("codOrder", order.getCodOrder());
            det.put("status", "OK");
            detalles.add(det);

        } catch (Exception e) {
            saltadas++;
            Map<String, Object> det = new HashMap<>();
            det.put("idOrder", order.getIdOrder());
            det.put("codOrder", order.getCodOrder());
            det.put("status", "ERROR");
            det.put("error", e.getMessage());
            detalles.add(det);

            log.error("Error recalculando métricas para orden {} (ID {})", order.getCodOrder(), order.getIdOrder(), e);
        }
    }

    Map<String, Object> result = new HashMap<>();
    result.put("totalCandidatas", total);
    result.put("recalculadas", recalculadas);
    result.put("saltadas", saltadas);
    result.put("detalles", detalles);

    log.info("Recalculo métricas completado - totalCandidatas: {}, recalculadas: {}, saltadas: {}",
            total, recalculadas, saltadas);

    return result;
}



    /**
     * Recalcula las métricas de una orden (útil si se modifican datos en BD).
     *
     * <p>Este método ELIMINA las métricas existentes y las recalcula desde cero.
     * Útil cuando se modifican manualmente datos como pausas, botes, etc.</p>
     *
     * @param idOrder ID de la orden
     * @return Metricas recalculadas
     * @throws IllegalArgumentException si la orden no existe o no está finalizada
     */
    @Transactional
    public Metricas recalcularMetricas(Long idOrder) {
        log.info("Recalculando métricas para orden ID: {}", idOrder);

        // Buscar la orden
        Order order = orderRepository.findById(idOrder)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada con ID: " + idOrder));

        // Validar que la orden esté en un estado que permita métricas
        if (order.getEstado() != EstadoOrder.FINALIZADA &&
                order.getEstado() != EstadoOrder.ESPERA_MANUAL &&
                order.getEstado() != EstadoOrder.PROCESO_MANUAL) {
            throw new IllegalArgumentException(
                    "Solo se pueden recalcular métricas de órdenes FINALIZADAS, ESPERA_MANUAL o PROCESO_MANUAL. Estado actual: " + order.getEstado());
        }

        // Eliminar métricas existentes
        metricasRepository.deleteByIdOrder(idOrder);
        log.info("Métricas anteriores eliminadas para orden {}", order.getCodOrder());

        // Recalcular
        Metricas nuevasMetricas = calcularYGuardarMetricas(order);

        log.info("Métricas recalculadas exitosamente para orden {}", order.getCodOrder());

        return nuevasMetricas;
    }

    /**
     * Obtiene las métricas de una orden.
     *
     * @param idOrder ID de la orden
     * @return Metricas o null si no existen
     */
    @Transactional(readOnly = true)
    public Metricas getMetricasByOrder(Long idOrder) {
        return metricasRepository.findByIdOrder(idOrder).orElse(null);
    }

    /**
     * Calcula las métricas de una orden en tiempo real sin guardarlas.
     * Útil para mostrar datos en vivo de órdenes en proceso.
     *
     * @param order Orden en proceso o finalizada
     * @return Metricas calculadas (sin ID persistido)
     */
    @Transactional(readOnly = true)
    public Metricas calcularMetricasSimuladas(Order order) {
        // 1. Si ya existen métricas guardadas (orden finalizada), devolverlas
        if (metricasRepository.existsByIdOrder(order.getIdOrder())) {
            return metricasRepository.findByIdOrder(order.getIdOrder()).orElse(null);
        }

        // 2. Si no tiene hora de inicio, retornar métricas vacías
        if (order.getHoraInicio() == null) {
            return Metricas.builder()
                    .idOrder(order.getIdOrder())
                    .tiempoTotal(0f)
                    .tiempoPausado(0f)
                    .tiempoActivo(0f)
                    .disponibilidad(0f)
                    .rendimiento(0f)
                    .calidad(0f)
                    .oee(0f)
                    .stdReal(0f)
                    .porCumpPedido(0f)
                    .build();
        }

        // 3. Calcular métricas usando lógica similar a calcularYGuardarMetricas
        LocalDateTime horaFin = order.getHoraFin() != null ? order.getHoraFin() : LocalDateTime.now();

        // CÁLCULO 1: TIEMPO BRUTO
        Duration duracionBruta = Duration.between(order.getHoraInicio(), horaFin);
        float tiempoBruto = duracionBruta.toSeconds() / 60.0f;

        // CÁLCULO 2: PAUSAS NO COMPUTABLES
        Float tiempoPausasNoComputables = pauseRepository.getNonComputedPauseTimeByOrder(order.getIdOrder());
        if (tiempoPausasNoComputables == null) tiempoPausasNoComputables = 0.0f;

        // CÁLCULO 3: TIEMPO TOTAL
        float tiempoTotal = tiempoBruto - tiempoPausasNoComputables;

        // CÁLCULO 4: TIEMPO PAUSADO COMPUTABLE
        Float tiempoPausado = pauseRepository.getComputedPauseTimeByOrder(order.getIdOrder());
        if (tiempoPausado == null) tiempoPausado = 0.0f;

        // CÁLCULO 5: TIEMPO ACTIVO
        float tiempoActivo = tiempoTotal - tiempoPausado;
        if (tiempoActivo <= 0) tiempoActivo = 1.0f; // Evitar div/0

        // CÁLCULO 6: DISPONIBILIDAD
        float disponibilidad = tiempoTotal > 0 ? tiempoActivo / tiempoTotal : 0.0f;

        // CÁLCULO 7: RENDIMIENTO
        int totalProducido = (order.getBotesBuenos() != null ? order.getBotesBuenos() : 0) +
                (order.getBotesMalos() != null ? order.getBotesMalos() : 0);

        float produccionEsperada = tiempoActivo * order.getStdReferencia();
        float rendimiento = produccionEsperada > 0 ? totalProducido / produccionEsperada : 0.0f;

        // CÁLCULO 8: CALIDAD
        float calidad = totalProducido > 0 ?
                (float) (order.getBotesBuenos() != null ? order.getBotesBuenos() : 0) / totalProducido :
                0.0f;

        // CÁLCULO 9: OEE
        float oee = disponibilidad * rendimiento * calidad;

        // CÁLCULO 10: STD REAL
        float stdReal = totalProducido > 0 ? tiempoActivo / totalProducido : 0.0f;

        // CÁLCULO 11: CUMPLIMIENTO
        int botesBuenos = order.getBotesBuenos() != null ? order.getBotesBuenos() : 0;
        int cantidadSolicitada = order.getCantidad() != null ? order.getCantidad() : 1;
        float porCumpPedido = (float) botesBuenos / cantidadSolicitada;

        return Metricas.builder()
                .idOrder(order.getIdOrder())
                .tiempoTotal(tiempoTotal)
                .tiempoPausado(tiempoPausado)
                .tiempoActivo(tiempoActivo)
                .disponibilidad(disponibilidad)
                .rendimiento(rendimiento)
                .calidad(calidad)
                .oee(oee)
                .stdReal(stdReal)
                .porCumpPedido(porCumpPedido)
                .build();
    }
}
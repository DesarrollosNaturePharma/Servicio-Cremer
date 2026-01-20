package com.rnp.cremer.repository;

import com.rnp.cremer.model.Pause;
import com.rnp.cremer.model.TipoPausa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para operaciones de acceso a datos de Pause.
 *
 * @author RNP Team
 * @version 1.0
 * @since 2024-11-25
 */
@Repository
public interface PauseRepository extends JpaRepository<Pause, Long> {

    /**
     * Busca todas las pausas de una orden.
     */
    List<Pause> findByIdOrderOrderByHoraInicioDesc(Long idOrder);

    /**
     * Busca pausas activas (sin hora fin) de una orden.
     */
    @Query("SELECT p FROM Pause p WHERE p.idOrder = :idOrder AND p.horaFin IS NULL")
    Optional<Pause> findActivePauseByOrder(@Param("idOrder") Long idOrder);

    /**
     * Verifica si una orden tiene pausas activas.
     */
    @Query("SELECT COUNT(p) > 0 FROM Pause p WHERE p.idOrder = :idOrder AND p.horaFin IS NULL")
    boolean hasActivePause(@Param("idOrder") Long idOrder);

    /**
     * Cuenta pausas por tipo de una orden.
     */
    long countByIdOrderAndTipo(Long idOrder, TipoPausa tipo);

    /**
     * Calcula tiempo total de pausas de una orden.
     */
    @Query("SELECT COALESCE(SUM(p.tiempoTotalPausa), 0) FROM Pause p WHERE p.idOrder = :idOrder")
    Float getTotalPauseTimeByOrder(@Param("idOrder") Long idOrder);

    /**
     * Calcula tiempo total de pausas que computan de una orden.
     */
    @Query("SELECT COALESCE(SUM(p.tiempoTotalPausa), 0) FROM Pause p WHERE p.idOrder = :idOrder AND p.computa = true")
    Float getComputedPauseTimeByOrder(@Param("idOrder") Long idOrder);

    /**
     * Calcula tiempo total de pausas que NO computan de una orden.
     */
    @Query("SELECT COALESCE(SUM(p.tiempoTotalPausa), 0) FROM Pause p WHERE p.idOrder = :idOrder AND p.computa = false")
    Float getNonComputedPauseTimeByOrder(@Param("idOrder") Long idOrder);

    /**
     * Busca IDs de órdenes que tienen pausas activas de un tipo específico.
     */
    @Query("SELECT DISTINCT p.idOrder FROM Pause p WHERE p.tipo = :tipo AND p.horaFin IS NULL")
    List<Long> findOrderIdsWithActivePauseType(@Param("tipo") TipoPausa tipo);

/**
 * Encuentra todas las pausas activas excepto las de tipo FABRICACION_PARCIAL.
 */
@Query("""
    SELECT p FROM Pause p 
    WHERE p.horaFin IS NULL 
    AND (p.tipo IS NULL OR p.tipo <> 'FABRICACION_PARCIAL')
    ORDER BY p.horaInicio DESC
    """)
List<Pause> findActivePausesExcludingFabricacionParcial();

    /**
     * Elimina todas las pausas asociadas a una orden.
     */
    void deleteByIdOrder(Long idOrder);
}
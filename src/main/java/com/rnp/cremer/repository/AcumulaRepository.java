package com.rnp.cremer.repository;

import com.rnp.cremer.model.Acumula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para la gestión de datos de acumulación (proceso manual).
 *
 * @author RNP Team
 * @version 1.0
 * @since 2024-11-26
 */
@Repository
public interface AcumulaRepository extends JpaRepository<Acumula, Long> {

    /**
     * Busca el registro de acumulación asociado a una orden.
     *
     * @param idOrder ID de la orden
     * @return Optional con el registro de acumulación si existe
     */
    Optional<Acumula> findByIdOrder(Long idOrder);

    /**
     * Verifica si existe un registro de acumulación para una orden.
     *
     * @param idOrder ID de la orden
     * @return true si existe, false en caso contrario
     */
    boolean existsByIdOrder(Long idOrder);

    /**
     * Busca un proceso manual activo (sin hora de fin) para una orden.
     *
     * @param idOrder ID de la orden
     * @return Optional con el registro activo si existe
     */
    @Query("SELECT a FROM Acumula a WHERE a.idOrder = :idOrder AND a.horaFin IS NULL")
    Optional<Acumula> findActiveByIdOrder(@Param("idOrder") Long idOrder);

    /**
     * Verifica si existe un proceso manual activo para una orden.
     *
     * @param idOrder ID de la orden
     * @return true si hay proceso activo, false en caso contrario
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Acumula a WHERE a.idOrder = :idOrder AND a.horaFin IS NULL")
    boolean hasActiveProcess(Long idOrder);

    /**
     * Elimina el registro de acumulación de una orden.
     *
     * @param idOrder ID de la orden
     */
    void deleteByIdOrder(Long idOrder);
}
package com.rnp.cremer.repository;

import com.rnp.cremer.model.BottleCounter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para gestión de contadores de botellas.
 *
 * @author RNP Team
 * @version 1.0
 * @since 2024-12-12
 */
@Repository
public interface BottleCounterRepository extends JpaRepository<BottleCounter, Long> {

    /**
     * Busca el contador asociado a una orden específica.
     */
    Optional<BottleCounter> findByIdOrder(Long idOrder);

    /**
     * Verifica si existe un contador para una orden.
     */
    boolean existsByIdOrder(Long idOrder);

    /**
     * ✅ CORREGIDO: Obtiene el contador activo más reciente
     * En caso de múltiples activos, devuelve el más recientemente actualizado
     */
    @Query("SELECT bc FROM BottleCounter bc WHERE bc.isActive = true ORDER BY bc.lastUpdated DESC LIMIT 1")
    Optional<BottleCounter> findActiveCounter();

    /**
     * Desactiva todos los contadores activos.
     */
    @Modifying
    @Query("UPDATE BottleCounter bc SET bc.isActive = false WHERE bc.isActive = true")
    void deactivateAllCounters();
}

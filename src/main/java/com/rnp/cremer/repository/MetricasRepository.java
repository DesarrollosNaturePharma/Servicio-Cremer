package com.rnp.cremer.repository;

import com.rnp.cremer.model.Metricas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para Metricas.
 *
 * @author RNP Team
 * @version 1.0
 * @since 2024-11-26
 */
@Repository
public interface MetricasRepository extends JpaRepository<Metricas, Long> {

    /**
     * Busca métricas por ID de orden.
     */
    Optional<Metricas> findByIdOrder(Long idOrder);

    /**
     * Verifica si existen métricas para una orden.
     */
    boolean existsByIdOrder(Long idOrder);

    /**
     * Elimina métricas por ID de orden.
     */
    void deleteByIdOrder(Long idOrder);


}
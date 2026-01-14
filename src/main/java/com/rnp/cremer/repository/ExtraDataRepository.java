package com.rnp.cremer.repository;

import com.rnp.cremer.model.ExtraData;
import com.rnp.cremer.model.Metricas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para ExtraData.
 *
 * @author RNP Team
 * @version 1.0
 * @since 2024-11-26
 */
@Repository
public interface ExtraDataRepository extends JpaRepository<ExtraData, Long> {

    /**
     * Busca ExtraData por ID de orden.
     */
    Optional<ExtraData> findByIdOrder(Long idOrder);

    /**
     * Verifica si existe ExtraData para una orden.
     */
    boolean existsByIdOrder(Long idOrder);

    /**
     * Elimina ExtraData por ID de orden.
     */
    void deleteByIdOrder(Long idOrder);

}
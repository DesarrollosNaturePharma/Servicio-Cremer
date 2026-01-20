package com.rnp.cremer.repository;

import com.rnp.cremer.model.OrderDeleteAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio para operaciones de acceso a datos de auditoría de eliminación de órdenes.
 *
 * @author RNP Team
 * @version 1.0
 * @since 2024-11-26
 */
@Repository
public interface OrderDeleteAuditRepository extends JpaRepository<OrderDeleteAudit, Long> {

    /**
     * Busca registros de auditoría por el usuario que eliminó.
     */
    Page<OrderDeleteAudit> findByDeletedByContainingIgnoreCase(String deletedBy, Pageable pageable);

    /**
     * Busca registros de auditoría por código de orden.
     */
    List<OrderDeleteAudit> findByCodOrderContainingIgnoreCase(String codOrder);

    /**
     * Busca registros de auditoría en un rango de fechas.
     */
    Page<OrderDeleteAudit> findByDeletedAtBetween(LocalDateTime inicio, LocalDateTime fin, Pageable pageable);

    /**
     * Busca registros de auditoría por múltiples filtros.
     */
    @Query("""
        SELECT a FROM OrderDeleteAudit a
        WHERE (:deletedBy IS NULL OR :deletedBy = '' OR LOWER(a.deletedBy) LIKE CONCAT('%', LOWER(:deletedBy), '%'))
          AND (:codOrder IS NULL OR :codOrder = '' OR LOWER(a.codOrder) LIKE CONCAT('%', LOWER(:codOrder), '%'))
          AND (:fechaInicio IS NULL OR a.deletedAt >= :fechaInicio)
          AND (:fechaFin IS NULL OR a.deletedAt <= :fechaFin)
        ORDER BY a.deletedAt DESC
        """)
    Page<OrderDeleteAudit> findByFilters(
            @Param("deletedBy") String deletedBy,
            @Param("codOrder") String codOrder,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            Pageable pageable
    );

    /**
     * Obtiene todos los registros ordenados por fecha de eliminación descendente.
     */
    Page<OrderDeleteAudit> findAllByOrderByDeletedAtDesc(Pageable pageable);

    /**
     * Cuenta eliminaciones realizadas por un usuario.
     */
    long countByDeletedBy(String deletedBy);

    /**
     * Busca el historial de eliminación de una orden específica por su ID original.
     */
    List<OrderDeleteAudit> findByIdOrderDeleted(Long idOrderDeleted);
}

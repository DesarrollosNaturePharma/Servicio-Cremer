package com.rnp.cremer.repository;

import com.rnp.cremer.model.EstadoOrder;
import com.rnp.cremer.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para operaciones de acceso a datos de Order.
 *
 * @author RNP Team
 * @version 1.0
 * @since 2024-11-25
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Busca una orden por su código único.
     */
    Optional<Order> findByCodOrder(String codOrder);

    /**
     * Verifica si existe una orden con el código dado.
     */
    boolean existsByCodOrder(String codOrder);

    /**
     * Busca órdenes por estado.
     */
    Page<Order> findByEstado(EstadoOrder estado, Pageable pageable);

    /**
     * Busca órdenes por operario.
     */
    Page<Order> findByOperarioContainingIgnoreCase(String operario, Pageable pageable);

    /**
     * Busca órdenes por lote.
     */
    Page<Order> findByLoteContainingIgnoreCase(String lote, Pageable pageable);

    /**
     * Busca órdenes por artículo.
     */
    Page<Order> findByArticuloContainingIgnoreCase(String articulo, Pageable pageable);

    /**
     * Busca órdenes creadas en un rango de fechas.
     */
    Page<Order> findByHoraCreacionBetween(LocalDateTime inicio, LocalDateTime fin, Pageable pageable);

    /**
     * Busca todas las órdenes ordenadas por fecha de creación descendente.
     */
    Page<Order> findAllByOrderByHoraCreacionDesc(Pageable pageable);

    /**
     * Busca órdenes por múltiples filtros (query compleja).
     */
    @Query("""
SELECT o FROM Order o
WHERE (:estado IS NULL OR o.estado = :estado)
  AND (:operario IS NULL OR :operario = '' OR LOWER(o.operario) LIKE CONCAT('%', :operario, '%'))
  AND (:lote IS NULL OR :lote = '' OR LOWER(o.lote) LIKE CONCAT('%', :lote, '%'))
  AND (:articulo IS NULL OR :articulo = '' OR LOWER(o.articulo) LIKE CONCAT('%', :articulo, '%'))
""")
    Page<Order> findByFilters(
            @Param("estado") EstadoOrder estado,
            @Param("operario") String operario,
            @Param("lote") String lote,
            @Param("articulo") String articulo,
            Pageable pageable
    );



    /**
     * Cuenta órdenes por estado.
     */
    long countByEstado(EstadoOrder estado);

    /**
     * Busca órdenes en los estados especificados.
     */
    List<Order> findByEstadoIn(List<EstadoOrder> estados);
}
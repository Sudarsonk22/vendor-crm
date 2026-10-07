package com.example.vendor_crm.repository;

import com.example.vendor_crm.model.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    // locks the row so two people changing stock at the same time can't overwrite each other
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    @Query("select p from Product p where p.stock <= p.lowStockThreshold " +
            "and p.status = com.example.vendor_crm.model.ProductStatus.ACTIVE order by p.stock asc")
    List<Product> findLowStock();
}
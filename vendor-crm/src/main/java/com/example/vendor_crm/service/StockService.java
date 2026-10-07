package com.example.vendor_crm.service;

import com.example.vendor_crm.dto.PageResponse;
import com.example.vendor_crm.dto.StockMovementResponse;
import com.example.vendor_crm.dto.StockRequest;
import com.example.vendor_crm.exception.BadRequestException;
import com.example.vendor_crm.exception.NotFoundException;
import com.example.vendor_crm.model.MovementType;
import com.example.vendor_crm.model.Product;
import com.example.vendor_crm.model.ProductStatus;
import com.example.vendor_crm.model.StockMovement;
import com.example.vendor_crm.repository.ProductRepository;
import com.example.vendor_crm.repository.StockMovementRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StockService {

    private final ProductRepository productRepository;
    private final StockMovementRepository movementRepository;

    public StockService(ProductRepository productRepository, StockMovementRepository movementRepository) {
        this.productRepository = productRepository;
        this.movementRepository = movementRepository;
    }

    @Transactional
    public StockMovementResponse stockIn(Long productId, StockRequest req, String user) {
        if (req.getQuantity() < 1) {
            throw new BadRequestException("Quantity must be at least 1");
        }
        Product product = getForUpdate(productId);
        int before = product.getStock();
        product.setStock(before + req.getQuantity());
        return saveMovement(product, MovementType.STOCK_IN, req.getQuantity(), before, req.getReason(), user);
    }

    @Transactional
    public StockMovementResponse stockOut(Long productId, StockRequest req, String user) {
        if (req.getQuantity() < 1) {
            throw new BadRequestException("Quantity must be at least 1");
        }
        Product product = getForUpdate(productId);
        int before = product.getStock();
        if (req.getQuantity() > before) {
            throw new BadRequestException("Not enough stock. Available: " + before);
        }
        product.setStock(before - req.getQuantity());
        return saveMovement(product, MovementType.STOCK_OUT, req.getQuantity(), before, req.getReason(), user);
    }

    // quantity here is the new total count, not the change
    @Transactional
    public StockMovementResponse adjust(Long productId, StockRequest req, String user) {
        if (req.getReason() == null || req.getReason().isBlank()) {
            throw new BadRequestException("Reason is required for an adjustment");
        }
        Product product = getForUpdate(productId);
        int before = product.getStock();
        int newCount = req.getQuantity();
        if (newCount == before) {
            throw new BadRequestException("Stock is already " + before);
        }
        product.setStock(newCount);
        return saveMovement(product, MovementType.ADJUSTMENT, Math.abs(newCount - before), before, req.getReason(), user);
    }

    public PageResponse<StockMovementResponse> history(Long productId, int page, int size) {
        if (page < 0) {
            throw new BadRequestException("Page cannot be negative");
        }
        if (size < 1 || size > 100) {
            throw new BadRequestException("Page size must be between 1 and 100");
        }

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<StockMovement> result;
        if (productId != null) {
            if (!productRepository.existsById(productId)) {
                throw new NotFoundException("Product not found with id " + productId);
            }
            result = movementRepository.findByProductId(productId, pageable);
        } else {
            result = movementRepository.findAll(pageable);
        }

        List<StockMovementResponse> items = result.getContent().stream().map(this::toResponse).toList();
        return new PageResponse<>(items, page, size, result.getTotalElements(), result.getTotalPages());
    }

    private Product getForUpdate(Long productId) {
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new NotFoundException("Product not found with id " + productId));
        if (product.getStatus() == ProductStatus.INACTIVE) {
            throw new BadRequestException("This product is inactive, stock cannot be changed");
        }
        return product;
    }

    private StockMovementResponse saveMovement(Product product, MovementType type, int quantity,
                                               int before, String reason, String user) {
        productRepository.save(product);

        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setType(type);
        movement.setQuantity(quantity);
        movement.setStockBefore(before);
        movement.setStockAfter(product.getStock());
        movement.setReason(reason);
        movement.setDoneBy(user);
        return toResponse(movementRepository.save(movement));
    }

    private StockMovementResponse toResponse(StockMovement m) {
        StockMovementResponse r = new StockMovementResponse();
        r.setId(m.getId());
        r.setProductId(m.getProduct().getId());
        r.setProductName(m.getProduct().getName());
        r.setSku(m.getProduct().getSku());
        r.setType(m.getType());
        r.setQuantity(m.getQuantity());
        r.setStockBefore(m.getStockBefore());
        r.setStockAfter(m.getStockAfter());
        r.setReason(m.getReason());
        r.setDoneBy(m.getDoneBy());
        r.setCreatedAt(m.getCreatedAt());
        return r;
    }
}
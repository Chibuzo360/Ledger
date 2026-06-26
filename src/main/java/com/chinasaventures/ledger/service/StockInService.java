package com.chinasaventures.ledger.service;

import com.chinasaventures.ledger.model.Product;
import com.chinasaventures.ledger.model.StockIn;
import com.chinasaventures.ledger.repository.ProductVariantsRepository;
import com.chinasaventures.ledger.repository.StockInRepository;
import com.chinasaventures.ledger.repository.ProductRepository;
import com.chinasaventures.ledger.model.ProductVariants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor

public class StockInService {
    private final StockInRepository stockInRepository;
    private final ProductRepository productRepository;
    private final ProductVariantsRepository productVariantsRepository;

    public List<StockIn> getAllStockIn() {

        return stockInRepository.findAll();
    }

    public StockIn getStockInById(Long id){
        return stockInRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("StockIn not found with id: "+ id));
    }

    public StockIn addStockIn(StockIn stockIn) {
        StockIn saved = stockInRepository.save(stockIn);

        if (saved.getProductVariant() != null) {
            // product has variants — update variant stock
            ProductVariants variant = saved.getProductVariant();
            variant.setCurrentStock(variant.getCurrentStock() + saved.getQuantity());
            productVariantsRepository.save(variant);
        } else {
            // product has no variants — update product stock directly
            Product product = saved.getProduct();
            product.setCurrentStock(product.getCurrentStock() + saved.getQuantity());
            productRepository.save(product);
        }

        return saved;
    }// I renamed this from "createStockIn to addStockIn"

    public void deleteStockIn(Long id){

        stockInRepository.deleteById(id);
    }
}

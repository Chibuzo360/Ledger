package com.chinasaventures.ledger.service;

import com.chinasaventures.ledger.model.Product;
import com.chinasaventures.ledger.model.StockIn;
import com.chinasaventures.ledger.repository.StockInRepository;
import com.chinasaventures.ledger.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor

public class StockInService {
    private final StockInRepository stockInRepository;
    private final ProductRepository productRepository;

    public List<StockIn> getAllStockIn() {
        return stockInRepository.findAll();
    }

    public StockIn getStockInById(Long id){
        return stockInRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("StockIn not found with id: "+ id));
    }

    public StockIn createStockIn(StockIn stockIn){
        StockIn saved = stockInRepository.save(stockIn);

        Product product = saved.getProduct();
        product.setCurrentStock(product.getCurrentStock() + saved.getQuantity());
        productRepository.save(product);

        return saved;
    }

    public void deleteStockIn(Long id){
        stockInRepository.deleteById(id);
    }
}

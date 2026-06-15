//package com.chinasaventures.ledger.service;
//
//import com.chinasaventures.ledger.model.BranchStock;
//import com.chinasaventures.ledger.repository.BranchStockRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//public class BranchStockService {
//
//    private final BranchStockRepository branchStockRepository;
//
//    // get stock for one product in one branch
//    public BranchStock getBranchStock(Long productId, Long branchId) {
//        return branchStockRepository.findByProductIdAndBranchId(productId, branchId)
//                .orElseThrow(() -> new RuntimeException("Stock not found for this product and branch"));
//    }
//
//    // get all stock for a branch — what workers see
//    public List<BranchStock> getStockByBranch(Long branchId) {
//        return branchStockRepository.findByBranchId(branchId);
//    }
//
//    // get total stock for a product across all branches — what director sees
//    public Integer getTotalStock(Long productId) {
//        Integer total = branchStockRepository.sumQuantityByProductId(productId);
//        return total != null ? total : 0;
//    }
//
//    // get stock per branch for a product — director's breakdown view
//    public List<BranchStock> getStockByProduct(Long productId) {
//        return branchStockRepository.findByProductId(productId);
//    }
//}
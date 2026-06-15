//package com.chinasaventures.ledger.repository;
//
//import com.chinasaventures.ledger.model.BranchStock;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//import java.util.Optional;
//
//@Repository
//public interface BranchStockRepository extends JpaRepository<BranchStock, Long> {
//
//    // get stock for a specific product in a specific branch
//    Optional<BranchStock> findByProductIdAndBranchId(Long productId, Long branchId);
//
//    // get all stock for a specific branch
//    List<BranchStock> findByBranchId(Long branchId);
//
//    // get all stock for a specific product across all branches
//    List<BranchStock> findByProductId(Long productId);
//
//    // calculate total stock for a product across all branches
//    @Query("SELECT SUM(b.quantity) FROM BranchStock b WHERE b.product.id = :productId")
//    Integer sumQuantityByProductId(Long productId);
//}
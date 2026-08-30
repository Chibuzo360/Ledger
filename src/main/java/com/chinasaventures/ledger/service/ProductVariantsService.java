package com.chinasaventures.ledger.service;


import com.chinasaventures.ledger.model.ProductVariants;
import com.chinasaventures.ledger.model.Users;
import com.chinasaventures.ledger.repository.ProductVariantsRepository;
import com.chinasaventures.ledger.dto.ProductVariantSummaryDTO;
import com.chinasaventures.ledger.repository.UsersRepository;
import com.chinasaventures.ledger.dto.ProductCategoryDTO;
import com.chinasaventures.ledger.dto.ProductSummaryDTO;
import com.chinasaventures.ledger.model.ProductCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductVariantsService {
    private final ProductVariantsRepository productVariantsRepository;
    private final UsersRepository usersRepository;

    public ProductCategory safeCategoryReturner (ProductVariants P){
        ProductCategory productCategory;
        if (P.getProduct() != null && P.getProduct().getCategory() !=null ) {
             productCategory = P.getProduct().getCategory();
        } else{
            productCategory = null;
        }
        return productCategory;
    }

    public ProductVariantSummaryDTO toDTO(ProductVariants pV){
        ProductCategoryDTO productCategory = safeCategoryReturner(pV) != null
                ? new ProductCategoryDTO(
                safeCategoryReturner(pV).getId(),
                safeCategoryReturner(pV).getName())
                : null;
        ProductSummaryDTO product = pV.getProduct() != null
                ? new ProductSummaryDTO(
                        pV.getProduct().getId(),
                pV.getProduct().getName(),
                pV.getProduct().getPricePerUnit(),
                productCategory,
                pV.getProduct().getCurrentStock()
                )
                : null;
        return new ProductVariantSummaryDTO(
                pV.getId(),product,pV.getPricePerUnit(),pV.getSize(),pV.getProducer(),pV.getCurrentStock()
        );
    }

    public List<ProductVariantSummaryDTO> getAllProductVariants() {

        return productVariantsRepository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public ProductVariants getProductVariantsById(Long id){
        return productVariantsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: "+ id));
    }

    public ProductVariantSummaryDTO getProductVariantsDTOById(Long id){return toDTO(getProductVariantsById(id));}

    public ProductVariantSummaryDTO createProductVariant(ProductVariants productVariant){
        return toDTO(productVariantsRepository.save(productVariant));
    }

    public List<ProductVariantSummaryDTO> getVariantsByProductId(Long productId) {
        return productVariantsRepository.findByProductId(productId)
                .stream()
                .map(this::toDTO)
                .toList();
    }


    public ProductVariantSummaryDTO updateProductVariant(Long id, ProductVariants updatedProductVariant){
        ProductVariants existing = getProductVariantsById(id);
        existing.setProduct(updatedProductVariant.getProduct());
        existing.setPricePerUnit(updatedProductVariant.getPricePerUnit());
        existing.setCurrentStock(updatedProductVariant.getCurrentStock());
        existing.setSize(updatedProductVariant.getSize());
        existing.setProducer(updatedProductVariant.getProducer());
        ProductVariants upToDateProductVariant = productVariantsRepository.save(existing);
        return toDTO(upToDateProductVariant);
    }

    public void deleteProductVariant(Long id){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String identifier = auth.getName();

        Users currentUser = usersRepository.findByEmailOrPhoneNumber(identifier, identifier)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found: " + identifier));
        //  look up the Users via usersRepository.findByEmailOrPhoneNumber(...)
        // if currentUser's role isn't "director", throw
        //         ResponseStatusException(HttpStatus.FORBIDDEN, "...")

        if(!"director".equals(currentUser.getRole())){
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Only a director can delete a ProductVariant");
        }
        productVariantsRepository.deleteById(id);
    }
}

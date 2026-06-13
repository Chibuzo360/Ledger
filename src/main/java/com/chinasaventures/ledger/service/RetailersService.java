package com.chinasaventures.ledger.service;

import com.chinasaventures.ledger.model.Expenses;
import com.chinasaventures.ledger.model.Retailers;
import com.chinasaventures.ledger.repository.ExpensesRepository;
import com.chinasaventures.ledger.repository.RetailersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.util.List;

@Service
@RequiredArgsConstructor
public class RetailersService {
    private final RetailersRepository retailersRepository;

    public List<Retailers> getAllRetailers() {
        return retailersRepository.findAll();
    }

    public Retailers getRetailerById(Long id){
        return retailersRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Retailer's record not found with id: "+ id));
    }

    public Retailers createRetailer(Retailers retailer){
        return retailersRepository.save(retailer);
    }

    public Retailers updateRetailers(Long id, Retailers updatedRetailers){
        Retailers existing = getRetailerById(id);
        existing.setBusinessName(updatedRetailers.getBusinessName());
        existing.setContactName(updatedRetailers.getContactName());
        existing.setPhone(updatedRetailers.getPhone());
        existing.setTotalOwed(updatedRetailers.getTotalOwed());
        existing.setCreditLimit(updatedRetailers.getCreditLimit());// this might be removed later
        return retailersRepository.save(existing);
    }

    public void deleteRetailer(Long id){
        retailersRepository.deleteById(id);
    }
}

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
        existing.setBalance(updatedRetailers.getBalance());
        existing.setCreditLimit(updatedRetailers.getCreditLimit());// this might be removed later
        return retailersRepository.save(existing);
        // if retailers balance is positive and not 0, it means we owe them. if its negative, it means they owe us.
        //i need to add a "balance details" this describes what the retailer bought/what owe the retailer
        // we need a new "Product owed" column that will track that, thisd will be in later versions
        // orderdetails table
        // The next version(if any), will have a feature that auto-calculates retailers balance from the transactions record
    }

    public void deleteRetailer(Long id){
        retailersRepository.deleteById(id);
    }
}

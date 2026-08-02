package com.chinasaventures.ledger.service;

import com.chinasaventures.ledger.model.Expenses;
import com.chinasaventures.ledger.model.Retailers;
import com.chinasaventures.ledger.model.Users;
import com.chinasaventures.ledger.repository.ExpensesRepository;
import com.chinasaventures.ledger.repository.UsersRepository;
import com.chinasaventures.ledger.repository.RetailersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;


import java.util.List;

@Service
@RequiredArgsConstructor
public class RetailersService {
    private final RetailersRepository retailersRepository;
    private final UsersRepository usersRepository;

    public List<Retailers> getAllRetailers() {
        return retailersRepository.findAll();
    }

    public Retailers getRetailerById(Long id){
        return retailersRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Retailer's record not found with id: "+ id));
    }

    public Retailers createRetailer(Retailers retailer){

        //gets Authentication from SecurityContextHolder i will need to go learn more about context holders
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String identifier = auth.getName();

        // extracts identifier via auth.getName()
        Users currentUser = usersRepository.findByEmailOrPhoneNumber(identifier, identifier)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found: " + identifier));

        retailer.setBranch(currentUser.getBranch());

        return retailersRepository.save(retailer);
    }

    public Retailers updateRetailers(Long id, Retailers updatedRetailers){

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String identifier = auth.getName();

        Users currentUser = usersRepository.findByEmailOrPhoneNumber(identifier, identifier)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found: " + identifier));

        Retailers existing = getRetailerById(id);
        existing.setBusinessName(updatedRetailers.getBusinessName());
        existing.setContactName(updatedRetailers.getContactName());
        existing.setPhone(updatedRetailers.getPhone());
        existing.setBalance(updatedRetailers.getBalance());
        existing.setBranch(currentUser.getBranch());
        existing.setCreditLimit(updatedRetailers.getCreditLimit());// this might be removed later
        return retailersRepository.save(existing);
        // if retailers balance is positive and not 0, it means we owe them. if its negative, it means they owe us.
        //i need to add a "balance details" this describes what the retailer bought/what owe the retailer
        // The product owed column will be a source of extra detail in this version.
        // The next version(if any), will have a feature that auto-calculates retailers balance from the transactions record
        //Retailers Branch should be edited as it tells us where the retailer bought from in the last transaction
    }

    public void deleteRetailer(Long id){

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String identifier = auth.getName();

        Users currentUser = usersRepository.findByEmailOrPhoneNumber(identifier, identifier)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found: " + identifier));

        if(!"director".equals(currentUser.getRole())){
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Only a director can delete a Retailer's account"
            );
        }
        retailersRepository.deleteById(id);
    }
}

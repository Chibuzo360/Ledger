package com.chinasaventures.ledger.service;

import com.chinasaventures.ledger.model.Expenses;
import com.chinasaventures.ledger.model.Retailers;
import com.chinasaventures.ledger.model.Users;
import com.chinasaventures.ledger.repository.ExpensesRepository;
import com.chinasaventures.ledger.repository.UsersRepository;
import com.chinasaventures.ledger.repository.RetailersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String identifier = auth.getName();

        Users currentUser = usersRepository.findByEmailOrPhoneNumber(identifier, identifier)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found: " + identifier));

        retailer.setBranch(currentUser.getBranch());

        return retailersRepository.save(retailer);
    }

    // CHANGED: this method had NO role check at all before -- any
    // logged-in user (worker or director) could edit any retailer's
    // details, including creditLimit and balance. Same gap shape as
    // TransactionsService.confirmPayment() had before it was fixed. Now
    // director-only, same enforcement pattern used everywhere else in
    // this codebase (checked in the service, not via @PreAuthorize).
    // Applied to the WHOLE update, not just name/contact/phone -- letting
    // a worker edit creditLimit or balance while name/contact/phone are
    // locked would be a stranger, less safe rule than locking everything
    // behind the same director check.
    public Retailers updateRetailers(Long id, Retailers updatedRetailers){

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String identifier = auth.getName();

        Users currentUser = usersRepository.findByEmailOrPhoneNumber(identifier, identifier)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found: " + identifier));

        if (!"director".equals(currentUser.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only a director can edit a retailer's details.");
        }

        Retailers existing = getRetailerById(id);
        existing.setBusinessName(updatedRetailers.getBusinessName());
        existing.setContactName(updatedRetailers.getContactName());
        existing.setPhone(updatedRetailers.getPhone());
        existing.setBalance(updatedRetailers.getBalance());
        existing.setCreditLimit(updatedRetailers.getCreditLimit());
        return retailersRepository.save(existing);
        // if retailers balance is positive and not 0, it means we owe them. if its negative, it means they owe us.
        //i need to add a "balance details" this describes what the retailer bought/what owe the retailer
        // The product owed column will be a source of extra detail in this version.
        // The next version(if any), will have a feature that auto-calculates retailers balance from the transactions record
        //Retailers Branch now tells us the home branch of the retailer. this maybe later utilized for some restrictive activities.
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
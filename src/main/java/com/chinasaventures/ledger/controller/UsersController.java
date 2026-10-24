package com.chinasaventures.ledger.controller;

import com.chinasaventures.ledger.dto.UserResponseDTO;
import com.chinasaventures.ledger.model.Users;
import com.chinasaventures.ledger.service.UsersService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UsersController {

    private final UsersService usersService;

    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> getAllUsers(){
        return ResponseEntity.ok(usersService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUsersById(@PathVariable Long id){
        return ResponseEntity.ok(usersService.getUserDTOById(id));
    }

    @PostMapping
    public ResponseEntity<UserResponseDTO> createUser(@RequestBody Users user){
        return ResponseEntity.ok(usersService.createUserAsDirector(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateUsers(@PathVariable Long id, @RequestBody Users updatedUser){
        return ResponseEntity.ok(usersService.updateUsers(id, updatedUser));
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<UserResponseDTO> updatePassword(@PathVariable Long id, @RequestBody String newPassword){
        return ResponseEntity.ok(usersService.updatePassword(id, newPassword));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id){
        usersService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
package com.chinasaventures.ledger.controller;

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
    public ResponseEntity<List<Users>> getAllUsers(){
        return ResponseEntity.ok(usersService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Users> getUsersById(@PathVariable Long id){
        return ResponseEntity.ok(usersService.getUserById(id));
    }

    @PostMapping
    public ResponseEntity<Users> createUser(@RequestBody Users user){
        return ResponseEntity.ok(usersService.createUser(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Users> updateUsers(@PathVariable Long id, @RequestBody Users updatedUser){
        return ResponseEntity.ok(usersService.updateUsers(id,updatedUser));
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<Users> updatePassword( @PathVariable Long id, @RequestBody String newPassword){
        return ResponseEntity.ok(usersService.updatePassword(id, newPassword));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id){
        usersService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}

//everything concerning branchstock can wait for now.  maybe after i finish building the core parts i would add it. i only  wrote the model,repo and controller, then commented the codes in it. i didnt modify any file to connect it, i will do those later

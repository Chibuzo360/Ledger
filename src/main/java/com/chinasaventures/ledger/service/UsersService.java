package com.chinasaventures.ledger.service;

import com.chinasaventures.ledger.model.Users;
import com.chinasaventures.ledger.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsersService {

        private final UsersRepository userRepository;

        public List<Users> getAllUsers() {
            return userRepository.findAll();
        }

        public Users getUserById(Long id){
            return userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User with id: "+ id+", was not found"));
        }

        public Users createUser(Users user){
            return userRepository.save(user);
        }

        public Users updateUsers(Long id, Users updatedUsers){
            Users existing = getUserById(id);
            existing.setName(updatedUsers.getName());
            existing.setRole(updatedUsers.getRole());
//            existing.setPhoneNumber(updatedPhoneNumber.getPhoneNumber());
//            existing.setBranch(updatedBranch.getUpdatedBranch);
            return userRepository.save(existing);
        }

        public void deleteUser(Long id){
            userRepository.deleteById(id);
        }

        public Users updatePassword (Long id, String newPassword){
            Users existing = getUserById(id);
            existing.setPassword(newPassword);
            return userRepository.save(existing);

        }

}

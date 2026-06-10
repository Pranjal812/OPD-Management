package com.smart.demo.repository;

import com.smart.demo.model.User;
import com.smart.demo.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmailAndPassword(String email, String password);
    Optional<User> findByUsernameAndPassword(String username, String password);
    Optional<User> findByEmailOrUsername(String email, String username);
    List<User> findByRole(Role role);
    long countByRole(Role role);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByContactNumber(String contactNumber);
}
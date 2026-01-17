package com.tlat.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tlat.Entity.User;
public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
}
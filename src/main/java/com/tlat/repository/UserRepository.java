package com.tlat.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tlat.entity.User;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
    List<User> findAllByRoles_Name(String roleName);
    long countByStudentGroup_Id(Long groupId);
}

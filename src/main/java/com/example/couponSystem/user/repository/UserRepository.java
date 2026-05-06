package com.example.couponSystem.user.repository;

import com.example.couponSystem.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}

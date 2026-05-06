package com.logic.Repository;

import com.logic.entity.Guest;
import com.logic.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuestRepository extends JpaRepository<Guest, Long> {
    List<Guest> findByUserId(Long userId);

    List<Guest> findByUser(User user);
}

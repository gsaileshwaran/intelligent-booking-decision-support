package com.booking.intelligent.repository;

import com.booking.intelligent.entity.Theatre;
import com.booking.intelligent.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TheatreRepository extends JpaRepository<Theatre, Long> {
    List<Theatre> findByOwnerUser(User ownerUser);
    List<Theatre> findByOwnerUserUserId(Long ownerUserId);
}

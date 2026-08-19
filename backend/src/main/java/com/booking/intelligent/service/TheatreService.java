package com.booking.intelligent.service;

import com.booking.intelligent.entity.Theatre;
import com.booking.intelligent.entity.User;
import com.booking.intelligent.enums.TheatreStatus;
import com.booking.intelligent.exception.ResourceNotFoundException;
import com.booking.intelligent.repository.TheatreRepository;
import com.booking.intelligent.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TheatreService {

    @Autowired
    private TheatreRepository theatreRepository;

    @Autowired
    private UserRepository userRepository;

    public List<Theatre> getAllTheatres() {
        return theatreRepository.findAll();
    }

    public List<Theatre> getTheatresByOwner(Long ownerUserId) {
        return theatreRepository.findByOwnerUserUserId(ownerUserId);
    }

    public Theatre getTheatreById(Long theatreId) {
        return theatreRepository.findById(theatreId)
                .orElseThrow(() -> new ResourceNotFoundException("Theatre", "id", theatreId));
    }

    @Transactional
    public Theatre createTheatre(Theatre theatre, Long ownerUserId) {
        User ownerUser = userRepository.findById(ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", ownerUserId));

        theatre.setOwnerUser(ownerUser);
        if (theatre.getStatus() == null) {
            theatre.setStatus(TheatreStatus.ACTIVE);
        }
        return theatreRepository.save(theatre);
    }
}

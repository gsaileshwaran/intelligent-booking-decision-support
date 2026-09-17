package com.pvk.cinemas.search.repository;

import com.pvk.cinemas.search.model.SearchIndexDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SearchIndexDocumentRepository extends JpaRepository<SearchIndexDocument, Long> {
    Optional<SearchIndexDocument> findByEntityTypeAndEntityId(String entityType, Long entityId);

    @Query("SELECT d FROM SearchIndexDocument d WHERE d.status = :status")
    List<SearchIndexDocument> findByDocumentStatus(@Param("status") String documentStatus);

    List<SearchIndexDocument> findByStatus(String status);
}

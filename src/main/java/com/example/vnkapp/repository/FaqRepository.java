package com.example.vnkapp.repository;

import com.example.vnkapp.entity.BaseEntity;
import com.example.vnkapp.entity.Faq;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FaqRepository extends JpaRepository<Faq, UUID> {

    Optional<Faq> findByIdAndStatus(UUID id, Integer status);

    default Optional<Faq> findByIdAndStatusActive(UUID id) {
        return findByIdAndStatus(id, BaseEntity.STATUS_ACTIVE);
    }

    List<Faq> findByStatusOrderByDisplayOrderAsc(Integer status);

    default List<Faq> findAllActive() {
        return findByStatusOrderByDisplayOrderAsc(BaseEntity.STATUS_ACTIVE);
    }

    List<Faq> findByStatusOrderByDisplayOrderAsc(Integer status, Pageable pageable);

    default List<Faq> findTopActive(int limit) {
        return findByStatusOrderByDisplayOrderAsc(BaseEntity.STATUS_ACTIVE, PageRequest.of(0, limit));
    }
}

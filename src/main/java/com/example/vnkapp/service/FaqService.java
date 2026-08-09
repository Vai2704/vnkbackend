package com.example.vnkapp.service;

import com.example.vnkapp.dto.faq.FaqCreateRequestDto;
import com.example.vnkapp.dto.faq.FaqResponseDto;
import com.example.vnkapp.dto.faq.FaqUpdateRequestDto;
import com.example.vnkapp.entity.BaseEntity;
import com.example.vnkapp.entity.Faq;
import com.example.vnkapp.repository.FaqRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class FaqService {

    private static final Logger log = LoggerFactory.getLogger(FaqService.class);

    private final FaqRepository faqRepository;

    public FaqService(FaqRepository faqRepository) {
        this.faqRepository = faqRepository;
    }

    @Transactional
    public FaqResponseDto createFaq(FaqCreateRequestDto dto) {
        log.debug("Creating FAQ: {}", dto.question());

        Faq faq = Faq.builder()
                .question(dto.question())
                .answer(dto.answer())
                .displayOrder(dto.displayOrder() != null ? dto.displayOrder() : 0)
                .build();

        Faq savedFaq = faqRepository.save(faq);
        log.info("FAQ created: {}", savedFaq.getId());
        return FaqResponseDto.fromEntity(savedFaq);
    }

    @Transactional
    public FaqResponseDto updateFaq(UUID faqId, FaqUpdateRequestDto dto) {
        log.debug("Updating FAQ: {}", faqId);
        Faq faq = faqRepository.findByIdAndStatusActive(faqId)
                .orElseThrow(() -> {
                    log.warn("FAQ not found: {}", faqId);
                    return new IllegalArgumentException("FAQ not found");
                });

        if (dto.question() != null && !dto.question().isBlank()) {
            faq.setQuestion(dto.question());
        }

        if (dto.answer() != null && !dto.answer().isBlank()) {
            faq.setAnswer(dto.answer());
        }

        if (dto.displayOrder() != null) {
            faq.setDisplayOrder(dto.displayOrder());
        }

        Faq updatedFaq = faqRepository.save(faq);
        log.info("FAQ updated: {}", faqId);
        return FaqResponseDto.fromEntity(updatedFaq);
    }

    @Transactional
    public void deleteFaq(UUID faqId) {
        log.debug("Deleting FAQ: {}", faqId);
        Faq faq = faqRepository.findByIdAndStatusActive(faqId)
                .orElseThrow(() -> {
                    log.warn("FAQ not found: {}", faqId);
                    return new IllegalArgumentException("FAQ not found");
                });

        // Soft delete - set status to inactive
        faq.setStatus(BaseEntity.STATUS_INACTIVE);
        faqRepository.save(faq);
        log.info("FAQ deleted: {}", faqId);
    }

    @Transactional(readOnly = true)
    public FaqResponseDto getFaq(UUID faqId) {
        log.debug("Fetching FAQ: {}", faqId);
        Faq faq = faqRepository.findByIdAndStatusActive(faqId)
                .orElseThrow(() -> {
                    log.warn("FAQ not found: {}", faqId);
                    return new IllegalArgumentException("FAQ not found");
                });

        return FaqResponseDto.fromEntity(faq);
    }

    @Transactional(readOnly = true)
    public List<FaqResponseDto> getAllFaqs() {
        log.debug("Fetching all FAQs");
        return faqRepository.findAllActive()
                .stream()
                .map(FaqResponseDto::fromEntity)
                .toList();
    }
}

package com.example.vnkapp.controller;

import com.example.vnkapp.dto.common.ApiResponseDto;
import com.example.vnkapp.dto.faq.FaqCreateRequestDto;
import com.example.vnkapp.dto.faq.FaqResponseDto;
import com.example.vnkapp.dto.faq.FaqUpdateRequestDto;
import com.example.vnkapp.dto.user.UserResponseDto;
import com.example.vnkapp.service.FaqService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/faqs")
public class FaqController {

    private static final Logger log = LoggerFactory.getLogger(FaqController.class);

    private final FaqService faqService;

    public FaqController(FaqService faqService) {
        this.faqService = faqService;
    }

    @PostMapping
    public ResponseEntity<?> createFaq(@Valid @RequestBody FaqCreateRequestDto request) {
        log.info("Create FAQ request");
        try {
            FaqResponseDto faq = faqService.createFaq(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponseDto<>("Ok", null, faq));
        } catch (IllegalArgumentException ex) {
            log.warn("Create FAQ failed: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            log.error("Create FAQ error", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't create FAQ due to some issue."));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateFaq(
            @PathVariable UUID id,
            @Valid @RequestBody FaqUpdateRequestDto request) {
        log.info("Update FAQ: {}", id);
        try {
            FaqResponseDto faq = faqService.updateFaq(id, request);
            return ResponseEntity.ok(new ApiResponseDto<>("Ok", null, faq));
        } catch (IllegalArgumentException ex) {
            log.warn("Update FAQ {} failed: {}", id, ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            log.error("Update FAQ {} error", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't update FAQ due to some issue."));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFaq(@PathVariable UUID id) {
        log.info("Delete FAQ: {}", id);
        try {
            faqService.deleteFaq(id);
            return ResponseEntity.ok(new UserResponseDto("Ok", null));
        } catch (IllegalArgumentException ex) {
            log.warn("Delete FAQ {} failed: {}", id, ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            log.error("Delete FAQ {} error", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't delete FAQ due to some issue."));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getFaq(@PathVariable UUID id) {
        log.info("Get FAQ: {}", id);
        try {
            FaqResponseDto faq = faqService.getFaq(id);
            return ResponseEntity.ok(new ApiResponseDto<>("Ok", null, faq));
        } catch (IllegalArgumentException ex) {
            log.warn("FAQ not found: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new UserResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            log.error("Get FAQ {} error", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't fetch FAQ due to some issue."));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllFaqs() {
        log.info("Get all FAQs");
        try {
            List<FaqResponseDto> faqs = faqService.getAllFaqs();
            return ResponseEntity.ok(new ApiResponseDto<>("Ok", null, faqs));
        } catch (Exception ex) {
            log.error("Get all FAQs error", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't fetch FAQs due to some issue."));
        }
    }
}

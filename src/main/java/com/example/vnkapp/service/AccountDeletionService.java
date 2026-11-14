package com.example.vnkapp.service;

import com.example.vnkapp.entity.BaseEntity;
import com.example.vnkapp.entity.User;
import com.example.vnkapp.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Permanently deletes a user's personal data (Google Play / Apple data-deletion requirement).
 *
 * <p>Strategy:
 * <ul>
 *   <li>Personal data (addresses, cart, wishlist, family/health data, devices, sessions,
 *       notifications, referral rewards, reset tokens) is hard-deleted.</li>
 *   <li>Orders and payments are kept for tax/accounting obligations; they only hold the
 *       user's UUID, which no longer maps to any identifiable person.</li>
 *   <li>The {@code users} row is anonymized (not removed) so those references stay valid,
 *       the original email/phone become reusable, and the account can never log in again.</li>
 * </ul>
 * Everything runs in one transaction: either all of it is applied or none of it.
 */
@Service
public class AccountDeletionService {

    private static final Logger log = LoggerFactory.getLogger(AccountDeletionService.class);

    @PersistenceContext
    private EntityManager em;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountDeletionService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Deletes the account that owns {@code email}, after verifying {@code password}.
     * A single generic error is used for unknown email, wrong password and already-deleted
     * accounts so the endpoint cannot be used to discover which emails are registered.
     */
    @Transactional
    public void deleteAccount(String email, String password) {
        User user = userRepository.findByEmail(email)
                .filter(User::isActive)
                .orElseThrow(() -> {
                    log.warn("Account deletion rejected - unknown or inactive account");
                    return new IllegalArgumentException("Invalid email or password");
                });

        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("Account deletion rejected - wrong password for user: {}", user.getId());
            throw new IllegalArgumentException("Invalid email or password");
        }

        UUID userId = user.getId();

        // Sessions first so the account is signed out everywhere.
        deleteByUser("UserSession", userId);
        deleteByUser("UserDevice", userId);
        deleteByUser("PasswordResetToken", userId);
        deleteByUser("Notification", userId);

        // Shopping data.
        deleteByUser("Address", userId);
        deleteByUser("Wishlist", userId);
        em.createQuery("delete from CartItem ci where ci.cartId in "
                        + "(select c.id from Cart c where c.userId = :u)")
                .setParameter("u", userId).executeUpdate();
        deleteByUser("Cart", userId);

        // Family and health data.
        deleteByUser("ReminderLog", userId);
        deleteByUser("MedicationReminder", userId);
        deleteByUser("UserMedication", userId);
        deleteByUser("UserDisease", userId);
        deleteByUser("FamilyMember", userId);

        // Referral rewards belong to the user; coupons may already be referenced by past orders,
        // so they are deactivated and unassigned instead of deleted.
        deleteByUser("ReferralReward", userId);
        em.createQuery("update Coupon c set c.status = :inactive, c.assignedUserId = null "
                        + "where c.assignedUserId = :u")
                .setParameter("inactive", BaseEntity.STATUS_INACTIVE)
                .setParameter("u", userId).executeUpdate();
        // Referral rows of other users point at this code, so keep it but stop it being used.
        em.createQuery("update ReferralCode r set r.status = :inactive where r.userId = :u")
                .setParameter("inactive", BaseEntity.STATUS_INACTIVE)
                .setParameter("u", userId).executeUpdate();

        anonymizeUser(user);
        log.info("Account deleted and anonymized for user: {}", userId);
    }

    private void deleteByUser(String entityName, UUID userId) {
        em.createQuery("delete from " + entityName + " e where e.userId = :u")
                .setParameter("u", userId)
                .executeUpdate();
    }

    private void anonymizeUser(User user) {
        UUID id = user.getId();
        user.setUsername("Deleted User");
        user.setEmail("deleted-" + id + "@deleted.invalid");
        user.setPhone(null);
        user.setProfileImageUrl(null);
        // Unknown random password: nobody can log in to this account again.
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setIsEmailVerified(false);
        user.setIsPhoneVerified(false);
        user.setActive(false);
        user.setStatus(BaseEntity.STATUS_INACTIVE);
        userRepository.save(user);
    }
}

package com.xpense.service;

import com.xpense.dto.AuthResponse;
import com.xpense.dto.LoginRequest;
import com.xpense.dto.RegisterRequest;
import com.xpense.exception.BadRequestException;
import com.xpense.exception.ResourceNotFoundException;
import com.xpense.model.UserProfile;
import com.xpense.model.Wallet;
import com.xpense.repository.UserProfileRepository;
import com.xpense.repository.WalletRepository;
import com.xpense.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private final UserProfileRepository userProfileRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserProfileRepository userProfileRepository,
                       WalletRepository walletRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this.userProfileRepository = userProfileRepository;
        this.walletRepository = walletRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String cleanEmail = request.getEmail().trim().toLowerCase();

        if (userProfileRepository.existsByEmail(cleanEmail)) {
            throw new BadRequestException("An account with this email address already exists.");
        }

        String userId = "user-" + UUID.randomUUID().toString();
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        UserProfile profile = new UserProfile();
        profile.setId(userId);
        profile.setEmail(cleanEmail);
        profile.setPasswordHash(hashedPassword);
        profile.setFullName(Inputs.requiredText(request.getFullName(), 100, "Your name", "Please enter your full name."));
        profile.setStudentId(cleanEmail);
        profile.setRole("user");
        profile.setAccountType(normalizeAccountType(request.getAccountType()));
        profile.setCurrency("INR");
        profile.setCurrencySymbol("₹");
        profile.setTotalBalance(BigDecimal.ZERO);
        if (!profile.getAccountType().toLowerCase().contains("student")) {
            // College details only apply to student accounts
            profile.setUniversity("");
            profile.setSemester("");
        }

        UserProfile savedProfile = userProfileRepository.save(profile);

        // Auto-provision initial spending envelopes tailored to user persona
        provisionDefaultWallets(userId, profile.getAccountType());

        String token = tokenProvider.generateToken(savedProfile);
        return new AuthResponse(token, savedProfile);
    }

    public AuthResponse login(LoginRequest request) {
        String cleanEmail = request.getEmail().trim().toLowerCase();

        UserProfile user = userProfileRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new BadRequestException("Invalid email address or password."));

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid email address or password.");
        }

        String token = tokenProvider.generateToken(user);
        return new AuthResponse(token, user);
    }

    public UserProfile getCurrentUser(String userId) {
        return userProfileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    /** Accepts the three account types (and friendly spellings); anything else is a mistake. */
    static String normalizeAccountType(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Student Account";
        }
        String t = raw.toLowerCase();
        if (t.contains("student")) return "Student Account";
        if (t.contains("personal")) return "Personal Account";
        if (t.contains("corporate") || t.contains("business") || t.contains("saas")) return "Corporate SaaS";
        throw new BadRequestException("Choose an account type: Student, Personal or Business.");
    }

    private void provisionDefaultWallets(String userId, String accountType) {
        String type = (accountType != null) ? accountType.toLowerCase() : "student";

        if (type.contains("corporate") || type.contains("saas") || type.contains("enterprise")) {
            Wallet w1 = new Wallet("wallet-" + UUID.randomUUID(), userId, "Cloud Infrastructure", "Cloud & Hosting",
                    BigDecimal.ZERO, new BigDecimal("15000.00"), "Server", "#3B82F6");
            Wallet w2 = new Wallet("wallet-" + UUID.randomUUID(), userId, "Software Licenses & Tooling", "Software Licenses",
                    BigDecimal.ZERO, new BigDecimal("8000.00"), "Cpu", "#8B5CF6");
            Wallet w3 = new Wallet("wallet-" + UUID.randomUUID(), userId, "Client Meetings & Dinners", "Business Travel",
                    BigDecimal.ZERO, new BigDecimal("6000.00"), "Coffee", "#F59E0B");
            Wallet w4 = new Wallet("wallet-" + UUID.randomUUID(), userId, "Tax & Contingency Reserve", "Corporate Tax",
                    BigDecimal.ZERO, new BigDecimal("25000.00"), "ShieldCheck", "#10B981");
            w4.setIsTaxReserve(true);

            walletRepository.saveAll(List.of(w1, w2, w3, w4));
        } else if (type.contains("personal")) {
            Wallet w1 = new Wallet("wallet-" + UUID.randomUUID(), userId, "Groceries & Provisions", "Groceries",
                    BigDecimal.ZERO, new BigDecimal("12000.00"), "ShoppingBag", "#10B981");
            Wallet w2 = new Wallet("wallet-" + UUID.randomUUID(), userId, "Rent & Utilities", "Housing & Bills",
                    BigDecimal.ZERO, new BigDecimal("20000.00"), "Home", "#3B82F6");
            Wallet w3 = new Wallet("wallet-" + UUID.randomUUID(), userId, "Healthcare & Wellness", "Healthcare",
                    BigDecimal.ZERO, new BigDecimal("5000.00"), "HeartPulse", "#EC4899");
            Wallet w4 = new Wallet("wallet-" + UUID.randomUUID(), userId, "Personal Lifestyle", "Shopping",
                    BigDecimal.ZERO, new BigDecimal("8000.00"), "ShoppingCart", "#F59E0B");

            walletRepository.saveAll(List.of(w1, w2, w3, w4));
        } else {
            // Default: Student Account (Campus Budgeting)
            Wallet w1 = new Wallet("wallet-" + UUID.randomUUID(), userId, "Food & Canteen", "Food & Dining",
                    BigDecimal.ZERO, new BigDecimal("2500.00"), "ShoppingBag", "#10B981");
            Wallet w2 = new Wallet("wallet-" + UUID.randomUUID(), userId, "Campus Transit", "Transportation",
                    BigDecimal.ZERO, new BigDecimal("800.00"), "Car", "#3B82F6");
            Wallet w3 = new Wallet("wallet-" + UUID.randomUUID(), userId, "Entertainment & Hangouts", "Entertainment",
                    BigDecimal.ZERO, new BigDecimal("1000.00"), "Gamepad2", "#A855F7");
            Wallet w4 = new Wallet("wallet-" + UUID.randomUUID(), userId, "Books & Academic Supplies", "Education",
                    BigDecimal.ZERO, new BigDecimal("1500.00"), "BookOpen", "#F59E0B");

            walletRepository.saveAll(List.of(w1, w2, w3, w4));
        }
    }
}

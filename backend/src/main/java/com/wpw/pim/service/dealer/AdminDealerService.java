package com.wpw.pim.service.dealer;

import com.wpw.pim.auth.domain.Role;
import com.wpw.pim.auth.domain.User;
import com.wpw.pim.auth.repository.RoleRepository;
import com.wpw.pim.auth.repository.UserRepository;
import com.wpw.pim.domain.dealer.Dealer;
import com.wpw.pim.domain.dealer.DealerContact;
import com.wpw.pim.repository.dealer.DealerContactRepository;
import com.wpw.pim.repository.dealer.DealerRepository;
import com.wpw.pim.web.dto.dealer.admin.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminDealerService {

    private static final String DEALER_ROLE_NAME = "dealer";

    private final DealerRepository dealerRepo;
    private final DealerContactRepository contactRepo;
    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional(readOnly = true)
    public List<DealerDto> listAll() {
        return dealerRepo.findAll().stream()
            .map(d -> toDto(d, contactRepo.findByDealerId(d.getId()).stream().map(this::toContactDto).toList()))
            .toList();
    }

    @Transactional(readOnly = true)
    public DealerDto getById(UUID id) {
        Dealer dealer = find(id);
        return toDto(dealer, contactRepo.findByDealerId(id).stream().map(this::toContactDto).toList());
    }

    @Transactional
    public DealerCreatedDto create(DealerSaveRequest req) {
        String username = req.dealerCode().trim().toLowerCase().replaceAll("[^a-z0-9_-]", "_");
        if (userRepo.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Пользователь с именем '" + username + "' уже существует. Измените код дилера.");
        }

        String rawPassword = generatePassword();
        Role dealerRole = roleRepo.findByName(DEALER_ROLE_NAME)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Роль 'dealer' не найдена"));

        User user = new User(username, passwordEncoder.encode(rawPassword), dealerRole);
        userRepo.save(user);

        Dealer dealer = new Dealer();
        dealer.setName(req.companyName());
        dealer.setApiKeyHash("");
        dealer.setUser(user);
        applyFields(dealer, req);
        dealerRepo.save(dealer);

        List<DealerContact> contacts = saveContacts(dealer, req.contacts());
        DealerDto dto = toDto(dealer, contacts.stream().map(this::toContactDto).toList());
        return new DealerCreatedDto(dto, username, rawPassword);
    }

    @Transactional
    public DealerDto update(UUID id, DealerSaveRequest req) {
        Dealer dealer = find(id);
        dealer.setName(req.companyName());
        applyFields(dealer, req);
        dealer.setUpdatedAt(OffsetDateTime.now());

        // sync user enabled status with dealer active flag
        if (dealer.getUser() != null) {
            dealer.getUser().setEnabled(req.isActive());
            userRepo.save(dealer.getUser());
        }

        dealerRepo.save(dealer);
        contactRepo.deleteByDealerId(id);
        List<DealerContact> contacts = saveContacts(dealer, req.contacts());
        return toDto(dealer, contacts.stream().map(this::toContactDto).toList());
    }

    @Transactional
    public void delete(UUID id) {
        Dealer dealer = find(id);
        User linkedUser = dealer.getUser();
        dealer.setUser(null);
        dealerRepo.save(dealer);
        dealerRepo.deleteById(id);
        if (linkedUser != null) {
            userRepo.delete(linkedUser);
        }
    }

    @Transactional
    public PasswordResetDto resetPassword(UUID id) {
        Dealer dealer = find(id);
        if (dealer.getUser() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "У дилера нет учётной записи");
        }
        String rawPassword = generatePassword();
        dealer.getUser().setPasswordHash(passwordEncoder.encode(rawPassword));
        userRepo.save(dealer.getUser());
        return new PasswordResetDto(dealer.getUser().getUsername(), rawPassword);
    }

    @Transactional
    public DealerContactDto addContact(UUID dealerId, DealerContactSaveRequest req) {
        Dealer dealer = find(dealerId);
        DealerContact contact = buildContact(dealer, req);
        contactRepo.save(contact);
        return toContactDto(contact);
    }

    @Transactional
    public DealerContactDto updateContact(UUID dealerId, UUID contactId, DealerContactSaveRequest req) {
        DealerContact contact = contactRepo.findById(contactId)
            .filter(c -> c.getDealer().getId().equals(dealerId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact not found"));
        contact.setContactName(req.contactName());
        contact.setRole(req.role());
        contact.setEmail(req.email());
        contact.setPhone(req.phone());
        contact.setPrimary(req.isPrimary());
        contactRepo.save(contact);
        return toContactDto(contact);
    }

    @Transactional
    public void deleteContact(UUID dealerId, UUID contactId) {
        DealerContact contact = contactRepo.findById(contactId)
            .filter(c -> c.getDealer().getId().equals(dealerId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact not found"));
        contactRepo.delete(contact);
    }

    // --- private helpers ---

    private Dealer find(UUID id) {
        return dealerRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dealer not found"));
    }

    private String generatePassword() {
        byte[] bytes = new byte[12];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void applyFields(Dealer d, DealerSaveRequest req) {
        d.setDealerCode(req.dealerCode());
        d.setCompanyName(req.companyName());
        d.setBrandName(req.brandName());
        d.setDealerType(req.dealerType());
        d.setPrivateLabelBrand(req.privateLabelBrand());
        d.setCountry(req.country());
        d.setRegion(req.region());
        d.setCity(req.city());
        d.setAddress(req.address());
        d.setPostalCode(req.postalCode());
        d.setWebsite(req.website());
        d.setHasEcommerce(req.hasEcommerce());
        d.setShopUrl(req.shopUrl());
        d.setLogo(req.logo());
        d.setCurrency(req.currency());
        d.setDiscountTier(req.discountTier());
        d.setNotes(req.notes());
        d.setActive(req.isActive());
    }

    private List<DealerContact> saveContacts(Dealer dealer, List<DealerContactSaveRequest> requests) {
        if (requests == null || requests.isEmpty()) return List.of();
        return requests.stream()
            .map(r -> { DealerContact c = buildContact(dealer, r); contactRepo.save(c); return c; })
            .toList();
    }

    private DealerContact buildContact(Dealer dealer, DealerContactSaveRequest req) {
        DealerContact c = new DealerContact();
        c.setDealer(dealer);
        c.setContactName(req.contactName());
        c.setRole(req.role());
        c.setEmail(req.email());
        c.setPhone(req.phone());
        c.setPrimary(req.isPrimary());
        return c;
    }

    private DealerContactDto toContactDto(DealerContact c) {
        return new DealerContactDto(c.getId(), c.getContactName(), c.getRole(),
            c.getEmail(), c.getPhone(), c.isPrimary());
    }

    private DealerDto toDto(Dealer d, List<DealerContactDto> contacts) {
        String priceListId = d.getPriceList() != null ? d.getPriceList().getId().toString() : null;
        String username = d.getUser() != null ? d.getUser().getUsername() : null;
        return new DealerDto(
            d.getId(), d.getDealerCode(), d.getCompanyName(), d.getBrandName(),
            d.getDealerType(), d.getPrivateLabelBrand(), d.getCountry(), d.getRegion(),
            d.getCity(), d.getAddress(), d.getPostalCode(), d.getWebsite(),
            d.isHasEcommerce(), d.getShopUrl(), d.getLogo(), priceListId,
            d.getCurrency(), d.getDiscountTier(), d.getNotes(), d.isActive(),
            username, d.getCreatedAt(), d.getUpdatedAt(), contacts
        );
    }
}

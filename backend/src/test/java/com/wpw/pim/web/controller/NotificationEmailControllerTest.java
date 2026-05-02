package com.wpw.pim.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wpw.pim.auth.service.JwtService;
import com.wpw.pim.auth.service.PimUserDetailsService;
import com.wpw.pim.config.SecurityConfig;
import com.wpw.pim.domain.notification.NotificationEmail;
import com.wpw.pim.repository.notification.NotificationEmailRepository;
import com.wpw.pim.security.ApiKeyAuthProvider;
import com.wpw.pim.web.dto.notification.NotificationEmailSaveRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(SecurityConfig.class)
@WebMvcTest(NotificationEmailController.class)
class NotificationEmailControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private NotificationEmailRepository repository;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private PimUserDetailsService pimUserDetailsService;
    @MockitoBean private ApiKeyAuthProvider apiKeyAuthProvider;

    private NotificationEmail buildEntity(UUID id, String email, boolean active) {
        NotificationEmail e = new NotificationEmail();
        e.setId(id);
        e.setEmail(email);
        e.setActive(active);
        e.setCreatedAt(OffsetDateTime.now());
        return e;
    }

    @Test
    @WithMockUser(authorities = "MANAGE_DEALERS")
    @DisplayName("GET /api/v1/admin/notification-emails -- list addresses")
    void list() throws Exception {
        when(repository.findAll()).thenReturn(List.of(
            buildEntity(UUID.randomUUID(), "a@x.com", true),
            buildEntity(UUID.randomUUID(), "b@x.com", false)
        ));

        mockMvc.perform(get("/api/v1/admin/notification-emails"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].email").value("a@x.com"))
            .andExpect(jsonPath("$[0].active").value(true))
            .andExpect(jsonPath("$[1].active").value(false));
    }

    @Test
    @WithMockUser(authorities = "MANAGE_DEALERS")
    @DisplayName("POST /api/v1/admin/notification-emails -- 201 creates address")
    void create() throws Exception {
        UUID newId = UUID.randomUUID();
        when(repository.save(any(NotificationEmail.class))).thenAnswer(inv -> {
            NotificationEmail e = inv.getArgument(0);
            e.setId(newId);
            if (e.getCreatedAt() == null) e.setCreatedAt(OffsetDateTime.now());
            return e;
        });

        NotificationEmailSaveRequest req = new NotificationEmailSaveRequest("new@x.com", true);

        mockMvc.perform(post("/api/v1/admin/notification-emails")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("new@x.com"))
            .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @WithMockUser(authorities = "MANAGE_DEALERS")
    @DisplayName("PUT /api/v1/admin/notification-emails/{id} -- 200 updates")
    void update() throws Exception {
        UUID id = UUID.randomUUID();
        NotificationEmail existing = buildEntity(id, "old@x.com", true);
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.save(any(NotificationEmail.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationEmailSaveRequest req = new NotificationEmailSaveRequest("updated@x.com", false);

        mockMvc.perform(put("/api/v1/admin/notification-emails/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("updated@x.com"))
            .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @WithMockUser(authorities = "MANAGE_DEALERS")
    @DisplayName("PUT /api/v1/admin/notification-emails/{id} -- 404 if not found")
    void updateNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        NotificationEmailSaveRequest req = new NotificationEmailSaveRequest("x@x.com", true);

        mockMvc.perform(put("/api/v1/admin/notification-emails/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "MANAGE_DEALERS")
    @DisplayName("DELETE /api/v1/admin/notification-emails/{id} -- 204")
    void deleteEmail() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/admin/notification-emails/" + id))
            .andExpect(status().isNoContent());

        verify(repository).deleteById(id);
    }

    @Test
    @DisplayName("without authentication -- 4xx")
    void unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/admin/notification-emails"))
            .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(authorities = "OTHER")
    @DisplayName("without MANAGE_DEALERS -- 403")
    void wrongAuthority() throws Exception {
        mockMvc.perform(get("/api/v1/admin/notification-emails"))
            .andExpect(status().isForbidden());
    }
}

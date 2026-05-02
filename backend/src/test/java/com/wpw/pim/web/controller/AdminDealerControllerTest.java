package com.wpw.pim.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wpw.pim.auth.service.JwtService;
import com.wpw.pim.auth.service.PimUserDetailsService;
import com.wpw.pim.config.SecurityConfig;
import com.wpw.pim.security.ApiKeyAuthProvider;
import com.wpw.pim.service.dealer.AdminDealerService;
import com.wpw.pim.web.dto.dealer.admin.DealerContactDto;
import com.wpw.pim.web.dto.dealer.admin.DealerContactSaveRequest;
import com.wpw.pim.web.dto.dealer.admin.DealerCreatedDto;
import com.wpw.pim.web.dto.dealer.admin.DealerDto;
import com.wpw.pim.web.dto.dealer.admin.DealerSaveRequest;
import com.wpw.pim.web.dto.dealer.admin.PasswordResetDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-тесты для {@link AdminDealerController}.
 * Все эндпоинты требуют MANAGE_DEALERS.
 */
@Import(SecurityConfig.class)
@WebMvcTest(AdminDealerController.class)
class AdminDealerControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private AdminDealerService service;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private PimUserDetailsService pimUserDetailsService;
    @MockitoBean private ApiKeyAuthProvider apiKeyAuthProvider;

    private DealerDto sampleDealer(UUID id) {
        return new DealerDto(id, "DEAL-1", "Acme", null, null, null,
            "US", null, null, null, null, null, false, null, null, null,
            "USD", null, null, true, "user1", null, null, List.of());
    }

    private DealerSaveRequest sampleRequest() {
        return new DealerSaveRequest("DEAL-1", "Acme", "US",
            null, null, null, null, null, null, null, null,
            false, null, null, null, "USD", null, null, true, List.of());
    }

    @Test
    @WithMockUser(authorities = "MANAGE_DEALERS")
    @DisplayName("GET /api/v1/admin/dealers -- list dealers")
    void list() throws Exception {
        when(service.listAll()).thenReturn(List.of(sampleDealer(UUID.randomUUID())));

        mockMvc.perform(get("/api/v1/admin/dealers"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].dealerCode").value("DEAL-1"));
    }

    @Test
    @WithMockUser(authorities = "MANAGE_DEALERS")
    @DisplayName("GET /api/v1/admin/dealers/{id} -- get dealer")
    void getById() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.getById(id)).thenReturn(sampleDealer(id));

        mockMvc.perform(get("/api/v1/admin/dealers/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.companyName").value("Acme"));
    }

    @Test
    @WithMockUser(authorities = "MANAGE_DEALERS")
    @DisplayName("POST /api/v1/admin/dealers -- creates dealer, returns 201")
    void create() throws Exception {
        UUID id = UUID.randomUUID();
        DealerCreatedDto result = new DealerCreatedDto(sampleDealer(id), "user1", "pass123");
        when(service.create(any(DealerSaveRequest.class))).thenReturn(result);

        mockMvc.perform(post("/api/v1/admin/dealers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequest())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value("user1"))
            .andExpect(jsonPath("$.generatedPassword").value("pass123"));
    }

    @Test
    @WithMockUser(authorities = "MANAGE_DEALERS")
    @DisplayName("PUT /api/v1/admin/dealers/{id} -- updates dealer")
    void update() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.update(eq(id), any(DealerSaveRequest.class))).thenReturn(sampleDealer(id));

        mockMvc.perform(put("/api/v1/admin/dealers/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequest())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @WithMockUser(authorities = "MANAGE_DEALERS")
    @DisplayName("DELETE /api/v1/admin/dealers/{id} -- 204")
    void delete204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/admin/dealers/" + id))
            .andExpect(status().isNoContent());

        verify(service).delete(id);
    }

    @Test
    @WithMockUser(authorities = "MANAGE_DEALERS")
    @DisplayName("POST /api/v1/admin/dealers/{id}/reset-password -- resets password")
    void resetPassword() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.resetPassword(id)).thenReturn(new PasswordResetDto("user1", "newpass"));

        mockMvc.perform(post("/api/v1/admin/dealers/" + id + "/reset-password"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("user1"))
            .andExpect(jsonPath("$.newPassword").value("newpass"));
    }

    @Test
    @WithMockUser(authorities = "MANAGE_DEALERS")
    @DisplayName("POST /api/v1/admin/dealers/{id}/contacts -- adds contact, 201")
    void addContact() throws Exception {
        UUID id = UUID.randomUUID();
        UUID contactId = UUID.randomUUID();
        DealerContactDto dto = new DealerContactDto(contactId, "Jane", "owner", "j@x", null, true);
        when(service.addContact(eq(id), any(DealerContactSaveRequest.class))).thenReturn(dto);

        DealerContactSaveRequest req = new DealerContactSaveRequest("Jane", "owner", "j@x", null, true);

        mockMvc.perform(post("/api/v1/admin/dealers/" + id + "/contacts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.contactName").value("Jane"));
    }

    @Test
    @WithMockUser(authorities = "MANAGE_DEALERS")
    @DisplayName("PUT /api/v1/admin/dealers/{id}/contacts/{contactId} -- updates contact")
    void updateContact() throws Exception {
        UUID id = UUID.randomUUID();
        UUID contactId = UUID.randomUUID();
        DealerContactDto dto = new DealerContactDto(contactId, "Jane2", null, null, null, false);
        when(service.updateContact(eq(id), eq(contactId), any(DealerContactSaveRequest.class)))
            .thenReturn(dto);

        DealerContactSaveRequest req = new DealerContactSaveRequest("Jane2", null, "jane2@example.com", null, false);

        mockMvc.perform(put("/api/v1/admin/dealers/" + id + "/contacts/" + contactId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contactName").value("Jane2"));
    }

    @Test
    @WithMockUser(authorities = "MANAGE_DEALERS")
    @DisplayName("DELETE /api/v1/admin/dealers/{id}/contacts/{contactId} -- 204")
    void deleteContact() throws Exception {
        UUID id = UUID.randomUUID();
        UUID contactId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/admin/dealers/" + id + "/contacts/" + contactId))
            .andExpect(status().isNoContent());

        verify(service).deleteContact(id, contactId);
    }

    @Test
    @DisplayName("without authentication returns 4xx")
    void unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dealers"))
            .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(authorities = "OTHER")
    @DisplayName("without MANAGE_DEALERS returns 403")
    void wrongAuthority() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dealers"))
            .andExpect(status().isForbidden());
    }
}

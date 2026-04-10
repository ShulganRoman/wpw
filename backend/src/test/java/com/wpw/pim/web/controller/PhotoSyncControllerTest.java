package com.wpw.pim.web.controller;

import com.wpw.pim.auth.service.JwtService;
import com.wpw.pim.auth.service.PimUserDetailsService;
import com.wpw.pim.config.SecurityConfig;
import com.wpw.pim.security.ApiKeyAuthProvider;
import com.wpw.pim.service.media.PhotoImportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(SecurityConfig.class)
@WebMvcTest(PhotoSyncController.class)
class PhotoSyncControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private PhotoImportService photoImportService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private PimUserDetailsService pimUserDetailsService;
    @MockitoBean private ApiKeyAuthProvider apiKeyAuthProvider;

    private MockMultipartFile imageFile(String name) {
        return new MockMultipartFile("files", name, "image/jpeg", new byte[]{1, 2, 3});
    }

    private MockMultipartFile archiveFile() {
        return new MockMultipartFile("archive", "photos.zip", "application/zip", new byte[]{1, 2, 3});
    }

    @Nested
    @DisplayName("Individual photos")
    class IndividualPhotos {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("POST /api/v1/admin/photos/validate -- validates uploaded photos")
        void validatePhotos_returnsReport() throws Exception {
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("totalFiles", 2);
            report.put("matched", 1);
            report.put("unmatched", 1);
            when(photoImportService.validatePhotos(any())).thenReturn(report);

            mockMvc.perform(multipart("/api/v1/admin/photos/validate")
                            .file(imageFile("WPW-001.jpg"))
                            .file(imageFile("UNKNOWN.jpg")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalFiles").value(2))
                    .andExpect(jsonPath("$.matched").value(1));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("POST /api/v1/admin/photos/import -- imports photos")
        void importPhotos_returnsReport() throws Exception {
            Map<String, Object> report = Map.of("converted", 2, "errors", 0);
            when(photoImportService.importPhotos(any())).thenReturn(report);

            mockMvc.perform(multipart("/api/v1/admin/photos/import")
                            .file(imageFile("WPW-001.jpg")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.converted").value(2));
        }
    }

    @Nested
    @DisplayName("Sync existing")
    class SyncExisting {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("POST /api/v1/admin/photos/sync -- syncs existing photos on disk")
        void syncPhotos_returnsReport() throws Exception {
            Map<String, Object> report = Map.of("matched", 5, "created", 3, "skipped", 2);
            when(photoImportService.syncExistingPhotos()).thenReturn(report);

            mockMvc.perform(post("/api/v1/admin/photos/sync"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.matched").value(5))
                    .andExpect(jsonPath("$.created").value(3));
        }
    }

    @Nested
    @DisplayName("Archive import")
    class ArchiveImport {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("POST /api/v1/admin/photos/archive/validate -- validates archive contents")
        void validateArchive_returnsReport() throws Exception {
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("archiveName", "photos.zip");
            report.put("matched", 3);
            report.put("unmatched", 1);
            when(photoImportService.validateArchive(any())).thenReturn(report);

            mockMvc.perform(multipart("/api/v1/admin/photos/archive/validate")
                            .file(archiveFile()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.archiveName").value("photos.zip"))
                    .andExpect(jsonPath("$.matched").value(3));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("POST /api/v1/admin/photos/archive/import -- imports from archive")
        void importArchive_returnsReport() throws Exception {
            Map<String, Object> report = Map.of("converted", 5, "errors", 0);
            when(photoImportService.importArchive(any())).thenReturn(report);

            mockMvc.perform(multipart("/api/v1/admin/photos/archive/import")
                            .file(archiveFile()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.converted").value(5));
        }
    }

    @Test
    @DisplayName("POST /api/v1/admin/photos/validate -- unauthenticated returns 401/403")
    void validatePhotos_unauthenticated_returns4xx() throws Exception {
        mockMvc.perform(multipart("/api/v1/admin/photos/validate")
                        .file(imageFile("test.jpg")))
                .andExpect(status().is4xxClientError());
    }
}

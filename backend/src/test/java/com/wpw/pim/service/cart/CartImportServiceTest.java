package com.wpw.pim.service.cart;

import com.wpw.pim.domain.cart.CartItem;
import com.wpw.pim.domain.cart.CartItemId;
import com.wpw.pim.domain.dealer.Dealer;
import com.wpw.pim.domain.product.Product;
import com.wpw.pim.repository.cart.CartItemRepository;
import com.wpw.pim.repository.dealer.DealerRepository;
import com.wpw.pim.repository.product.ProductRepository;
import com.wpw.pim.web.dto.cart.CartDto;
import com.wpw.pim.web.dto.cart.CartImportResult;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CartImportServiceTest {

    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private DealerRepository dealerRepository;
    @Mock private CartService cartService;

    @InjectMocks
    private CartImportService service;

    private UUID dealerId;
    private Dealer dealer;

    @BeforeEach
    void setUp() {
        dealerId = UUID.randomUUID();
        dealer = new Dealer();
        dealer.setId(dealerId);
        dealer.setName("Test Dealer");
        when(dealerRepository.findById(dealerId)).thenReturn(Optional.of(dealer));
        when(cartService.getCart(dealerId)).thenReturn(
            new CartDto(List.of(), "USD", java.math.BigDecimal.ZERO, 0, List.of())
        );
    }

    private MockMultipartFile buildExcel(String[][] data) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Cart Import");
            // Row 0: instruction
            sheet.createRow(0).createCell(0).setCellValue("instruction");
            // Row 1: header
            Row header = sheet.createRow(1);
            header.createCell(0).setCellValue("Tool No");
            header.createCell(1).setCellValue("Quantity");
            // Rows 2+: data
            for (int i = 0; i < data.length; i++) {
                Row row = sheet.createRow(2 + i);
                row.createCell(0).setCellValue(data[i][0]);
                row.createCell(1).setCellValue(Integer.parseInt(data[i][1]));
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return new MockMultipartFile("file", "import.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                out.toByteArray());
        }
    }

    @Nested
    @DisplayName("importFromExcel")
    class ImportFromExcel {

        @Test
        @DisplayName("new items are added to cart")
        void addsNewItems() throws Exception {
            UUID productId = UUID.randomUUID();
            Product product = new Product();
            product.setId(productId);
            product.setToolNo("ATP5005D");

            when(productRepository.findByToolNoUpperIn(anyCollection()))
                .thenReturn(List.of(product));
            when(cartItemRepository.findById(any(CartItemId.class))).thenReturn(Optional.empty());

            CartImportResult result = service.importFromExcel(dealerId,
                buildExcel(new String[][]{{"ATP5005D", "5"}}));

            assertThat(result.imported()).isEqualTo(1);
            assertThat(result.replaced()).isEqualTo(0);
            assertThat(result.errors()).isEmpty();
            verify(cartItemRepository).save(argThat(ci -> ci.getQty() == 5));
        }

        @Test
        @DisplayName("existing items have quantity replaced")
        void replacesExistingQty() throws Exception {
            UUID productId = UUID.randomUUID();
            Product product = new Product();
            product.setId(productId);
            product.setToolNo("ATP5005D");

            CartItem existing = new CartItem(dealer, product, 3);

            when(productRepository.findByToolNoUpperIn(anyCollection()))
                .thenReturn(List.of(product));
            when(cartItemRepository.findById(any(CartItemId.class)))
                .thenReturn(Optional.of(existing));

            CartImportResult result = service.importFromExcel(dealerId,
                buildExcel(new String[][]{{"ATP5005D", "10"}}));

            assertThat(result.replaced()).isEqualTo(1);
            assertThat(result.imported()).isEqualTo(0);
            assertThat(existing.getQty()).isEqualTo(10);
        }

        @Test
        @DisplayName("unknown Tool No appears in errors")
        void unknownToolNoIsError() throws Exception {
            when(productRepository.findByToolNoUpperIn(anyCollection())).thenReturn(List.of());

            CartImportResult result = service.importFromExcel(dealerId,
                buildExcel(new String[][]{{"UNKNOWN-SKU", "2"}}));

            assertThat(result.errors()).hasSize(1);
            assertThat(result.errors().get(0)).contains("UNKNOWN-SKU");
            assertThat(result.imported()).isEqualTo(0);
            verify(cartItemRepository, never()).save(any());
        }

        @Test
        @DisplayName("Tool No matching is case-insensitive")
        void caseInsensitiveToolNo() throws Exception {
            UUID productId = UUID.randomUUID();
            Product product = new Product();
            product.setId(productId);
            product.setToolNo("ATP5005D");

            when(productRepository.findByToolNoUpperIn(anyCollection()))
                .thenReturn(List.of(product));
            when(cartItemRepository.findById(any(CartItemId.class))).thenReturn(Optional.empty());

            CartImportResult result = service.importFromExcel(dealerId,
                buildExcel(new String[][]{{"atp5005d", "3"}}));

            assertThat(result.imported()).isEqualTo(1);
            assertThat(result.errors()).isEmpty();
        }

        @Test
        @DisplayName("invalid quantity row goes to errors")
        void invalidQtyIsError() throws Exception {
            UUID productId = UUID.randomUUID();
            Product product = new Product();
            product.setId(productId);
            product.setToolNo("ATP5005D");

            when(productRepository.findByToolNoUpperIn(anyCollection()))
                .thenReturn(List.of(product));

            CartImportResult result = service.importFromExcel(dealerId,
                buildExcel(new String[][]{{"ATP5005D", "0"}}));

            assertThat(result.errors()).hasSize(1);
            assertThat(result.errors().get(0)).contains("invalid quantity");
        }

        @Test
        @DisplayName("dealer not found — 404")
        void dealerNotFound() {
            when(dealerRepository.findById(dealerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.importFromExcel(dealerId,
                new MockMultipartFile("f", new byte[0])))
                .isInstanceOf(ResponseStatusException.class);
        }
    }
}

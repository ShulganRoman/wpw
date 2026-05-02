package com.wpw.pim.service.dealer;

import com.wpw.pim.domain.dealer.Dealer;
import com.wpw.pim.domain.dealer.DealerSkuMapping;
import com.wpw.pim.domain.dealer.DealerSkuMappingId;
import com.wpw.pim.repository.dealer.DealerRepository;
import com.wpw.pim.repository.dealer.DealerSkuMappingRepository;
import com.wpw.pim.repository.product.ProductRepository;
import com.wpw.pim.web.dto.dealer.SkuMappingDto;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SkuMappingService {

    private final DealerSkuMappingRepository mappingRepo;
    private final DealerRepository dealerRepo;
    private final ProductRepository productRepo;

    // --- read ---

    @Transactional(readOnly = true)
    public List<SkuMappingDto> list(UUID dealerId) {
        return mappingRepo.findByDealerId(dealerId).stream()
            .map(m -> new SkuMappingDto(m.getId().getWpwSku(), m.getDealerSku(), m.getDealerBrand()))
            .toList();
    }

    // --- manual CRUD ---

    @Transactional
    public SkuMappingDto upsert(UUID dealerId, SkuMappingDto req) {
        Dealer dealer = findDealer(dealerId);
        DealerSkuMappingId id = new DealerSkuMappingId(dealerId, req.wpwSku());
        DealerSkuMapping m = mappingRepo.findById(id).orElse(new DealerSkuMapping());
        m.setId(id);
        m.setDealer(dealer);
        m.setDealerSku(req.dealerSku());
        m.setDealerBrand(req.dealerBrand());
        m.setUpdatedAt(OffsetDateTime.now());
        mappingRepo.save(m);
        return req;
    }

    @Transactional
    public void delete(UUID dealerId, String wpwSku) {
        DealerSkuMappingId id = new DealerSkuMappingId(dealerId, wpwSku);
        if (!mappingRepo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mapping not found");
        }
        mappingRepo.deleteById(id);
    }

    // --- import ---

    public record ValidationReport(
        int total,
        List<SkuMappingDto> valid,
        List<SkuMappingDto> ghosts,
        List<String> errors
    ) {}

    @Transactional(readOnly = true)
    public ValidationReport validate(MultipartFile file) throws IOException {
        List<SkuMappingDto> rows = parseExcel(file);
        return classify(rows);
    }

    @Transactional
    public SkuMappingImportResult execute(UUID dealerId, MultipartFile file, boolean skipGhosts) throws IOException {
        Dealer dealer = findDealer(dealerId);
        List<SkuMappingDto> rows = parseExcel(file);
        ValidationReport report = classify(rows);

        List<SkuMappingDto> toImport = skipGhosts ? report.valid() : rows;

        int created = 0, updated = 0;
        for (SkuMappingDto row : toImport) {
            DealerSkuMappingId id = new DealerSkuMappingId(dealerId, row.wpwSku());
            boolean exists = mappingRepo.existsById(id);
            DealerSkuMapping m = exists ? mappingRepo.findById(id).get() : new DealerSkuMapping();
            m.setId(id);
            m.setDealer(dealer);
            m.setDealerSku(row.dealerSku());
            m.setDealerBrand(row.dealerBrand());
            m.setUpdatedAt(OffsetDateTime.now());
            mappingRepo.save(m);
            if (exists) updated++; else created++;
        }

        return new SkuMappingImportResult(
            toImport.size(), created, updated,
            report.ghosts().size(),
            skipGhosts ? report.ghosts().stream().map(SkuMappingDto::wpwSku).toList() : List.of()
        );
    }

    public record SkuMappingImportResult(
        int imported, int created, int updated,
        int ghostCount, List<String> skippedGhosts
    ) {}

    // --- export ---

    @Transactional(readOnly = true)
    public byte[] export(UUID dealerId) throws IOException {
        List<SkuMappingDto> rows = list(dealerId);
        return buildWorkbook(rows);
    }

    public byte[] template() throws IOException {
        return buildWorkbook(List.of());
    }

    // --- private helpers ---

    private Dealer findDealer(UUID dealerId) {
        return dealerRepo.findById(dealerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dealer not found"));
    }

    private ValidationReport classify(List<SkuMappingDto> rows) {
        List<SkuMappingDto> valid = new ArrayList<>();
        List<SkuMappingDto> ghosts = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        Set<String> seen = new LinkedHashSet<>();
        for (SkuMappingDto row : rows) {
            if (row.wpwSku() == null || row.wpwSku().isBlank()) {
                errors.add("Empty WPW SKU in row skipped");
                continue;
            }
            if (row.dealerSku() == null || row.dealerSku().isBlank()) {
                errors.add("Empty Dealer SKU for WPW SKU: " + row.wpwSku());
                continue;
            }
            if (!seen.add(row.wpwSku())) {
                errors.add("Duplicate WPW SKU in file: " + row.wpwSku() + " (last row will be used)");
            }
            if (productRepo.existsByToolNo(row.wpwSku())) {
                valid.add(row);
            } else {
                ghosts.add(row);
            }
        }
        return new ValidationReport(rows.size(), valid, ghosts, errors);
    }

    private List<SkuMappingDto> parseExcel(MultipartFile file) throws IOException {
        List<SkuMappingDto> rows = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            // skip header row
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String wpwSku    = cellStr(row, 0);
                String dealerSku = cellStr(row, 1);
                String brand     = cellStr(row, 2);
                if (wpwSku.isBlank() && dealerSku.isBlank()) continue; // skip empty rows
                rows.add(new SkuMappingDto(wpwSku, dealerSku, brand.isBlank() ? null : brand));
            }
        }
        return rows;
    }

    private String cellStr(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double v = cell.getNumericCellValue();
                yield v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default      -> "";
        };
    }

    private byte[] buildWorkbook(List<SkuMappingDto> rows) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("SKU Mapping");

            // header style
            CellStyle hStyle = wb.createCellStyle();
            Font hFont = wb.createFont();
            hFont.setBold(true);
            hStyle.setFont(hFont);
            hStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            hStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            hStyle.setBorderBottom(BorderStyle.THIN);

            Row header = sheet.createRow(0);
            String[] cols = {"wpw_sku", "dealer_sku", "dealer_brand"};
            for (int i = 0; i < cols.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(hStyle);
                sheet.setColumnWidth(i, 7000);
            }

            int rowIdx = 1;
            for (SkuMappingDto dto : rows) {
                Row r = sheet.createRow(rowIdx++);
                r.createCell(0).setCellValue(dto.wpwSku() != null ? dto.wpwSku() : "");
                r.createCell(1).setCellValue(dto.dealerSku() != null ? dto.dealerSku() : "");
                r.createCell(2).setCellValue(dto.dealerBrand() != null ? dto.dealerBrand() : "");
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }
}

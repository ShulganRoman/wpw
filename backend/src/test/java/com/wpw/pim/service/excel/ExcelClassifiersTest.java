package com.wpw.pim.service.excel;

import com.wpw.pim.service.excel.classifier.MachineClassifier;
import com.wpw.pim.service.excel.classifier.MaterialClassifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-тесты для {@link MaterialClassifier} и {@link MachineClassifier}.
 * Проверяют корректную классификацию сырых строк Materials/Machines из Excel.
 */
class ExcelClassifiersTest {

    // ========================= MaterialClassifier =========================

    @Nested
    @DisplayName("MaterialClassifier")
    class MaterialClassifierTest {

        private MaterialClassifier classifier;

        @BeforeEach
        void setUp() {
            classifier = new MaterialClassifier();
        }

        @Test
        void classify_null_returnsEmptySets() {
            var result = classifier.classify(null);
            assertThat(result.toolMaterials()).isEmpty();
            assertThat(result.workpieceMaterials()).isEmpty();
        }

        @Test
        void classify_blank_returnsEmptySets() {
            var result = classifier.classify("   ");
            assertThat(result.toolMaterials()).isEmpty();
            assertThat(result.workpieceMaterials()).isEmpty();
        }

        @Test
        void classify_carbide_isToolMaterial() {
            var result = classifier.classify("Carbide");
            assertThat(result.toolMaterials()).contains("carbide");
            assertThat(result.workpieceMaterials()).isEmpty();
        }

        @Test
        void classify_tct_isToolMaterial() {
            var result = classifier.classify("TCT");
            assertThat(result.toolMaterials()).contains("tct");
            assertThat(result.workpieceMaterials()).isEmpty();
        }

        @Test
        void classify_solidCarbide_isToolMaterial() {
            var result = classifier.classify("Solid Carbide");
            assertThat(result.toolMaterials()).contains("solid_carbide");
        }

        @Test
        void classify_hss_isToolMaterial() {
            var result = classifier.classify("HSS");
            assertThat(result.toolMaterials()).contains("hss");
        }

        @Test
        void classify_pcd_isToolMaterial() {
            var result = classifier.classify("PCD");
            assertThat(result.toolMaterials()).contains("pcd");
        }

        @Test
        void classify_diamond_isToolMaterial() {
            var result = classifier.classify("Diamond");
            assertThat(result.toolMaterials()).contains("diamond");
        }

        @Test
        void classify_wood_isWorkpieceMaterial() {
            var result = classifier.classify("Wood");
            assertThat(result.workpieceMaterials()).contains("wood");
            assertThat(result.toolMaterials()).isEmpty();
        }

        @Test
        void classify_mdf_isWorkpieceMaterial() {
            var result = classifier.classify("MDF");
            assertThat(result.workpieceMaterials()).contains("mdf");
        }

        @Test
        void classify_mixedCommaSeparated_classifiesCorrectly() {
            var result = classifier.classify("Carbide, Wood, MDF, HSS");
            assertThat(result.toolMaterials()).containsExactlyInAnyOrder("carbide", "hss");
            assertThat(result.workpieceMaterials()).containsExactlyInAnyOrder("wood", "mdf");
        }

        @Test
        void classify_emptyPartsIgnored() {
            var result = classifier.classify(",, Carbide,,");
            assertThat(result.toolMaterials()).contains("carbide");
        }

        @Test
        void classify_normalizeCode_removesSpecialChars() {
            var result = classifier.classify("Plywood/Laminate");
            assertThat(result.workpieceMaterials()).isNotEmpty();
            // спецсимволы вроде '/' должны быть убраны
            String code = result.workpieceMaterials().iterator().next();
            assertThat(code).doesNotContain("/");
        }

        @Test
        void classify_densimet_isToolMaterial() {
            var result = classifier.classify("Densimet");
            assertThat(result.toolMaterials()).contains("densimet");
        }

        @Test
        void classify_tungsten_isToolMaterial() {
            var result = classifier.classify("Tungsten");
            assertThat(result.toolMaterials()).contains("tungsten");
        }

        @Test
        void classify_steel_isToolMaterial() {
            var result = classifier.classify("Steel");
            assertThat(result.toolMaterials()).contains("steel");
        }
    }

    // ========================= MachineClassifier =========================

    @Nested
    @DisplayName("MachineClassifier")
    class MachineClassifierTest {

        private MachineClassifier classifier;

        @BeforeEach
        void setUp() {
            classifier = new MachineClassifier();
        }

        @Test
        void classify_null_returnsEmptySets() {
            var result = classifier.classify(null);
            assertThat(result.machineTypes()).isEmpty();
            assertThat(result.machineBrands()).isEmpty();
        }

        @Test
        void classify_blank_returnsEmptySets() {
            var result = classifier.classify("  ");
            assertThat(result.machineTypes()).isEmpty();
            assertThat(result.machineBrands()).isEmpty();
        }

        @Test
        void classify_cncRouter_isMachineType() {
            var result = classifier.classify("CNC Router");
            assertThat(result.machineTypes()).contains("cnc_router");
            assertThat(result.machineBrands()).isEmpty();
        }

        @Test
        void classify_biesse_isMachineBrand() {
            var result = classifier.classify("Biesse");
            assertThat(result.machineBrands()).contains("biesse");
            assertThat(result.machineTypes()).isEmpty();
        }

        @Test
        void classify_homag_isMachineBrand() {
            var result = classifier.classify("Homag");
            assertThat(result.machineBrands()).contains("homag");
        }

        @Test
        void classify_ima_isMachineBrand() {
            var result = classifier.classify("IMA");
            assertThat(result.machineBrands()).contains("ima");
        }

        @Test
        void classify_weeke_isMachineBrand() {
            var result = classifier.classify("Weeke");
            assertThat(result.machineBrands()).contains("weeke");
        }

        @Test
        void classify_scmi_isMachineBrand() {
            var result = classifier.classify("SCMI");
            assertThat(result.machineBrands()).contains("scmi");
        }

        @Test
        void classify_mixedCommaSeparated_classifiesCorrectly() {
            var result = classifier.classify("CNC Router, Biesse, Edge Bander, Homag");
            assertThat(result.machineTypes()).containsExactlyInAnyOrder("cnc_router", "edge_bander");
            assertThat(result.machineBrands()).containsExactlyInAnyOrder("biesse", "homag");
        }

        @Test
        void classify_emptyPartsIgnored() {
            var result = classifier.classify(",, CNC Router,,");
            assertThat(result.machineTypes()).contains("cnc_router");
        }

        @Test
        void classify_morbidelli_isMachineBrand() {
            var result = classifier.classify("Morbidelli");
            assertThat(result.machineBrands()).contains("morbidelli");
        }

        @Test
        void classify_ayen_isMachineBrand() {
            var result = classifier.classify("Ayen");
            assertThat(result.machineBrands()).contains("ayen");
        }

        @Test
        void classify_scheer_isMachineBrand() {
            var result = classifier.classify("Scheer");
            assertThat(result.machineBrands()).contains("scheer");
        }

        @Test
        void classify_normalizeCode_handlesSpacesAndSpecialChars() {
            var result = classifier.classify("S.C.M.I");
            assertThat(result.machineBrands()).isNotEmpty();
            String code = result.machineBrands().iterator().next();
            assertThat(code).doesNotContain(".");
        }
    }
}

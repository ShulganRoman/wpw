package com.wpw.pim.service.cutting;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-тесты для {@link CuttingTypeNormalizer}.
 * Проверяют нормализацию сырых значений Cutting Type в стандартные коды.
 */
class CuttingTypeNormalizerTest {

    private CuttingTypeNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new CuttingTypeNormalizer();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("normalize — null, пустая строка и пробелы возвращают null")
    void normalize_nullOrBlank_returnsNull(String input) {
        assertThat(normalizer.normalize(input)).isNull();
    }

    @ParameterizedTest
    @CsvSource({
        "straight, straight",
        "Straight, straight",
        "STRAIGHT, straight",
        "standard, straight",
        "Standard, straight"
    })
    @DisplayName("normalize — straight/standard варианты")
    void normalize_straight_variants(String input, String expected) {
        assertThat(normalizer.normalize(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
        "up-shear, spiral_upcut",
        "upcut, spiral_upcut",
        "Up Cut, spiral_upcut",
        "upcut/downcut, spiral_upcut"
    })
    @DisplayName("normalize — spiral upcut варианты")
    void normalize_spiralUpcut_variants(String input, String expected) {
        assertThat(normalizer.normalize(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
        "down-cut, spiral_downcut",
        "downcut, spiral_downcut",
        "Down Cut, spiral_downcut"
    })
    @DisplayName("normalize — spiral downcut варианты")
    void normalize_spiralDowncut_variants(String input, String expected) {
        assertThat(normalizer.normalize(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
        "compression, compression",
        "up/down compression, compression"
    })
    @DisplayName("normalize — compression варианты")
    void normalize_compression_variants(String input, String expected) {
        assertThat(normalizer.normalize(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
        "trim, flush_trim",
        "flush trim, flush_trim",
        "flush-trim, flush_trim",
        "template, flush_trim"
    })
    @DisplayName("normalize — flush trim варианты")
    void normalize_flushTrim_variants(String input, String expected) {
        assertThat(normalizer.normalize(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
        "conical, conical",
        "taper-point, conical"
    })
    @DisplayName("normalize — conical варианты")
    void normalize_conical_variants(String input, String expected) {
        assertThat(normalizer.normalize(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("normalize — bevel")
    void normalize_bevel() {
        assertThat(normalizer.normalize("bevel")).isEqualTo("bevel");
    }

    @Test
    @DisplayName("normalize — left-hand")
    void normalize_leftHand() {
        assertThat(normalizer.normalize("left-hand")).isEqualTo("left_hand");
    }

    @Test
    @DisplayName("normalize — right-hand")
    void normalize_rightHand() {
        assertThat(normalizer.normalize("right-hand")).isEqualTo("right_hand");
    }

    @Test
    @DisplayName("normalize — cove")
    void normalize_cove() {
        assertThat(normalizer.normalize("cove")).isEqualTo("cove");
    }

    @Test
    @DisplayName("normalize — rabbet")
    void normalize_rabbet() {
        assertThat(normalizer.normalize("rabbet")).isEqualTo("rabbet");
    }

    @Test
    @DisplayName("normalize — dovetail")
    void normalize_dovetail() {
        assertThat(normalizer.normalize("dovetail")).isEqualTo("dovetail");
    }

    @ParameterizedTest
    @CsvSource({
        "v-groove, v_groove",
        "v groove, v_groove"
    })
    @DisplayName("normalize — v-groove варианты")
    void normalize_vGroove_variants(String input, String expected) {
        assertThat(normalizer.normalize(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("normalize — ogee")
    void normalize_ogee() {
        assertThat(normalizer.normalize("ogee")).isEqualTo("ogee");
    }

    @Test
    @DisplayName("normalize — slow spiral")
    void normalize_slowSpiral() {
        assertThat(normalizer.normalize("slow spiral")).isEqualTo("slow_spiral");
    }

    @Test
    @DisplayName("normalize — plunge")
    void normalize_plunge() {
        assertThat(normalizer.normalize("plunge")).isEqualTo("plunge");
    }

    @Test
    @DisplayName("normalize — stepped")
    void normalize_stepped() {
        assertThat(normalizer.normalize("stepped")).isEqualTo("stepped");
    }

    @Test
    @DisplayName("normalize — wavy")
    void normalize_wavy() {
        assertThat(normalizer.normalize("wavy")).isEqualTo("wavy");
    }

    @Test
    @DisplayName("normalize — неизвестный тип возвращается как lowercase trimmed")
    void normalize_unknownType_returnsLowercaseTrimmed() {
        assertThat(normalizer.normalize("  Some Custom Type  "))
            .isEqualTo("some custom type");
    }

    @Test
    @DisplayName("normalize — case insensitive")
    void normalize_caseInsensitive() {
        assertThat(normalizer.normalize("COMPRESSION")).isEqualTo("compression");
        assertThat(normalizer.normalize("Flush Trim")).isEqualTo("flush_trim");
    }

    @Test
    @DisplayName("normalize — leading/trailing spaces trimmed")
    void normalize_trimmed() {
        assertThat(normalizer.normalize("  straight  ")).isEqualTo("straight");
    }
}

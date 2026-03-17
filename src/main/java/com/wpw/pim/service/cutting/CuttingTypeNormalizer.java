package com.wpw.pim.service.cutting;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CuttingTypeNormalizer {

    private static final Map<String, String> RAW_TO_CODE = Map.ofEntries(
        Map.entry("straight", "straight"),
        Map.entry("standard", "straight"),
        Map.entry("up-shear", "spiral_upcut"),
        Map.entry("upcut", "spiral_upcut"),
        Map.entry("up cut", "spiral_upcut"),
        Map.entry("upcut/downcut", "spiral_upcut"),
        Map.entry("down-cut", "spiral_downcut"),
        Map.entry("downcut", "spiral_downcut"),
        Map.entry("down cut", "spiral_downcut"),
        Map.entry("compression", "compression"),
        Map.entry("up/down compression", "compression"),
        Map.entry("bevel", "bevel"),
        Map.entry("trim", "flush_trim"),
        Map.entry("flush trim", "flush_trim"),
        Map.entry("flush-trim", "flush_trim"),
        Map.entry("template", "flush_trim"),
        Map.entry("conical", "conical"),
        Map.entry("taper-point", "conical"),
        Map.entry("left-hand", "left_hand"),
        Map.entry("right-hand", "right_hand"),
        Map.entry("cove", "cove"),
        Map.entry("rabbet", "rabbet"),
        Map.entry("dovetail", "dovetail"),
        Map.entry("v-groove", "v_groove"),
        Map.entry("v groove", "v_groove"),
        Map.entry("ogee", "ogee"),
        Map.entry("slow spiral", "slow_spiral"),
        Map.entry("plunge", "plunge"),
        Map.entry("stepped", "stepped"),
        Map.entry("wavy", "wavy")
    );

    public String normalize(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return RAW_TO_CODE.getOrDefault(raw.toLowerCase().trim(), raw.toLowerCase().trim());
    }
}

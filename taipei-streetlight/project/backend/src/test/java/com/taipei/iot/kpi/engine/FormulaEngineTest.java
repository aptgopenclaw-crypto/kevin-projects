package com.taipei.iot.kpi.engine;

import com.taipei.iot.kpi.enums.FormulaType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FormulaEngineTest {

    private final SpelEvaluator spelEvaluator = new SpelEvaluator();
    private final FormulaEngine engine = new FormulaEngine(spelEvaluator);

    // ── SpEL happy paths ──

    @Test
    void evaluate_spel_simpleRatio() {
        BigDecimal result = engine.evaluate(FormulaType.SPEL, "#value / #target * 100",
                Map.of("value", new BigDecimal("95"), "target", new BigDecimal("100")));
        assertEquals(0, new BigDecimal("95.00").compareTo(result.setScale(2)));
    }

    @Test
    void evaluate_spel_weightedScore() {
        BigDecimal result = engine.evaluate(FormulaType.SPEL, "#value * #weight",
                Map.of("value", new BigDecimal("88.5"), "weight", new BigDecimal("0.3")));
        assertEquals(0, new BigDecimal("26.55").compareTo(result.setScale(2)));
    }

    @Test
    void evaluate_spel_complexFormula() {
        BigDecimal result = engine.evaluate(FormulaType.SPEL,
                "(#repaired / #total) * 100",
                Map.of("repaired", new BigDecimal("47"), "total", new BigDecimal("50")));
        assertEquals(0, new BigDecimal("94.00").compareTo(result.setScale(2)));
    }

    @Test
    void evaluate_spel_zeroTarget() {
        BigDecimal result = engine.evaluate(FormulaType.SPEL, "#value + #target",
                Map.of("value", new BigDecimal("100"), "target", BigDecimal.ZERO));
        assertEquals(0, new BigDecimal("100").compareTo(result));
    }

    // ── SpEL error paths ──

    @Test
    void evaluate_spel_invalidFormula_throwsException() {
        assertThrows(Exception.class, () ->
                engine.evaluate(FormulaType.SPEL, "#value / ",
                        Map.of("value", new BigDecimal("100"))));
    }

    @Test
    void evaluate_spel_missingVariable_throwsException() {
        assertThrows(Exception.class, () ->
                engine.evaluate(FormulaType.SPEL, "#value / #missing",
                        Map.of("value", new BigDecimal("100"))));
    }

    // ── JS not supported ──

    @Test
    void evaluate_js_throwsUnsupported() {
        assertThrows(UnsupportedOperationException.class, () ->
                engine.evaluate(FormulaType.JS, "value / target * 100",
                        Map.of("value", new BigDecimal("100"), "target", new BigDecimal("100"))));
    }
}

package com.taipei.iot.kpi.engine;

import com.taipei.iot.kpi.enums.FormulaType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 公式引擎統一入口，依 formulaType 分派至對應 evaluator。
 * <p>Phase 6 僅實作 SpEL；JS 分支預留，呼叫時拋出 UnsupportedOperationException (D1)。</p>
 */
@Component
@RequiredArgsConstructor
public class FormulaEngine {

    private final SpelEvaluator spelEvaluator;

    /**
     * 執行公式計算。
     *
     * @param formulaType 公式類型 (SPEL / JS)
     * @param formula     公式表達式
     * @param variables   變數綁定 (key=變數名, value=數值)
     * @return 計算結果
     */
    public BigDecimal evaluate(FormulaType formulaType, String formula, Map<String, Object> variables) {
        return switch (formulaType) {
            case SPEL -> spelEvaluator.evaluate(formula, variables);
            case JS -> throw new UnsupportedOperationException(
                    "GraalJS 公式引擎尚未啟用，請使用 SPEL 類型。如需 JS 支援請聯繫開發團隊。");
        };
    }

    /**
     * 驗證公式語法是否正確（不實際計算）。
     */
    public void validate(FormulaType formulaType, String formula) {
        switch (formulaType) {
            case SPEL -> spelEvaluator.validate(formula);
            case JS -> throw new UnsupportedOperationException(
                    "GraalJS 公式引擎尚未啟用，請使用 SPEL 類型。");
        }
    }
}

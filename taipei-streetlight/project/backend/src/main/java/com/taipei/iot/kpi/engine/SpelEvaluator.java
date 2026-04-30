package com.taipei.iot.kpi.engine;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelCompilerMode;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * SpEL 公式計算器。
 * <p>
 * 安全措施:
 * <ul>
 *   <li>使用 {@link SimpleEvaluationContext} — 禁止 T() 型別存取、禁止 bean 引用、禁止建構子呼叫</li>
 *   <li>僅允許讀取 root 物件的屬性與預先綁定的變數</li>
 *   <li>數值運算支援: +, -, *, /, %, 三元運算子, 比較運算子</li>
 * </ul>
 * </p>
 */
@Component
public class SpelEvaluator {

    private final ExpressionParser parser;

    public SpelEvaluator() {
        var config = new SpelParserConfiguration(SpelCompilerMode.MIXED, getClass().getClassLoader());
        this.parser = new SpelExpressionParser(config);
    }

    /**
     * 計算 SpEL 公式。
     *
     * @param formula   SpEL 表達式，如 "(value / target) * 100 * weight"
     * @param variables 變數綁定
     * @return 計算結果 (scale=4)
     */
    public BigDecimal evaluate(String formula, Map<String, Object> variables) {
        Expression expression = parser.parseExpression(formula);
        EvaluationContext context = buildContext(variables);
        Object result = expression.getValue(context);
        return toBigDecimal(result);
    }

    /**
     * 僅驗證公式語法，不實際計算。
     */
    public void validate(String formula) {
        parser.parseExpression(formula);
    }

    private EvaluationContext buildContext(Map<String, Object> variables) {
        SimpleEvaluationContext.Builder builder = SimpleEvaluationContext
                .forReadOnlyDataBinding()
                .withInstanceMethods();

        SimpleEvaluationContext context = builder.build();

        // 綁定變數 — 使用 #variableName 語法存取
        if (variables != null) {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                context.setVariable(entry.getKey(), toNumber(entry.getValue()));
            }
        }
        return context;
    }

    private Object toNumber(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof Number num) return num.doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return value;
        }
    }

    private BigDecimal toBigDecimal(Object result) {
        if (result == null) return BigDecimal.ZERO;
        if (result instanceof BigDecimal bd) return bd.setScale(4, RoundingMode.HALF_UP);
        if (result instanceof Number num) return BigDecimal.valueOf(num.doubleValue()).setScale(4, RoundingMode.HALF_UP);
        return new BigDecimal(result.toString()).setScale(4, RoundingMode.HALF_UP);
    }
}

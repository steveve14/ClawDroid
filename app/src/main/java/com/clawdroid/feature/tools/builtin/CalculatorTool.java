package com.clawdroid.feature.tools.builtin;

import com.clawdroid.core.model.ToolResult;
import com.clawdroid.feature.tools.tool.Tool;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;

public class CalculatorTool implements Tool {

    @Inject
    public CalculatorTool() {}

    @Override public String getName() { return "calculator"; }

    @Override
    public String getDescription() {
        return "수학 계산을 수행합니다. 사칙연산, 거듭제곱, 제곱근 등을 지원합니다.";
    }

    @Override
    public JsonObject getParameters() {
        JsonObject params = new JsonObject();
        params.addProperty("type", "object");
        JsonObject properties = new JsonObject();

        JsonObject expression = new JsonObject();
        expression.addProperty("type", "string");
        expression.addProperty("description", "계산할 수학 표현식 (예: 2+3*4, sqrt(16), 2^10)");
        properties.add("expression", expression);

        params.add("properties", properties);
        JsonArray required = new JsonArray();
        required.add("expression");
        params.add("required", required);
        return params;
    }

    @Override
    public Single<ToolResult> execute(JsonObject params) {
        return Single.fromCallable(() -> {
            String expr = params.has("expression")
                    ? params.get("expression").getAsString() : null;
            if (expr == null || expr.isEmpty()) {
                return new ToolResult("calculator", false, "expression 파라미터가 필요합니다.");
            }
            if (expr.length() > 500) {
                return new ToolResult("calculator", false, "수식이 너무 깁니다 (최대 500자).");
            }

            try {
                double result = evaluate(expr);
                String formatted = result == Math.floor(result)
                        ? String.valueOf((long) result)
                        : String.valueOf(result);
                return new ToolResult("calculator", true, formatted);
            } catch (Exception e) {
                return new ToolResult("calculator", false, "계산 오류: " + e.getMessage());
            }
        });
    }

    private double evaluate(String expr) {
        expr = expr.trim()
                .replace("sqrt(", "Math.sqrt(")
                .replace("abs(", "Math.abs(")
                .replace("^", "**");

        // Simple expression evaluator
        return evaluateExpression(expr, new int[]{0});
    }

    private double evaluateExpression(String expr, int[] pos) {
        double result = evaluateTerm(expr, pos);
        while (pos[0] < expr.length()) {
            char op = expr.charAt(pos[0]);
            if (op == '+') { pos[0]++; result += evaluateTerm(expr, pos); }
            else if (op == '-') { pos[0]++; result -= evaluateTerm(expr, pos); }
            else break;
        }
        return result;
    }

    private double evaluateTerm(String expr, int[] pos) {
        double result = evaluatePower(expr, pos);
        while (pos[0] < expr.length()) {
            char op = expr.charAt(pos[0]);
            if (op == '*' && pos[0] + 1 < expr.length() && expr.charAt(pos[0] + 1) == '*') {
                pos[0] += 2;
                result = Math.pow(result, evaluatePower(expr, pos));
            } else if (op == '*') { pos[0]++; result *= evaluatePower(expr, pos); }
            else if (op == '/') { pos[0]++; result /= evaluatePower(expr, pos); }
            else if (op == '%') { pos[0]++; result %= evaluatePower(expr, pos); }
            else break;
        }
        return result;
    }

    private double evaluatePower(String expr, int[] pos) {
        return evaluateUnary(expr, pos);
    }

    private double evaluateUnary(String expr, int[] pos) {
        skipSpaces(expr, pos);
        if (pos[0] < expr.length() && expr.charAt(pos[0]) == '-') {
            pos[0]++;
            return -evaluateAtom(expr, pos);
        }
        return evaluateAtom(expr, pos);
    }

    private double evaluateAtom(String expr, int[] pos) {
        skipSpaces(expr, pos);

        // Handle functions
        if (pos[0] < expr.length() && Character.isLetter(expr.charAt(pos[0]))) {
            int start = pos[0];
            while (pos[0] < expr.length() && (Character.isLetterOrDigit(expr.charAt(pos[0])) || expr.charAt(pos[0]) == '.')) {
                pos[0]++;
            }
            String name = expr.substring(start, pos[0]);
            if (pos[0] < expr.length() && expr.charAt(pos[0]) == '(') {
                pos[0]++; // skip '('
                double arg = evaluateExpression(expr, pos);
                if (pos[0] < expr.length() && expr.charAt(pos[0]) == ')') pos[0]++;
                return applyFunction(name, arg);
            }
            // Constants
            if ("PI".equalsIgnoreCase(name) || "pi".equals(name)) return Math.PI;
            if ("E".equalsIgnoreCase(name) || "e".equals(name)) return Math.E;
            throw new RuntimeException("Unknown: " + name);
        }

        // Handle parentheses
        if (pos[0] < expr.length() && expr.charAt(pos[0]) == '(') {
            pos[0]++;
            double result = evaluateExpression(expr, pos);
            if (pos[0] < expr.length() && expr.charAt(pos[0]) == ')') pos[0]++;
            return result;
        }

        // Parse number
        int start = pos[0];
        while (pos[0] < expr.length() && (Character.isDigit(expr.charAt(pos[0])) || expr.charAt(pos[0]) == '.')) {
            pos[0]++;
        }
        if (start == pos[0]) throw new RuntimeException("예상치 못한 문자: " + (pos[0] < expr.length() ? expr.charAt(pos[0]) : "EOF"));
        return Double.parseDouble(expr.substring(start, pos[0]));
    }

    private double applyFunction(String name, double arg) {
        switch (name.toLowerCase()) {
            case "math.sqrt": case "sqrt": return Math.sqrt(arg);
            case "math.abs": case "abs": return Math.abs(arg);
            case "sin": return Math.sin(Math.toRadians(arg));
            case "cos": return Math.cos(Math.toRadians(arg));
            case "tan": return Math.tan(Math.toRadians(arg));
            case "log": return Math.log10(arg);
            case "ln": return Math.log(arg);
            case "ceil": return Math.ceil(arg);
            case "floor": return Math.floor(arg);
            case "round": return Math.round(arg);
            default: throw new RuntimeException("Unknown function: " + name);
        }
    }

    private void skipSpaces(String expr, int[] pos) {
        while (pos[0] < expr.length() && expr.charAt(pos[0]) == ' ') pos[0]++;
    }
}

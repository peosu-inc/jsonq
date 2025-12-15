package com.peosu.fn.ds;

import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evaluates boolean expressions provided as strings.
 * Supports various operators including arithmetic (+, -, *, /, ^),
 * comparison (>, <, ==, !=, >=, <=), logical (&, |, !, &&, ||),
 * and string-specific operators (~ for pattern matching).
 */
class BoolEvaluator {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\d+\\.?\\d*|'[^']*'|[a-zA-Z]+|[+\\-*/^<>!=&|~]+|[()]");
    private static final Pattern POSTFIX_TOKEN_PATTERN = Pattern.compile("\\d+\\.?\\d*|'[^']*'|[a-zA-Z]+|[+\\-*/^()<>!=&|~]+");

    /**
     * Evaluates the given boolean expression.
     * <p>
     * Converts the input string from infix to postfix notation and evaluates it.
     * Operands can be numbers, strings (enclosed in single quotes), or variables.
     * </p>
     *
     * @param expression the boolean expression to evaluate
     * @return true if the expression evaluates to true, false otherwise
     * @throws IllegalArgumentException if the expression contains invalid tokens or unsupported operations
     */
    public boolean evaluate(String expression) {
        return evaluatePostfix(toPostfix(expression));
    }

    /**
     * Handles an operator by appending it to the output or pushing it to the stack based on precedence.
     *
     * @param output the StringBuilder holding the postfix output
     * @param operators the stack of operators
     * @param token the current operator token
     */
    private void handleOperator(StringBuilder output, Stack<String> operators, String token) {
        while (!operators.isEmpty() &&
                !operators.peek().equals("(") &&
                precedence(operators.peek()) >= precedence(token)) {
            output.append(operators.pop()).append(' ');
        }
        operators.push(token);
    }

    /**
     * Converts the given infix expression to postfix notation (Reverse Polish Notation).
     *
     * @param infix the infix expression to convert
     * @return the postfix representation of the expression as a string
     * @throws IllegalArgumentException if an unexpected token is encountered
     */
    private String toPostfix(String infix) {
        StringBuilder output = new StringBuilder();
        Stack<String> operators = new Stack<>();
        Matcher matcher = TOKEN_PATTERN.matcher(infix);

        while (matcher.find()) {
            String token = matcher.group();
            if (token.matches("\\d+\\.?\\d*|'[^']*'|[a-zA-Z]+"))
                output.append(token).append(' ');
            else if (token.equals(")") || token.equals("("))
                handleParenthesis(output, operators, token);
            else if (isOperator(token))
                handleOperator(output, operators, token);
            else
                throw new IllegalArgumentException("Unexpected token: " + token);
        }

        while (!operators.isEmpty()) {
            output.append(operators.pop()).append(' ');
        }
        return output.toString();
    }

    /**
     * Handles parentheses in the infix-to-postfix conversion process.
     *
     * @param output the StringBuilder holding the postfix output
     * @param operators the stack of operators
     * @param parenthesis the parenthesis token ("(" or ")")
     */
    private static void handleParenthesis(StringBuilder output, Stack<String> operators, String parenthesis) {
        if (parenthesis.equals("("))
            operators.push(parenthesis);
        else if (parenthesis.equals(")")) {
            while (!operators.isEmpty() && !operators.peek().equals("(")) {
                output.append(operators.pop()).append(' ');
            }
            if (!operators.isEmpty() && operators.peek().equals("(")) {
                operators.pop();
            }
        }
    }

    /**
     * Returns the precedence level of the given operator.
     *
     * @param operator the operator to check
     * @return the precedence level (e.g., 1 for +, -, 4 for comparison operators)
     */
    private static int precedence(String operator) {
        return switch (operator) {
            case "+", "-" -> 1;
            case "*", "/" -> 2;
            case "^" -> 3;
            case "<", ">", "<=", ">=", "==", "!=", "~" -> 4;
            case "!", "&&", "||", "&", "|" -> 5;
            default -> -1;
        };
    }

    /**
     * Evaluates the given postfix expression.
     *
     * @param postfix the postfix expression to evaluate
     * @return true if the expression evaluates to true, false otherwise
     * @throws IllegalArgumentException if an unexpected token or unsupported operation is encountered
     */
    private boolean evaluatePostfix(String postfix) {
        Stack<Object> stack = new Stack<>();
        Matcher matcher = POSTFIX_TOKEN_PATTERN.matcher(postfix);

        while (matcher.find()) {
            String token = matcher.group();
            if (token.equals("true") || token.equals("false"))
                stack.push(token.equals("true") ? 1.0 : 0.0);
            else if (token.matches("\\d+\\.?\\d*"))
                stack.push(Double.parseDouble(token));
            else if (token.matches("'[^']*'"))
                stack.push(token.substring(1, token.length() - 1));
            else if (isOperator(token))
                operate(token, stack);
            else if (token.matches("\\w+"))
                stack.push(token);
            else
                throw new IllegalArgumentException("Unexpected token: " + token);
        }
        Object result = stack.pop();
        return result instanceof Double && ((Double) result) != 0.0;
    }

    /**
     * Applies an operator to operands on the stack.
     *
     * @param token the operator token
     * @param stack the stack of operands
     * @throws IllegalArgumentException if the operator is unsupported or operands are incompatible
     */
    private void operate(String token, Stack<Object> stack) {
        String error = "Unsupported operand type for operator: %s operand %s and %s";
        Object a = null, b = stack.pop();
        double answer;

        if (token.equals("!") && (b instanceof Double))
            answer = (double) b == 0 ? 1.0 : 0.0;
        else {
            a = stack.pop();
            answer = a instanceof Double && b instanceof Double
                    ? applyOperator(token, (Double) a, (Double) b)
                    : isStringOperator(token) ? applyStringOperator(token, String.valueOf(a), String.valueOf(b))
                    : Double.MIN_VALUE;
        }
        if (answer == Double.MIN_VALUE) {
            throw new IllegalArgumentException(String.format(error, token, a, b));
        }
        stack.push(answer);
    }

    /**
     * Checks if the given token is an operator.
     *
     * @param token the token to check
     * @return true if the token is an operator, false otherwise
     */
    private boolean isOperator(String token) {
        return token.matches("[+\\-*/^<>!=&|~]+");
    }

    /**
     * Checks if the given token is a string-compatible operator.
     *
     * @param token the token to check
     * @return true if the token is a string operator, false otherwise
     */
    private boolean isStringOperator(String token) {
        return token.matches("[<>=~!]+");
    }

    /**
     * Applies the given operator to two numeric operands.
     *
     * @param op the operator
     * @param a the first operand
     * @param b the second operand
     * @return the result of the operation (1.0 for true, 0.0 for false for comparisons)
     * @throws IllegalArgumentException if the operator is unsupported
     */
    private double applyOperator(String op, double a, double b) {
        switch (op) {
            case "+": return a + b;
            case "-": return a - b;
            case "*": return a * b;
            case "/": return a / b;
            case ">": return a > b ? 1.0 : 0.0;
            case "<": return a < b ? 1.0 : 0.0;
            case "==": return a == b ? 1.0 : 0.0;
            case "!=": return a != b ? 1.0 : 0.0;
            case ">=": return a >= b ? 1.0 : 0.0;
            case "<=": return a <= b ? 1.0 : 0.0;
            case "&": case "&&": return (a != 0 && b != 0) ? 1.0 : 0.0;
            case "|": case "||": return (a != 0 || b != 0) ? 1.0 : 0.0;
            default: throw new IllegalArgumentException("Unsupported operator: " + op);
        }
    }

    /**
     * Applies the given operator to two string operands.
     *
     * @param op the operator
     * @param a the first operand
     * @param b the second operand
     * @return the result of the operation as a double (1.0 for true, 0.0 for false)
     * @throws IllegalArgumentException if the operator is unsupported for strings
     */
    private double applyStringOperator(String op, String a, String b) {
        switch (op) {
            case "==": return a.equals(b) ? 1.0 : 0.0;
            case "!=": return !a.equals(b) ? 1.0 : 0.0;
            case ">": return a.compareTo(b) > 0 ? 1.0 : 0.0;
            case "<": return a.compareTo(b) < 0 ? 1.0 : 0.0;
            case ">=": return a.compareTo(b) >= 0 ? 1.0 : 0.0;
            case "<=": return a.compareTo(b) <= 0 ? 1.0 : 0.0;
            case "~": return a.matches(b) ? 1.0 : 0.0;
            default: throw new IllegalArgumentException("Unsupported operator for strings: " + op);
        }
    }
}
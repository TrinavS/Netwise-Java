package isp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Small JSON reader/writer for this dependency-free teaching project. */
public final class Json {
    private Json() { }

    public static Object parse(String source) {
        Parser parser = new Parser(source == null ? "" : source);
        Object value = parser.value();
        parser.space();
        if (!parser.done()) throw new IllegalArgumentException("Unexpected content after JSON value.");
        return value;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> object(Object value) {
        if (!(value instanceof Map<?, ?>)) throw new IllegalArgumentException("Expected a JSON object.");
        return (Map<String, Object>) value;
    }

    public static String stringify(Object value) {
        StringBuilder out = new StringBuilder();
        write(value, out);
        return out.toString();
    }

    private static void write(Object value, StringBuilder out) {
        if (value == null) { out.append("null"); return; }
        if (value instanceof String text) { quote(text, out); return; }
        if (value instanceof Number || value instanceof Boolean) { out.append(value); return; }
        if (value instanceof Map<?, ?> map) {
            out.append('{'); boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) out.append(','); first = false;
                quote(String.valueOf(entry.getKey()), out); out.append(':'); write(entry.getValue(), out);
            }
            out.append('}'); return;
        }
        if (value instanceof Collection<?> values) {
            out.append('['); boolean first = true;
            for (Object item : values) { if (!first) out.append(','); first = false; write(item, out); }
            out.append(']'); return;
        }
        if (value.getClass().isArray()) {
            out.append('[');
            for (int i = 0; i < java.lang.reflect.Array.getLength(value); i++) {
                if (i > 0) out.append(','); write(java.lang.reflect.Array.get(value, i), out);
            }
            out.append(']'); return;
        }
        quote(String.valueOf(value), out);
    }

    private static void quote(String value, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> { if (c < 0x20) out.append(String.format("\\u%04x", (int) c)); else out.append(c); }
            }
        }
        out.append('"');
    }

    private static final class Parser {
        private final String input;
        private int index;
        Parser(String input) { this.input = input; }
        boolean done() { return index >= input.length(); }
        void space() { while (!done() && Character.isWhitespace(input.charAt(index))) index++; }

        Object value() {
            space();
            if (done()) throw new IllegalArgumentException("JSON body is empty.");
            char c = input.charAt(index);
            if (c == '{') return objectValue();
            if (c == '[') return arrayValue();
            if (c == '"') return stringValue();
            if (input.startsWith("true", index)) { index += 4; return true; }
            if (input.startsWith("false", index)) { index += 5; return false; }
            if (input.startsWith("null", index)) { index += 4; return null; }
            return numberValue();
        }

        private Map<String, Object> objectValue() {
            LinkedHashMap<String, Object> map = new LinkedHashMap<>(); index++;
            space(); if (take('}')) return map;
            while (true) {
                space(); if (done() || input.charAt(index) != '"') fail("Expected an object key.");
                String key = stringValue(); space(); require(':'); map.put(key, value()); space();
                if (take('}')) return map; require(',');
            }
        }

        private List<Object> arrayValue() {
            ArrayList<Object> list = new ArrayList<>(); index++;
            space(); if (take(']')) return list;
            while (true) { list.add(value()); space(); if (take(']')) return list; require(','); }
        }

        private String stringValue() {
            require('"'); StringBuilder out = new StringBuilder();
            while (!done()) {
                char c = input.charAt(index++);
                if (c == '"') return out.toString();
                if (c != '\\') { out.append(c); continue; }
                if (done()) fail("Invalid string escape.");
                char escaped = input.charAt(index++);
                switch (escaped) {
                    case '"', '\\', '/' -> out.append(escaped);
                    case 'b' -> out.append('\b'); case 'f' -> out.append('\f');
                    case 'n' -> out.append('\n'); case 'r' -> out.append('\r'); case 't' -> out.append('\t');
                    case 'u' -> {
                        if (index + 4 > input.length()) fail("Invalid Unicode escape.");
                        try { out.append((char) Integer.parseInt(input.substring(index, index + 4), 16)); }
                        catch (NumberFormatException ex) { fail("Invalid Unicode escape."); }
                        index += 4;
                    }
                    default -> fail("Invalid string escape.");
                }
            }
            fail("Unterminated string."); return "";
        }

        private Number numberValue() {
            int start = index;
            if (!done() && input.charAt(index) == '-') index++;
            while (!done() && Character.isDigit(input.charAt(index))) index++;
            if (!done() && input.charAt(index) == '.') { index++; while (!done() && Character.isDigit(input.charAt(index))) index++; }
            if (!done() && (input.charAt(index) == 'e' || input.charAt(index) == 'E')) {
                index++; if (!done() && (input.charAt(index) == '+' || input.charAt(index) == '-')) index++;
                while (!done() && Character.isDigit(input.charAt(index))) index++;
            }
            if (start == index) fail("Expected a JSON value.");
            try { return Double.valueOf(input.substring(start, index)); }
            catch (NumberFormatException ex) { fail("Invalid number."); return 0; }
        }

        private boolean take(char expected) { if (!done() && input.charAt(index) == expected) { index++; return true; } return false; }
        private void require(char expected) { if (!take(expected)) fail("Expected '" + expected + "'."); }
        private void fail(String message) { throw new IllegalArgumentException(message + " At character " + index + "."); }
    }
}

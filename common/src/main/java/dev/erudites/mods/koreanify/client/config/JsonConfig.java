package dev.erudites.mods.koreanify.client.config;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.erudites.mods.koreanify.client.KoreanifyClientMod;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class JsonConfig<T extends JsonConfig<T>> {

    private static final Gson GSON = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create();

    private Path configDir;

    protected abstract String fileName();

    public T setup(Path configDir) {
        T loaded = this.loadFrom(configDir);
        ((JsonConfig<?>) loaded).configDir = configDir;
        return loaded;
    }

    public void saveConfig() {
        if (this.configDir != null) {
            this.saveTo(this.configDir);
        }
    }

    @SuppressWarnings("unchecked")
    public T loadFrom(Path configDir) {
        Path file = configDir.resolve(this.fileName());
        if (!Files.exists(file)) {
            this.saveTo(configDir);
            return (T) this;
        }
        try {
            String raw = Files.readString(file);
            T loaded = (T) GSON.fromJson(stripComments(raw), this.getClass());
            if (loaded == null) {
                loaded = (T) this;
            }
            loaded.saveTo(configDir);
            return loaded;
        } catch (Exception e) {
            KoreanifyClientMod.LOGGER.warn("Failed to load config '{}', using defaults", this.fileName(), e);
            return (T) this;
        }
    }

    public void saveTo(Path configDir) {
        Path file = configDir.resolve(this.fileName());
        try {
            Files.createDirectories(configDir);
            String content;
            if (Files.exists(file)) {
                String existing = Files.readString(file)
                    .replace("\r\n", "\n")
                    .replace("\r", "\n");
                content = Merger.merge(existing, this);
            } else {
                content = serialize(this);
            }
            Files.writeString(file, content);
        } catch (IOException e) {
            KoreanifyClientMod.LOGGER.warn("Failed to save config '{}'", this.fileName(), e);
        }
    }

    private static final class Merger {
        private final JsonConfig<?> config;
        private final Field[] topFields;
        private final String[] lines;
        private final List<String> output;

        private Merger(String existing, JsonConfig<?> config) {
            this.config = config;
            this.topFields = instanceFields(config.getClass());
            this.lines = existing.split("\n", -1);
            this.output = new ArrayList<>(lines.length + 16);
        }

        static String merge(String existing, JsonConfig<?> config) {
            return new Merger(existing, config).run();
        }

        private String run() {
            Set<String> handledTopKeys = new HashSet<>();
            int i = 0;
            while (i < lines.length) {
                String line = lines[i];
                if (isClosingBrace(line.trim())) {
                    insertMissingFields(topFields, config, "  ", handledTopKeys);
                    output.add(line);
                    i++;
                } else {
                    int next = tryMatchTopField(i, handledTopKeys);
                    if (next > i) {
                        i = next;
                    } else {
                        output.add(line);
                        i++;
                    }
                }
            }
            return String.join("\n", output);
        }

        private int tryMatchTopField(int i, Set<String> handledTopKeys) {
            String line = lines[i];
            for (Field topField : topFields) {
                String key = toSnakeCase(topField);
                if (!handledTopKeys.contains(key)) {
                    try {
                        Object value = topField.get(config);
                        boolean isCategory = isConfigCategory(topField.getType());
                        if (isCategory && matchesCategoryStart(line, key)) {
                            handledTopKeys.add(key);
                            output.add(line);
                            return mergeCategoryBlock(i + 1, topField.getType(), value);
                        } else if (!isCategory && matchesFieldLine(line, key)) {
                            handledTopKeys.add(key);
                            output.add(replaceValue(line, key, value));
                            return i + 1;
                        }
                    } catch (IllegalAccessException _) {}
                }
            }
            return i;
        }

        private int mergeCategoryBlock(int i, Class<?> catType, Object catObj) {
            Field[] catFields = instanceFields(catType);
            Set<String> handledCatKeys = new HashSet<>();
            while (i < lines.length) {
                String line = lines[i];
                if (isClosingBrace(line.trim())) {
                    insertMissingFields(catFields, catObj, "    ", handledCatKeys);
                    output.add(line);
                    return i + 1;
                }
                int next = tryMatchCatField(line, i, catFields, catObj, handledCatKeys);
                if (next > i) {
                    i = next;
                } else {
                    output.add(line);
                    i++;
                }
            }
            return i;
        }

        private int tryMatchCatField(String line, int i, Field[] catFields, Object catObj, Set<String> handledCatKeys) {
            for (Field catField : catFields) {
                String key = toSnakeCase(catField);
                if (!handledCatKeys.contains(key) && !isConfigCategory(catField.getType()) && matchesFieldLine(line, key)) {
                    try {
                        handledCatKeys.add(key);
                        output.add(replaceValue(line, key, catField.get(catObj)));
                        return i + 1;
                    } catch (IllegalAccessException _) {}
                }
            }
            return i;
        }

        private void insertMissingFields(Field[] fields, Object obj, String indent, Set<String> handled) {
            List<Field> missing = new ArrayList<>();
            for (Field f : fields) {
                if (!handled.contains(toSnakeCase(f))) {
                    missing.add(f);
                }
            }
            if (missing.isEmpty()) {
                return;
            }
            addTrailingCommaIfNeeded();
            for (int j = 0; j < missing.size(); j++) {
                StringBuilder sb = new StringBuilder();
                appendComment(sb, missing.get(j), indent);
                appendEntry(sb, missing.get(j), obj, indent, j < missing.size() - 1);
                output.addAll(Arrays.asList(sb.toString().stripTrailing().split("\n", -1)));
            }
        }

        private void addTrailingCommaIfNeeded() {
            for (int k = output.size() - 1; k >= 0; k--) {
                String t = output.get(k).trim();
                if (!t.isEmpty() && !t.startsWith("//")) {
                    if (!t.endsWith("{") && !t.endsWith(",")) {
                        output.set(k, output.get(k) + ",");
                    }
                    break;
                }
            }
        }

        private static boolean isClosingBrace(String trimmed) {
            return trimmed.equals("}") || trimmed.equals("},");
        }

        private static boolean matchesCategoryStart(String line, String key) {
            return line.matches("\\s*\"" + Pattern.quote(key) + "\"\\s*:\\s*\\{.*");
        }

        private static boolean matchesFieldLine(String line, String key) {
            return line.matches("\\s*\"" + Pattern.quote(key) + "\"\\s*:[^{].*");
        }

        private static String replaceValue(String line, String key, Object newValue) {
            Pattern pattern = Pattern.compile("^(\\s*\"" + Pattern.quote(key) + "\"\\s*:\\s*)(.*?)(,?)(\\s*)$");
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                return matcher.group(1) + formatValue(newValue) + matcher.group(3) + matcher.group(4);
            }
            return line;
        }

        private static String toSnakeCase(Field field) {
            return FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES.translateName(field);
        }
    }

    private static String serialize(JsonConfig<?> config) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        appendFields(builder, config, "  ");
        builder.append("}\n");
        return builder.toString();
    }

    private static void appendFields(StringBuilder builder, Object obj, String indent) {
        Field[] fields = instanceFields(obj.getClass());
        for (int i = 0; i < fields.length; i++) {
            appendComment(builder, fields[i], indent);
            appendEntry(builder, fields[i], obj, indent, i < fields.length - 1);
        }
    }

    private static void appendComment(StringBuilder builder, Field field, String indent) {
        Comment comment = field.getAnnotation(Comment.class);
        if (comment == null) {
            return;
        }
        for (String line : comment.value()) {
            builder.append(indent).append("// ").append(line).append("\n");
        }
        appendValueHint(builder, field.getType(), indent);
    }

    private static void appendValueHint(StringBuilder builder, Class<?> type, String indent) {
        if (type == boolean.class || type == Boolean.class) {
            builder.append(indent).append("// values: true / false\n");
        } else if (type.isEnum()) {
            String values = Arrays.stream(type.getEnumConstants())
                .map(Object::toString)
                .collect(Collectors.joining(", ", "[", "]"));
            builder.append(indent).append("// values: ").append(values).append("\n");
        }
    }

    private static void appendEntry(StringBuilder builder, Field field, Object obj, String indent, boolean trailingComma) {
        String key = FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES.translateName(field);
        String comma = trailingComma ? "," : "";
        try {
            Object value = field.get(obj);
            if (isConfigCategory(field.getType())) {
                Category category = field.getType().getAnnotation(Category.class);
                if (category != null) {
                    for (String line : category.value()) {
                        builder.append(indent)
                            .append("// ")
                            .append(line)
                            .append("\n");
                    }
                }
                builder.append(indent)
                    .append("\"")
                    .append(key)
                    .append("\": {\n");
                appendFields(builder, value, indent + "  ");
                builder.append(indent)
                    .append("}")
                    .append(comma)
                    .append("\n");
            } else {
                builder.append(indent)
                    .append("\"")
                    .append(key)
                    .append("\": ")
                    .append(formatValue(value))
                    .append(comma)
                    .append("\n");
            }
        } catch (IllegalAccessException e) {
            KoreanifyClientMod.LOGGER.warn("Failed to serialize config field '{}'", field.getName(), e);
        }
    }

    private static String formatValue(Object value) {
        return switch (value) {
            case String s -> "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
            default -> String.valueOf(value);
        };
    }

    private static String stripComments(String source) {
        StringBuilder builder = new StringBuilder(source.length());
        int i = 0;
        while (i < source.length()) {
            char c = source.charAt(i);
            if (c == '"') {
                i = copyString(source, builder, i);
            } else if (c == '/' && i + 1 < source.length()) {
                i = skipOrCopySlash(source, builder, i);
            } else {
                builder.append(c);
                i++;
            }
        }
        return builder.toString();
    }

    private static int copyString(String source, StringBuilder builder, int i) {
        builder.append(source.charAt(i++));
        while (i < source.length()) {
            char c = source.charAt(i++);
            builder.append(c);
            if (c == '\\' && i < source.length()) {
                builder.append(source.charAt(i++));
            } else if (c == '"') {
                break;
            }
        }
        return i;
    }

    private static int skipOrCopySlash(String source, StringBuilder builder, int i) {
        char next = source.charAt(i + 1);
        if (next == '/') {
            while (i < source.length() && source.charAt(i) != '\n') {
                i++;
            }
            return i;
        }
        if (next == '*') {
            i += 2;
            while (i + 1 < source.length() && !(source.charAt(i) == '*' && source.charAt(i + 1) == '/')) {
                i++;
            }
            return i + 2;
        }
        builder.append(source.charAt(i));
        return i + 1;
    }

    private static boolean isConfigCategory(Class<?> type) {
        return type.isAnnotationPresent(Category.class);
    }

    private static Field[] instanceFields(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
            .filter(f -> !Modifier.isStatic(f.getModifiers()))
            .toArray(Field[]::new);
    }
}

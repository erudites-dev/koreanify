package dev.erudites.mods.koreanify.client.config;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import dev.erudites.mods.koreanify.client.KoreanifyClientMod;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

public abstract class JsonConfig<T extends JsonConfig<T>> {

    private Path configDir;

    protected abstract String fileName();

    @SuppressWarnings("unchecked")
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
            T loaded = (T) new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create()
                .fromJson(stripComments(raw), this.getClass());
            return loaded != null ? loaded : (T) this;
        } catch (Exception e) {
            KoreanifyClientMod.LOGGER.warn("Failed to load config '{}', using defaults", this.fileName(), e);
            return (T) this;
        }
    }

    public void saveTo(Path configDir) {
        Path file = configDir.resolve(this.fileName());
        try {
            Files.createDirectories(configDir);
            Files.writeString(file, serialize(this));
        } catch (IOException e) {
            KoreanifyClientMod.LOGGER.warn("Failed to save config '{}'", this.fileName(), e);
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
        if (comment == null) return;
        for (String line : comment.value()) {
            builder.append(indent).append("// ").append(line).append("\n");
        }
        appendValueHint(builder, field.getType(), indent);
    }

    private static void appendValueHint(StringBuilder builder, Class<?> type, String indent) {
        if (type == boolean.class || type == Boolean.class) {
            builder.append(indent).append("// true / false\n");
        } else if (type.isEnum()) {
            String values = Arrays.stream(type.getEnumConstants())
                .map(Object::toString)
                .collect(Collectors.joining(" / "));
            builder.append(indent).append("// ").append(values).append("\n");
        }
    }

    private static void appendEntry(StringBuilder builder, Field field, Object obj, String indent, boolean trailingComma) {
        String key = camelToSnake(field.getName());
        String comma = trailingComma ? "," : "";
        try {
            Object value = field.get(obj);
            if (isConfigCategory(field.getType())) {
                builder.append(indent).append("\"").append(key).append("\": {\n");
                appendFields(builder, value, indent + "  ");
                builder.append(indent).append("}").append(comma).append("\n");
            } else {
                builder.append(indent).append("\"").append(key).append("\": ")
                    .append(formatValue(value)).append(comma).append("\n");
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

    private static boolean isConfigCategory(Class<?> type) {
        Class<?> enclosing = type.getEnclosingClass();
        return enclosing != null && JsonConfig.class.isAssignableFrom(enclosing);
    }

    private static Field[] instanceFields(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
            .filter(f -> !Modifier.isStatic(f.getModifiers()))
            .toArray(Field[]::new);
    }

    private static String camelToSnake(String name) {
        return name.replaceAll("([A-Z])", "_$1").toLowerCase();
    }

    private static String stripComments(String source) {
        StringBuilder sb = new StringBuilder(source.length());
        int i = 0;
        while (i < source.length()) {
            char c = source.charAt(i);
            if (c == '"') {
                i = copyString(source, sb, i);
            } else if (c == '/' && i + 1 < source.length()) {
                i = skipOrCopySlash(source, sb, i);
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
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
            while (i < source.length() && source.charAt(i) != '\n') i++;
            return i;
        }
        if (next == '*') {
            i += 2;
            while (i + 1 < source.length() && !(source.charAt(i) == '*' && source.charAt(i + 1) == '/')) i++;
            return i + 2;
        }
        builder.append(source.charAt(i));
        return i + 1;
    }
}

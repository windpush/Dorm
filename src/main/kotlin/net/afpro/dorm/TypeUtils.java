package net.afpro.dorm;

import net.afpro.dorm.annotation.DormTarget;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

public class TypeUtils {
    public static int TYPE_OTHER = 0;
    public static int TYPE_PURE_PRIMITIVE = 1;
    public static int TYPE_BOXED_PRIMITIVE = 2;
    public static int TYPE_PRIMITIVE = 3;
    public static int TYPE_ARRAY = 4;
    public static int TYPE_STRING = 8;
    public static int TYPE_DORM_TARGET = 16;
    public static int TYPE_NULL = 32;
    public static int TYPE_VOID = 64;
    public static int TYPE_INTERFACE = 128;
    public static int TYPE_ABSTRACT_CLASS = 256;

    public static int checkType(Class<?> type) {
        if (type == null) {
            return TYPE_NULL;
        }

        if (type.isArray()) {
            return TYPE_ARRAY;
        }

        if (type == void.class || type == Void.class) {
            return TYPE_VOID;
        }

        if (type.isAssignableFrom(String.class)) {
            return TYPE_STRING;
        }

        if (type == byte.class
                || type == char.class
                || type == short.class
                || type == int.class
                || type == long.class
                || type == float.class
                || type == double.class) {
            return TYPE_PURE_PRIMITIVE;
        }

        if (type == Byte.class
                || type == Character.class
                || type == Short.class
                || type == Integer.class
                || type == Long.class
                || type == Float.class
                || type == Double.class) {
            return TYPE_BOXED_PRIMITIVE;
        }

        final int classCheckResult =
                (DormTarget.class.isAssignableFrom(type) ? TYPE_DORM_TARGET : 0)
                        | (type.isInterface() ? TYPE_INTERFACE : 0)
                        | (Modifier.isAbstract(type.getModifiers()) ? TYPE_ABSTRACT_CLASS : 0);
        if (classCheckResult != 0) {
            return classCheckResult;
        }

        return TYPE_OTHER;
    }

    public static Class<?> wrap(Class<?> type) {
        if (type == byte.class)
            return Byte.class;
        if (type == char.class)
            return Character.class;
        if (type == short.class)
            return Short.class;
        if (type == int.class)
            return Integer.class;
        if (type == long.class)
            return Long.class;
        if (type == float.class)
            return Float.class;
        if (type == double.class)
            return Double.class;
        return type;
    }

    public static Object parse(Class<?> type, String text) {
        if (type == byte.class || type == Byte.class)
            return Byte.valueOf(text);
        if (type == char.class || type == Character.class) {
            if (text.length() != 1) {
                throw new RuntimeException(String.format("'%s' is not a char", text));
            }
            return text.charAt(0);
        }
        if (type == short.class || type == Short.class)
            return Short.valueOf(text);
        if (type == int.class || type == Integer.class)
            return Integer.valueOf(text);
        if (type == long.class || type == Long.class)
            return Long.valueOf(text);
        if (type == float.class || type == Float.class)
            return Float.valueOf(text);
        if (type == double.class || type == Double.class)
            return Double.valueOf(text);
        throw new RuntimeException(String.format("parse invalid type %s", type == null ? "null" : type.getName()));
    }
}

package org.simdjson;

import org.simdjson.annotations.JsonFieldName;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ResolvedClass {

    public enum ResolvedClassCategory {
        BOOLEAN_PRIMITIVE(boolean.class, new boolean[0]),
        BOOLEAN(Boolean.class, new Boolean[0]),
        BYTE_PRIMITIVE(byte.class, new byte[0]),
        BYTE(Byte.class, new Byte[0]),
        CHAR_PRIMITIVE(char.class, new char[0]),
        CHAR(Character.class, new Character[0]),
        SHORT_PRIMITIVE(short.class, new short[0]),
        SHORT(Short.class, new Short[0]),
        INT_PRIMITIVE(int.class, new int[0]),
        INT(Integer.class, new Integer[0]),
        LONG_PRIMITIVE(long.class, new long[0]),
        LONG(Long.class, new Long[0]),
        DOUBLE_PRIMITIVE(double.class, new double[0]),
        DOUBLE(Double.class, new Double[0]),
        FLOAT_PRIMITIVE(float.class, new float[0]),
        FLOAT(Float.class, new Float[0]),
        STRING(String.class, new String[0]),
        CUSTOM(null, null),
        ARRAY(null, null),
        LIST(List.class, null);

        private final Class<?> cclass;
        private final Object emptyArray;

        ResolvedClassCategory(Class<?> cclass, Object emptyArray) {
            this.cclass = cclass;
            this.emptyArray = emptyArray;
        }

        Object getEmptyArray() {
            return emptyArray;
        }
    }

    private final ResolvedClassCategory classCategory;
    private final Class<?> rawClass;
    private final ResolvedClass elementClass;
    private final Constructor<?> constructor;
    private final ConstructorArgumentsMap argumentsMap;

    ResolvedClass(Type targetType, ClassResolver classResolver) {
        if (targetType instanceof ParameterizedType parameterizedType) {
            rawClass = (Class<?>) parameterizedType.getRawType();
            elementClass = resolveElementClass(parameterizedType, classResolver);
        } else {
            rawClass = (Class<?>) targetType;
            elementClass = resolveElementClass(rawClass, classResolver);
        }

        classCategory = resolveClassType(rawClass);
        if (classCategory == ResolvedClassCategory.CUSTOM) {
            checkIfCustomClassIsSupported(rawClass);
            constructor = rawClass.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            Parameter[] parameters = constructor.getParameters();
            argumentsMap = new ConstructorArgumentsMap(parameters.length);
            for (int i = 0; i < parameters.length; i++) {
                Type parameterType = parameters[i].getAnnotatedType().getType();
                String fieldName = resolveFieldName(parameters[i], rawClass);
                byte[] fieldNameBytes = fieldName.getBytes(StandardCharsets.UTF_8);
                argumentsMap.put(fieldNameBytes, new ConstructorArgument(i, classResolver.resolveClass(parameterType)));
            }
        } else {
            constructor = null;
            argumentsMap = null;
        }
    }

    private static ResolvedClass resolveElementClass(ParameterizedType parameterizedType, ClassResolver classResolver) {
        if (parameterizedType.getRawType() != List.class) {
            throw new JsonParsingException("Parametrized types other than java.util.List are not supported.");
        }
        return classResolver.resolveClass(parameterizedType.getActualTypeArguments()[0]);
    }

    private static ResolvedClass resolveElementClass(Class<?> cls, ClassResolver classResolver) {
        if (cls == List.class) {
            throw new JsonParsingException("Undefined list element type.");
        }
        if (cls.componentType() != null) {
            return classResolver.resolveClass(cls.componentType());
        } else {
            return null;
        }
    }

    private static ResolvedClassCategory resolveClassType(Class<?> cls) {
        if (Iterable.class.isAssignableFrom(cls) && cls != List.class) {
            throw new JsonParsingException("Unsupported class: " + cls.getName() +
                    ". For JSON arrays at the root, use Java arrays. For inner JSON arrays, use either Java arrays or java.util.List.");
        }
        if (cls.isArray()) {
            return ResolvedClassCategory.ARRAY;
        }
        for (ResolvedClassCategory t : ResolvedClassCategory.values()) {
            if (t.cclass == cls) {
                return t;
            }
        }
        return ResolvedClassCategory.CUSTOM;
    }

    private static void checkIfCustomClassIsSupported(Class<?> cls) {
        int modifiers = cls.getModifiers();
        if (cls.isMemberClass() && !Modifier.isStatic(modifiers)) {
            throw new JsonParsingException("Unsupported class: " + cls.getName() + ". Inner non-static classes are not supported.");
        }
        if (Modifier.isAbstract(modifiers) || Modifier.isInterface(modifiers)) {
            throw new JsonParsingException("Unsupported class: " + cls.getName() + ". Interfaces and abstract classes are not supported.");
        }
        Constructor<?>[] constructors = cls.getDeclaredConstructors();
        if (constructors.length > 1) {
            throw new JsonParsingException("Class: " + cls.getName() + " has more than one constructor.");
        }
        if (constructors.length == 0) {
            throw new JsonParsingException("Class: " + cls.getName() + " doesn't have any constructor.");
        }
    }

    private static String resolveFieldName(Parameter parameter, Class<?> targetClass) {
        JsonFieldName annotation = parameter.getAnnotation(JsonFieldName.class);
        if (annotation != null) {
            return annotation.value();
        }
        if (!targetClass.isRecord()) {
            throw new JsonParsingException("Some of " + targetClass.getName() + "'s constructor arguments are not annotated with @JsonFieldName.");
        }
        return parameter.getName();
    }

    ConstructorArgumentsMap getArgumentsMap() {
        return argumentsMap;
    }

    Constructor<?> getConstructor() {
        return constructor;
    }

    ResolvedClassCategory getClassCategory() {
        return classCategory;
    }

    ResolvedClass getElementClass() {
        return elementClass;
    }

    Class<?> getRawClass() {
        return rawClass;
    }
}

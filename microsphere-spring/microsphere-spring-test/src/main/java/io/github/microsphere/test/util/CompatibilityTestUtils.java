package io.github.microsphere.test.util;

import org.slf4j.Logger;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * 兼容性测试工具
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @since 1.0.0
 */
public class CompatibilityTestUtils {

    public static final Predicate<Method> PUBLIC_METHOD_FILTER = method -> Modifier.isPublic(method.getModifiers())
            && !Object.class.equals(method.getDeclaringClass());

    public static final Predicate<Field> PUBLIC_STATIC_FIELD_FILTER = field -> {
        int modifiers = field.getModifiers();
        return Modifier.isStatic(modifiers) &&
                Modifier.isPublic(modifiers) &&
                !field.getType().equals(Logger.class) // 排除日志对象
                ;
    };

    /**
     * 测试方法兼容性
     *
     * @param originalClass 原定义类
     * @param testedClass   被测试类
     * @param methodFilter  方法过滤器
     */
    public static void testCompatibilityOnMethods(Class<?> originalClass, Class<?> testedClass, Predicate<Method> methodFilter) {
        ReflectionUtils.doWithMethods(originalClass, method -> {
            String methodName = method.getName();
            Class<?> returnType = method.getReturnType();
            Class<?>[] parameterTypes = method.getParameterTypes();
            Method targetMethod = ReflectionUtils.findMethod(testedClass, methodName, parameterTypes);
            assertNotNull(String.format("方法[名称：%s，参数：%s] 没有在目标类[%s]中定义！", methodName, Arrays.asList(parameterTypes), testedClass.getName()), targetMethod);
            assertTrue(String.format("原方法[名称：%s，参数：%s]的返回类型无法被目标方法[%s]兼容！", methodName, Arrays.asList(parameterTypes), method),
                    targetMethod.getReturnType().isAssignableFrom(returnType));
        }, method -> methodFilter == null ? true : methodFilter.test(method));
    }

    /**
     * 测试字段兼容性
     *
     * @param originalClass 原定义类
     * @param testedClass   被测试类
     */
    public static void testCompatibilityOnFields(Class<?> originalClass, Class<?> testedClass) {
        testCompatibilityOnFields(originalClass, testedClass, null);
    }

    /**
     * 测试字段兼容性
     *
     * @param originalClass 原定义类
     * @param testedClass   被测试类
     * @param fieldFilter   字段过滤器
     */
    public static void testCompatibilityOnFields(Class<?> originalClass, Class<?> testedClass, Predicate<Field> fieldFilter) {
        List<String> errorMessages = new LinkedList<>();
        ReflectionUtils.doWithFields(originalClass, field -> {
            String fieldName = field.getName();
            Class<?> fieldType = field.getType();
            Field targetField = ReflectionUtils.findField(testedClass, fieldName, fieldType);
            if (targetField == null) {
                errorMessages.add(String.format("字段[名称：%s，类型：%s] 没有在目标类[%s]中定义！",
                        fieldName, fieldType.getName(), testedClass.getName()));
                return;
            }


            if (Modifier.isStatic(field.getModifiers())) {
                Object fieldValue = field.get(null);
                Object targetValue = targetField.get(null);
                if (!Objects.equals(fieldValue, targetValue)) {
//                    errorMessages.add(String.format("原 Class[类型：%s]的静态字段[名称：%s，类型：%s]的内容：%s，不同于目标静态字段的内容：%s！",
//                            originalClass.getName(), fieldName, fieldType.getName(), fieldValue, targetValue));
                    errorMessages.add(originalClass.getName() + " , " + fieldName + " = " + fieldValue);
                }
            }

        }, field -> fieldFilter == null ? true : fieldFilter.test(field));

        errorMessages.forEach(System.err::println);

    }
}

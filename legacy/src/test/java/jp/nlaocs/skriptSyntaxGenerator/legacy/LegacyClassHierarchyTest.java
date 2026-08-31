package jp.nlaocs.skriptSyntaxGenerator.legacy;

import jp.nlaocs.skriptSyntaxGenerator.data.ClassMethodData;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyClassHierarchyTest {
    @Test
    void readsMethodsFromBytecodeWhenADeclaredTypeIsUnavailable() throws Exception {
        URL testClasses = BrokenMethodOwner.class.getProtectionDomain().getCodeSource().getLocation();
        try (URLClassLoader loader = new URLClassLoader(new URL[]{testClasses}, null) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (MissingDependency.class.getName().equals(name)) {
                    throw new ClassNotFoundException(name);
                }
                return super.loadClass(name, resolve);
            }
        }) {

            Class<?> brokenType = Class.forName(BrokenMethodOwner.class.getName(), false, loader);
            LegacyClassHierarchy hierarchy = new LegacyClassHierarchy(new LegacyAddonResolver(loader));
            hierarchy.add(brokenType);

            Map<String, Object> record = hierarchy.toData().stream()
                .filter(value -> value.get("name").equals(BrokenMethodOwner.class.getName()))
                .findFirst()
                .orElseThrow(AssertionError::new);
            @SuppressWarnings("unchecked")
            List<ClassMethodData> methods = (List<ClassMethodData>) record.get("methods");

            assertTrue(methods.stream().anyMatch(method ->
                method.getName().equals("missing") &&
                    method.getParameterTypes().equals(Collections.singletonList(MissingDependency.class.getName())) &&
                    method.getReturnType().equals(MissingDependency.class.getName())
            ));
            assertEquals(1, methods.stream().filter(method -> method.getName().equals("missing")).count());
        }
    }

    static final class MissingDependency {
    }

    static final class BrokenMethodOwner {
        public MissingDependency missing(MissingDependency value) {
            return value;
        }
    }
}

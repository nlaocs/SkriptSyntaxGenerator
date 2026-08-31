package jp.nlaocs.skriptSyntaxGenerator.legacy;

import jp.nlaocs.skriptSyntaxGenerator.data.ClassMethodData;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

final class LegacyClassFileMethodReader {
    private LegacyClassFileMethodReader() {
    }

    static List<ClassMethodData> read(Class<?> type) {
        String resourceName = type.getName().replace('.', '/') + ".class";
        InputStream input = classLoaderResource(type, resourceName);
        if (input == null) {
            throw new IllegalStateException("Cannot read class file of " + type.getName());
        }

        final Map<String, ClassMethodData> methods = new TreeMap<String, ClassMethodData>();
        try {
            try (InputStream stream = input) {
                new ClassReader(stream).accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override
                    public MethodVisitor visitMethod(
                        int access,
                        String name,
                        String descriptor,
                        String signature,
                        String[] exceptions
                    ) {
                        if ("<init>".equals(name) || "<clinit>".equals(name)) return null;

                        List<String> parameterTypes = new ArrayList<String>();
                        for (Type parameterType : Type.getArgumentTypes(descriptor)) {
                            parameterTypes.add(parameterType.getClassName());
                        }
                        ClassMethodData method = new ClassMethodData(
                            name,
                            parameterTypes,
                            Type.getReturnType(descriptor).getClassName(),
                            (access & Opcodes.ACC_STATIC) != 0
                        );
                        methods.put(method.signatureKey(), method);
                        return null;
                    }
                }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            }
        } catch (IOException | RuntimeException | LinkageError failure) {
            throw new IllegalStateException(
                "Cannot read declared methods from class file of " + type.getName(),
                failure
            );
        }
        return new ArrayList<ClassMethodData>(methods.values());
    }

    private static InputStream classLoaderResource(Class<?> type, String resourceName) {
        ClassLoader loader = type.getClassLoader();
        InputStream input = loader == null ? null : loader.getResourceAsStream(resourceName);
        if (input != null) return input;
        input = ClassLoader.getSystemResourceAsStream(resourceName);
        if (input != null) return input;
        return type.getResourceAsStream("/" + resourceName);
    }
}

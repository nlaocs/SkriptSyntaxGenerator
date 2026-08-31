package jp.nlaocs.skriptSyntaxGenerator.collector

import jp.nlaocs.skriptSyntaxGenerator.data.ClassMethodData
import net.bytebuddy.jar.asm.ClassReader
import net.bytebuddy.jar.asm.ClassVisitor
import net.bytebuddy.jar.asm.MethodVisitor
import net.bytebuddy.jar.asm.Opcodes
import net.bytebuddy.jar.asm.Type

internal object ClassFileMethodReader {
    fun read(type: Class<*>): List<ClassMethodData> {
        val resourceName = type.name.replace('.', '/') + ".class"
        val input = type.classLoader?.getResourceAsStream(resourceName)
            ?: ClassLoader.getSystemResourceAsStream(resourceName)
            ?: type.getResourceAsStream("/$resourceName")
            ?: throw IllegalStateException("Cannot read class file of ${type.name}")
        val methods = sortedMapOf<String, ClassMethodData>()
        input.use { stream ->
            ClassReader(stream).accept(
                object : ClassVisitor(Opcodes.ASM9) {
                    override fun visitMethod(
                        access: Int,
                        name: String,
                        descriptor: String,
                        signature: String?,
                        exceptions: Array<out String>?
                    ): MethodVisitor? {
                        if (name == "<init>" || name == "<clinit>") {
                            return null
                        }
                        val method = ClassMethodData(
                            name,
                            Type.getArgumentTypes(descriptor).map(Type::getClassName),
                            Type.getReturnType(descriptor).className,
                            access and Opcodes.ACC_STATIC != 0
                        )
                        methods[method.signatureKey()] = method
                        return null
                    }
                },
                ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES
            )
        }
        return methods.values.toList()
    }
}

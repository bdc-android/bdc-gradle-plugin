package com.billionsfinance.android.plugin.utils

import org.objectweb.asm.*

/**
 *
 */
public class ModifyClassUtil {

    public static byte[] modifyClasses(String className, byte[] srcByteCode) {
        byte[] classBytesCode = null;
        try {
            //Log.info("ModifyClassUtil className ${className}")
            classBytesCode = modifyClass(srcByteCode);
            return classBytesCode;
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (classBytesCode == null) {
            classBytesCode = srcByteCode;
        }
        return classBytesCode;
    }

    private
    static byte[] modifyClass(byte[] srcClass) throws IOException {
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        ClassVisitor classAdapter = new ClassVisitorAdapter(classWriter);
        ClassReader cr = new ClassReader(srcClass);
        cr.accept(classAdapter, 0);
        return classWriter.toByteArray();
    }

    private static boolean shouldModifyClass(String className){

    }
}
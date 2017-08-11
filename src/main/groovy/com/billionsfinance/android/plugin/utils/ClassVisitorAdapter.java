package com.billionsfinance.android.plugin.utils;

import org.objectweb.asm.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;

/**
 * Created by cbw on 2017/8/8.
 */

public class ClassVisitorAdapter extends ClassVisitor {

    private ClassVisitor cv;
    private String superName;
    private String className;
    private boolean noHook;
    private List<String> methodList = new ArrayList<>();
    private List<String> interfaceList = new ArrayList<>();

    public ClassVisitorAdapter(ClassVisitor cv) {
        super(Opcodes.ASM4, cv);
        this.cv = cv;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.superName = superName;
        this.className = name;
        if (name.startsWith("android") || name.startsWith("R")) {
            noHook = true;
        } else {
            //Log.info("[BehaviorAgent.info] [" + className  + "]");
        }

        if (interfaces != null && interfaces.length > 0) {
            Collections.addAll(interfaceList, interfaces);
            for (int i = 0; i < interfaceList.size(); i++) {
            }
        }
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitSource(String source, String debug) {
        super.visitSource(source, debug);
    }

    @Override
    public void visitOuterClass(String owner, String name, String desc) {
        super.visitOuterClass(owner, name, desc);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return super.visitAnnotation(desc, visible);
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
        return super.visitTypeAnnotation(typeRef, typePath, desc, visible);
    }

    @Override
    public void visitAttribute(Attribute attr) {
        super.visitAttribute(attr);
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        super.visitInnerClass(name, outerName, innerName, access);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        return super.visitField(access, name, desc, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        methodList.add(name);

        //fragment onResume onPause onHiddenChanged setUserVisibleHint
        if (!noHook && (superName != null) && (superName.equals("android/app/Fragment")||superName.equals("android/support/v4/app/Fragment"))) {
            if (name.equals("onResume")&&desc.equals("()V")) {
                Log.info("[BehaviorAgent.info] [" + className + "] hooked method onResume()V");
                MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
                mv.visitCode();
                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "com/billionsfinance/behaviorsdk/BehaviorAgent", "onFragmentResume", "(Ljava/lang/Object;)V", false);
                return mv;
            }
            if (name.equals("onPause")&&desc.equals("()V")) {
                Log.info("[BehaviorAgent.info] [" + className + "] hooked method onPause()V");
                MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
                mv.visitCode();
                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "com/billionsfinance/behaviorsdk/BehaviorAgent", "onFragmentPause", "(Ljava/lang/Object;)V", false);
                return mv;
            }
            if (name.equals("onHiddenChanged")&&desc.equals("(Z)V")) {
                Log.info("[BehaviorAgent.info] [" + className + "] hooked method onHiddenChanged(Z)V");
                MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
                mv.visitCode();
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ILOAD, 1);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "com/billionsfinance/behaviorsdk/BehaviorAgent", "onFragmentHiddenChanged", "(Ljava/lang/Object;Z)V", false);
                return mv;
            }
            if (name.equals("setUserVisibleHint")&&desc.equals("(Z)V")) {
                Log.info("[BehaviorAgent.info] [" + className + "] hooked method setUserVisibleHint(Z)V");
                MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
                mv.visitCode();
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ILOAD, 1);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "com/billionsfinance/behaviorsdk/BehaviorAgent", "setFragmentUserVisibleHint", "(Ljava/lang/Object;Z)V", false);
                return mv;
            }
        }

        if (!noHook && name.equals("onClick") && desc.equals("(Landroid/view/View;)V") && interfaceList.contains("android/view/View$OnClickListener")) {
            Log.info("[BehaviorAgent.info] [" + className + "] hooked method onClick(Landroid/view/View;)V");
            MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "com/billionsfinance/behaviorsdk/BehaviorAgent", "onViewClick", "(Ljava/lang/Object;)V", false);
            return mv;
        }

        if (!noHook && (superName != null) && (superName.equals("android/support/v4/app/Fragment"))) {

        }

        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    @Override
    public void visitEnd() {
        if (!noHook && (superName != null) && (superName.equals("android/app/Fragment"))) {
            if (!methodList.contains("onResume")) {
                Log.info("[BehaviorAgent.info] [" + className + "] hooked method onResume()V");
                MethodVisitor mv = cv.visitMethod(ACC_PROTECTED, "onResume", "()V", null, null);
                mv.visitCode();
                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(INVOKESPECIAL, "android/app/Fragment", "onResume", "()V", false);

                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "com/billionsfinance/behaviorsdk/BehaviorAgent", "onFragmentResume", "(Ljava/lang/Object;)V", false);
                mv.visitInsn(RETURN);
                mv.visitMaxs(2, 3);
                mv.visitEnd();
            }
            if (!methodList.contains("onPause")) {
                Log.info("[BehaviorAgent.info] [" + className + "] hooked method onPause()V");
                MethodVisitor mv = cv.visitMethod(ACC_PROTECTED, "onPause", "()V", null, null);
                mv.visitCode();
                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(INVOKESPECIAL, "android/app/Fragment", "onPause", "()V", false);

                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "com/billionsfinance/behaviorsdk/BehaviorAgent", "onFragmentPause", "(Ljava/lang/Object;)V", false);
                mv.visitInsn(RETURN);
                mv.visitMaxs(2, 3);
                mv.visitEnd();
            }

            if (!methodList.contains("onHiddenChanged")) {
                Log.info("[BehaviorAgent.info] [" + className + "] hooked method onHiddenChanged(Z)V");
                MethodVisitor mv = cv.visitMethod(ACC_PROTECTED, "onHiddenChanged", "(Z)V", null, null);
                mv.visitCode();
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ILOAD, 1);
                mv.visitMethodInsn(INVOKESPECIAL, "android/app/Fragment", "onHiddenChanged", "(Z)V", false);

                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ILOAD, 1);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "com/billionsfinance/behaviorsdk/BehaviorAgent", "onFragmentHiddenChanged", "(Ljava/lang/Object;Z)V", false);
                mv.visitInsn(RETURN);
                mv.visitMaxs(4, 5);
                mv.visitEnd();
            }

            if (!methodList.contains("setUserVisibleHint")) {
                Log.info("[BehaviorAgent.info] [" + className + "] hooked method setUserVisibleHint(Z)V");

                MethodVisitor mv = cv.visitMethod(ACC_PROTECTED, "setUserVisibleHint", "(Z)V", null, null);
                mv.visitCode();
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ILOAD, 1);
                mv.visitMethodInsn(INVOKESPECIAL, "android/app/Fragment", "setUserVisibleHint", "(Z)V", false);

                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ILOAD, 1);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "com/billionsfinance/behaviorsdk/BehaviorAgent", "setFragmentUserVisibleHint", "(Ljava/lang/Object;Z)V", false);
                mv.visitInsn(RETURN);
                mv.visitMaxs(4, 5);
                mv.visitEnd();
            }
        }

        if (!noHook && (superName != null) && (superName.equals("android/support/v4/app/Fragment"))) {
            if (!methodList.contains("onResume")) {
                Log.info("[BehaviorAgent.info] [" + className + "] hooked method onResume()V");
                MethodVisitor mv = cv.visitMethod(ACC_PROTECTED, "onResume", "()V", null, null);
                mv.visitCode();
                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(INVOKESPECIAL, "android/support/v4/app/Fragment", "onResume", "()V", false);

                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "com/billionsfinance/behaviorsdk/BehaviorAgent", "onFragmentResume", "(Ljava/lang/Object;)V", false);
                mv.visitInsn(RETURN);
                mv.visitMaxs(2, 3);
                mv.visitEnd();
            }
            if (!methodList.contains("onPause")) {
                Log.info("[BehaviorAgent.info] [" + className + "] hooked method onPause()V");
                MethodVisitor mv = cv.visitMethod(ACC_PROTECTED, "onPause", "()V", null, null);
                mv.visitCode();
                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(INVOKESPECIAL, "android/support/v4/app/Fragment", "onPause", "()V", false);

                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "com/billionsfinance/behaviorsdk/BehaviorAgent", "onFragmentPause", "(Ljava/lang/Object;)V", false);
                mv.visitInsn(RETURN);
                mv.visitMaxs(2, 3);
                mv.visitEnd();
            }

            if (!methodList.contains("onHiddenChanged")) {
                Log.info("[BehaviorAgent.info] [" + className + "] hooked method onHiddenChanged(Z)V");
                MethodVisitor mv = cv.visitMethod(ACC_PROTECTED, "onHiddenChanged", "(Z)V", null, null);
                mv.visitCode();
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ILOAD, 1);
                mv.visitMethodInsn(INVOKESPECIAL, "android/support/v4/app/Fragment", "onHiddenChanged", "(Z)V", false);

                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ILOAD, 1);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "com/billionsfinance/behaviorsdk/BehaviorAgent", "onFragmentHiddenChanged", "(Ljava/lang/Object;Z)V", false);
                mv.visitInsn(RETURN);
                mv.visitMaxs(4, 5);
                mv.visitEnd();
            }

            if (!methodList.contains("setUserVisibleHint")) {
                Log.info("[BehaviorAgent.info] [" + className + "] hooked method setUserVisibleHint(Z)V");

                MethodVisitor mv = cv.visitMethod(ACC_PROTECTED, "setUserVisibleHint", "(Z)V", null, null);
                mv.visitCode();
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ILOAD, 1);
                mv.visitMethodInsn(INVOKESPECIAL, "android/support/v4/app/Fragment", "setUserVisibleHint", "(Z)V", false);

                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ILOAD, 1);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "com/billionsfinance/behaviorsdk/BehaviorAgent", "setFragmentUserVisibleHint", "(Ljava/lang/Object;Z)V", false);
                mv.visitInsn(RETURN);
                mv.visitMaxs(4, 5);
                mv.visitEnd();
            }
        }

//        if (superName.equals("android/support/v7/app/AppCompatActivity")) {
//            if (!methodList.contains("onPause")){
//                MethodVisitor mv = cv.visitMethod(ACC_PROTECTED, "onPause", "()V", null, null);
//                mv.visitCode();
//                mv.visitVarInsn(ALOAD, 0);
//                mv.visitMethodInsn(INVOKESPECIAL, "android/support/v7/app/AppCompatActivity", "onPause", "()V");
//                mv.visitInsn(RETURN);
//                mv.visitMaxs(1, 1);
//                mv.visitEnd();
//            }
//            if (!methodList.contains("onDestory")){
//                MethodVisitor mv = cv.visitMethod(ACC_PROTECTED, "onDestory", "()V", null, null);
//                mv.visitCode();
//                mv.visitVarInsn(ALOAD, 0);
//                mv.visitMethodInsn(INVOKESPECIAL, "android/support/v7/app/AppCompatActivity", "onDestory", "()V");
//
//                mv.visitLdcInsn("onDestory");
//                mv.visitLdcInsn("onDestory");
//                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "bruce/com/testhibeaver/MainActivity", "hookXM", "(Ljava/lang/Object;Ljava/lang/Object;)V");
//                mv.visitInsn(RETURN);
//                mv.visitMaxs(3, 4);
//                mv.visitEnd();
//            }
//
//        }


        super.visitEnd();
    }
}

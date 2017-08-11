package com.billionsfinance.android.plugin

import com.android.annotations.NonNull
import com.android.annotations.Nullable
import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.billionsfinance.android.plugin.utils.*
import groovy.io.FileType
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils

/**
 *
 */
public class InjectTransform extends Transform {

    @Override
    String getName() {
        return "BehaviorAgent"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<QualifiedContent.Scope> getScopes() {
        if (DataHelper.ext.projectType == DataHelper.TYPE_APP) {
            return TransformManager.SCOPE_FULL_PROJECT
        } else if (DataHelper.ext.projectType == DataHelper.TYPE_LIB) {
            return TransformManager.SCOPE_FULL_LIBRARY
        }
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    public void transform(
            @NonNull Context context,
            @NonNull Collection<TransformInput> inputs,
            @NonNull Collection<TransformInput> referencedInputs,
            @Nullable TransformOutputProvider outputProvider,
            boolean isIncremental) throws IOException, TransformException, InterruptedException {

        Log.info "[BehaviorAgent.info] BehaviorAgent transform start"

        /**
         * 获取所有依赖的classPaths
         */
        boolean sdkOnPath = false
        def classPaths = []
        String buildTypes
        String productFlavors
        inputs.each { TransformInput input ->
            input.directoryInputs.each { DirectoryInput directoryInput ->
                classPaths.add(directoryInput.file.absolutePath)
                buildTypes = directoryInput.file.name
                productFlavors = directoryInput.file.parentFile.name
                //Log.info("[BehaviorAgent.info] 项目包含的class文件夹：${directoryInput.file.absolutePath}");
            }
            input.jarInputs.each { JarInput jarInput ->
                classPaths.add(jarInput.file.absolutePath)
                //Log.info("[BehaviorAgent.info] 项目包含的jar包：${jarInput.file.absolutePath}");
                if (jarInput.file.absolutePath.contains("behaviorsdk")){
                    sdkOnPath = true;
                }
            }
        }
        def paths = [Util.getExtension().bootClasspath.get(0).absolutePath/*, injectClassPath*/]
        paths.addAll(classPaths)

        if (!sdkOnPath){
            Log.info("[BehaviorAgent.info] behaviorsdk未接入")
            return
        }
        /**遍历输入文件*/
        inputs.each { TransformInput input ->
            /**
             * 遍历jar
             * JarInput和DirectoryInput两个接口都继承自QualifiedContent这个接口
             * 他们的scope属性（枚举，类型为QualifiedContent.Scope）表明这个Input所属的类型可见源码注释
             * @see QualifiedContent.Scope
             */
            input.jarInputs.each { JarInput jarInput ->
                String destName = jarInput.file.name;
                /** 重名名输出文件,因为可能同名,会覆盖*/
                def hexName = DigestUtils.md5Hex(jarInput.file.absolutePath).substring(0, 8);
                if (destName.endsWith(".jar")) {
                    destName = destName.substring(0, destName.length() - 4);
                }
                /** 获得输出文件*/
                File dest = outputProvider.getContentLocation(destName + "_" + hexName, jarInput.contentTypes, jarInput.scopes, Format.JAR);
                def modifiedJar = null;
                modifiedJar = modifyJarFile(jarInput.file, context.getTemporaryDir());
                if (modifiedJar == null) {
                    modifiedJar = jarInput.file;
                } else {
                    saveModifiedJarForCheck(modifiedJar);
                }
                FileUtils.copyFile(modifiedJar, dest);
            }
            /**
             * 遍历目录
             */
            input.directoryInputs.each { DirectoryInput directoryInput ->
                File dest = outputProvider.getContentLocation(directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY);
                File dir = directoryInput.file
                if (dir) {
                    HashMap<String, File> modifyMap = new HashMap<>();
                    dir.traverse(type: FileType.FILES, nameFilter: ~/.*\.class/) {
                        File classFile ->
                            File modified = modifyClassFile(dir, classFile, context.getTemporaryDir());
                            if (modified != null) {
                                //key为相对路径
                                modifyMap.put(classFile.absolutePath.replace(dir.absolutePath, ""), modified);
                            }
                    }
                    Log.info("[BehaviorAgent.info] BehaviorAgent transform end")
                    FileUtils.copyDirectory(directoryInput.file, dest);
                    modifyMap.entrySet().each {
                        Map.Entry<String, File> en ->
                            File target = new File(dest.absolutePath + en.getKey());
                            //Log.info(target.getAbsolutePath());
                            if (target.exists()) {
                                target.delete();
                            }
                            FileUtils.copyFile(en.getValue(), target);
                            saveModifiedJarForCheck(en.getValue());
                            en.getValue().delete();
                    }
                }
            }
        }
    }

    private static void saveModifiedJarForCheck(File optJar) {
        File dir = DataHelper.ext.behaviorDir;
        File checkJarFile = new File(dir, optJar.getName());
        if (checkJarFile.exists()) {
            checkJarFile.delete();
        }
        FileUtils.copyFile(optJar, checkJarFile);
    }

    /**
     * 植入代码
     * @param buildDir 是项目的build class目录,就是我们需要注入的class所在地
     * @param lib 这个是hackdex的目录,就是AntilazyLoad类的class文件所在地
     */
    public static File modifyJarFile(File jarFile, File tempDir) {
        if (jarFile) {
            return ModifyFiles.modifyJar(jarFile, tempDir, true)
        }
        return null;
    }

    public static File modifyClassFile(File dir, File classFile, File tempDir) {
        File modified;
        try {
            String className = Util.path2Classname(classFile.absolutePath.replace(dir.absolutePath + File.separator, ""));
            byte[] sourceClassBytes = IOUtils.toByteArray(new FileInputStream(classFile));
            byte[] modifiedClassBytes = ModifyClassUtil.modifyClasses(className, sourceClassBytes);
            if (modifiedClassBytes) {
                modified = new File(tempDir, className.replace('.', '') + '.class')
                if (modified.exists()) {
                    modified.delete();
                }
                modified.createNewFile()
                new FileOutputStream(modified).write(modifiedClassBytes)
            }
        } catch (Exception e) {
        }
        return modified;

    }

    private static void writeStreamWithBuffer(InputStream inputStream, OutputStream out) {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }
}

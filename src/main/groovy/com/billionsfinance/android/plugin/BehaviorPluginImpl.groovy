package com.billionsfinance.android.plugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.billionsfinance.android.plugin.utils.Const
import com.billionsfinance.android.plugin.utils.DataHelper
import com.billionsfinance.android.plugin.utils.Log
import com.billionsfinance.android.plugin.utils.Util
import org.gradle.api.Plugin
import org.gradle.api.Project

class BehaviorPluginImpl implements Plugin<Project> {
    @Override
    void apply(Project project) {
        //println "apply bdc-gradle-plugin"
        Util.setProject(project)
        try {
            if(Class.forName("com.android.build.gradle.BaseExtension")){
                BaseExtension android = project.extensions.getByType(BaseExtension)
                if (android instanceof LibraryExtension) {
                    DataHelper.ext.projectType = DataHelper.TYPE_LIB;
                } else if (android instanceof AppExtension) {
                    DataHelper.ext.projectType = DataHelper.TYPE_APP;
                } else {
                    DataHelper.ext.projectType = -1
                }
                if (DataHelper.ext.projectType != -1) {
                    registerTransform(android)
                }
            }
        } catch (Exception e) {
            DataHelper.ext.projectType = -1
        }

        initDir(project);

        project.afterEvaluate {
            Log.setQuiet(Const.keepQuiet);
            Log.setShowHelp(Const.showHelp);
            Log.logHelp();
            if (Const.watchTimeConsume) {
                Log.info "watchTimeConsume enabled"
                project.gradle.addListener(new TimeListener())
            } else {
                Log.info "watchTimeConsume disabled"
            }
        }
    }

    def static registerTransform(BaseExtension android) {
        InjectTransform transform = new InjectTransform()
        android.registerTransform(transform)
    }

    static void initDir(Project project) {
        if (!project.buildDir.exists()) {
            project.buildDir.mkdirs()
        }
        File behaviorDir = new File(project.buildDir, "behaviorDir")
        if (!behaviorDir.exists()) {
            behaviorDir.mkdir()
        }
        File tempDir = new File(behaviorDir, "temp")
        if (!tempDir.exists()) {
            tempDir.mkdir()
        }
        DataHelper.ext.behaviorDir = behaviorDir
        DataHelper.ext.behaviorTempDir = tempDir
    }
}

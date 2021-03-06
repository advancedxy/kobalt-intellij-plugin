package com.beust.kobalt.intellij.frameworkSupport

import com.intellij.framework.FrameworkTypeEx
import com.intellij.icons.AllIcons
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModifiableModelsProvider
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.util.io.FileUtilRt
import java.io.File

/**
 * @author Dmitry Zhuravlev
 *         Date:  11.05.2016
 */
class KobaltJavaFrameworkSupportProvider : KobaltFrameworkSupportProvider() {
    companion object {
        @JvmField val ID = "java"
    }

    override fun getFrameworkType() = object : FrameworkTypeEx(ID) {
        override fun getIcon() = AllIcons.Nodes.Module

        override fun getPresentableName() = "Java"

        override fun createProvider() = this@KobaltJavaFrameworkSupportProvider

    }


    override fun addSupport(module: Module, rootModel: ModifiableRootModel, modifiableModelsProvider: ModifiableModelsProvider, buildScriptBuilder: KobaltBuildScriptBuilder) {
        with(buildScriptBuilder) {
            val kobaltModuleRootDir = File(contentRootDir, projectId.artifactId)
            val kobaltModuleSourceDir = File(kobaltModuleRootDir, "src/main/java")
            val kobaltModuleTestDir = File(kobaltModuleRootDir, "src/test/java")
            FileUtilRt.createDirectory(kobaltModuleSourceDir)
            FileUtilRt.createDirectory(kobaltModuleTestDir)
            addSourceDirectory("""path("src/main/java")""")
            addSourceTestDirectory("""path("src/test/java")""")
            addDependencyTest("""compile("org.testng:testng:6.9.9")""")
        }
    }
}
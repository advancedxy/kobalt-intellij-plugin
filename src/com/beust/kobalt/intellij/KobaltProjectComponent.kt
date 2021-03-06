package com.beust.kobalt.intellij;

import com.beust.kobalt.intellij.server.ServerUtil
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project

class KobaltProjectComponent(val project: Project) : ProjectComponent {
    companion object {
        val LOG = Logger.getInstance(KobaltProjectComponent::class.java)

        val KOBALT_JAR = "kobalt.jar"
        val BUILD_MODULE_NAME = "Build.kt"
        val BUILD_IML_NAME = BUILD_MODULE_NAME + ".iml"
    }

    override fun projectOpened() {
    }

    override fun getComponentName() = "kobalt.ProjectComponent"

    override fun initComponent() {}
    override fun disposeComponent() {}
    override fun projectClosed() {
        ServerUtil.stopServer()
    }

}

package com.beust.kobalt.intellij.manager

import com.beust.kobalt.intellij.Constants
import com.beust.kobalt.intellij.KFiles
import com.beust.kobalt.intellij.KobaltApplicationComponent
import com.beust.kobalt.intellij.resolver.KobaltProjectResolver
import com.beust.kobalt.intellij.settings.*
import com.beust.kobalt.intellij.task.KobaltTaskManager
import com.google.gson.JsonParser
import com.intellij.execution.configurations.SimpleJavaParameters
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.ExternalSystemAutoImportAware
import com.intellij.openapi.externalSystem.ExternalSystemConfigurableAware
import com.intellij.openapi.externalSystem.ExternalSystemManager
import com.intellij.openapi.externalSystem.service.ui.DefaultExternalSystemUiAware
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.Function
import com.intellij.util.PathUtil
import com.intellij.util.containers.ContainerUtilRt
import com.intellij.util.net.HttpConfigurable
import icons.KobaltIcons
import okhttp3.OkHttpClient
import okio.Sink
import org.apache.http.auth.Credentials
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URL

/**
 * @author Dmitry Zhuravlev
 *         Date:  26.04.2016
 */
class KobaltManager : DefaultExternalSystemUiAware(), ExternalSystemConfigurableAware, ExternalSystemAutoImportAware,
        StartupActivity,
        ExternalSystemManager<KobaltProjectSettings, KobaltSettingsListener, KobaltSettings, KobaltLocalSettings,
                KobaltExecutionSettings> {

    companion object {
        internal var LOG = Logger.getInstance("#" + KobaltManager::class.java.name)
    }

    override fun getConfigurable(project: Project) = KobaltConfigurable(project);

    override fun getExternalProjectConfigDescriptor(): FileChooserDescriptor? = null //TODO

    override fun getProjectRepresentationName(targetProjectPath: String, rootProjectPath: String?) =
            ExternalSystemApiUtil.getProjectRepresentationName(targetProjectPath, rootProjectPath);

    override fun getAffectedExternalProjectPath(changedFileOrDirPath: String, project: Project): String? = null //TODO

    override fun runActivity(project: Project) {
        val connection = project.messageBus.connect(project)
        connection.subscribe(KobaltSettings.getInstance(project).changesTopic,
                object : KobaltSettingsListenerAdapter() {
            override fun onKobaltHomeChange(oldPath: String?, newPath: String?, linkedProjectPath: String) {
                ensureProjectsRefresh()
            }

            private fun ensureProjectsRefresh() {
                ExternalSystemUtil.refreshProjects(project, Constants.KOBALT_SYSTEM_ID, true)
            }
        })
        //TODO
    }

    override fun enhanceLocalProcessing(urls: MutableList<URL>) {
    }

    override fun enhanceRemoteProcessing(parameters: SimpleJavaParameters) {
        // Add Kotlin runtime. This is a workaround because at the moment RemoteExternalSystemCommunicationManager
        // has classpath without Kotlin and cannot call ProjectResolver
        val additionalClasspath = mutableListOf<String>()
        ContainerUtilRt.addIfNotNull(additionalClasspath, PathUtil.getJarPathForClass(Unit::class.java))
        ContainerUtilRt.addIfNotNull(additionalClasspath, PathUtil.getJarPathForClass(JsonParser::class.java))
        ContainerUtilRt.addIfNotNull(additionalClasspath, PathUtil.getJarPathForClass(Retrofit::class.java))
        ContainerUtilRt.addIfNotNull(additionalClasspath, PathUtil.getJarPathForClass(OkHttpClient::class.java))
        ContainerUtilRt.addIfNotNull(additionalClasspath, PathUtil.getJarPathForClass(Sink::class.java))
        ContainerUtilRt.addIfNotNull(additionalClasspath, PathUtil.getJarPathForClass(GsonConverterFactory::class.java))
        ContainerUtilRt.addIfNotNull(additionalClasspath, PathUtil.getJarPathForClass(Credentials::class.java))
        parameters.classPath.addAll(additionalClasspath)
        parameters.vmParametersList.addProperty(
                ExternalSystemConstants.EXTERNAL_SYSTEM_ID_KEY, Constants.KOBALT_SYSTEM_ID.id)
        with(HttpConfigurable.getInstance()){
            with(parameters.vmParametersList) {
                if (!StringUtil.isEmpty(PROXY_EXCEPTIONS)) {
                    val nonProxyHosts = PROXY_EXCEPTIONS.split(",").joinToString(separator = "|", transform = { StringUtil.trim(it) })
                    add("http.nonProxyHosts",nonProxyHosts)
                    add("https.nonProxyHosts",nonProxyHosts)
                }
                if (USE_HTTP_PROXY && StringUtil.isNotEmpty(PROXY_LOGIN)) {
                    add("http.proxyUser", PROXY_LOGIN)
                    add("https.proxyUser", PROXY_LOGIN)
                    add("http.proxyPassword", plainProxyPassword)
                    add("https.proxyPassword", plainProxyPassword)
                }
            }

        }
    }

    override fun getExecutionSettingsProvider(): Function<Pair<Project, String>, KobaltExecutionSettings> =
            Function { pair ->
                KobaltExecutionSettings(KFiles.distributionsDir, KobaltApplicationComponent.kobaltJar.get().toFile().absolutePath)
            }

    override fun getTaskManagerClass() = KobaltTaskManager::class.java

    override fun getProjectResolverClass() = KobaltProjectResolver::class.java

    override fun getLocalSettingsProvider(): Function<Project, KobaltLocalSettings> = Function { project ->
        KobaltLocalSettings.getInstance(project)
    }

    override fun getSettingsProvider(): Function<Project, KobaltSettings> = Function { project ->
        KobaltSettings.getInstance(project)
    }

    override fun getSystemId() = Constants.KOBALT_SYSTEM_ID

    override fun getExternalProjectDescriptor() =
            FileChooserDescriptorFactory.createSingleFileDescriptor(Constants.BUILD_FILE_EXTENSIONS)

    override fun getProjectIcon() = KobaltIcons.Kobalt
}


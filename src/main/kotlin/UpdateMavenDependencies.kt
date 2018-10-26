package name.tachenov.intellij.plugins.updateMavenDependencies

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import org.jetbrains.idea.maven.dom.MavenDomUtil
import org.jetbrains.idea.maven.model.MavenId
import org.jetbrains.idea.maven.project.MavenProjectsManager

class UpdateMavenDependencies : AnAction() {
    override fun actionPerformed(e: AnActionEvent?) {
        val project = (e ?: return).project ?: return
        val mavenManager = MavenProjectsManager.getInstance(project)
        val projects = mavenManager.projects
        val artifactVersions = projects.asSequence()
                .filter { it.mavenId.uniqueArtifactId != null && it.mavenId.version != null}
                .associate { it.mavenId.uniqueArtifactId!! to it.mavenId.version!! }
        for (mavenProject in projects) {
            val domModel = MavenDomUtil.getMavenDomProjectModel(project, mavenProject.file) ?: continue
            for (dependency in domModel.dependencies.dependencies) {
                val dependencyArtifact = UniqueArtifactId(dependency.groupId.value!!, dependency.artifactId.value!!)
                val currentVersion = artifactVersions[dependencyArtifact]
                if (currentVersion != null && dependency.version.value != currentVersion) {
                    WriteCommandAction.runWriteCommandAction(project) {
                        dependency.version.value = currentVersion
                    }
                }
            }
        }
    }
}

data class UniqueArtifactId(val groupId: String, val artifactId: String)

val MavenId.uniqueArtifactId: UniqueArtifactId?
    get() {
        val gid = groupId
        val aid = artifactId
        if (gid == null || aid == null)
            return null
        return UniqueArtifactId(gid, aid)
    }

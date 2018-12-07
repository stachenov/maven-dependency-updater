package name.tachenov.intellij.plugins.mavenDependencyUpdater

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.command.WriteCommandAction
import org.jetbrains.idea.maven.dom.MavenDomUtil
import org.jetbrains.idea.maven.model.MavenId
import org.jetbrains.idea.maven.project.MavenProjectsManager

class UpdateMavenDependencies : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val projects = MavenProjectsManager.getInstance(project).projects
        val currentVersions = projects.asSequence()
                .filter { it.mavenId.uniqueArtifactId != null && it.mavenId.version != null}
                .associate { it.mavenId.uniqueArtifactId!! to it.mavenId.version!! }
        val updates = ArrayList<() -> Unit>()
        for (mavenProject in projects) {
            val domModel = MavenDomUtil.getMavenDomProjectModel(project, mavenProject.file) ?: continue
            for (dependency in domModel.dependencies.dependencies) {
                val dependencyArtifact = nullableUniqueArtifactId(dependency.groupId.value, dependency.artifactId.value)
                val currentVersion = currentVersions[dependencyArtifact]
                if (currentVersion != null && dependency.version.value != currentVersion) {
                    updates.add { dependency.version.value = currentVersion }
                }
            }
        }
        WriteCommandAction.writeCommandAction(project)
                .withName("Update Maven Dependencies")
                .withGlobalUndo()
                .withUndoConfirmationPolicy(UndoConfirmationPolicy.REQUEST_CONFIRMATION)
                .run<Nothing> {
            updates.forEach { it() }
        }
    }
}

private data class UniqueArtifactId(val groupId: String, val artifactId: String)

private val MavenId.uniqueArtifactId: UniqueArtifactId?
    get() {
        return nullableUniqueArtifactId(groupId, artifactId)
    }

private fun nullableUniqueArtifactId(groupId: String?, artifactId: String?): UniqueArtifactId? {
    return UniqueArtifactId((groupId ?: return null), (artifactId ?: return null))
}

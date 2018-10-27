package name.tachenov.intellij.plugins.mavenDependencyUpdater

import com.intellij.openapi.command.CommandEvent
import com.intellij.openapi.command.CommandListener
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.projectRoots.impl.JavaAwareProjectJdkTableImpl
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture
import com.intellij.testFramework.fixtures.JavaTestFixtureFactory
import org.jetbrains.idea.maven.indices.MavenIndicesManager
import org.jetbrains.idea.maven.model.MavenExplicitProfiles
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.idea.maven.server.MavenServerManager
import org.junit.After
import org.junit.Before
import org.junit.Test

class UpdateMavenDependenciesTest : UsefulTestCase() {

    private lateinit var fixture: JavaCodeInsightTestFixture

    private lateinit var dependentFile: VirtualFile

    private lateinit var executedCommands: MutableList<CommandProperties>

    @Before
    override fun setUp() {
        super.setUp()
        val builder = IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder(getTestName(true))
        fixture = JavaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(builder.fixture)
        fixture.testDataPath = "src/test/data/simpleProject"
        val dependentModule = builder.addModule(JavaModuleFixtureBuilder::class.java)
        val dependencyModule = builder.addModule(JavaModuleFixtureBuilder::class.java)
        fixture.setUp()
        val dependentRoot = fixture.tempDirFixture.findOrCreateDir("dependent")
        val dependencyRoot = fixture.tempDirFixture.findOrCreateDir("dependency")
        ModuleRootModificationUtil.updateModel(dependentModule.fixture.module) { model -> model.addContentEntry(dependentRoot)}
        ModuleRootModificationUtil.updateModel(dependencyModule.fixture.module) {model -> model.addContentEntry(dependencyRoot)}
        dependentFile = fixture.copyFileToProject("dependent/pom.xml")
        val dependencyFile = fixture.copyFileToProject("dependency/pom.xml")
        val mavenManager = MavenProjectsManager.getInstance(fixture.project)
        mavenManager.initForTests()
        val pomFiles = listOf(dependentFile, dependencyFile)
        mavenManager.resetManagedFilesAndProfilesInTests(pomFiles, MavenExplicitProfiles.NONE)
        mavenManager.waitForReadingCompletion()
        mavenManager.waitForResolvingCompletion()
        mavenManager.scheduleImportInTests(pomFiles)
        mavenManager.importProjects()
        executedCommands = ArrayList()
        CommandProcessor.getInstance().addCommandListener(object: CommandListener {
            override fun commandFinished(event: CommandEvent?) {
                executedCommands.add(CommandProperties(event?.commandName ?: return,
                        event.undoConfirmationPolicy))
            }
        })
    }

    @After
    override fun tearDown() {
        // I have no idea how this works. This magic is assembled from
        // various pieces found mostly in the Maven Integration plugin tests.
        JavaAwareProjectJdkTableImpl.removeInternalJdkInTests()
        MavenServerManager.getInstance().shutdown(true)
        fixture.tearDown()
        MavenIndicesManager.getInstance().clear()
        super.tearDown()
    }

    @Test
    fun testFileContents() {
        fixture.openFileInEditor(dependentFile)
        fixture.testAction(UpdateMavenDependencies())
        fixture.checkResultByFile("dependent/pom_after.xml")
    }

    @Test
    fun testCommandProperties() {
        executedCommands.clear()
        fixture.testAction(UpdateMavenDependencies())
        val capturedCommands = ArrayList(executedCommands)
        assertOrderedEquals(capturedCommands, listOf(CommandProperties("Update Maven Dependencies",
                UndoConfirmationPolicy.REQUEST_CONFIRMATION)))
        assertTrue(UndoManager.getInstance(fixture.project).isUndoAvailable(null))
    }

    private data class CommandProperties(val name: String,
                                         val undoConfirmationPolicy: UndoConfirmationPolicy)
}
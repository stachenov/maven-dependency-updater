package name.tachenov.intellij.plugins.updateMavenDependencies

import com.intellij.openapi.command.CommandEvent
import com.intellij.openapi.command.CommandListener
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture
import com.intellij.testFramework.fixtures.JavaTestFixtureFactory
import org.jetbrains.idea.maven.model.MavenExplicitProfiles
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.junit.Before
import org.junit.Test

class UpdateMavenDependenciesTest : UsefulTestCase() {

    private lateinit var fixture: JavaCodeInsightTestFixture

    private lateinit var dependentFile: VirtualFile

    private lateinit var executedCommands: MutableList<String>

    @Before
    override fun setUp() {
        super.setUp()
        val builder = IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder(getTestName(true))
        fixture = JavaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(builder.fixture)
        fixture.testDataPath = "src/test/data/${getTestName(true)}"
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
                executedCommands.add(event?.commandName ?: return)
            }
        })
    }

    @Test
    fun testSimpleProject() {
        fixture.openFileInEditor(dependentFile)
        executedCommands.clear()
        fixture.testAction(UpdateMavenDependencies())
        val capturedCommands = ArrayList(executedCommands)
        fixture.checkResultByFile("dependent/pom_after.xml")
        assertOrderedEquals(capturedCommands, listOf("Update Maven dependencies"))
    }
}
Maven Dependency Updater plugin for IntelliJ platform

Updates dependency versions in pom.xml files to match currently open projects (modules).

Sometimes, when you have a few modules represented by Maven projects, managing dependencies seems like a lot of more work than it should be. You update one project version, then other projects suddenly depend on external artifacts in the local Maven repo instead of the module currently open in the IDE. As you update dependencies and change versions to reflect that, it leads to even more broken dependencies. If you have about 5 modules and release often, it can be a lot of pain to do it over and over again.

This plugin adds a single button in the Maven Projects tool window. When you click it, all dependency versions are automatically updated to the currently open versions. External dependencies are not affected in any way.

For better experience, enable Maven auto-update in IDEA. This is done differently in different versions, but essentialy you want IDEA to pick up any changes to POM files without having to click any annoying buttons every time. 

**Example workflow**

If you have a dependency graph like this:

A:1.0->B:1.0->C:1.0

Change the version of C to 1.1 and click the magic button. You'll have this:

A:1.0->B:1.0->C:1.1

Now you realize that since B has changed, it's no longer 1.0 either. So you change its version to 1.0.1 and click the magic button again. Now you have:

A:1.0->B:1.0.1->C:1.1

Repeat again for A and you'll have:

A:1.0.1->B:1.0.1->C:1.1

**Example with automatic version bumping**

If you bump the versions right after every release, you don't even need to bother with these steps. Suppose you start with

A:1.0->B:1.0->C:1.0

You make a release, which automatically (with help of some Maven plugins, for example) bumps every version to 1.0.1. Now you have a rather broken dependency graph:

A:1.0.1->B:1.0

B:1.0.1->C:1.0

Click the button, and it is fixed:

A:1.0.1->B:1.0.1->C:1.0.1

If you decide then update C to 1.1, just do so and click the button again.

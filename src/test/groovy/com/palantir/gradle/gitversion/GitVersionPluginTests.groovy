/*
 * Copyright 2015 Palantir Technologies
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * <http://www.apache.org/licenses/LICENSE-2.0>
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.palantir.gradle.gitversion

import org.eclipse.jgit.api.Git
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder

import spock.lang.Specification

class GitVersionPluginTests extends Specification {

    @Rule
    TemporaryFolder temporaryFolder = new TemporaryFolder()

    File projectDir
    File buildFile
    File gitIgnoreFile
    File dirtyContentFile
    List<File> pluginClasspath

    def 'exception when project root does not have a git repo' () {
        given:
        buildFile << '''
            plugins {
                id 'com.palantir.git-version'
            }
            version gitVersion()
        '''.stripIndent()

        when:
        BuildResult buildResult = with('printVersion').buildAndFail()

        then:
        buildResult.standardError.contains('> Cannot find \'.git\' directory')
    }

    def 'unspecified when no tags are present' () {
        given:
        buildFile << '''
            plugins {
                id 'com.palantir.git-version'
            }
            version gitVersion()
        '''.stripIndent()
        git_init()

        when:
        BuildResult buildResult = with('printVersion').build()

        then:
        buildResult.standardOutput.contains(':printVersion\nunspecified\n')
    }

    def 'unspecified when no annotated tags are present' () {
        given:
        buildFile << '''
            plugins {
                id 'com.palantir.git-version'
            }
            version gitVersion()
        '''.stripIndent()
        gitIgnoreFile << 'build'
        git_init_commit()

        when:
        BuildResult buildResult = with('printVersion').build()

        then:
        buildResult.standardOutput.contains(':printVersion\nunspecified\n')
    }

    def 'unspecified and dirty when no annotated tags are present and dirty content' () {
        given:
        buildFile << '''
            plugins {
                id 'com.palantir.git-version'
            }
            version gitVersion()
        '''.stripIndent()
        gitIgnoreFile << 'build'
        git_init_commit()
        dirtyContentFile << 'dirty-content'

        when:
        BuildResult buildResult = with('printVersion').build()

        then:
        buildResult.standardOutput.contains(':printVersion\nunspecified.dirty\n')
    }

    def 'git describe when annotated tag is present' () {
        given:
        buildFile << '''
            plugins {
                id 'com.palantir.git-version'
            }
            version gitVersion()
        '''.stripIndent()
        gitIgnoreFile << 'build'
        git_init_commit_tag()

        when:
        BuildResult buildResult = with('printVersion').build()

        then:
        buildResult.standardOutput.contains(":printVersion\n1.0.0\n")
    }

    def 'git describe and dirty when annotated tag is present and dirty content' () {
        given:
        buildFile << '''
            plugins {
                id 'com.palantir.git-version'
            }
            version gitVersion()
        '''.stripIndent()
        gitIgnoreFile << 'build'
        git_init_commit_tag()
        dirtyContentFile << 'dirty-content'

        when:
        BuildResult buildResult = with('printVersion').build()

        then:
        buildResult.standardOutput.contains(':printVersion\n1.0.0.dirty\n')
    }

    def 'git describe long when annotated tag is present and long is true' () {
        given:
        buildFile << '''
            plugins {
                id 'com.palantir.git-version'
            }
            gitVersionDescriber().setLong(true)
            version gitVersion()
        '''.stripIndent()
        gitIgnoreFile << 'build'
        git_init_commit_tag()

        when:
        BuildResult buildResult = with('printVersion').build()

        then:
        buildResult.standardOutput.find(':printVersion\n1.0.0-0-g[a-f0-9]{7}\n')
    }

    def 'git describe long and dirty when annotated tag is present, long is true and repo is dirty' () {
        given:
        buildFile << '''
            plugins {
                id 'com.palantir.git-version'
            }
            gitVersionDescriber().setLong(true)
            version gitVersion()
        '''.stripIndent()
        gitIgnoreFile << 'build'
        git_init_commit_tag();
        dirtyContentFile << 'Dirty content'

        when:
        BuildResult buildResult = with('printVersion').build()

        then:
        buildResult.standardOutput.find(':printVersion\n1.0.0-0-g[a-f0-9]{7}\\.dirty\n')
    }

    private GradleRunner with(String... tasks) {
        GradleRunner.create()
            .withPluginClasspath(pluginClasspath)
            .withProjectDir(projectDir)
            .withArguments(tasks)
    }

    def setup() {
        projectDir = temporaryFolder.root
        buildFile = temporaryFolder.newFile('build.gradle')
        gitIgnoreFile = temporaryFolder.newFile('.gitignore')
        dirtyContentFile = temporaryFolder.newFile('dirty')

        def pluginClasspathResource = getClass().classLoader.findResource("plugin-classpath.txt")
        if (pluginClasspathResource == null) {
            throw new IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")
        }

        pluginClasspath = pluginClasspathResource.readLines()
            .collect { it.replace('\\', '\\\\') } // escape backslashes in Windows paths
            .collect { new File(it) }
    }

    private Git git;

    private git_init(){
        git = Git.init().setDirectory(projectDir).call();
    }

    private git_init_commit(){
        git_init()
        git.add().addFilepattern('.').call()
        git.commit().setMessage('initial commit').call()
    }

    private git_init_commit_tag(){
        git_init_commit();
        git.tag().setAnnotated(true).setMessage('1.0.0').setName('1.0.0').call()
    }
}

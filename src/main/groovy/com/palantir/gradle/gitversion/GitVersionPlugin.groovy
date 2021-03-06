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

import org.eclipse.jgit.api.DescribeCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.gradle.api.Plugin
import org.gradle.api.Project

class GitVersionPlugin implements Plugin<Project> {

    // Gradle returns 'unspecified' when no version is set
    private static final String UNSPECIFIED_VERSION = 'unspecified'
    private DescribeCommand describeCommand = null
    private Git git;

    void apply(Project project) {
        project.ext.gitVersion = {
            doDescribe(project)
        }

        project.ext.gitVersionDescriber = {
            return ensureDescribeCommand(project)
        }

        project.tasks.create('printVersion') << {
            println project.version
        }
    }

    private String doDescribe(Project project) {

        Git git = ensureGit(project);
        try {
            String version = ensureDescribeCommand(project).call() ?: UNSPECIFIED_VERSION
            boolean isClean = git.status().call().isClean()
            return version + (isClean ? '' : '.dirty')

        } catch (Throwable t) {
            return UNSPECIFIED_VERSION
        }
    }

    private DescribeCommand ensureDescribeCommand(Project project) {
        if (describeCommand == null) {
            Git git = ensureGit(project)
            describeCommand = git.describe()
        }

        return describeCommand
    }

    private Git ensureGit(Project project) {
        if (git == null) {
            File gitDir = new File(project.rootDir, '.git')

            if (!gitDir.exists()) {
                throw new IllegalArgumentException('Cannot find \'.git\' directory')
            }

            git = Git.wrap(new FileRepository(gitDir))
        }

        return git
    }
}


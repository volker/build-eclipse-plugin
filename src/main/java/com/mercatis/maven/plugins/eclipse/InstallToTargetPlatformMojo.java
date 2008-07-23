/*
 * Copyright 2008 mercatis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package com.mercatis.maven.plugins.eclipse;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

/**
 * Install jar to target platform location
 * 
 * @author <a href="mailto:volker.fritzsch@mercatis.com">Volker Fritzsch</a>
 * 
 * @goal installToTargetPlatformLocation
 * @phase install
 * 
 * @description Install jar to target platform location
 * @requiresProject
 */
public class InstallToTargetPlatformMojo extends AbstractMojo {

	/**
	 * @parameter expression="${project}"
	 * @readonly
	 * @required
	 */
	private MavenProject project;
	
	/**
	 * @parameter
	 * @required
	 */
	private File targetPlatformLocation;
	
	public void execute() throws MojoExecutionException, MojoFailureException {
		File src = project.getArtifact().getFile();
		File pluginFolder = new File(new File(targetPlatformLocation, "eclipse"), "plugins");
		try {
			FileUtils.copyFileToDirectory(src, pluginFolder);
		} catch (IOException ex) {
			throw new MojoExecutionException(ex.getMessage(), ex);
		}
		getLog().info("Copied " + src.getAbsolutePath() + " to plugin folder " + pluginFolder.getAbsolutePath());
	}

}

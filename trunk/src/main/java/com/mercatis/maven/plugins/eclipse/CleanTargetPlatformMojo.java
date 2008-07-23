/*
 * Copyright 2008 Volker Fritzsch
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
import org.codehaus.plexus.util.FileUtils;

/**
 * Materializes an eclipse target platform
 * 
 * @author <a href="mailto:volker.fritzsch@mercatis.com">Volker Fritzsch</a>
 * 
 * @goal cleanTargetPlatform
 * @phase clean
 * 
 * @description Cleans an eclipse target platform
 * @requiresProject
 */
public class CleanTargetPlatformMojo extends AbstractMojo {

	/**
	 * @parameter
	 * @required
	 */
	private File targetPlatformLocation;
	
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (!targetPlatformLocation.exists())
			return;
		
		getLog().info("Deleting directory " + targetPlatformLocation.getAbsolutePath());
		try {
			FileUtils.deleteDirectory(targetPlatformLocation.getAbsolutePath());
		} catch (IOException ex) {
			throw new MojoExecutionException(ex.getMessage(), ex);
		}
	}

}

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
import java.net.URL;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.ant.taskdefs.Get;

/**
 * Materializes an eclipse target platform
 * 
 * @author <a href="mailto:volker.fritzsch@mercatis.com">Volker Fritzsch</a>
 * 
 * @goal materializeTargetPlatform
 * @phase validate
 * 
 * @description Materializes an eclipse target platform
 * @requiresProject
 */
public class MaterializeTargetPlatformMojo extends AbstractMojo {

	/**
	 * @parameter alias="eclipsePackages"
	 */
	private URL[] eclipsePackages;

	/**
	 * @parameter alias="packageRepository"
	 */
	private File packageRepository;
	
	/**
	 * @parameter expression="${localRepository}"
	 * @readonly
	 * @required
	 */
	private ArtifactRepository repository;

	/**
	 * @parameter
	 * @required
	 */
	private File targetPlatformLocation;

	public void execute() throws MojoExecutionException, MojoFailureException {
		if (packageRepository == null) {
			packageRepository = new File(repository.getBasedir());
			packageRepository = new File(packageRepository.getParentFile(), "eclipse-target-repository");
			if (!packageRepository.exists())
				packageRepository.mkdir();
		}
		
		getLog().info("Using packageRepository " + packageRepository.getAbsolutePath() + "'.");
		getLog().info("Materializing target platform '" + targetPlatformLocation.getAbsolutePath() + "'.");
		
		if (!targetPlatformLocation.exists()) {
			getLog().debug("Creating target platform folder.");
			targetPlatformLocation.mkdirs();
		}
		
		if (!targetPlatformLocation.isDirectory()) {
			getLog().error(
					"Target platform location '" + targetPlatformLocation.toString()
							+ "' is a file, not a folder.");
			throw new MojoFailureException("Target Platform Location must be a folder.");
		}

		for (URL eclipsePackage : eclipsePackages) {
			fetchPackage(eclipsePackage);
		}
	}

	private void fetchPackage(URL pkg) throws MojoExecutionException {
		Get antTask = new Get();
		antTask.setProject(new Project());

		try {
			final String filename = pkg.getPath().substring(pkg.getPath().lastIndexOf("/") + 1);
			antTask.setDest(new File(packageRepository, filename));
			antTask.setSrc(pkg);
			antTask.setUseTimestamp(true);

			antTask.doGet(0, new Get.DownloadProgress() {

				public void onTick() {
					System.out.print(".");
				}

				public void endDownload() {
					System.out.println(" succeeded.");
				}

				public void beginDownload() {
					getLog().info("Downloading '" + filename + "'");
				}
			});

			extractPackage(new File(packageRepository, filename));

		} catch (Exception ex) {
			throw new MojoExecutionException(ex.getMessage(), ex);
		}
	}

	private void extractPackage(File pkg) {
		getLog().info("Extracting '" + pkg.getName() + "' to target platform.");
		Expand antTask = new Expand();
		antTask.setProject(new Project());
		antTask.setDest(targetPlatformLocation);
		antTask.setSrc(pkg);
		antTask.setOverwrite(false);
		antTask.perform();
	}

}

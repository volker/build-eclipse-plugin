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

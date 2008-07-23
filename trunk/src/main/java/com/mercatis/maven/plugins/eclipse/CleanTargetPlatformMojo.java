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

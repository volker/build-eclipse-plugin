package com.mercatis.maven.plugins.eclipse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.model.Plugin;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Add resources defined in build.properties to the finally packaged jar
 * 
 * @author <a href="mailto:volker.fritzsch@mercatis.com">Volker Fritzsch</a>
 * 
 * @goal analyseBuildProperties
 * @phase package
 * 
 * @description Add resources defined in build.properties to the finally packaged jar
 * @requiresProject
 */
public class AnalyseBuildPropertiesMojo extends AbstractMojo {

	/**
	 * @parameter expression="${project}"
	 * @readonly
	 * @required
	 */
	private MavenProject project;
	
	/**
	 * @parameter default-value="${basedir}/build.properties"
	 */
	private File buildProperties;
	
	/**
	 * @parameter expression="${basedir}"
	 */
	private File basedir;
	
	private Map<String,Resource> resourcesLookup;
	
	public void execute() throws MojoExecutionException, MojoFailureException {
		initializeResourcesLookup();
		Properties props = readBuildProperties();
		
		addBinIncludesToResources(props);
		
		if (getLog().isDebugEnabled()) {
			logDebugResources();
		}

		adjustJarMojoConfiguration();
	}

	@SuppressWarnings("unchecked")
	private void adjustJarMojoConfiguration() {
		getLog().debug("Altering Jar Mojo configuration to use default manifest file.");
		List<Plugin> plugins = (List<Plugin>) project.getBuildPlugins();
		for (Plugin plugin : plugins) {
			if (plugin.getGroupId().compareTo("org.apache.maven.plugins") == 0
					&& plugin.getArtifactId().compareTo("maven-jar-plugin") == 0) {
				
				Xpp3Dom dom = (Xpp3Dom) plugin.getConfiguration();
				if (dom == null) {
					dom = new Xpp3Dom("jar");
				}
				Xpp3Dom child = dom.getChild("useDefaultManifestFile");
				if (child == null) {
					child = new Xpp3Dom("useDefaultManifestFile");
					dom.addChild(child);
				}
				child.setValue("true");
				plugin.setConfiguration(dom);
				break;
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void logDebugResources() {
		List<Resource> resources = (List<Resource>) project.getResources();
		for (Resource res : resources) {
			getLog().debug("Resource: " + res.getDirectory() + ":" + res.getIncludes() + ":" + res.getTargetPath());
		}
	}

	private void addBinIncludesToResources(Properties props) {
		String binIncludes = props.getProperty("bin.includes");
		if (binIncludes == null)
			return;

		String[] includes = binIncludes.split(",");
		for (String include : includes) {
			if (include.compareTo(".") != 0) {
				addResourceFile(basedir, include);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void initializeResourcesLookup() {
		resourcesLookup = new HashMap<String,Resource>();
		List<Resource> resources = (List<Resource>) project.getResources();
		for (Resource res : resources) {
			resourcesLookup.put(res.getDirectory(), res);
		}
	}

	private Properties readBuildProperties() throws MojoExecutionException {
		if (getLog().isDebugEnabled()) {
			getLog().debug("Reading build.properties '" + buildProperties.getAbsolutePath() + "'.");
		}
		Properties props = new Properties();
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(buildProperties);
			props.load(fis);
		} catch (IOException ex) {
			throw new MojoExecutionException(ex.getMessage(), ex);
		} finally {
			try {
				fis.close();
			} catch (Exception ex) {
				throw new MojoExecutionException(ex.getMessage(), ex);
			}
		}
		return props;
	}

	private Resource addResourceFolder(File folder) {
		String path = folder.getAbsolutePath();
		Resource resource = resourcesLookup.get(path);
		if (resource == null) {
			resource = new Resource();
			resource.setDirectory(path);
			resourcesLookup.put(path, resource);
			project.addResource(resource);
		}
		return resource;
	}
	
	private void addResourceFile(File folder, String name) {
		Resource resource = addResourceFolder(folder);
		resource.addInclude(name);
	}
}

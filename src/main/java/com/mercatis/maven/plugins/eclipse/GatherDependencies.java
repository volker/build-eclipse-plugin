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
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.osgi.framework.internal.core.FilterImpl;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;

/**
 * Analysis the dependencies and populates the target accordingly. Also, adjusts
 * the classpath for compilation.
 * 
 * @author <a href="mailto:volker.fritzsch@mercatis.com">Volker Fritzsch</a>
 * 
 * @goal gatherDependencies
 * @phase process-resources
 * 
 * @description Analysis the dependencies
 * @requiresProject
 * @requiresDependencyResolution compile
 */
public class GatherDependencies extends AbstractMojo {
	
	/**
	 * @parameter expression="${project}"
	 */
	private MavenProject project;
	
	/**
	 * @parameter default-value="${basedir}/META-INF/MANIFEST.MF"
	 */
	private File bundleManifest;
	
	/**
	 * @parameter expression="${component.org.apache.maven.artifact.factory.ArtifactFactory}"
	 */
	private ArtifactFactory factory;

	/**
	 * @parameter
	 * @required
	 */
	private File targetPlatformLocation;
	
	private Map<String,File> symbolicNameLookup;

	private Map<String,String> exportPackageLookup;
	
	private Map<String,String> fragmentHostLookup;
	
	private File pluginFolder;
	
	private Set<String> bundlesAlreadyAnalysed = new HashSet<String>();
	
	private Dictionary<String,String> platformFilterDictionary;

	public void execute() throws MojoExecutionException, MojoFailureException {
		initializePlatformFilterDictionary();
		pluginFolder = new File(new File(targetPlatformLocation, "eclipse"), "plugins");
		
		copyDeclaredMavenDependencies();
		
		scanTarget();
		
		try {
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(bundleManifest);
				Manifest mf = new Manifest(fis);

				addDependencies(mf);
//				analyseRequireBundleDirectives(header);
//				analyseImportPackageDirectives(attributes);
			} finally {
				fis.close();
			}
		} catch (Exception ex) {
			throw new MojoExecutionException(ex.getMessage(), ex);
		}
	}
	
	private void initializePlatformFilterDictionary() {
		platformFilterDictionary = new Hashtable<String,String>();
		platformFilterDictionary.put("osgi.ws", "win32");
		platformFilterDictionary.put("osgi.os", "win32");
		platformFilterDictionary.put("osgi.arch", "x86");
		platformFilterDictionary.put("osgi.nl", "en");
	}
	
	private void addDependencies(Manifest mf) throws MojoExecutionException {
		if (bundlesAlreadyAnalysed.contains(mf.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME)))
			return;
		analyseRequireBundleDirectives(mf);
		analyseImportPackageDirectives(mf);
		bundlesAlreadyAnalysed.add(mf.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME));
	}

	private void analyseRequireBundleDirectives(Manifest manifest) throws MojoExecutionException {
		ManifestElement[] elements = null;
		try {
			elements = ManifestElement.parseHeader(Constants.REQUIRE_BUNDLE, (String) manifest.getMainAttributes().getValue(Constants.REQUIRE_BUNDLE));
		} catch (BundleException ex) {
			throw new MojoExecutionException(ex.getMessage(), ex);
		}
		
		if (elements == null)
			return;
		
		for (ManifestElement element : elements) {
			if (symbolicNameLookup.containsKey(element.getValue())) {
				String bundleName = element.getValue();
				File bundle = symbolicNameLookup.get(bundleName);
				try {
					Manifest requireBundleManifest = (new JarFile(bundle)).getManifest();
					addDependencies(requireBundleManifest);
				} catch (IOException ex) {
					throw new MojoExecutionException(ex.getMessage(), ex);
				}

				addDependencyToMavenProject(bundleName, bundle);
				
				getLog().debug("RequireBundle directive for " + element.getValue() + " is satisfied.");
			}
			else {
				getLog().warn("RequireBundle directive for " + element.getValue() + " is not satisfied.");
			}
		}
	}
	
	private void analyseImportPackageDirectives(Manifest mf) throws MojoExecutionException {
		ManifestElement[] elements = null;
		try {
			elements = ManifestElement.parseHeader(Constants.REQUIRE_BUNDLE, (String) mf.getMainAttributes().getValue(Constants.REQUIRE_BUNDLE));
		} catch (BundleException ex) {
			throw new MojoExecutionException(ex.getMessage(), ex);
		}
		
		if (elements == null)
			return;
		
		for (ManifestElement element : elements) {
			if (exportPackageLookup.containsKey(element.getValue())) {
				String bundleName = exportPackageLookup.get(element.getValue());
				File bundle = symbolicNameLookup.get(bundleName);
				try {
					Manifest requireBundleManifest = (new JarFile(bundle)).getManifest();
					addDependencies(requireBundleManifest);
				} catch (IOException ex) {
					throw new MojoExecutionException(ex.getMessage(), ex);
				}
				
				addDependencyToMavenProject(bundleName, bundle);
				
				// if we've got a fragment for this bundle, attach it as well
				if (fragmentHostLookup.containsKey(bundleName)) {
					String fragment = fragmentHostLookup.get(bundleName);
					File fragmentFile = symbolicNameLookup.get(fragment);
					addDependencyToMavenProject(fragment, fragmentFile);
				}
				
				getLog().debug("ImportPackage directive for " + element.getValue() + " is satisfied.");
			} else {
				getLog().warn("ImportPackage directive for " + element.getValue() + " is not satisfied.");
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void addDependencyToMavenProject(String bundleName, File bundle) {
		Artifact fact = factory.createDependencyArtifact("ECLIPSE", bundleName, VersionRange.createFromVersion("0.0"), "jar", null, Artifact.SCOPE_SYSTEM);
		fact.setFile(bundle);
		fact.setResolved(true);
		project.getDependencyArtifacts().add(fact);
	}

	@SuppressWarnings("unchecked")
	private void copyDeclaredMavenDependencies() throws MojoExecutionException {
		try {
			Set<Artifact> artifacts = project.getArtifacts();
			for (Artifact artifact : artifacts) {
				getLog().info("Copying dependency " + artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion() + " to plugin folder.");
				FileUtils.copyFileToDirectory(artifact.getFile(), pluginFolder);
			}

			// remove repository based dependencies
			project.getDependencyArtifacts().clear();
		} catch (IOException ex) {
			throw new MojoExecutionException(ex.getMessage(), ex);
		}
	}
	
	private void scanTarget() throws MojoExecutionException {
		File target = new File(this.targetPlatformLocation, "eclipse/plugins");
		File[] bundles = target.listFiles(new FilenameFilter() {

			public boolean accept(File dir, String name) {
				return name.endsWith("jar");
			}
		});

		try {
			symbolicNameLookup = new HashMap<String,File>();
			exportPackageLookup = new HashMap<String,String>();
			fragmentHostLookup = new HashMap<String,String>();
			
			for (File bundle : bundles) {
				JarFile jar = new JarFile(bundle);
				Manifest manifest = jar.getManifest();
				Attributes attributes = manifest.getMainAttributes();

				String symbolicName = null;
				String version = null;
				String[] exportedPkgs = new String[0];

				symbolicName = attributes.getValue(Constants.BUNDLE_SYMBOLICNAME);
				if (symbolicName == null)
					continue; // no osgi bundle
				
				if (symbolicName.contains(";"))
					symbolicName = symbolicName.substring(0, symbolicName
							.indexOf(";"));

				ManifestElement[] elements = ManifestElement.parseHeader(Constants.FRAGMENT_HOST, attributes.getValue(Constants.FRAGMENT_HOST));
				if (elements != null && elements.length > 0) {
					//this bundle is actually a fragment
					//now lets look if it fits our platform
					
					ManifestElement[] filter = ManifestElement.parseHeader("Eclipse-PlatformFilter", attributes.getValue("Eclipse-PlatformFilter"));
					if (filter != null && filter.length > 0) {
						Filter f = new FilterImpl(filter[0].getValue());
						if (f.match(platformFilterDictionary)) {
							fragmentHostLookup.put(elements[0].getValue(), symbolicName);
							symbolicNameLookup.put(symbolicName.trim(), bundle);
						}
					}
					continue;
				}
				
				version = attributes.getValue(Constants.BUNDLE_VERSION);
				org.eclipse.osgi.service.resolver.VersionRange vr = new org.eclipse.osgi.service.resolver.VersionRange(version);

				if (attributes.getValue(Constants.EXPORT_PACKAGE) != null) {
					exportedPkgs = attributes
							.getValue(Constants.EXPORT_PACKAGE).split(",");
					for (int i = 0; i < exportedPkgs.length; i++) {
						if (exportedPkgs[i].contains(";"))
							exportedPkgs[i] = exportedPkgs[i].substring(0,
									exportedPkgs[i].indexOf(";"));
					}
				}

				getLog().debug("Identified bundle " + symbolicName + " / " + version);
				symbolicNameLookup.put(symbolicName.trim(), bundle);
				for (String pkg : exportedPkgs) {
					getLog().debug(" Exports: " + pkg);
					exportPackageLookup.put(pkg.trim(), symbolicName.trim());
				}
			}
			
		} catch (Exception ex) {
			throw new MojoExecutionException(ex.getMessage(), ex);
		}
	}

}

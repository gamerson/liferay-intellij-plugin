/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.ide.idea.util;

import static org.jetbrains.plugins.gradle.settings.GradleExtensionsSettings.GradleExtensionsData;
import static org.jetbrains.plugins.gradle.settings.GradleExtensionsSettings.GradleTask;
import static org.jetbrains.plugins.gradle.settings.GradleExtensionsSettings.Settings;
import static org.jetbrains.plugins.gradle.settings.GradleExtensionsSettings.getInstance;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.net.JarURLConnection;
import java.net.URL;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;

import org.gradle.internal.impldep.org.apache.commons.io.FileUtils;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;

import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;
import org.jetbrains.plugins.gradle.settings.GradleSettings;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;

/**
 * @author Terry Jia
 * @author Charles Wu
 */
public class GradleUtil {

	/**
	 * @param file build.gradle file
	 */
	public static void addGradleDependencies(PsiFile file, String... dependencies) {
		Project project = file.getProject();

		WriteCommandAction.Builder builder = WriteCommandAction.writeCommandAction(project, file);

		builder.withName(
			"Add Gradle Dependency"
		).run(
			() -> {
				GroovyPsiElementFactory groovyPsiElementFactory = GroovyPsiElementFactory.getInstance(project);

				List<GrMethodCall> grMethodCalls = PsiTreeUtil.getChildrenOfTypeAsList(file, GrMethodCall.class);

				GrCall dependenciesBlock = ContainerUtil.find(
					grMethodCalls,
					call -> {
						GrExpression grExpression = call.getInvokedExpression();

						return Objects.equals("dependencies", grExpression.getText());
					});

				if (dependenciesBlock == null) {
					StringBuilder stringBuilder = new StringBuilder();

					for (String dependency : dependencies) {
						stringBuilder.append(String.format("compileOnly '%s'\n", dependency));
					}

					dependenciesBlock = (GrCall)groovyPsiElementFactory.createStatementFromText(
						"dependencies{\n" + stringBuilder + "}");

					file.add(dependenciesBlock);
				}
				else {
					GrClosableBlock grClosableBlock = ArrayUtil.getFirstElement(
						dependenciesBlock.getClosureArguments());

					if (grClosableBlock != null) {
						for (String dependency : dependencies) {
							grClosableBlock.addStatementBefore(
								groovyPsiElementFactory.createStatementFromText(
									String.format("compileOnly '%s'\n", dependency)),
								null);
						}
					}
				}
			}
		);

		GradleSettings gradleSettings = GradleSettings.getInstance(project);

		String projectRoot = project.getBasePath();

		if (projectRoot != null) {
			GradleProjectSettings gradleProjectSettings = gradleSettings.getLinkedProjectSettings(projectRoot);

			if ((gradleProjectSettings != null) && !gradleProjectSettings.isUseAutoImport()) {
				ExternalSystemUtil.refreshProjects(new ImportSpecBuilder(project, GradleConstants.SYSTEM_ID));
			}
		}
	}

	public static <T> T getModel(Class<T> modelClass, VirtualFile virtualFile) throws Exception {
		T retval = null;

		Path cachePath = Paths.get(System.getProperty("user.home", "") + "/.liferay-ide");

		try {
			File depsDir = new File(cachePath.toFile(), "deps");

			depsDir.mkdirs();

			String path = depsDir.getAbsolutePath();

			path = path.replaceAll("\\\\", "/");

			_extractJar(depsDir, "gradle-tooling");

			ClassLoader bladeClassLoader = GradleUtil.class.getClassLoader();

			File scriptFile = new File(cachePath.toFile(), "init.gradle");

			try (InputStream input = bladeClassLoader.getResourceAsStream("com/liferay/ide/idea/util/init.gradle")) {
				String initScriptTemplate = CoreUtil.readStreamToString(input);

				String initScriptContents = initScriptTemplate.replaceFirst("%deps%", path);

				if (FileUtil.notExists(scriptFile)) {
					scriptFile.createNewFile();
				}

				FileUtils.writeByteArrayToFile(scriptFile, initScriptContents.getBytes());
			}

			GradleConnector gradleConnector = GradleConnector.newConnector(
			).forProjectDirectory(
				Paths.get(
					virtualFile.getPath()
				).toFile()
			);

			ProjectConnection connection = gradleConnector.connect();

			ModelBuilder<T> model = connection.model(modelClass);

			model.withArguments("--init-script", scriptFile.getAbsolutePath(), "--stacktrace");

			retval = model.get();
		}
		catch (Exception e) {
			throw e;
		}

		return retval;
	}

	public static boolean isWatchableProject(Module module) {
		Settings settings = getInstance(module.getProject());

		GradleExtensionsData gradleExtensionsData = settings.getExtensionsFor(module);

		if (gradleExtensionsData == null) {
			return false;
		}

		for (GradleTask gradleTask : gradleExtensionsData.tasks) {
			if (Objects.equals("watch", gradleTask.name) &&
				Objects.equals("com.liferay.gradle.plugins.tasks.WatchTask", gradleTask.typeFqn)) {

				return true;
			}
		}

		return false;
	}

	private static void _extractJar(File depsDir, String jarName) throws IOException {
		String fullFileName = jarName + ".jar";

		File toolingJar = new File(depsDir, fullFileName);

		ClassLoader bladeClassLoader = GradleUtil.class.getClassLoader();

		URL url = bladeClassLoader.getResource("/libs/" + fullFileName);

		boolean needToCopy = true;

		try (InputStream in = bladeClassLoader.getResourceAsStream("/libs/" + fullFileName)) {
			JarURLConnection jarURLConnection = (JarURLConnection)url.openConnection();

			JarEntry jarEntry = jarURLConnection.getJarEntry();

			Long bladeJarTimestamp = jarEntry.getTime();

			if (toolingJar.exists()) {
				Long destTimestamp = toolingJar.lastModified();

				if (destTimestamp < bladeJarTimestamp) {
					toolingJar.delete();
				}
				else {
					needToCopy = false;
				}
			}

			if (needToCopy) {
				FileUtil.writeFile(toolingJar, in);
				toolingJar.setLastModified(bladeJarTimestamp);
			}
		}
		catch (IOException ioe) {
		}
	}

}
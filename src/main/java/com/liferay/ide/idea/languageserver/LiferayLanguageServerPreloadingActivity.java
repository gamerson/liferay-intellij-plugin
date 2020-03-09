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

package com.liferay.ide.idea.languageserver;

import com.intellij.openapi.application.PreloadingActivity;
import com.intellij.openapi.progress.ProgressIndicator;

import com.liferay.ide.idea.util.FileUtil;
import com.liferay.ide.idea.util.SocketSupport;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.net.JarURLConnection;
import java.net.URL;

import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import org.wso2.lsp4intellij.IntellijLanguageClient;
import org.wso2.lsp4intellij.client.languageserver.serverdefinition.RawCommandServerDefinition;

/**
 * @author Dominik Marks
 * @author Terry Jia
 */
public class LiferayLanguageServerPreloadingActivity extends PreloadingActivity implements SocketSupport {

	@Override
	public void preload(@NotNull ProgressIndicator progressIndicator) {
		String[] args = {
			_computeJavaPath(), "-DliferayLanguageServerStandardIO=true", "-jar",
			_computeLiferayPropertiesServerJarPath()
		};

		RawCommandServerDefinition rawCommandServerDefinition = new RawCommandServerDefinition("properties", args);

		IntellijLanguageClient.addServerDefinition(rawCommandServerDefinition);
	}

	private static String _computeJavaPath() {
		String javaPath = "java";

		String paths = System.getenv("PATH");

		boolean existsInPath = Stream.of(
			paths.split(Pattern.quote(File.pathSeparator))
		).map(
			Paths::get
		).anyMatch(
			path -> Files.exists(path.resolve("java"))
		);

		if (!existsInPath) {
			String javaHome = System.getProperty("java.home");

			File file = new File(javaHome, "bin/java" + (_isWindows() ? ".exe" : ""));

			javaPath = file.getAbsolutePath();
		}

		return javaPath;
	}

	private static String _computeLiferayPropertiesServerJarPath() {
		Properties properties = System.getProperties();

		File temp = new File(properties.getProperty("user.home"), ".liferay-intellij-plugin");

		File liferayPropertiesServerJar = new File(temp, _PROPERTIES_LSP_JAR_FILE_NAME);

		boolean needToCopy = true;

		ClassLoader classLoader = LiferayLanguageServerPreloadingActivity.class.getClassLoader();

		URL url = classLoader.getResource("/libs/" + _PROPERTIES_LSP_JAR_FILE_NAME);

		try (InputStream in = classLoader.getResourceAsStream("/libs/" + _PROPERTIES_LSP_JAR_FILE_NAME)) {
			JarURLConnection jarURLConnection = (JarURLConnection)url.openConnection();

			JarEntry jarEntry = jarURLConnection.getJarEntry();

			Long bladeJarTimestamp = jarEntry.getTime();

			if (liferayPropertiesServerJar.exists()) {
				Long destTimestamp = liferayPropertiesServerJar.lastModified();

				if (destTimestamp < bladeJarTimestamp) {
					liferayPropertiesServerJar.delete();
				}
				else {
					needToCopy = false;
				}
			}

			if (needToCopy) {
				FileUtil.writeFile(liferayPropertiesServerJar, in);
				liferayPropertiesServerJar.setLastModified(bladeJarTimestamp);
			}
		}
		catch (IOException ioe) {
		}

		return liferayPropertiesServerJar.getAbsolutePath();
	}

	private static boolean _isWindows() {
		String osName = System.getProperty("os.name");

		osName = osName.toLowerCase();

		return osName.contains("windows");
	}

	private static final String _PROPERTIES_LSP_JAR_FILE_NAME = "liferay-properties-server-all.jar";

}
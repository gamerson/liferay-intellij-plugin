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

package com.liferay.ide.idea.language.lsp;

import com.intellij.openapi.application.PreloadingActivity;
import com.intellij.openapi.progress.ProgressIndicator;

import java.io.File;

import java.util.Properties;

import org.jetbrains.annotations.NotNull;

import org.wso2.lsp4intellij.IntellijLanguageClient;

/**
 * @author Dominik Marks
 */
public class BladeLanguageServerPreloadingActivity extends PreloadingActivity {

	@Override
	public void preload(@NotNull ProgressIndicator progressIndicator) {
		Properties properties = System.getProperties();

		File temp = new File(properties.getProperty("user.home"), ".liferay-ide");

		File bladeJar = new File(temp, "blade.jar");

		if (bladeJar.exists()) {
			IntellijLanguageClient.addServerDefinition(
				new SocketCommandServerDefinition(
					"properties",
					new String[] {"java", "-jar", bladeJar.getAbsolutePath(), "languageServer", "-p", "12345"}, 12345));
		}
	}

}
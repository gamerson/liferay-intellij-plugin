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

package com.liferay.ide.idea.core;

import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.OutputWrapper;
import com.intellij.util.execution.ParametersListUtil;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.List;
import java.util.stream.Collectors;

import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.CancellationTokenSource;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProgressListener;
import org.gradle.tooling.ProjectConnection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.service.execution.GradleProgressListener;
import org.jetbrains.plugins.gradle.service.task.GradleTaskManagerExtension;
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings;
import org.jetbrains.plugins.gradle.util.GradleConstants;

/**
 * @author Simon Jiang
 */
public class LiferayGradleTaskManager implements GradleTaskManagerExtension {

	@Override
	public boolean cancelTask(
			@NotNull ExternalSystemTaskId id, @NotNull ExternalSystemTaskNotificationListener listener)
		throws ExternalSystemException {

		listener.onCancel(id);

		return true;
	}

	@Override
	public boolean executeTasks(
			@NotNull ExternalSystemTaskId id, @NotNull List<String> taskNames, @NotNull String projectPath,
			@Nullable GradleExecutionSettings settings, @Nullable String jvmParametersSetup,
			@NotNull ExternalSystemTaskNotificationListener listener)
		throws ExternalSystemException {

		if (ExternalSystemApiUtil.isInProcessMode(GradleConstants.SYSTEM_ID)) {
			final List<String> tasks = taskNames.stream(
			).flatMap(
				s -> ParametersListUtil.parse(
					s, false, true
				).stream()
			).collect(
				Collectors.toList()
			);

			if (tasks.contains("startDockerContainer") && tasks.contains("logsDockerContainer")) {
				try {
					GradleConnector gradleConnector = GradleConnector.newConnector();

					Path projectVirtualFilePath = Paths.get(projectPath);

					gradleConnector.forProjectDirectory(projectVirtualFilePath.toFile());

					ProjectConnection connection = gradleConnector.connect();

					BuildLauncher buildLauncher = connection.newBuild();

					buildLauncher.addArguments(settings.getArguments());

					buildLauncher.forTasks(tasks.toArray(new String[0]));

					GradleProgressListener gradleProgressListener = new GradleProgressListener(listener, id);

					buildLauncher.addProgressListener((ProgressListener)gradleProgressListener);

					buildLauncher.addProgressListener(
						(org.gradle.tooling.events.ProgressListener)gradleProgressListener);

					buildLauncher.setStandardOutput(new OutputWrapper(listener, id, true));

					buildLauncher.setStandardError(new OutputWrapper(listener, id, false));

					CancellationTokenSource cancellationTokenSource = GradleConnector.newCancellationTokenSource();

					buildLauncher.withCancellationToken(cancellationTokenSource.token());

					buildLauncher.run();
				}
				catch (Exception exception) {
					throw new ExternalSystemException(exception);
				}

				return true;
			}

			return GradleTaskManagerExtension.super.executeTasks(
				id, taskNames, projectPath, settings, jvmParametersSetup, listener);
		}

		return false;
	}

}
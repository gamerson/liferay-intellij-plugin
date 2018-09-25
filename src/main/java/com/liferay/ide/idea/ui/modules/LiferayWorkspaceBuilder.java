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

package com.liferay.ide.idea.ui.modules;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.SdkSettingsStep;
import com.intellij.ide.util.projectWizard.SettingsStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.StdModuleTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.Condition;

import com.liferay.ide.idea.util.BladeCLI;
import com.liferay.ide.idea.util.StringUtil;
import com.liferay.ide.idea.util.WorkspaceConstants;

import java.io.File;

import java.util.stream.Stream;

import javax.swing.JComboBox;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Terry Jia
 * @author Joye Luo
 */
public abstract class LiferayWorkspaceBuilder extends ModuleBuilder {

	public LiferayWorkspaceBuilder(String liferayProjectType) {
		_liferayProjectType = liferayProjectType;
	}

	@Nullable
	@Override
	public ModuleWizardStep getCustomOptionsStep(WizardContext context, Disposable parentDisposable) {
		return super.getCustomOptionsStep(context, parentDisposable);
	}

	@Override
	public ModuleType getModuleType() {
		return StdModuleTypes.JAVA;
	}

	@Nullable
	@Override
	public ModuleWizardStep modifySettingsStep(@NotNull SettingsStep settingsStep) {
		JComboBox liferayVersionComboBox = new ComboBox();

		Stream.of(
			WorkspaceConstants.LIFERAY_VERSIONS
		).forEach(
			liferayVersion -> liferayVersionComboBox.addItem(liferayVersion)
		);

		settingsStep.addSettingsField("Liferay version:", liferayVersionComboBox);

		JComboBox targetPlatformComboBox = new ComboBox();

		Stream.of(
			WorkspaceConstants.TARGET_PLATFORM_VERSIONS_7_0
		).forEach(
			targetPlatform -> targetPlatformComboBox.addItem(targetPlatform)
		);

		Stream.of(
			WorkspaceConstants.TARGET_PLATFORM_VERSIONS_7_1
		).forEach(
			targetPlatform -> targetPlatformComboBox.addItem(targetPlatform)
		);

		targetPlatformComboBox.addActionListener(
			e -> _targetPlatform = StringUtil.toString(targetPlatformComboBox.getSelectedItem()));

		if ("LiferayGradleWorkspce".equals(_liferayProjectType)) {
			settingsStep.addSettingsField("Target platform:", targetPlatformComboBox);
		}

		liferayVersionComboBox.addActionListener(
			e -> {
				String selected = StringUtil.toString(liferayVersionComboBox.getSelectedItem());

				_liferayVersion = selected;

				if ("7.0".equals(selected)) {
					targetPlatformComboBox.setSelectedItem(WorkspaceConstants.TARGET_PLATFORM_VERSIONS_7_0[0]);
				}
				else if ("7.1".equals(selected)) {
					targetPlatformComboBox.setSelectedItem(WorkspaceConstants.TARGET_PLATFORM_VERSIONS_7_1[0]);
				}
			});

		return new SdkSettingsStep(
			settingsStep, this,
			new Condition<SdkTypeId>() {

				@Override
				public boolean value(SdkTypeId sdkType) {
					return isSuitableSdkType(sdkType);
				}

			});
	}

	protected void initWorkspace(Project project) {
		StringBuilder sb = new StringBuilder();

		sb.append("--base ");
		sb.append("\"");
		sb.append(project.getBasePath());
		sb.append("\" ");
		sb.append("init ");
		sb.append("-v ");
		sb.append(_liferayVersion);
		sb.append(" ");
		sb.append("-f ");

		if (_liferayProjectType.equals(LiferayProjectType.LIFERAY_MAVEN_WORKSPACE)) {
			sb.append("-b ");
			sb.append("maven");
		}

		PropertiesComponent component = PropertiesComponent.getInstance(project);
		String selectedLiferayVersionProperty = "selected.liferay.version";

		component.setValue(selectedLiferayVersionProperty, _liferayVersion);

		BladeCLI.execute(sb.toString());

		if ("LiferayGradleWorkspce".equals(_liferayProjectType)) {
			try {
				PropertiesConfiguration config = new PropertiesConfiguration(
					new File(project.getBasePath(), "gradle.properties"));

				config.setProperty(WorkspaceConstants.DEFAULT_TARGET_PLATFORM_VERSION_PROPERTY, _targetPlatform);

				config.save();
			}
			catch (ConfigurationException ce) {
			}
		}
	}

	private String _liferayProjectType;
	private String _liferayVersion = WorkspaceConstants.LIFERAY_VERSIONS[0];
	private String _targetPlatform = WorkspaceConstants.TARGET_PLATFORM_VERSIONS_7_0[0];

}
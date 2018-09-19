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

import com.liferay.blade.cli.BladeCLI;
import com.liferay.blade.cli.command.InitArgs;
import com.liferay.blade.cli.command.InitCommand;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;

import javax.swing.JComboBox;

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

		liferayVersionComboBox.addItem("7.0");
		liferayVersionComboBox.addItem("7.1");

		liferayVersionComboBox.addActionListener(
			new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					Object selectedVersion = liferayVersionComboBox.getSelectedItem();

					_liferayVersion = selectedVersion.toString();
				}

			});

		settingsStep.addSettingsField("Liferay version:", liferayVersionComboBox);

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
		BladeCLI bladeCLI = new BladeCLI();

		InitArgs initArgs = new InitArgs();

		initArgs.setForce(true);
		initArgs.setBase(new File(project.getBasePath()));
		initArgs.setLiferayVersion(_liferayVersion);

		if (_liferayProjectType.equals(LiferayProjectType.LIFERAY_MAVEN_WORKSPACE)) {
			initArgs.setBuild("maven");
		}

		InitCommand initCommand = new InitCommand();

		initCommand.setBlade(bladeCLI);
		initCommand.setArgs(initArgs);

		try {
			initCommand.execute();
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		PropertiesComponent component = PropertiesComponent.getInstance(project);
		String selectedLiferayVersionProperty = "selected.liferay.version";

		component.setValue(selectedLiferayVersionProperty, _liferayVersion);
	}

	private String _liferayProjectType;
	private String _liferayVersion = "7.0";

}
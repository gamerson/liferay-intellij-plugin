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

package com.liferay.ide.idea.extensions;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.actions.CodeInsightAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiFile;

import com.liferay.ide.idea.core.MessagesBundle;

import icons.LiferayIcons;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.jetbrains.plugins.groovy.GroovyFileType;

/**
 * @author Charles Wu
 */
public class AddGradleDependencyAction extends CodeInsightAction {

	public AddGradleDependencyAction() {
		getTemplatePresentation().setDescription(MessagesBundle.message("action.add_liferay_dependency.description"));
		getTemplatePresentation().setText(MessagesBundle.message("action.add_liferay_dependency.text"));
		getTemplatePresentation().setIcon(LiferayIcons.LIFERAY_ICON);
	}

	@NotNull
	@Override
	protected CodeInsightActionHandler getHandler() {
		return new AddGradleDependencyActionHandler();
	}

	@Override
	protected boolean isValidForFile(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
		if (file instanceof PsiCompiledElement) {
			return false;
		}

		if (!GroovyFileType.GROOVY_FILE_TYPE.equals(file.getFileType())) {
			return false;
		}

		String fileName = file.getName();

		if (!fileName.equals(GradleConstants.SETTINGS_FILE_NAME) && fileName.endsWith(GradleConstants.EXTENSION)) {
			return true;
		}

		return false;
	}

}
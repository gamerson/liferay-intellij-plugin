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
import com.intellij.codeInsight.FileModificationService;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;

import com.liferay.ide.idea.ui.compoments.LiferayArtifactSearchDialog;
import com.liferay.ide.idea.util.GradleUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.indices.MavenArtifactSearchDialog;
import org.jetbrains.idea.maven.model.MavenId;

/**
 * @author Charles Wu
 */
public class AddGradleDependencyActionHandler implements CodeInsightActionHandler {

	@Override
	public void invoke(@NotNull final Project project, @NotNull final Editor editor, @NotNull final PsiFile file) {
		if (!EditorModificationUtil.checkModificationAllowed(editor) ||
			!FileModificationService.getInstance().preparePsiElementsForWrite(file)) {

			return;
		}

		final List<MavenId> mavenIds = LiferayArtifactSearchDialog.searchForArtifact(project);

		if (mavenIds.isEmpty()) {
			return;
		}

		String[] dependencies = mavenIds.stream().map(
			AddGradleDependencyActionHandler::getMavenArtifactKey
		).toArray(
			String[]::new
		);

		GradleUtil.addGradleDependencies(file, dependencies);
	}

	@Override
	public boolean startInWriteAction() {
		return false;
	}

	private static void append(StringBuilder builder, String part) {
		if (builder.length() != 0) {
			builder.append(':');
		}
		builder.append(part == null ? "" : part);
	}

	@NotNull
	private static String getMavenArtifactKey(MavenId mavenId) {
		StringBuilder builder = new StringBuilder();

		append(builder, mavenId.getGroupId());
		append(builder, mavenId.getArtifactId());
		append(builder, mavenId.getVersion());

		return builder.toString();
	}

}
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

import java.util.List;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.model.MavenId;

/**
 * @author Charles Wu
 */
public class AddGradleDependencyActionHandler implements CodeInsightActionHandler {

	@Override
	public void invoke(@NotNull final Project project, @NotNull final Editor editor, @NotNull final PsiFile file) {
		if (!EditorModificationUtil.checkModificationAllowed(editor)) {
			return;
		}

		FileModificationService fileModificationService = FileModificationService.getInstance();

		if (!fileModificationService.preparePsiElementsForWrite(file)) {
			return;
		}

		final List<MavenId> mavenIds = LiferayArtifactSearchDialog.searchForArtifact(project);

		if (mavenIds.isEmpty()) {
			return;
		}

		Stream<MavenId> stream = mavenIds.stream();

		String[] dependencies = stream.map(
			AddGradleDependencyActionHandler::_getMavenArtifactKey
		).toArray(
			String[]::new
		);

		GradleUtil.addGradleDependencies(file, dependencies);
	}

	@Override
	public boolean startInWriteAction() {
		return false;
	}

	private static void _append(StringBuilder builder, String part) {
		if (builder.length() != 0) {
			builder.append(':');
		}

		builder.append(part == null ? "" : part);
	}

	@NotNull
	private static String _getMavenArtifactKey(MavenId mavenId) {
		StringBuilder builder = new StringBuilder();

		_append(builder, mavenId.getGroupId());
		_append(builder, mavenId.getArtifactId());
		_append(builder, mavenId.getVersion());

		return builder.toString();
	}

}
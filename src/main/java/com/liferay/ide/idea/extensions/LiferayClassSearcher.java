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

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.model.MavenArtifactInfo;

/**
 * @author Charles Wu
 */
public class LiferayClassSearcher extends Searcher<LiferayClassSearchResult> {

	@Override
	protected String makeSortKey(LiferayClassSearchResult result) {
		return _makeKey(result.className, result.versions.get(0));
	}

	@Override
	protected List<LiferayClassSearchResult> searchImpl(Project project, String pattern, int maxResult) {
		PsiShortNamesCache psiShortNamesCache = PsiShortNamesCache.getInstance(project);

		Application application = ApplicationManager.getApplication();

		PsiClass[] psiClasses = application.runReadAction(
			(Computable<PsiClass[]>)() -> psiShortNamesCache.getClassesByName(
				pattern, GlobalSearchScope.allScope(project)));

		return _processResults(project, psiClasses);
	}

	@Nullable
	private LiferayClassSearchResult _extractPsiToArtifact(Project project, PsiClass psiClass) {
		ProjectRootManager projectRootManager = ProjectRootManager.getInstance(project);

		ProjectFileIndex projectFileIndex = projectRootManager.getFileIndex();

		PsiFile psiFile = psiClass.getContainingFile();

		for (OrderEntry orderEntry : projectFileIndex.getOrderEntriesForFile(psiFile.getVirtualFile())) {
			if (orderEntry instanceof LibraryOrderEntry) {
				final LibraryOrderEntry libraryOrderEntry = (LibraryOrderEntry)orderEntry;

				final Library library = libraryOrderEntry.getLibrary();

				if (library == null) {
					continue;
				}

				VirtualFile[] virtualFiles = library.getFiles(OrderRootType.CLASSES);

				if (virtualFiles.length == 0) {
					continue;
				}

				if (virtualFiles[0] == null) {
					continue;
				}

				VirtualFile vf = virtualFiles[0];

				LiferayClassSearchResult result = new LiferayClassSearchResult();

				result.className = psiClass.getName();
				result.packageName = psiClass.getQualifiedName();

				String path = vf.getPath();

				String[] split = path.split("/");

				result.versions = Collections.singletonList(
					new MavenArtifactInfo(
						split[split.length - 5], split[split.length - 4], split[split.length - 3], "jar", null));

				return result;
			}
		}

		return null;
	}

	private String _makeKey(String className, MavenArtifactInfo info) {
		return className + " " + super.makeKey(info);
	}

	private List<LiferayClassSearchResult> _processResults(Project project, PsiClass[] psiClasses) {
		return Stream.of(
			psiClasses
		).map(
			psiClass -> _extractPsiToArtifact(project, psiClass)
		).filter(
			Objects::nonNull
		).collect(
			Collectors.toList()
		);
	}

}
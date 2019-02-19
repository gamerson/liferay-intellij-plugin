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

import com.intellij.openapi.externalSystem.model.project.LibraryData;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtil;
import com.liferay.ide.idea.util.LiferayWorkspaceUtil;
import org.jetbrains.idea.maven.model.MavenArtifactInfo;

import java.util.ArrayList;
import java.util.List;

import static com.intellij.openapi.util.text.StringUtil.*;

/**
 * @author Charles Wu
 */
public class LiferayArtifactSearcher extends Searcher<LiferayArtifactSearchResult> {

	@Override
	protected List<LiferayArtifactSearchResult> searchImpl(Project project, String pattern, int maxResult) {
		List<String> parts = new ArrayList<>();
		for (String each : tokenize(pattern, " :")) {
			parts.add(trimStart(trimEnd(each, "*"), "*"));
		}

		List<LiferayArtifactSearchResult> searchResults = ContainerUtil.newSmartList();
		int count = 0;

		for (LibraryData libraryData : LiferayWorkspaceUtil.getTargetPlatformArtifacts(project)) {
			if (count >= maxResult) {
				break;
			}

			//matched getGroupId id
			if (parts.size() < 1 || contains(libraryData.getGroupId(), parts.get(0))) {
				//matched artifact id
				if (parts.size() < 2 || contains(libraryData.getArtifactId(), parts.get(1))) {
					LiferayArtifactSearchResult searchResult = new LiferayArtifactSearchResult();
					searchResult.versions.add(new MavenArtifactInfo(libraryData.getGroupId(), libraryData.getArtifactId(), libraryData.getVersion(), "jar", null));
					searchResults.add(searchResult);
					if (++count >= maxResult) {
						break;
					}
				}
			}
		}

		return searchResults;
	}

}
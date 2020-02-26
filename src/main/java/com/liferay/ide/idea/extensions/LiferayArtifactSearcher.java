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

import static com.intellij.openapi.util.text.StringUtil.contains;
import static com.intellij.openapi.util.text.StringUtil.tokenize;
import static com.intellij.openapi.util.text.StringUtil.trimEnd;
import static com.intellij.openapi.util.text.StringUtil.trimStart;

import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtil;

import com.liferay.ide.idea.util.LiferayWorkspaceSupport;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.idea.maven.model.MavenArtifactInfo;

/**
 * @author Charles Wu
 * @author Terry Jia
 */
public class LiferayArtifactSearcher extends Searcher<LiferayArtifactSearchResult> implements LiferayWorkspaceSupport {

	@Override
	protected List<LiferayArtifactSearchResult> searchImpl(Project project, String pattern, int maxResult) {
		List<String> parts = new ArrayList<>();

		for (String each : tokenize(pattern, " :")) {
			parts.add(trimStart(trimEnd(each, "*"), "*"));
		}

		List<LiferayArtifactSearchResult> searchResults = ContainerUtil.newSmartList();
		int count = 0;

		List<String> dependencies = getTargetPlatformDependencies(project);

		for (String dependency : dependencies) {
			if (count >= maxResult) {
				break;
			}

			int i1 = dependency.indexOf(":");
			int i2 = dependency.indexOf(" ");

			if ((i1 < 0) || (i2 < 0)) {
				continue;
			}

			String groupId = dependency.substring(0, i1);
			String artifactId = dependency.substring(i1 + 1, i2);
			String version = dependency.substring(i2 + 1);

			//matched getGroupId id

			if (parts.isEmpty() || contains(groupId, parts.get(0))) {
				//matched artifact id

				if ((parts.size() < 2) || contains(artifactId, parts.get(1))) {
					LiferayArtifactSearchResult searchResult = new LiferayArtifactSearchResult();

					searchResult.versions.add(new MavenArtifactInfo(groupId, artifactId, version, "jar", null));

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
/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 * <p>
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * <p>
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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.jetbrains.idea.maven.indices.MavenProjectIndicesManager;
import org.jetbrains.idea.maven.model.MavenArtifactInfo;

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
		MavenProjectIndicesManager m = MavenProjectIndicesManager.getInstance(project);
		int count = 0;
		List<MavenArtifactInfo> versions = new ArrayList<>();

		for (String groupId : m.getGroupIds()) {
			if (count >= maxResult)break;

			if (parts.size() < 1 || contains(groupId, parts.get(0))) {
				for (String artifactId : m.getArtifactIds(groupId)) {
					if (parts.size() < 2 || contains(artifactId, parts.get(1))) {
						for (String version : m.getVersions(groupId, artifactId)) {
							if (parts.size() < 3 || contains(version, parts.get(2))) {
								versions.add(new MavenArtifactInfo(groupId, artifactId, version, "jar", null));

								if (++count >= maxResult)break;
							}
						}
					}
					else if (parts.size() == 2 && VERSION_PATTERN.matcher(parts.get(1)).matches()) {
						for (String version : m.getVersions(groupId, artifactId)) {
							if (contains(version, parts.get(1))) {
								versions.add(new MavenArtifactInfo(groupId, artifactId, version, "jar", null));

								if (++count >= maxResult)break;
							}
						}
					}

					if (!versions.isEmpty()) {
						LiferayArtifactSearchResult searchResult = new LiferayArtifactSearchResult();

						searchResult.versions.addAll(versions);
						searchResults.add(searchResult);
						versions.clear();
					}

					if (count >= maxResult)break;
				}
			}
		}

		return searchResults;
	}

	private static final Pattern VERSION_PATTERN = Pattern.compile("[.\\d]+");

}
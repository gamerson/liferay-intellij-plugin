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

import com.intellij.openapi.project.Project;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

import org.jetbrains.idea.maven.dom.MavenVersionComparable;
import org.jetbrains.idea.maven.model.MavenArtifactInfo;

/**
 * @author Charles Wu
 */
public abstract class Searcher<RESULT_TYPE extends LiferayArtifactSearchResult> {

	public List<RESULT_TYPE> search(Project project, String pattern, int maxResult) {
		return sort(searchImpl(project, pattern, maxResult));
	}

	protected String makeKey(MavenArtifactInfo result) {
		return result.getGroupId() + ":" + result.getArtifactId();
	}

	protected String makeSortKey(RESULT_TYPE result) {
		return makeKey(result.versions.get(0));
	}

	protected abstract List<RESULT_TYPE> searchImpl(Project project, String pattern, int maxResult);

	private List<RESULT_TYPE> sort(List<RESULT_TYPE> result) {
		for (RESULT_TYPE each : result) {
			if (each.versions.size() > 1) {
				TreeMap<MavenVersionComparable, MavenArtifactInfo> tree = new TreeMap<>(Collections.reverseOrder());

				for (MavenArtifactInfo artifactInfo : each.versions) {
					tree.put(new MavenVersionComparable(artifactInfo.getVersion()), artifactInfo);
				}

				each.versions.clear();
				each.versions.addAll(tree.values());
			}
		}

		Collections.sort(result, Comparator.comparing(this::makeSortKey));

		return result;
	}

}
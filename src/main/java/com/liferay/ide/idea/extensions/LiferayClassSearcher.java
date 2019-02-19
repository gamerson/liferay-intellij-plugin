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
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;

import gnu.trove.THashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.WildcardQuery;

import org.jetbrains.idea.maven.indices.MavenProjectIndicesManager;
import org.jetbrains.idea.maven.model.MavenArtifactInfo;
import org.jetbrains.idea.maven.server.MavenServerIndexer;

/**
 * @author Charles Wu
 */
public class LiferayClassSearcher extends Searcher<LiferayClassSearchResult> {

	public static final String TERM = MavenServerIndexer.SEARCH_TERM_CLASS_NAMES;

	@Override
	protected String makeSortKey(LiferayClassSearchResult result) {
		return makeKey(result.className, result.versions.get(0));
	}

	protected Pair<String, Query> preparePatternAndQuery(String pattern) {
		pattern = pattern.toLowerCase();

		if (pattern.trim().length() == 0) {
			return new Pair<>(pattern, new MatchAllDocsQuery());
		}

		List<String> parts = StringUtil.split(pattern, ".");

		StringBuilder newPattern = new StringBuilder();

		for (int i = 0; i < parts.size() - 1; i++) {
			String each = parts.get(i);

			newPattern.append(each.trim());
			newPattern.append("*.");
		}

		String className = parts.get(parts.size() - 1);

		boolean exactSearch = className.endsWith(" ");
		newPattern.append(className.trim());

		if (!exactSearch)newPattern.append("*");

		pattern = newPattern.toString();
		String queryPattern = "*/" + pattern.replaceAll("\\.", "/");

		return new Pair<>(pattern, new WildcardQuery(new Term(TERM, queryPattern)));
	}

	protected Collection<LiferayClassSearchResult> processResults(
		Set<MavenArtifactInfo> infos, String pattern, int maxResult) {

		if (pattern.length() == 0 || pattern.equals("*")) {
			pattern = "^/(.*)$";
		}
		else {
			pattern = pattern.replace(".", "/");

			int lastDot = pattern.lastIndexOf("/");
			String packagePattern = lastDot == -1 ? "" : (pattern.substring(0, lastDot) + "/");
			String classNamePattern = lastDot == -1 ? pattern : pattern.substring(lastDot + 1);

			packagePattern = packagePattern.replaceAll("\\*", ".*?");
			classNamePattern = classNamePattern.replaceAll("\\*", "[^/]*?");

			pattern = packagePattern + classNamePattern;

			pattern = ".*?/" + pattern;
			pattern = "^(" + pattern + ")$";
		}

		Pattern p;

		try {
			p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
		}
		catch (PatternSyntaxException pse) {
			return Collections.emptyList();
		}

		Map<String, LiferayClassSearchResult> result = new THashMap<>();

		for (MavenArtifactInfo each : infos) {
			if (each.getClassNames() == null) continue;

			Matcher matcher = p.matcher(each.getClassNames());
			while (matcher.find()) {
				String classFQName = matcher.group(1);

				classFQName = classFQName.replace("/", ".");
				classFQName = StringUtil.trimStart(classFQName, ".");

				String key = makeKey(classFQName, each);

				LiferayClassSearchResult classResult = result.get(key);

				if (classResult == null) {
					classResult = new LiferayClassSearchResult();
					int pos = classFQName.lastIndexOf(".");

					if (pos == -1) {
						classResult.packageName = "default package";
						classResult.className = classFQName;
					}
					else {
						classResult.packageName = classFQName.substring(0, pos);
						classResult.className = classFQName.substring(pos + 1);
					}

					result.put(key, classResult);
				}

				classResult.versions.add(each);

				if (result.size() > maxResult) break;
			}
		}

		return result.values();
	}

	@Override
	protected List<LiferayClassSearchResult> searchImpl(Project project, String pattern, int maxResult) {
		Pair<String, Query> patternAndQuery = preparePatternAndQuery(pattern);

		MavenProjectIndicesManager m = MavenProjectIndicesManager.getInstance(project);

		Set<MavenArtifactInfo> infos = m.search(patternAndQuery.second, maxResult);

		return new ArrayList<>(processResults(infos, patternAndQuery.first, maxResult));
	}

	private String makeKey(String className, MavenArtifactInfo info) {
		return className + " " + super.makeKey(info);
	}

}
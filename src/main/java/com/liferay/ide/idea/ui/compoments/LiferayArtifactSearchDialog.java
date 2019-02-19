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

package com.liferay.ide.idea.ui.compoments;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.TabbedPaneWrapper;
import com.intellij.util.ui.JBUI;

import gnu.trove.THashMap;

import java.util.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.dom.model.MavenDomDependency;
import org.jetbrains.idea.maven.model.MavenId;

/**
 * @author Charles Wu
 */
public class LiferayArtifactSearchDialog extends DialogWrapper {

	public static List<MavenId> ourResultForTest;

	@NotNull
	public static List<MavenId> searchForArtifact(Project project, Collection<MavenDomDependency> managedDependencies) {
		if (ApplicationManager.getApplication().isUnitTestMode()) {
			assert ourResultForTest != null;

			List<MavenId> res = ourResultForTest;
			ourResultForTest = null;

			return res;
		}

		LiferayArtifactSearchDialog d = new LiferayArtifactSearchDialog(project, "", false);

		d.setManagedDependencies(managedDependencies);

		if (!d.showAndGet()) {
			return Collections.emptyList();
		}

		return d.getResult();
	}

	@NotNull
	public static List<MavenId> searchForClass(Project project, String className) {
		if (ApplicationManager.getApplication().isUnitTestMode()) {
			assert ourResultForTest != null;

			List<MavenId> res = ourResultForTest;
			ourResultForTest = null;

			return res;
		}

		LiferayArtifactSearchDialog d = new LiferayArtifactSearchDialog(project, className, true);

		if (!d.showAndGet()) {
			return Collections.emptyList();
		}

		return d.getResult();
	}

	@Override
	public JComponent getPreferredFocusedComponent() {
		if (myTabbedPane.getSelectedIndex() == 0) {
			return myArtifactsPanel.getSearchField();
		}

		return myClassesPanel.getSearchField();
	}

	@NotNull
	public List<MavenId> getResult() {
		return myResult;
	}

	public void setManagedDependencies(Collection<MavenDomDependency> managedDependencies) {
		myManagedDependenciesMap.clear();

		for (MavenDomDependency dependency : managedDependencies) {
			String groupId = dependency.getGroupId().getStringValue();
			String artifactId = dependency.getArtifactId().getStringValue();
			String version = dependency.getVersion().getStringValue();

			if (StringUtil.isNotEmpty(groupId) && StringUtil.isNotEmpty(artifactId) && StringUtil.isNotEmpty(version)) {
				myManagedDependenciesMap.put(Pair.create(groupId, artifactId), version);
			}
		}
	}

	@Override
	protected JComponent createCenterPanel() {
		return myTabbedPane.getComponent();
	}

	@Override
	protected void doOKAction() {
		LiferayArtifactSearchPanel panel = myTabbedPane.getSelectedIndex() == 0 ? myArtifactsPanel : myClassesPanel;

		myResult = panel.getResult();
		super.doOKAction();
	}

	@Override
	protected String getDimensionServiceKey() {
		return "Maven.ArtifactSearchDialog";
	}

	@NotNull
	@Override
	protected Action getOKAction() {
		Action result = super.getOKAction();

		result.putValue(Action.NAME, "Add");

		return result;
	}

	private LiferayArtifactSearchDialog(Project project, String initialText, boolean classMode) {
		super(project, true);

		initComponents(project, initialText, classMode);

		setTitle("Maven Artifact Search");
		updateOkButtonState();
		init();

		myArtifactsPanel.scheduleSearch();
		myClassesPanel.scheduleSearch();
	}

	private void initComponents(Project project, String initialText, boolean classMode) {
		myTabbedPane = new TabbedPaneWrapper(project);

		LiferayArtifactSearchPanel.Listener listener = new LiferayArtifactSearchPanel.Listener() {

			@Override
			public void canSelectStateChanged(@NotNull LiferayArtifactSearchPanel from, boolean canSelect) {
				myOkButtonStates.put(from, canSelect);
				updateOkButtonState();
			}

			@Override
			public void itemSelected() {
				clickDefaultButton();
			}

		};

		myArtifactsPanel = new LiferayArtifactSearchPanel(
			project, !classMode ? initialText : "", false, listener, this, myManagedDependenciesMap);
		myClassesPanel = new LiferayArtifactSearchPanel(
			project, classMode ? initialText : "", true, listener, this, myManagedDependenciesMap);

		myTabbedPane.addTab("Search for artifact", myArtifactsPanel);
		myTabbedPane.addTab("Search for class", myClassesPanel);
		myTabbedPane.setSelectedIndex(classMode ? 1 : 0);

		myTabbedPane.getComponent().setPreferredSize(JBUI.size(900, 600));

		myTabbedPane.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				updateOkButtonState();
			}

		});

		updateOkButtonState();
	}

	private void updateOkButtonState() {
		Boolean canSelect = myOkButtonStates.get(myTabbedPane.getSelectedComponent());

		if (canSelect == null)canSelect = false;
		setOKActionEnabled(canSelect);
	}

	private LiferayArtifactSearchPanel myArtifactsPanel;
	private LiferayArtifactSearchPanel myClassesPanel;
	private final Map<Pair<String, String>, String> myManagedDependenciesMap = new HashMap<>();
	private final Map<LiferayArtifactSearchPanel, Boolean> myOkButtonStates = new THashMap<>();
	private List<MavenId> myResult = Collections.emptyList();
	private TabbedPaneWrapper myTabbedPane;

}
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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.TabbedPaneWrapper;
import com.intellij.util.ui.JBUI;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.dom.model.MavenDomDependency;
import org.jetbrains.idea.maven.model.MavenId;

import javax.swing.*;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Charles Wu
 */
public class LiferayArtifactSearchDialog extends DialogWrapper {

	@NotNull
	public static List<MavenId> searchForArtifact(Project project) {
		LiferayArtifactSearchDialog dialog = new LiferayArtifactSearchDialog(project, "", false);

		if (!dialog.showAndGet()) {
			return Collections.emptyList();
		}

		return dialog.getResult();
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
		return "Liferay.ArtifactSearchDialog";
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

		setTitle("Liferay Artifact Search");
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
		myTabbedPane.addTab("Search for class name", myClassesPanel);
		myTabbedPane.setSelectedIndex(classMode ? 1 : 0);

		myTabbedPane.getComponent().setPreferredSize(JBUI.size(900, 600));

		myTabbedPane.addChangeListener(e -> updateOkButtonState());

		updateOkButtonState();
	}

	private void updateOkButtonState() {
		Boolean canSelect = myOkButtonStates.get(myTabbedPane.getSelectedComponent());

		if (canSelect == null) {
			canSelect = false;
		}

		setOKActionEnabled(canSelect);
	}

	private LiferayArtifactSearchPanel myArtifactsPanel;
	private LiferayArtifactSearchPanel myClassesPanel;
	private final Map<Pair<String, String>, String> myManagedDependenciesMap = new HashMap<>();
	private final Map<LiferayArtifactSearchPanel, Boolean> myOkButtonStates = new THashMap<>();
	private List<MavenId> myResult = Collections.emptyList();
	private TabbedPaneWrapper myTabbedPane;

}
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
import com.intellij.ui.TabbedPaneWrapper;
import com.intellij.util.ui.JBUI;

import gnu.trove.THashMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.JComponent;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.model.MavenId;

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
		return _liferayArtifactSearchPanel.getSearchField();
	}

	@NotNull
	public List<MavenId> getResult() {
		return _result;
	}

	@Override
	protected JComponent createCenterPanel() {
		return _tabbedPane.getComponent();
	}

	@Override
	protected void doOKAction() {
		_result = _liferayArtifactSearchPanel.getResult();

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

		_initComponents(project, initialText, classMode);

		setTitle("Liferay Artifact Search");
		_updateOkButtonState();
		init();

		_liferayArtifactSearchPanel.scheduleSearch();
	}

	private void _initComponents(Project project, String initialText, boolean classMode) {
		_tabbedPane = new TabbedPaneWrapper(project);

		LiferayArtifactSearchPanel.Listener listener = new LiferayArtifactSearchPanel.Listener() {

			@Override
			public void canSelectStateChanged(
				@NotNull LiferayArtifactSearchPanel liferayArtifactSearchPanel, boolean canSelect) {

				_okButtonStates.put(liferayArtifactSearchPanel, canSelect);
				_updateOkButtonState();
			}

			@Override
			public void itemSelected() {
				clickDefaultButton();
			}

		};

		_liferayArtifactSearchPanel = new LiferayArtifactSearchPanel(
			project, !classMode ? initialText : "", false, listener, this, _managedDependenciesMap);

		_tabbedPane.addTab("Search for artifact", _liferayArtifactSearchPanel);

		_tabbedPane.setSelectedIndex(0);

		JComponent component = _tabbedPane.getComponent();

		component.setPreferredSize(JBUI.size(900, 600));

		_tabbedPane.addChangeListener(e -> _updateOkButtonState());

		_updateOkButtonState();
	}

	private void _updateOkButtonState() {
		Boolean canSelect = _okButtonStates.get(_tabbedPane.getSelectedComponent());

		if (canSelect == null) {
			canSelect = false;
		}

		setOKActionEnabled(canSelect);
	}

	private LiferayArtifactSearchPanel _liferayArtifactSearchPanel;
	private final Map<Pair<String, String>, String> _managedDependenciesMap = new HashMap<>();
	private final Map<LiferayArtifactSearchPanel, Boolean> _okButtonStates = new THashMap<>();
	private List<MavenId> _result = Collections.emptyList();
	private TabbedPaneWrapper _tabbedPane;

}
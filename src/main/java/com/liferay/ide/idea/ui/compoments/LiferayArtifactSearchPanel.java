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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Pair;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.Alarm;
import com.intellij.util.ui.AbstractLayoutManager;
import com.intellij.util.ui.StatusText;
import com.intellij.util.ui.UIUtil;

import com.liferay.ide.idea.extensions.LiferayArtifactSearchResult;
import com.liferay.ide.idea.extensions.LiferayArtifactSearcher;
import com.liferay.ide.idea.extensions.LiferayClassSearchResult;
import com.liferay.ide.idea.extensions.LiferayClassSearcher;
import com.liferay.ide.idea.extensions.Searcher;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.text.Document;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.model.MavenArtifactInfo;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.idea.maven.utils.MavenLog;

/**
 * @author Charles Wu
 */
public class LiferayArtifactSearchPanel extends JPanel {

	public LiferayArtifactSearchPanel(
		Project project, String initialText, boolean classMode, Listener listener, LiferayArtifactSearchDialog dialog,
		Map<Pair<String, String>, String> managedDependenciesMap) {

		_project = project;
		_classMode = classMode;
		_listener = listener;
		_dialog = dialog;
		_managedDependenciesMap = managedDependenciesMap;

		_initComponents(initialText);
		_alarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, dialog.getDisposable());
	}

	@NotNull
	public List<MavenId> getResult() {
		List<MavenId> result = new ArrayList<>();

		for (TreePath each : _resultList.getSelectionPaths()) {
			Object sel = each.getLastPathComponent();
			MavenArtifactInfo info;

			if (sel instanceof MavenArtifactInfo) {
				info = (MavenArtifactInfo)sel;
			}
			else {
				info = ((LiferayArtifactSearchResult)sel).versions.get(0);
			}

			result.add(new MavenId(info.getGroupId(), info.getArtifactId(), info.getVersion()));
		}

		return result;
	}

	public JTextField getSearchField() {
		return _searchField;
	}

	public void scheduleSearch() {
		_listener.canSelectStateChanged(this, false);
		_resultList.setPaintBusy(true);

		// evaluate text value in the swing thread

		final String text = _searchField.getText();

		_alarm.cancelAllRequests();
		_alarm.addRequest(
			() -> {
				try {
					_doSearch(text);
				}
				catch (Throwable e) {
					MavenLog.LOG.warn(e);
				}
			},
			500);
	}

	public interface Listener {

		public void canSelectStateChanged(@NotNull LiferayArtifactSearchPanel from, boolean canSelect);

		public void itemSelected();

	}

	private void _doSearch(String searchText) {
		Searcher<? extends LiferayArtifactSearchResult> searcher =
			_classMode ? new LiferayClassSearcher() : new LiferayArtifactSearcher();

		List<? extends LiferayArtifactSearchResult> result = searcher.search(_project, searchText, _MAX_RESULT);

		_resortUsingDependencyVersionMap(result);

		final TreeModel model = new MyTreeModel(result);

		SwingUtilities.invokeLater(
			() -> {
				if (!_dialog.isVisible()) {
					return;
				}

				StatusText text = _resultList.getEmptyText();

				text.setText("No results");

				_resultList.setModel(model);
				_resultList.setSelectionRow(0);
				_resultList.setPaintBusy(false);
			});
	}

	private void _initComponents(String initialText) {
		_resultList = new Tree();

		_resultList.setExpandableItemsEnabled(false);

		StatusText text = _resultList.getEmptyText();

		text.setText("Loading...");

		_resultList.setRootVisible(false);
		_resultList.setShowsRootHandles(true);
		_resultList.setModel(null);

		MyArtifactCellRenderer renderer;

		if (_classMode) {
			renderer = new MyClassCellRenderer(_resultList);
		}
		else {
			renderer = new MyArtifactCellRenderer(_resultList);
		}

		_resultList.setCellRenderer(renderer);

		Dimension preferredSize = renderer.getPreferredSize();

		_resultList.setRowHeight(preferredSize.height);

		_searchField = new JTextField(initialText);

		_searchField.addKeyListener(
			new KeyAdapter() {

				@Override
				public void keyPressed(KeyEvent e) {
					int direction;

					if (e.getKeyCode() == KeyEvent.VK_DOWN) {
						direction = 1;
					}
					else if (e.getKeyCode() == KeyEvent.VK_UP) {
						direction = -1;
					}
					else {
						return;
					}

					TreeSelectionModel selectionModel = _resultList.getSelectionModel();

					int row = selectionModel.getLeadSelectionRow();

					row += direction;

					if ((row >= 0) && (row < _resultList.getRowCount())) {
						_resultList.setSelectionRow(row);
					}
				}

			});

		setLayout(new BorderLayout());
		add(_searchField, BorderLayout.NORTH);
		JScrollPane pane = ScrollPaneFactory.createScrollPane(_resultList);

		pane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS); // Don't remove this line.

		// Without VERTICAL_SCROLLBAR_ALWAYS policy our custom layout
		// works incorrectly, see https://youtrack.jetbrains.com/issue/IDEA-72986

		add(pane, BorderLayout.CENTER);

		Document document = _searchField.getDocument();

		document.addDocumentListener(
			new DocumentAdapter() {

				@Override
				protected void textChanged(@NotNull DocumentEvent e) {
					scheduleSearch();
				}

			});

		_resultList.addTreeSelectionListener(
			e -> {
				if (!_alarm.isEmpty()) {
					return;
				}

				boolean hasSelection = !_resultList.isSelectionEmpty();

				_listener.canSelectStateChanged(LiferayArtifactSearchPanel.this, hasSelection);
			});

		_resultList.addKeyListener(
			new KeyAdapter() {

				@Override
				public void keyPressed(KeyEvent event) {
					if ((event.getKeyCode() == KeyEvent.VK_ENTER) &&
						(_resultList.getLastSelectedPathComponent() != null)) {

						_listener.itemSelected();
						event.consume();
					}
				}

			});

		new DoubleClickListener() {

			@Override
			protected boolean onDoubleClick(MouseEvent event) {
				final TreePath path = _resultList.getPathForLocation(event.getX(), event.getY());

				if ((path != null) && _resultList.isPathSelected(path)) {
					Object sel = path.getLastPathComponent();

					TreeModel treeModel = _resultList.getModel();

					if ((sel != null) && treeModel.isLeaf(sel)) {
						_listener.itemSelected();

						return true;
					}
				}

				return false;
			}

		}.installOn(_resultList);
	}

	private void _resortUsingDependencyVersionMap(List<? extends LiferayArtifactSearchResult> result) {
		for (LiferayArtifactSearchResult searchResult : result) {
			if (searchResult.versions.isEmpty()) {
				continue;
			}

			MavenArtifactInfo artifactInfo = searchResult.versions.get(0);

			final String managedVersion = _managedDependenciesMap.get(
				Pair.create(artifactInfo.getGroupId(), artifactInfo.getArtifactId()));

			if (managedVersion != null) {
				searchResult.versions.sort(
					(o1, o2) -> {
						String v1 = o1.getVersion();
						String v2 = o2.getVersion();

						if (Comparing.equal(v1, v2)) {
							return 0;
						}

						if (managedVersion.equals(v1)) {
							return -1;
						}

						if (managedVersion.equals(v2)) {
							return 1;
						}

						return 0;
					});
			}
		}
	}

	private static final int _MAX_RESULT = 1000;

	private final Alarm _alarm;
	private final boolean _classMode;
	private final LiferayArtifactSearchDialog _dialog;
	private final Listener _listener;
	private final Map<Pair<String, String>, String> _managedDependenciesMap;
	private final Project _project;
	private Tree _resultList;
	private JTextField _searchField;

	private static class MyTreeModel implements TreeModel {

		@Override
		public void addTreeModelListener(TreeModelListener l) {
		}

		@Override
		public Object getChild(Object parent, int index) {
			return getList(parent).get(index);
		}

		@Override
		public int getChildCount(Object parent) {
			List list = getList(parent);

			assert list != null : parent;

			return list.size();
		}

		@Override
		public int getIndexOfChild(Object parent, Object child) {
			return getList(parent).indexOf(child);
		}

		public List getList(Object parent) {
			if (parent == _items) {
				return _items;
			}

			if (parent instanceof LiferayArtifactSearchResult) {
				return ((LiferayArtifactSearchResult)parent).versions;
			}

			return null;
		}

		@Override
		public Object getRoot() {
			return _items;
		}

		@Override
		public boolean isLeaf(Object node) {
			if ((node != _items) && ((getList(node) == null) || (getChildCount(node) < 2))) {
				return true;
			}

			return false;
		}

		@Override
		public void removeTreeModelListener(TreeModelListener l) {
		}

		@Override
		public void valueForPathChanged(TreePath path, Object newValue) {
		}

		private MyTreeModel(List<? extends LiferayArtifactSearchResult> items) {
			_items = items;
		}

		private List<? extends LiferayArtifactSearchResult> _items;

	}

	private class MyArtifactCellRenderer extends JPanel implements TreeCellRenderer {

		@Override
		public Component getTreeCellRendererComponent(
			JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {

			leftComponent.clear();
			rightComponent.clear();

			setBackground(selected ? UIUtil.getTreeSelectionBackground(hasFocus) : tree.getBackground());

			leftComponent.setForeground(selected ? UIUtil.getTreeSelectionForeground(hasFocus) : null);
			rightComponent.setForeground(selected ? UIUtil.getTreeSelectionForeground(hasFocus) : null);

			TreeModel treeModel = tree.getModel();

			if (value == treeModel.getRoot()) {
				leftComponent.append("Results", SimpleTextAttributes.REGULAR_ATTRIBUTES);
			}
			else if (value instanceof LiferayArtifactSearchResult) {
				formatSearchResult(tree, (LiferayArtifactSearchResult)value, selected);
			}
			else if (value instanceof MavenArtifactInfo) {
				MavenArtifactInfo info = (MavenArtifactInfo)value;

				String version = info.getVersion();

				String managedVersion = _managedDependenciesMap.get(
					Pair.create(info.getGroupId(), info.getArtifactId()));

				if ((managedVersion != null) && managedVersion.equals(version)) {
					leftComponent.append(version, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
					leftComponent.append(" (from <dependencyManagement>)", SimpleTextAttributes.GRAYED_ATTRIBUTES);
				}
				else {
					leftComponent.append(version, SimpleTextAttributes.REGULAR_ATTRIBUTES);
				}
			}

			return this;
		}

		protected void appendArtifactInfo(SimpleColoredComponent component, MavenArtifactInfo info, boolean selected) {
			component.append(info.getGroupId() + ":", getGrayAttributes(selected));
			component.append(info.getArtifactId(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
			component.append(":" + info.getVersion(), getGrayAttributes(selected));
		}

		protected void formatSearchResult(JTree tree, LiferayArtifactSearchResult searchResult, boolean selected) {
			MavenArtifactInfo info = searchResult.versions.get(0);
			leftComponent.setIcon(AllIcons.Nodes.PpLib);

			appendArtifactInfo(leftComponent, info, selected);
		}

		protected SimpleTextAttributes getGrayAttributes(boolean selected) {
			if (!selected) {
				return SimpleTextAttributes.GRAY_ATTRIBUTES;
			}

			return SimpleTextAttributes.REGULAR_ATTRIBUTES;
		}

		protected SimpleColoredComponent leftComponent = new SimpleColoredComponent();
		protected SimpleColoredComponent rightComponent = new SimpleColoredComponent();

		private MyArtifactCellRenderer(final Tree tree) {
			leftComponent.setOpaque(false);
			rightComponent.setOpaque(false);
			leftComponent.setIconOpaque(false);
			rightComponent.setIconOpaque(false);
			add(leftComponent);
			add(rightComponent);

			EditorColorsManager colorsManager = EditorColorsManager.getInstance();

			EditorColorsScheme globalScheme = colorsManager.getGlobalScheme();

			Font font = globalScheme.getFont(EditorFontType.PLAIN);

			leftComponent.setFont(font);
			rightComponent.setFont(font);

			Dimension preferredSize = leftComponent.getPreferredSize();

			setPreferredSize(new Dimension(2000, preferredSize.height));

			setLayout(
				new AbstractLayoutManager() {

					@Override
					public void layoutContainer(Container parent) {
						int width = _getVisibleWidth();

						Dimension leftSize = leftComponent.getPreferredSize();

						Dimension rightSize = rightComponent.getPreferredSize();

						int leftWith = width - rightSize.width - 10;
						int rightWidth = rightSize.width;

						leftComponent.setBounds(0, 0, leftWith, leftSize.height);
						rightComponent.setBounds(width - rightWidth, 0, rightWidth, rightSize.height);
					}

					@Override
					public Dimension preferredLayoutSize(Container parent) {
						return new Dimension(_getVisibleWidth(), preferredSize.height);
					}

					private int _getVisibleWidth() {
						Rectangle rectangle = tree.getVisibleRect();

						int width = rectangle.width - 10;

						Insets insets = tree.getInsets();

						width -= insets.left + insets.right;

						JScrollPane scrollPane = JBScrollPane.findScrollPane(tree);

						if (scrollPane != null) {
							JScrollBar sb = scrollPane.getVerticalScrollBar();

							if (sb != null) {
								width -= sb.getWidth();
							}
						}

						return width;
					}

				});
		}

	}

	private class MyClassCellRenderer extends MyArtifactCellRenderer {

		@Override
		protected void formatSearchResult(JTree tree, LiferayArtifactSearchResult searchResult, boolean selected) {
			LiferayClassSearchResult classResult = (LiferayClassSearchResult)searchResult;
			MavenArtifactInfo info = searchResult.versions.get(0);

			leftComponent.setIcon(AllIcons.Nodes.Class);
			leftComponent.append(classResult.className, SimpleTextAttributes.REGULAR_ATTRIBUTES);
			leftComponent.append(" (" + classResult.packageName + ")", getGrayAttributes(selected));

			appendArtifactInfo(rightComponent, info, selected);
		}

		private MyClassCellRenderer(Tree tree) {
			super(tree);
		}

	}

}
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
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Pair;
import com.intellij.ui.*;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.Alarm;
import com.intellij.util.ui.AbstractLayoutManager;
import com.intellij.util.ui.UIUtil;

import com.liferay.ide.idea.extensions.LiferayArtifactSearchResult;
import com.liferay.ide.idea.extensions.LiferayArtifactSearcher;
import com.liferay.ide.idea.extensions.LiferayClassSearchResult;
import com.liferay.ide.idea.extensions.LiferayClassSearcher;
import com.liferay.ide.idea.extensions.Searcher;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.model.MavenArtifactInfo;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.idea.maven.utils.MavenLog;

/**
 * @author Charles Wu
 */
public class LiferayArtifactSearchPanel extends JPanel {

	public LiferayArtifactSearchPanel(Project project, String initialText, boolean classMode, Listener listener,
									LiferayArtifactSearchDialog dialog,
									Map<Pair<String, String>, String> managedDependenciesMap) {

		myProject = project;
		myDialog = dialog;
		myClassMode = classMode;
		myListener = listener;
		myManagedDependenciesMap = managedDependenciesMap;

		initComponents(initialText);
		myAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, dialog.getDisposable());
	}

	@NotNull
	public List<MavenId> getResult() {
		List<MavenId> result = new ArrayList<>();

		for (TreePath each : myResultList.getSelectionPaths()) {
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
		return mySearchField;
	}

	public void scheduleSearch() {
		myListener.canSelectStateChanged(this, false);
		myResultList.setPaintBusy(true);

		// evaluate text value in the swing thread

		final String text = mySearchField.getText();

		myAlarm.cancelAllRequests();
		myAlarm.addRequest(() -> {
			try {
				doSearch(text);
			}
			catch (Throwable e) {
				MavenLog.LOG.warn(e);
			}
		}, 500);
	}

	public interface Listener {

		void itemSelected();

		void canSelectStateChanged(@NotNull LiferayArtifactSearchPanel from, boolean canSelect);

	}

	private void doSearch(String searchText) {
		Searcher<? extends LiferayArtifactSearchResult> searcher = myClassMode ? new LiferayClassSearcher() : new LiferayArtifactSearcher();

		List<? extends LiferayArtifactSearchResult> result = searcher.search(myProject, searchText, MAX_RESULT);

		resortUsingDependencyVersionMap(result);

		final TreeModel model = new MyTreeModel(result);

		SwingUtilities.invokeLater(() -> {
			if (!myDialog.isVisible()) return;

			myResultList.getEmptyText().setText("No results");
			myResultList.setModel(model);
			myResultList.setSelectionRow(0);
			myResultList.setPaintBusy(false);
		});
	}

	private void initComponents(String initialText) {
		myResultList = new Tree();
		myResultList.setExpandableItemsEnabled(false);
		myResultList.getEmptyText().setText("Loading...");
		myResultList.setRootVisible(false);
		myResultList.setShowsRootHandles(true);
		myResultList.setModel(null);
		MyArtifactCellRenderer renderer = myClassMode ? new MyClassCellRenderer(myResultList)
			: new MyArtifactCellRenderer(myResultList);

		myResultList.setCellRenderer(renderer);
		myResultList.setRowHeight(renderer.getPreferredSize().height);

		mySearchField = new JTextField(initialText);
		mySearchField.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
				int d;

				if (e.getKeyCode() == KeyEvent.VK_DOWN) {
					d = 1;
				}
				else if (e.getKeyCode() == KeyEvent.VK_UP) {
					d = -1;
				}
				else {
					return;
				}

				int row = myResultList.getSelectionModel().getLeadSelectionRow();
				row += d;

				if (row >= 0 && row < myResultList.getRowCount()) {
					myResultList.setSelectionRow(row);
				}
			}

		});

		setLayout(new BorderLayout());
		add(mySearchField, BorderLayout.NORTH);
		JScrollPane pane = ScrollPaneFactory.createScrollPane(myResultList);

		pane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS); // Don't remove this line.

		// Without VERTICAL_SCROLLBAR_ALWAYS policy our custom layout
		// works incorrectly, see https://youtrack.jetbrains.com/issue/IDEA-72986

		add(pane, BorderLayout.CENTER);

		mySearchField.getDocument().addDocumentListener(new DocumentAdapter() {

			@Override
			protected void textChanged(@NotNull DocumentEvent e) {
				scheduleSearch();
			}

		});

		myResultList.addTreeSelectionListener(new TreeSelectionListener() {

			@Override
			public void valueChanged(TreeSelectionEvent e) {
				if (!myAlarm.isEmpty()) return;

				boolean hasSelection = !myResultList.isSelectionEmpty();
				myListener.canSelectStateChanged(LiferayArtifactSearchPanel.this, hasSelection);
			}

		});

		myResultList.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER && myResultList.getLastSelectedPathComponent() != null) {
					myListener.itemSelected();
					e.consume();
				}
			}

		});

		new DoubleClickListener() {

			@Override
			protected boolean onDoubleClick(MouseEvent e) {
				final TreePath path = myResultList.getPathForLocation(e.getX(), e.getY());

				if (path != null && myResultList.isPathSelected(path)) {
					Object sel = path.getLastPathComponent();

					if (sel != null && myResultList.getModel().isLeaf(sel)) {
						myListener.itemSelected();

						return true;
					}
				}

				return false;
			}

		}.installOn(myResultList);
	}

	private void resortUsingDependencyVersionMap(List<? extends LiferayArtifactSearchResult> result) {
		for (LiferayArtifactSearchResult searchResult : result) {
			if (searchResult.versions.isEmpty())continue;

			MavenArtifactInfo artifactInfo = searchResult.versions.get(0);

			final String managedVersion = myManagedDependenciesMap.get(
				Pair.create(artifactInfo.getGroupId(), artifactInfo.getArtifactId()));

			if (managedVersion != null) {
				Collections.sort(searchResult.versions, (o1, o2) -> {
					String v1 = o1.getVersion();
					String v2 = o2.getVersion();

					if (Comparing.equal(v1, v2)) return 0;

					if (managedVersion.equals(v1)) return -1;

					if (managedVersion.equals(v2)) return 1;

					return 0;
				});
			}
		}
	}

	private static final int MAX_RESULT = 1000;

	private final Alarm myAlarm;
	private final boolean myClassMode;
	private final LiferayArtifactSearchDialog myDialog;
	private final Listener myListener;
	private final Map<Pair<String, String>, String> myManagedDependenciesMap;
	private final Project myProject;
	private Tree myResultList;
	private JTextField mySearchField;

	private static class MyTreeModel implements TreeModel {

		List<? extends LiferayArtifactSearchResult> myItems;

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
			if (parent == myItems) return myItems;

			if (parent instanceof LiferayArtifactSearchResult)

				return ((LiferayArtifactSearchResult)parent).versions;

			return null;
		}

		@Override
		public Object getRoot() {
			return myItems;
		}

		@Override
		public boolean isLeaf(Object node) {
			if (node != myItems && (getList(node) == null || getChildCount(node) < 2)) {
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
			myItems = items;
		}

	}

	private class MyArtifactCellRenderer extends JPanel implements TreeCellRenderer {

		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row,
													boolean hasFocus) {

			myLeftComponent.clear();
			myRightComponent.clear();

			setBackground(selected ? UIUtil.getTreeSelectionBackground(hasFocus) : tree.getBackground());

			myLeftComponent.setForeground(selected ? UIUtil.getTreeSelectionForeground(hasFocus) : null);
			myRightComponent.setForeground(selected ? UIUtil.getTreeSelectionForeground(hasFocus) : null);

			if (value == tree.getModel().getRoot()) {
				myLeftComponent.append("Results", SimpleTextAttributes.REGULAR_ATTRIBUTES);
			}
			else if (value instanceof LiferayArtifactSearchResult) {
				formatSearchResult(tree, (LiferayArtifactSearchResult)value, selected);
			}
			else if (value instanceof MavenArtifactInfo) {
				MavenArtifactInfo info = (MavenArtifactInfo)value;

				String version = info.getVersion();

				String managedVersion = myManagedDependenciesMap.get(
					Pair.create(info.getGroupId(), info.getArtifactId()));

				if (managedVersion != null && managedVersion.equals(version)) {
					myLeftComponent.append(version, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
					myLeftComponent.append(" (from <dependencyManagement>)", SimpleTextAttributes.GRAYED_ATTRIBUTES);
				}
				else {
					myLeftComponent.append(version, SimpleTextAttributes.REGULAR_ATTRIBUTES);
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
			myLeftComponent.setIcon(AllIcons.Nodes.PpLib);
			appendArtifactInfo(myLeftComponent, info, selected);
		}

		protected SimpleTextAttributes getGrayAttributes(boolean selected) {
			if (!selected) {
				return SimpleTextAttributes.GRAY_ATTRIBUTES;
			}

			return SimpleTextAttributes.REGULAR_ATTRIBUTES;
		}

		protected SimpleColoredComponent myLeftComponent = new SimpleColoredComponent();
		protected SimpleColoredComponent myRightComponent = new SimpleColoredComponent();

		private MyArtifactCellRenderer(final Tree tree) {
			myLeftComponent.setOpaque(false);
			myRightComponent.setOpaque(false);
			myLeftComponent.setIconOpaque(false);
			myRightComponent.setIconOpaque(false);
			add(myLeftComponent);
			add(myRightComponent);

			Font font = EditorColorsManager.getInstance().getGlobalScheme().getFont(EditorFontType.PLAIN);

			myLeftComponent.setFont(font);
			myRightComponent.setFont(font);

			setPreferredSize(new Dimension(2000, myLeftComponent.getPreferredSize().height));

			setLayout(new AbstractLayoutManager() {

				@Override
				public Dimension preferredLayoutSize(Container parent) {
					return new Dimension(getVisibleWidth(), myLeftComponent.getPreferredSize().height);
				}

				@Override
				public void layoutContainer(Container parent) {
					int w = getVisibleWidth();

					Dimension ls = myLeftComponent.getPreferredSize();
					Dimension rs = myRightComponent.getPreferredSize();

					int lw = w - rs.width - 10;
					int rw = rs.width;

					myLeftComponent.setBounds(0, 0, lw, ls.height);
					myRightComponent.setBounds(w - rw, 0, rw, rs.height);
				}

				private int getVisibleWidth() {
					int w = tree.getVisibleRect().width - 10;
					Insets insets = tree.getInsets();

					w -= insets.left + insets.right;

					JScrollPane scrollPane = JBScrollPane.findScrollPane(tree);

					if (scrollPane != null) {
						JScrollBar sb = scrollPane.getVerticalScrollBar();

						if (sb != null) {
							w -= sb.getWidth();
						}
					}

					return w;
				}

			});
		}

	}

	private class MyClassCellRenderer extends MyArtifactCellRenderer {

		@Override
		protected void formatSearchResult(JTree tree, LiferayArtifactSearchResult searchResult, boolean selected) {
			LiferayClassSearchResult classResult = (LiferayClassSearchResult)searchResult;
			MavenArtifactInfo info = searchResult.versions.get(0);

			myLeftComponent.setIcon(AllIcons.Nodes.Class);
			myLeftComponent.append(classResult.className, SimpleTextAttributes.REGULAR_ATTRIBUTES);
			myLeftComponent.append(" (" + classResult.packageName + ")", getGrayAttributes(selected));

			appendArtifactInfo(myRightComponent, info, selected);
		}

		private MyClassCellRenderer(Tree tree) {
			super(tree);
		}

	}

}
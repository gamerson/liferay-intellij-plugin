package com.liferay.ide.idea.core;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;

import com.liferay.ide.idea.util.CoreUtil;

public abstract class AbstractWorkspaceProvider implements WorkspaceProvider {

	public AbstractWorkspaceProvider() {
	}

	public AbstractWorkspaceProvider(Project project) {
		this.project = project;
	}

	@Override
	public String getLiferayVersion() {
		String targetPlatformVersion = getTargetPlatformVersion();

		if (!CoreUtil.isNullOrEmpty(targetPlatformVersion)) {
			String[] versionArr = targetPlatformVersion.split("\\.");

			return versionArr[0] + "." + versionArr[1];
		}

		return null;
	}

	public VirtualFile getWorkspaceVirtualFile() {
		if (project == null) {
			return null;
		}

		String projectBasePath = project.getBasePath();

		if (projectBasePath == null) {
			return null;
		}

		LocalFileSystem fileSystem = LocalFileSystem.getInstance();

		return fileSystem.findFileByPath(projectBasePath);
	}

	protected Project project;

}
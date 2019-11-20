package com.liferay.ide.idea.ui.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.liferay.ide.idea.util.LiferayWorkspaceSupport;
import com.liferay.ide.idea.util.ProjectConfigurationUtil;
import icons.LiferayIcons;

public class InitDockerBundleAction extends AbstractLiferayGradleTaskAction implements LiferayWorkspaceSupport {

    public InitDockerBundleAction() {
        super("InitDockerBundle", "Run init docker Bundle task", LiferayIcons.LIFERAY_ICON, "createDockerContainer");
    }

    @Override
    protected void handleProcessTerminated(Project project) {
        super.handleProcessTerminated(project);

        String homeDir = getHomeDir(project.getBasePath());

        ProjectConfigurationUtil.configExcludedFolder(project, homeDir);
    }

    @Override
    protected void afterTask(VirtualFile projectDir) {
    }
}

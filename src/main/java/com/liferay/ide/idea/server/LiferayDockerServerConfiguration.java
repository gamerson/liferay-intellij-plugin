package com.liferay.ide.idea.server;

import com.intellij.diagnostic.logging.LogConfigurationPanel;
import com.intellij.execution.CommonProgramRunConfigurationParameters;
import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.JavaRunConfigurationExtensionManager;
import com.intellij.execution.configuration.EnvironmentVariablesComponent;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.JavaRunConfigurationModule;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.configurations.SearchScopeProvidingRunProfile;
import com.intellij.execution.util.ProgramParametersUtil;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.options.SettingsEditorGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopes;
import com.intellij.util.xmlb.SkipDefaultsSerializationFilter;
import com.intellij.util.xmlb.XmlSerializer;
import com.intellij.util.xmlb.XmlSerializerUtil;
import java.util.LinkedHashMap;
import java.util.Map;
import org.assertj.core.util.Lists;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LiferayDockerServerConfiguration extends ExternalSystemRunConfiguration implements CommonProgramRunConfigurationParameters, SearchScopeProvidingRunProfile{


	public LiferayDockerServerConfiguration(Project project, ConfigurationFactory factory, String name) {
            super(ProjectSystemId.IDE, project, factory, name);

            _javaRunConfigurationModule = new JavaRunConfigurationModule(project, true);
        }

        @Override
        public void checkConfiguration() throws RuntimeConfigurationException {
            ProgramParametersUtil.checkWorkingDirectoryExist(this, getProject(), null);

            JavaRunConfigurationExtensionManager.checkConfigurationIsValid(this);
        }

        @Override
        public ExternalSystemRunConfiguration clone() {
            LiferayDockerServerConfiguration clone = (LiferayDockerServerConfiguration)super.clone();

            clone.setConfig(XmlSerializerUtil.createCopy(_liferayDockerServerConfig));

            JavaRunConfigurationModule configurationModule = new JavaRunConfigurationModule(getProject(), true);

            configurationModule.setModule(_javaRunConfigurationModule.getModule());

            clone.setConfigurationModule(configurationModule);

            clone.setEnvs(new LinkedHashMap<>(clone.getEnvs()));

            return clone;
        }

        public String getBundleType() {
            return _liferayDockerServerConfig.buildType;
        }

        @NotNull
        @Override
        public SettingsEditor<ExternalSystemRunConfiguration> getConfigurationEditor() {
            final SettingsEditor<ExternalSystemRunConfiguration> editor = super.getConfigurationEditor();
            if (editor instanceof SettingsEditorGroup) {
                SettingsEditorGroup<LiferayDockerServerConfiguration> group = (SettingsEditorGroup)editor;;

                String title = ExecutionBundle.message("run.configuration.configuration.tab.title");

                group.addEditor(title, new LiferayDockerServerConfigurable(getProject()));

                JavaRunConfigurationExtensionManager javaRunConfigurationExtensionManager =
                        JavaRunConfigurationExtensionManager.getInstance();

                javaRunConfigurationExtensionManager.appendEditors(this, group);

                group.addEditor(ExecutionBundle.message("logs.tab.title"), new LogConfigurationPanel<>());
            }
            return editor;
        }

        @NotNull
        @Override
        public Map<String, String> getEnvs() {
            return _envs;
        }

        public Module getModule() {
            return _javaRunConfigurationModule.getModule();
        }

        @NotNull
        public Module[] getModules() {
            Module module = _javaRunConfigurationModule.getModule();

            if (module != null) {
                return new Module[] {module};
            }

            return Module.EMPTY_ARRAY;
        }

        @Nullable
        @Override
        public String getProgramParameters() {
            return null;
        }

        @Nullable
        @Override
        public GlobalSearchScope getSearchScope() {
            return GlobalSearchScopes.executionScope(Lists.newArrayList(getModules()));
        }

        @Nullable
        @Override
        public String getWorkingDirectory() {
            return null;
        }

        @Override
        public boolean isPassParentEnvs() {
            return _liferayDockerServerConfig.passParentEnvironments;
        }

        @Override
        public void onNewConfigurationCreated() {
            super.onNewConfigurationCreated();

            if (StringUtil.isEmpty(getWorkingDirectory())) {
                String baseDir = FileUtil.toSystemIndependentName(StringUtil.notNullize(getProject().getBasePath()));

                setWorkingDirectory(baseDir);
            }
        }

        @Override
        public void readExternal(Element element) throws InvalidDataException {
            super.readExternal(element);

            JavaRunConfigurationExtensionManager javaRunConfigurationExtensionManager =
                    JavaRunConfigurationExtensionManager.getInstance();

            javaRunConfigurationExtensionManager.readExternal(this, element);

            XmlSerializer.deserializeInto(_liferayDockerServerConfig, element);
            EnvironmentVariablesComponent.readExternal(element, getEnvs());

            _javaRunConfigurationModule.readExternal(element);
        }

        public void setBundleType(String bundleType) {
            _liferayDockerServerConfig.buildType = bundleType;
        }

        public void setConfig(LiferayDockerServerConfig config) {
            _liferayDockerServerConfig = config;
        }

        public void setConfigurationModule(JavaRunConfigurationModule configurationModule) {
            _javaRunConfigurationModule = configurationModule;
        }

        @Override
        public void setEnvs(@NotNull Map<String, String> envs) {
            _envs.clear();
            _envs.putAll(envs);
        }

        public void setModule(Module module) {
            _javaRunConfigurationModule.setModule(module);
        }

        @Override
        public void setPassParentEnvs(boolean passParentEnvs) {
            _liferayDockerServerConfig.passParentEnvironments = passParentEnvs;
        }

        @Override
        public void setProgramParameters(@Nullable String value) {
        }

        @Override
        public void setWorkingDirectory(@Nullable String value) {
        }

        @Override
        public void writeExternal(Element element) throws WriteExternalException {
            super.writeExternal(element);

            JavaRunConfigurationExtensionManager javaRunConfigurationExtensionManager =
                    JavaRunConfigurationExtensionManager.getInstance();

            javaRunConfigurationExtensionManager.writeExternal(this, element);

            XmlSerializer.serializeInto(_liferayDockerServerConfig, element, new SkipDefaultsSerializationFilter());
            EnvironmentVariablesComponent.writeExternal(element, getEnvs());

            if (_javaRunConfigurationModule.getModule() != null) {
                _javaRunConfigurationModule.writeExternal(element);
            }
        }

        private Map<String, String> _envs = new LinkedHashMap<>();
        private JavaRunConfigurationModule _javaRunConfigurationModule;
        private LiferayDockerServerConfig _liferayDockerServerConfig = new LiferayDockerServerConfig();


    private static class LiferayDockerServerConfig {

        public String dockerImageId = "";
        public String dockerImageRepo = "";
        public String dockerImageTag = "";
        public String dockerContainerHealthCheckUrl = "";
        public String dockerContainerId = "";
        public String dockerContainerName = "";
        public String buildType;
        public boolean passParentEnvironments = true;

    }

}

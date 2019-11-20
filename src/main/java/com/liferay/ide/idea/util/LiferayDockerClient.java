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

package com.liferay.ide.idea.util;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.netty.NettyDockerCmdExecFactory;

import com.intellij.util.net.NetUtils;

import java.net.URI;

import org.apache.commons.lang.SystemUtils;

/**
 * @author Simon Jiang
 */
public class LiferayDockerClient {

	public static DockerClient getDockerClient() throws Exception {
		DefaultDockerClientConfig.Builder createDefaultConfigBuilder =
			DefaultDockerClientConfig.createDefaultConfigBuilder();

		createDefaultConfigBuilder.withRegistryUrl("https://registry.hub.docker.com/v2/repositories/liferay/portal");

		if (SystemUtils.IS_OS_WINDOWS) {
			createDefaultConfigBuilder.withDockerHost("tcp://localhost:2375");
		}

		DefaultDockerClientConfig config = createDefaultConfigBuilder.build();

		if (SystemUtils.IS_OS_WINDOWS) {
			URI dockerHostUri = config.getDockerHost();

			boolean connectToSocket = NetUtils.canConnectToRemoteSocket(
				dockerHostUri.getHost(), dockerHostUri.getPort());

			if (!connectToSocket) {
				throw new Exception("Can not connect to docker api. Please Check Configuration.");
			}
		}

		NettyDockerCmdExecFactory cmdFactory = new NettyDockerCmdExecFactory();

		DockerClientBuilder dockerClientBuilder = DockerClientBuilder.getInstance(config);

		dockerClientBuilder.withDockerCmdExecFactory(cmdFactory);

		return dockerClientBuilder.build();
	}

}
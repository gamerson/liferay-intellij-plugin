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

package com.liferay.ide.idea.language.lsp;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.wso2.lsp4intellij.client.connection.StreamConnectionProvider;
import org.wso2.lsp4intellij.client.languageserver.serverdefinition.CommandServerDefinition;

/**
 * @author Dominik Marks
 */
public class SocketCommandServerDefinition extends CommandServerDefinition {

	public SocketCommandServerDefinition(String ext, Map<String, String> languageIds, String[] command, int port) {
		this.ext = ext;
		this.languageIds = languageIds;
		this.command = command;
		typ = "socketCommand";
		presentableTyp = "Socket command";

		_port = port;
	}

	/**
	 * Creates new instance.
	 *
	 * @param ext The extension
	 * @param command The command to run
	 * @param port The port to listen to
	 */
	public SocketCommandServerDefinition(String ext, String[] command, int port) {
		this(ext, Collections.emptyMap(), command, port);
	}

	@Override
	public StreamConnectionProvider createConnectionProvider(String workingDir) {
		return new ProcessSocketStreamConnectionProvider(Arrays.asList(command), workingDir, _port);
	}

	private int _port;

}
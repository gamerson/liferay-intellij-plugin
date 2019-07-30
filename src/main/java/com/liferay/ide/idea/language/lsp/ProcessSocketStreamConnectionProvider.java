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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.ServerSocket;
import java.net.Socket;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import org.wso2.lsp4intellij.client.connection.ProcessStreamConnectionProvider;

/**
 * @author Dominik Marks
 */
public class ProcessSocketStreamConnectionProvider extends ProcessStreamConnectionProvider {

	public ProcessSocketStreamConnectionProvider(List<String> commands, String workingDir, int port) {
		super(commands, workingDir);

		_port = port;
	}

	@Nullable
	@Override
	public InputStream getInputStream() {
		return _inputStream;
	}

	@Nullable
	@Override
	public OutputStream getOutputStream() {
		return _outputStream;
	}

	@Override
	public void start() throws IOException {
		final ServerSocket serverSocket = new ServerSocket(_port);

		Thread socketThread = new Thread(
			() -> {
				try {
					_socket = serverSocket.accept();
				}
				catch (IOException ioe) {
					ioe.printStackTrace();
				}
				finally {
					try {
						serverSocket.close();
					}
					catch (IOException ioe) {
						ioe.printStackTrace();
					}
				}
			});

		socketThread.start();

		super.start();

		try {
			socketThread.join();
		}
		catch (InterruptedException ie) {
			ie.printStackTrace();
		}

		if (_socket == null) {
			throw new IOException("Unable to make socket connection");
		}

		_inputStream = _socket.getInputStream();
		_outputStream = _socket.getOutputStream();
	}

	@Override
	public void stop() {
		super.stop();

		if (_socket != null) {
			try {
				_socket.close();
			}
			catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}

	private InputStream _inputStream;
	private OutputStream _outputStream;
	private int _port;
	private Socket _socket;

}
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

import java.io.IOException;

import java.net.ServerSocket;
import java.net.SocketException;

import java.util.Random;

/**
 * @author Terry Jia
 */
public interface SocketSupport {

	public default int findUnusedPort(int low, int high) {
		if (high < low) {
			return -1;
		}

		for (int i = 0; i < 10; ++i) {
			int port = getRandomPort(low, high);

			if (!isPortInUse(port)) {
				return port;
			}
		}

		return -1;
	}

	public default int getRandomPort(int low, int high) {
		return rand.nextInt(high - low) + low;
	}

	public default boolean isPortInUse(int port) {
		ServerSocket serverSocket = null;

		try {
			serverSocket = new ServerSocket(port, 0, null);
		}
		catch (SocketException se) {
			return true;
		}
		catch (IOException ioe) {
			return true;
		}
		finally {
			if (serverSocket != null) {
				try {
					serverSocket.close();
				}
				catch (Exception e) {
				}
			}
		}

		return false;
	}

	public final Random rand = new Random(System.currentTimeMillis());

}
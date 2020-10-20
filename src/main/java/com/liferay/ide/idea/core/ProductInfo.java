package com.liferay.ide.idea.core;

import com.google.gson.annotations.SerializedName;

public class ProductInfo {

	public String getAppServerTomcatVersion() {
		return _appServerTomcatVersion;
	}

	public String getBundleUrl() {
		return _bundleUrl;
	}

	public String getLiferayDockerImage() {
		return _liferayDockerImage;
	}

	public String getLiferayProductVersion() {
		return _liferayProductVersion;
	}

	public String getReleaseDate() {
		return _releaseDate;
	}

	public String getTargetPlatformVersion() {
		return _targetPlatformVersion;
	}

	public boolean isInitialVersion() {
		return _initialVersion;
	}

	@SerializedName("appServerTomcatVersion")
	private String _appServerTomcatVersion;

	@SerializedName("bundleUrl")
	private String _bundleUrl;

	@SerializedName("initialVersion")
	private boolean _initialVersion;

	@SerializedName("liferayDockerImage")
	private String _liferayDockerImage;

	@SerializedName("liferayProductVersion")
	private String _liferayProductVersion;

	@SerializedName("releaseDate")
	private String _releaseDate;

	@SerializedName("targetPlatformVersion")
	private String _targetPlatformVersion;

}
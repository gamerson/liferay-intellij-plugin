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

package com.liferay.ide.idea.bnd.parser;

import com.intellij.codeInsight.daemon.JavaErrorBundle;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiReference;

import com.liferay.ide.idea.bnd.psi.BndHeader;
import com.liferay.ide.idea.bnd.psi.BndHeaderValue;
import com.liferay.ide.idea.bnd.psi.BndHeaderValuePart;
import com.liferay.ide.idea.bnd.psi.Clause;
import com.liferay.ide.idea.bnd.psi.util.BndPsiUtil;
import com.liferay.ide.idea.util.LiferayAnnotationUtil;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Dominik Marks
 */
public class BasePackageParser extends BndHeaderParser {

	public static final BasePackageParser INSTANCE = new BasePackageParser();

	@Override
	public boolean annotate(@NotNull BndHeader bndHeader, @NotNull AnnotationHolder annotationHolder) {
		boolean annotated = false;

		for (BndHeaderValue bndHeaderValue : bndHeader.getBndHeaderValues()) {
			if (bndHeaderValue instanceof Clause) {
				Clause clause = (Clause)bndHeaderValue;

				BndHeaderValuePart bndHeaderValuePart = clause.getValue();

				if (bndHeaderValuePart != null) {
					String packageName = bndHeaderValuePart.getUnwrappedText();

					packageName = StringUtil.trimEnd(packageName, ".*");

					if (StringUtil.isEmptyOrSpaces(packageName)) {
						LiferayAnnotationUtil.createAnnotation(
							annotationHolder, HighlightSeverity.ERROR, "Invalid reference",
							bndHeaderValuePart.getHighlightingRange());

						annotated = true;

						continue;
					}

					packageName = packageName.trim();

					if (packageName.charAt(0) == '!') {
						packageName = packageName.substring(1);
					}

					if (!packageName.equals("*")) {
						PsiDirectory[] psiDirectories = BndPsiUtil.resolvePackage(bndHeader, packageName);

						if (psiDirectories.length == 0) {
							LiferayAnnotationUtil.createAnnotation(
								annotationHolder, HighlightSeverity.ERROR,
								JavaErrorBundle.message("cannot.resolve.package", packageName),
								bndHeaderValuePart.getHighlightingRange());
							annotated = true;
						}
					}
				}
			}
		}

		return annotated;
	}

	@Nullable
	@Override
	public Object getConvertedValue(@NotNull BndHeader bndHeader) {
		List<BndHeaderValue> bndHeaderValues = bndHeader.getBndHeaderValues();

		if (!bndHeaderValues.isEmpty()) {
			List<String> packages = new ArrayList<>();

			for (BndHeaderValue bndHeaderValue : bndHeaderValues) {
				if (bndHeaderValue instanceof Clause) {
					Clause clause = (Clause)bndHeaderValue;

					BndHeaderValuePart bndHeaderValuePart = clause.getValue();

					if (bndHeaderValuePart != null) {
						String packageName = bndHeaderValuePart.getText();

						packageName = packageName.replaceAll("\\s+", "");

						packageName = packageName.trim();

						if (packageName.charAt(0) == '!') {
							packageName = packageName.substring(1);
						}

						packages.add(packageName);
					}
				}
			}

			return packages;
		}

		return null;
	}

	@NotNull
	@Override
	public PsiReference[] getReferences(@NotNull BndHeaderValuePart bndHeaderValuePart) {
		if (bndHeaderValuePart.getParent() instanceof Clause) {
			return BndPsiUtil.getPackageReferences(bndHeaderValuePart);
		}

		return PsiReference.EMPTY_ARRAY;
	}

}
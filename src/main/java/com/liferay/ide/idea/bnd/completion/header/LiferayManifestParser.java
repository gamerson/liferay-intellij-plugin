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

package com.liferay.ide.idea.bnd.completion.header;

import static com.intellij.util.ObjectUtils.notNull;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.lang.manifest.ManifestBundle;
import org.jetbrains.lang.manifest.header.HeaderParser;
import org.jetbrains.lang.manifest.header.HeaderParserRepository;
import org.jetbrains.lang.manifest.header.impl.StandardHeaderParser;
import org.jetbrains.lang.manifest.psi.ManifestElementType;
import org.jetbrains.lang.manifest.psi.ManifestTokenType;

/**
 * @author Seiphon Wang
 */
public class LiferayManifestParser implements PsiParser {

	public static final TokenSet HEADER_END_TOKENS = TokenSet.create(
		ManifestTokenType.SECTION_END, ManifestTokenType.HEADER_NAME);

	public LiferayManifestParser() {
		_myRepository = ServiceManager.getService(HeaderParserRepository.class);
	}

	@NotNull
	@Override
	public ASTNode parse(@NotNull IElementType root, @NotNull PsiBuilder builder) {
		builder.setDebugMode(
			ApplicationManager.getApplication(
			).isUnitTestMode());

		PsiBuilder.Marker rootMarker = builder.mark();

		while (!builder.eof()) {
			_parseSection(builder);
		}

		rootMarker.done(root);

		return builder.getTreeBuilt();
	}

	private static void _consumeHeaderValue(PsiBuilder builder) {
		while (!builder.eof() && !HEADER_END_TOKENS.contains(builder.getTokenType())) {
			builder.advanceLexer();
		}
	}

	private void _parseHeader(PsiBuilder builder) {
		PsiBuilder.Marker header = builder.mark();

		String headerName = builder.getTokenText();

        assert headerName != null : "[" + builder.getOriginalText() + "]@" + builder.getCurrentOffset();
		builder.advanceLexer();

		if (builder.getTokenType() == ManifestTokenType.COLON) {
			builder.advanceLexer();

			HeaderParser headerParser = notNull(
				_myRepository.getHeaderParser(headerName), StandardHeaderParser.INSTANCE);

			headerParser.parse(builder);
		}
		else {
			PsiBuilder.Marker marker = builder.mark();
			_consumeHeaderValue(builder);
			marker.error(ManifestBundle.message("manifest.colon.expected"));
		}

		header.done(ManifestElementType.HEADER);
	}

	private void _parseSection(PsiBuilder builder) {
		PsiBuilder.Marker section = builder.mark();

		while (!builder.eof()) {
			IElementType tokenType = builder.getTokenType();

			if (tokenType == ManifestTokenType.HEADER_NAME) {
				_parseHeader(builder);
			}
			else if (tokenType == ManifestTokenType.SECTION_END) {
				builder.advanceLexer();

				break;
			}
			else {
				PsiBuilder.Marker marker = builder.mark();
				_consumeHeaderValue(builder);
				marker.error(ManifestBundle.message("manifest.header.expected"));
			}
		}

		section.done(ManifestElementType.SECTION);
	}

	private final HeaderParserRepository _myRepository;

}
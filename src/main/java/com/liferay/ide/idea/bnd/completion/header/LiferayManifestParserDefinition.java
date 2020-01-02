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

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiUtilCore;

import com.liferay.ide.idea.bnd.parser.LiferayManifestLexer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.lang.manifest.psi.Header;
import org.jetbrains.lang.manifest.psi.ManifestElementType;
import org.jetbrains.lang.manifest.psi.impl.ManifestFileImpl;

/**
 * @author Seiphon Wang
 */
public class LiferayManifestParserDefinition implements ParserDefinition {

	@NotNull
	@Override
	public PsiElement createElement(ASTNode node) {
		IElementType type = node.getElementType();

		if (type instanceof ManifestElementType) {
			ManifestElementType manifestElementType = (ManifestElementType)type;

			return manifestElementType.createPsi(node);
		}

		return PsiUtilCore.NULL_PSI_ELEMENT;
	}

	@Override
	public PsiFile createFile(FileViewProvider viewProvider) {
		return new ManifestFileImpl(viewProvider);
	}

	@NotNull
	@Override
	public Lexer createLexer(Project project) {
		return new LiferayManifestLexer();
	}

	@Override
	public PsiParser createParser(Project project) {
		return new LiferayManifestParser();
	}

	@NotNull
	@Override
	public TokenSet getCommentTokens() {
		return TokenSet.EMPTY;
	}

	@Override
	public IFileElementType getFileNodeType() {
		return ManifestElementType.FILE;
	}

	@NotNull
	@Override
	public TokenSet getStringLiteralElements() {
		return TokenSet.EMPTY;
	}

	@NotNull
	@Override
	public TokenSet getWhitespaceTokens() {
		return TokenSet.EMPTY;
	}

	@Override
	public SpaceRequirements spaceExistenceTypeBetweenTokens(ASTNode left, ASTNode right) {
		if ((left.getPsi() instanceof Header) || (right.getPsi() instanceof Header)) {
			return SpaceRequirements.MUST_LINE_BREAK;
		}

		return SpaceRequirements.MUST_NOT;
	}

}
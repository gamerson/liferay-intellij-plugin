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

import com.intellij.lexer.LexerBase;
import com.intellij.psi.tree.IElementType;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.lang.manifest.psi.ManifestTokenType;

/**
 * @author Seiphon Wang
 */
public class LiferayManifestLexer extends LexerBase {

	@Override
	public void advance() {
		_myTokenStart = _myTokenEnd;
		_parseNextToken();
	}

	@Override
	public int getBufferEnd() {
		return _myEndOffset;
	}

	@NotNull
	@Override
	public CharSequence getBufferSequence() {
		return _myBuffer;
	}

	@Override
	public int getState() {
		if (_myDefaultState) {
			return 0;
		}

		return 1;
	}

	@Override
	public int getTokenEnd() {
		return _myTokenEnd;
	}

	@Override
	public int getTokenStart() {
		return _myTokenStart;
	}

	@Nullable
	@Override
	public IElementType getTokenType() {
		return _myTokenType;
	}

	@Override
	public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
		_myBuffer = buffer;
		_myEndOffset = endOffset;
		_myTokenStart = _myTokenEnd = startOffset;
		_myDefaultState = initialState == 0;

		_parseNextToken();
	}

	private void _parseNextToken() {
		if (_myTokenStart >= _myEndOffset) {
			_myTokenType = null;
			_myTokenEnd = _myTokenStart;

			return;
		}

		boolean atLineStart = false;

		if ((_myTokenStart == 0) || (_myBuffer.charAt(_myTokenStart - 1) == '\n')) {
			atLineStart = true;
		}

		char c = _myBuffer.charAt(_myTokenStart);

		if (atLineStart) {
			_myDefaultState = true;

			if (c == ' ') {
				_myTokenType = ManifestTokenType.SIGNIFICANT_SPACE;
				_myTokenEnd = _myTokenStart + 1;
			}
			else if (c == '\n') {
				_myTokenType = ManifestTokenType.SECTION_END;
				_myTokenEnd = _myTokenStart + 1;
			}
			else if (c == '\t') {
				_myTokenType = ManifestTokenType.HEADER_VALUE_PART;
				_myTokenEnd = _myTokenStart + 1;
			}
			else {
				int headerEnd = _myTokenStart + 1;

				while (headerEnd < _myEndOffset) {
					c = _myBuffer.charAt(headerEnd);

					if (c == ':') {
						_myDefaultState = false;

						break;
					}
					else if (c == '\n') {
						break;
					}

					++headerEnd;
				}

				_myTokenType = ManifestTokenType.HEADER_NAME;
				_myTokenEnd = headerEnd;
			}
		}
		else if (!_myDefaultState && (c == ':')) {
			_myTokenType = ManifestTokenType.COLON;
			_myTokenEnd = _myTokenStart + 1;
		}
		else if (!_myDefaultState && (c == ' ')) {
			_myTokenType = ManifestTokenType.SIGNIFICANT_SPACE;
			_myTokenEnd = _myTokenStart + 1;
			_myDefaultState = true;
		}
		else {
			_myDefaultState = true;
			IElementType special;

			if (c == '\n') {
				_myTokenType = ManifestTokenType.NEWLINE;
				_myTokenEnd = _myTokenStart + 1;
			}
			else if ((special = _specialCharactersTokenMapping.get(c)) != null) {
				_myTokenType = special;
				_myTokenEnd = _myTokenStart + 1;
			}
			else {
				int valueEnd = _myTokenStart + 1;

				while (valueEnd < _myEndOffset) {
					c = _myBuffer.charAt(valueEnd);

					if ((c == '\n') || _specialCharactersTokenMapping.containsKey(c)) {
						break;
					}

					++valueEnd;
				}

				_myTokenType = ManifestTokenType.HEADER_VALUE_PART;
				_myTokenEnd = valueEnd;
			}
		}
	}

	private static final Map<Character, IElementType> _specialCharactersTokenMapping =
		new HashMap<Character, IElementType>() {
			{
				put('(', ManifestTokenType.OPENING_PARENTHESIS_TOKEN);
				put(')', ManifestTokenType.CLOSING_PARENTHESIS_TOKEN);
				put(',', ManifestTokenType.COMMA);
				put(':', ManifestTokenType.COLON);
				put(';', ManifestTokenType.SEMICOLON);
				put('=', ManifestTokenType.EQUALS);
				put('[', ManifestTokenType.OPENING_BRACKET_TOKEN);
				put('\"', ManifestTokenType.QUOTE);
				put('\t', ManifestTokenType.HEADER_VALUE_PART);
				put(']', ManifestTokenType.CLOSING_BRACKET_TOKEN);
			}
		};

	private CharSequence _myBuffer;
	private boolean _myDefaultState;
	private int _myEndOffset;
	private int _myTokenEnd;
	private int _myTokenStart;
	private IElementType _myTokenType;

}
/*
 * This file is part of Commons.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * Copyright (C) hdsdi3g for hd3g.tv 2019
 *
 */
package tv.hd3g.commons.codepolicyvalidation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CheckPolicyOkTest {

	private static CheckPolicy checkPolicy;

	@BeforeAll
	static void globalInit() {
		CheckPolicy.globalInit(
		        "src/test/java/tv/hd3g/commons/codepolicyvalidation/ok",
		        "src/main/java");
		checkPolicy = new CheckPolicy();
	}

	@Test
	void testNoIllegalArgumentExceptionWOConstructor() {
		assertTrue(checkPolicy.noIllegalArgumentExceptionWOConstructor());
	}

	@Test
	void testXToOneMustToSetOptional() {
		assertTrue(checkPolicy.xToOneMustToSetOptional());
	}

	@Test
	void testXToManyMustNotUseEAGER() {
		assertTrue(checkPolicy.xToManyMustNotUseEAGER());
	}

	@Test
	void testOkPrintStackTrace_with_PrintStream_Or_PrintWriter() {
		assertTrue(checkPolicy.printStackTrace());
	}

}

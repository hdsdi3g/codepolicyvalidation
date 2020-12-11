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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

class CheckPolicyKoTest {

	private static CheckPolicy checkPolicy;

	@BeforeAll
	static void globalInit() {
		CheckPolicy.globalInit("src/test/java/tv/hd3g/commons/codepolicyvalidation/ko");
		checkPolicy = new CheckPolicy();
	}

	@Test
	void testNoIllegalArgumentExceptionWOConstructor() {
		Assertions.assertThrows(AssertionFailedError.class,
		        () -> checkPolicy.noIllegalArgumentExceptionWOConstructor());
	}

	@Test
	void testNoSysOutSysErr() {
		Assertions.assertThrows(AssertionFailedError.class,
		        () -> checkPolicy.noSysOutSysErr());
	}

	@Test
	void testNoPrintStackTrace() {
		Assertions.assertThrows(AssertionFailedError.class,
		        () -> checkPolicy.noSimplePrintStackTrace());
	}

	@Test
	void testXToOneMustToSetOptional() {
		Assertions.assertThrows(AssertionFailedError.class,
		        () -> checkPolicy.xToOneMustToSetOptional());
	}

	@Test
	void testXToManyMustNotUseEAGER() {
		Assertions.assertThrows(AssertionFailedError.class,
		        () -> checkPolicy.xToManyMustNotUseEAGER());
	}

	@Test
	void testNoSuppressWarnings() {
		Assertions.assertThrows(AssertionFailedError.class,
		        () -> checkPolicy.noSuppressWarnings());
	}

	@Test
	void testNoRuntimeException() {
		Assertions.assertThrows(AssertionFailedError.class,
		        () -> checkPolicy.noRuntimeException());
	}

	@Test
	void testNoNullPointerException() {
		Assertions.assertThrows(AssertionFailedError.class,
		        () -> checkPolicy.noNullPointerException());
	}

	@Test
	void testNotOldJunitAssert() {
		Assertions.assertThrows(BadImportClass.class,
		        () -> checkPolicy.notOldJunitAssert());
	}

	@Test
	void testNotOldJunitRunner() {
		Assertions.assertThrows(BadImportClass.class,
		        () -> checkPolicy.notOldJunitRunner());
	}

	@Test
	void testNotOldJunit() {
		Assertions.assertThrows(BadImportClass.class,
		        () -> checkPolicy.notOldJunit());
	}

	@Test
	void testNotJunitFramework() {
		Assertions.assertThrows(BadImportClass.class,
		        () -> checkPolicy.notJunitFramework());
	}

	@Test
	void testNotSQLDate() {
		Assertions.assertThrows(BadImportClass.class,
		        () -> checkPolicy.notSQLDate());
	}

	@Test
	void testNotCommonsLang2_use3() {
		Assertions.assertThrows(BadImportClass.class,
		        () -> checkPolicy.notCommonsLang2Use3());
	}

	@Test
	void testNotCommonsCollection3_use4() {
		Assertions.assertThrows(BadImportClass.class,
		        () -> checkPolicy.notCommonsCollection3Use4());
	}

	@Test
	void classExtendsCheckPolicyNamesMustEndsByTest() {
		Assertions.assertThrows(AssertionError.class,
		        () -> checkPolicy.classExtendsCheckPolicyNamesMustEndsByTest());
	}

}

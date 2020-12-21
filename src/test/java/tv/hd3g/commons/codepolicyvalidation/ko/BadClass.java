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
package tv.hd3g.commons.codepolicyvalidation.ko;

import static javax.persistence.FetchType.EAGER;
import static org.junit.Assert.assertEquals;

import java.sql.Date;
import java.util.Optional;

import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.apache.commons.collections.ThisClassIsFromAnOlderPackage;
import org.apache.commons.lang.ThisClassIsFromAn2OlderPackage;
import org.junit.Test;
import org.junit.runner.Result;

import junit.framework.TestFailure;

/**
 * NEVER USE THIS CLASS
 * Only set for trigger policy tests
 */
class BadClass {

	@OneToMany(fetch = EAGER)
	private String var1;

	@OneToOne
	private String var2;

	@Test // NOSONAR
	@SuppressWarnings("unused")
	void a() {
		assertEquals(0, 0);// NOSONAR
		new Result();
		new ThisClassIsFromAnOlderPackage();
		final Date a;
		final TestFailure f;
		new ThisClassIsFromAn2OlderPackage();
		System.out.println();
		System.err.println();
		throw new IllegalArgumentException();
	}

	void b() {
		try {
		} catch (final NullPointerException e) {
			e.printStackTrace();
			throw e;
		}
	}

	void c() {
		throw new RuntimeException();
	}

	void d() {
		Optional.of("");
	}

}

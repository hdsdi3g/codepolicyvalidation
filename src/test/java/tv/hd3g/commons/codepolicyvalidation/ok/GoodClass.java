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
package tv.hd3g.commons.codepolicyvalidation.ok;

import static javax.persistence.FetchType.LAZY;

import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

/**
 * NEVER USE THIS CLASS
 * Only set for trigger policy tests
 */
class GoodClass {

	@OneToMany(fetch = LAZY)
	private String var1;

	@OneToOne(optional = false)
	private String var2;

	void a() {
		throw new IllegalArgumentException("This is an argument");
	}

}

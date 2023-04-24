/*
 * This file is part of codepolicyvalidation.
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
 * Copyright (C) hdsdi3g for hd3g.tv 2023
 *
 */
package tv.hd3g.commons.codepolicyvalidation;

import java.util.function.Predicate;

import spoon.reflect.declaration.CtType;
import spoon.support.reflect.declaration.CtClassImpl;
import spoon.support.reflect.declaration.CtInterfaceImpl;

public enum CtTypeCat {
	CLASS(getClassFilter(false)),
	ABSTRACT(getClassFilter(true)),
	INTERFACE(t -> CtInterfaceImpl.class.equals(t.getClass()));

	final Predicate<CtType<?>> filter;

	CtTypeCat(final Predicate<CtType<?>> filter) {
		this.filter = filter;
	}

	static final Predicate<CtType<?>> getClassFilter(final boolean mustAbstract) {// NOSONAR S1452
		return t -> {
			if (CtClassImpl.class.equals(t.getClass()) == false) {
				return false;
			}
			final var c = (CtClassImpl<?>) t;
			return c.isAbstract() == mustAbstract
				   && c.isAnonymous() == false
				   && c.isClass() == true
				   && c.isLocalType() == false
				   && c.isPrivate() == false
				   && c.isStatic() == false
				   && c.isEnum() == false
				   && c.isImplicit() == false;
		};
	}

}

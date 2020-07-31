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
 * Copyright (C) hdsdi3g for hd3g.tv 2020
 *
 */
package tv.hd3g.commons.codepolicyvalidation;

import java.util.Collection;
import java.util.stream.Collectors;

public class BadClassAnnotation extends AssertionError {

	public BadClassAnnotation(final String className, final String currentPackage, final String expectedAnnotation) {
		super("Class \"" + className + "\" located on package \"" + currentPackage + "\" should have \"@"
		      + expectedAnnotation + "\"");
	}

	public BadClassAnnotation(final Collection<BadClassAnnotation> bclList) {
		super(bclList.stream()
		        .map(Throwable::getMessage)
		        .collect(Collectors.joining(". ", "", ".")));
	}

}

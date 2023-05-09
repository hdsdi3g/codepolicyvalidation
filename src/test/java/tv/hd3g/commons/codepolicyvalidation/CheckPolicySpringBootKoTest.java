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

class CheckPolicySpringBootKoTest {

	private static CheckPolicy checkPolicy;

	@BeforeAll
	static void globalInit() {
		Policies.globalInit("src/test/java/tv/hd3g/commons/codepolicyvalidation/springboot/ko");
		checkPolicy = new CheckPolicy();
	}

	@Test
	void springBootEntitiesInEntityPackage() {
		Assertions.assertThrows(BadClassLocation.class,
				() -> checkPolicy.springBootEntitiesInEntityPackage());
	}

	@Test
	void springBootControllersInControllerPackage() {
		Assertions.assertThrows(BadClassLocation.class,
				() -> checkPolicy.springBootControllersInControllerPackage());
	}

	@Test
	void springBootRepositoriesInRepositoryPackage() {
		Assertions.assertThrows(BadClassLocation.class,
				() -> checkPolicy.springBootRepositoriesInRepositoryPackage());
	}

	@Test
	void springBootServicesInServicePackage() {
		Assertions.assertThrows(BadClassLocation.class,
				() -> checkPolicy.springBootServicesInServicePackage());
	}

	@Test
	void springBootNotControllerInControllerPackage() {
		Assertions.assertThrows(BadClassAnnotation.class,
				() -> checkPolicy.springBootNotControllerInControllerPackage());
	}

	@Test
	void springBootNotEntityInEntityPackage() {
		Assertions.assertThrows(BadClassAnnotation.class,
				() -> checkPolicy.springBootNotEntityInEntityPackage());
	}

	@Test
	void springBootNotRepositoryInRepositoryPackage() {
		Assertions.assertThrows(BadClassAnnotation.class,
				() -> checkPolicy.springBootNotRepositoryInRepositoryPackage());
	}

	@Test
	void springBootNotServiceInServicePackage() {
		Assertions.assertThrows(BadClassAnnotation.class,
				() -> checkPolicy.springBootNotServiceInServicePackage());
	}

	@Test
	void springBootNotClassInControllerPackage() {
		Assertions.assertThrows(AssertionError.class,
				() -> checkPolicy.springBootNotClassInControllerPackage());
	}

	/*@Test
	void springBootNotClassInEntityPackage() {
		Assertions.assertThrows(AssertionError.class,
		        () -> checkPolicy.springBootNotClassInEntityPackage());
	}*/

	@Test
	void springBootNotInterfaceInRepositoryPackage() {
		Assertions.assertThrows(AssertionError.class,
				() -> checkPolicy.springBootNotInterfaceInRepositoryPackage());
	}

	@Test
	void springBootNotClassOrInterfaceInServicePackage() {
		Assertions.assertThrows(AssertionError.class,
				() -> checkPolicy.springBootNotClassOrInterfaceInServicePackage());
	}

	@Test
	void springBootServiceBadName() {
		Assertions.assertThrows(AssertionError.class,
				() -> checkPolicy.springBootServiceBadName());
	}

	@Test
	void springBootServiceDontImplInterface() {
		Assertions.assertThrows(AssertionError.class,
				() -> checkPolicy.springBootServiceDontImplInterface());
	}

	@Test
	void springBootServiceInterfaceNames() {
		Assertions.assertThrows(AssertionError.class,
				() -> checkPolicy.springBootServiceInterfaceNames());
	}

	@Test
	void springBootComponentInComponentPackage() {
		Assertions.assertThrows(AssertionError.class,
				() -> checkPolicy.springBootComponentInComponentPackage());
	}

	@Test
	void springBootNotComponentInComponentPackage() {
		Assertions.assertThrows(AssertionError.class,
				() -> checkPolicy.springBootNotComponentInComponentPackage());
	}

	@Test
	void springBootRESTControllerMethodsMustReturnResponseEntity() {
		Assertions.assertThrows(AssertionError.class,
				() -> checkPolicy.springBootRESTControllerMethodsMustReturnResponseEntity());
	}

}

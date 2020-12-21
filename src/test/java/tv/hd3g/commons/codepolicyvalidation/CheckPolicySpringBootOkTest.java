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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CheckPolicySpringBootOkTest {

	private static CheckPolicy checkPolicy;

	@BeforeAll
	static void globalInit() {
		CheckPolicy.globalInit("src/test/java/tv/hd3g/commons/codepolicyvalidation/springboot/ok");
		checkPolicy = new CheckPolicy();
	}

	@Test
	void springBootEntitiesInEntityPackage() {// NOSONAR S2699
		checkPolicy.springBootEntitiesInEntityPackage();
	}

	@Test
	void springBootControllersInControllerPackage() {// NOSONAR S2699
		checkPolicy.springBootControllersInControllerPackage();
	}

	@Test
	void springBootRepositoriesInRepositoryPackage() {// NOSONAR S2699
		checkPolicy.springBootRepositoriesInRepositoryPackage();
	}

	@Test
	void springBootServicesInServicePackage() {// NOSONAR S2699
		checkPolicy.springBootServicesInServicePackage();
	}

	@Test
	void springBootNotControllerInControllerPackage() {// NOSONAR S2699
		checkPolicy.springBootNotControllerInControllerPackage();
	}

	@Test
	void springBootNotEntityInEntityPackage() {// NOSONAR S2699
		checkPolicy.springBootNotEntityInEntityPackage();
	}

	@Test
	void springBootNotRepositoryInRepositoryPackage() {// NOSONAR S2699
		checkPolicy.springBootNotRepositoryInRepositoryPackage();
	}

	@Test
	void springBootNotServiceInServicePackage() {// NOSONAR S2699
		checkPolicy.springBootNotServiceInServicePackage();
	}

	@Test
	void springBootNotClassInControllerPackage() {// NOSONAR S2699
		checkPolicy.springBootNotClassInControllerPackage();
	}

	@Test
	void springBootNotClassInEntityPackage() {// NOSONAR S2699
		checkPolicy.springBootNotClassInEntityPackage();
	}

	@Test
	void springBootNotInterfaceInRepositoryPackage() {// NOSONAR S2699
		checkPolicy.springBootNotInterfaceInRepositoryPackage();
	}

	@Test
	void springBootNotClassOrInterfaceInServicePackage() {// NOSONAR S2699
		checkPolicy.springBootNotClassOrInterfaceInServicePackage();
	}

	@Test
	void springBootServiceBadName() {// NOSONAR S2699
		checkPolicy.springBootServiceBadName();
	}

	@Test
	void springBootServiceDontImplInterface() {// NOSONAR S2699
		checkPolicy.springBootServiceDontImplInterface();
	}

	@Test
	void springBootServiceInterfaceNames() {// NOSONAR S2699
		checkPolicy.springBootServiceInterfaceNames();
	}

	@Test
	void springBootComponentInComponentPackage() {// NOSONAR S2699
		checkPolicy.springBootComponentInComponentPackage();
	}

	@Test
	void springBootNotComponentInComponentPackage() {// NOSONAR S2699
		checkPolicy.springBootNotComponentInComponentPackage();
	}

}

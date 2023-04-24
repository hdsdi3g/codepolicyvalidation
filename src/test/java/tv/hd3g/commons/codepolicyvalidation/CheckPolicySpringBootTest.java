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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tv.hd3g.commons.codepolicyvalidation.CtTypeCat.CLASS;
import static tv.hd3g.commons.codepolicyvalidation.Policies.typeFactory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import spoon.reflect.declaration.CtType;
import tv.hd3g.commons.codepolicyvalidation.springboot.ok.SpringBootApp;
import tv.hd3g.commons.codepolicyvalidation.springboot.ok.controller.AController;
import tv.hd3g.commons.codepolicyvalidation.springboot.ok.entity.AnEntity;

class CheckPolicySpringBootTest {

	static Policies p;

	@BeforeAll
	static void globalInit() {
		Policies.globalInit("src/test/java/tv/hd3g/commons/codepolicyvalidation/springboot/ok");
		p = new Policies();
	}

	@Test
	void searchByAnnotationinClass() {
		final var list = p.searchByAnnotationInClass(SpringBootApplication.class);
		assertFalse(list.isEmpty());
		assertEquals(typeFactory.get(SpringBootApp.class).getReference(), list.get(0).getReference());
	}

	@Test
	void assertInPackageName() {
		final var itemOk = typeFactory.get(AController.class);
		p.assertInPackageName(itemOk, "controller");

		final var itemKo = typeFactory.get(AnEntity.class);
		Assertions.assertThrows(BadClassLocation.class,
				() -> p.assertInPackageName(itemKo, "controller"));
	}

	@Test
	void assertSpringBootStereotypeInItsPackage() {
		p.assertSpringBootStereotypeInItsPackage(Controller.class.getName(), "controller");
	}

	@Test
	void assertSpringBootStereotypeInItsPackage_reverseCheck() {
		final var annotationName = SpringBootApplication.class.getName();
		Assertions.assertThrows(BadClassLocation.class,
				() -> p.assertSpringBootStereotypeInItsPackage(annotationName, "controller"));
	}

	@Test
	void assertSpringBootStereotypeInItsPackage_WithoutSpring() {
		p.assertSpringBootStereotypeInItsPackage("invalid.package.name", "controller");
	}

	@Test
	void searchClassByPackageName() {
		final var list = p.searchPackagesByPackageName("ok");
		assertEquals(1, list.size());
		assertEquals("tv.hd3g.commons.codepolicyvalidation.springboot.ok", list.get(0).getQualifiedName());
	}

	@Test
	void searchClassesByPackages() {
		final var listP = p.searchPackagesByPackageName("ok");
		final var listC = p.searchClassesByPackages(listP, element -> true);
		assertEquals(3, listC.size());
		assertTrue(listC.stream()
				.map(CtType::getQualifiedName)
				.anyMatch(qn -> qn.equalsIgnoreCase("tv.hd3g.commons.codepolicyvalidation.springboot.ok.SimpleClass")));
	}

	@Test
	void getIsAnnotatedClass() {
		final var predicate = p.getIsAnnotatedClass(Controller.class);
		assertTrue(predicate.test(typeFactory.get(AController.class)));
		assertFalse(predicate.test(typeFactory.get(AnEntity.class)));
	}

	@Test
	void assertClassesByPackageIsAnnotated() {
		assertTrue(p.assertClassesByPackageIsAnnotated("foobar", "this.is.not.found", CLASS));

		final var predicate = p.getIsAnnotatedClass(RestController.class);
		assertTrue(p.assertClassesByPackageIsAnnotated("controller", Controller.class.getName(), CLASS,
				predicate));

		final var serviceClassName = Service.class.getName();
		Assertions.assertThrows(BadClassAnnotation.class,
				() -> p.assertClassesByPackageIsAnnotated("controller", serviceClassName, CLASS));
	}

}

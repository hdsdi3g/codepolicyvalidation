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

import static java.util.stream.Collectors.toUnmodifiableList;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.junit.jupiter.api.Assertions.fail;
import static tv.hd3g.commons.codepolicyvalidation.CtTypeCat.CLASS;
import static tv.hd3g.commons.codepolicyvalidation.CtTypeCat.INTERFACE;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtThrow;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtModifiable;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeInformation;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.AbstractFilter;

@Disabled
public class CheckPolicy extends Policies {

	private static final String SRC_TEST_JAVA = "src/test/java";
	private static final String COMPONENT_ANNOTATION_NAME = "org.springframework.stereotype.Component";
	private static final String CONTROLLER_ANNOTATION_NAME = "org.springframework.stereotype.Controller";
	private static final String REST_CONTROLLER_ANNOTATION_NAME = "org.springframework.web.bind.annotation.RestController";
	private static final String ENTITY_ANNOTATION_NAME = "jakarta.persistence.Entity";
	private static final String REPOSITORY_ANNOTATION_NAME = "org.springframework.stereotype.Repository";
	private static final String SERVICE_ANNOTATION_NAME = "org.springframework.stereotype.Service";

	private static final String COMPONENT_BASE_PKG = "component";
	private static final String CONTROLLER_BASE_PKG = "controller";
	private static final String ENTITY_BASE_PKG = "entity";
	private static final String REPOSITORY_BASE_PKG = "repository";
	private static final String SERVICE_BASE_PKG = "service";

	private static final Set<String> annotationsControllerRequestNames = Set.of(
			"@org.springframework.web.bind.annotation.RequestMapping",
			"@org.springframework.web.bind.annotation.PostMapping",
			"@org.springframework.web.bind.annotation.PutMapping",
			"@org.springframework.web.bind.annotation.DeleteMapping",
			"@org.springframework.web.bind.annotation.GetMapping",
			"@org.springframework.web.bind.annotation.PatchMapping");

	private static final Set<String> validRepositoriesClassesNames = Set.of(
			"org.springframework.data.jpa.repository.JpaRepository",
			"org.springframework.data.repository.PagingAndSortingRepository",
			"org.springframework.data.repository.CrudRepository",
			"org.springframework.data.repository.Repository");

	/**
	 * Scan in "this" src/main/java and src/test/java
	 */
	@BeforeAll
	public static void globalInit() {
		globalInit("src/main/java", SRC_TEST_JAVA);
	}

	@Test
	public void noIllegalArgumentExceptionWOConstructor() {
		final var typeIAException = typeFactory.get(IllegalArgumentException.class);
		final var list = launcher.getFactory().Package().getRootPackage().getElements(
				new AbstractFilter<CtConstructorCall<?>>() {
					@Override
					public boolean matches(final CtConstructorCall<?> element) {
						if (element.getType().getQualifiedName().equals(typeIAException.getQualifiedName())) {
							return element.getArguments().isEmpty();
						}
						return false;
					}
				});
		if (list.isEmpty()) {
			return;
		}
		fail(list.stream().map(l -> "Don't use " // NOSONAR S5960
									+ l.getType().getQualifiedName()
									+ " without message in "
									+ mapPathElementToString(l))
				.collect(Collectors.joining(System.lineSeparator())));
	}

	@Test
	public void noSysOutSysErr() {
		final var list = launcher.getFactory().Package().getRootPackage().getElements(
				new AbstractFilter<CtInvocation<?>>() {
					@Override
					public boolean matches(final CtInvocation<?> element) {
						if (element.getTarget() == null) {
							return false;
						}
						final var target = element.getTarget();
						if (target instanceof CtFieldRead == false) {
							return false;
						}
						final var prettyprint = target.prettyprint();
						return prettyprint.startsWith("System.out") || prettyprint.startsWith("System.err");
					}
				});
		if (list.isEmpty()) {
			return;
		}
		fail(list.stream().map(l -> "Don't use sys.out/sys.err in " + mapPathElementToString(l))
				.collect(Collectors.joining(System.lineSeparator())));
	}

	@Test
	public void noSimplePrintStackTrace() {
		final var typeThrowable = typeFactory.get(Throwable.class).getReference();

		final var list = launcher.getFactory().Package().getRootPackage().getElements(
				new AbstractFilter<CtExecutableReference<?>>() {
					@Override
					public boolean matches(final CtExecutableReference<?> element) {
						if (element.getReferencedTypes().contains(typeThrowable) == false
							|| element.getSimpleName().equals("printStackTrace") == false) {
							return false;
						}
						/**
						 * @return false (good) for printStackTrace(printstream) or printStackTrace(printwriter)
						 *         else true (bad)
						 */
						final var param = element.getParameters();
						if (param == null || param.isEmpty()) {
							return true;
						}
						final var paramSimpleName = param.get(0).getSimpleName();
						return "printstream".equalsIgnoreCase(paramSimpleName) == false
							   && "printwriter".equalsIgnoreCase(paramSimpleName) == false;
					}
				});
		if (list.isEmpty()) {
			return;
		}
		fail(list.stream().map(l -> "Don't use printStackTrace in " + mapPathElementToString(l.getParent()))
				.collect(Collectors.joining(System.lineSeparator())));
	}

	@Test
	public void noOptionalOf() {
		final var typeOptional = typeFactory.get(Optional.class).getReference();

		final var list = launcher.getFactory().Package().getRootPackage().getElements(
				new AbstractFilter<CtExecutableReference<?>>() {
					@Override
					public boolean matches(final CtExecutableReference<?> element) {
						return element.getReferencedTypes().contains(typeOptional)
							   && element.getSimpleName().equals("of");
					}
				});
		if (list.isEmpty()) {
			return;
		}
		fail(list.stream().map(l -> "Don't use Optional.of in " + mapPathElementToString(l.getParent()))
				.collect(Collectors.joining(System.lineSeparator())));
	}

	@Test
	public void xToOneMustToSetOptional() {
		try {
			final var typeManyToOne = typeFactory.get(ManyToOne.class).getReference();
			final var typeOneToOne = typeFactory.get(OneToOne.class).getReference();

			final var list = launcher.getFactory().Package().getRootPackage().getElements(
					new AbstractFilter<CtTypeReference<?>>() {
						@Override
						public boolean matches(final CtTypeReference<?> element) {
							final var topLevelType = element.getTopLevelType();
							if (typeManyToOne.equals(topLevelType) == false
								&& typeOneToOne.equals(topLevelType) == false
								|| element.getParent() instanceof CtAnnotation<?> == false) {
								return false;
							}
							return ((CtAnnotation<?>) element.getParent()).getValues().containsKey("optional") == false;
						}
					});
			if (list.isEmpty()) {
				return;
			}
			fail(list.stream().map(l -> "You must set ToOne with optional in " + mapPathElementToString(l.getParent()))
					.distinct().collect(Collectors.joining(System.lineSeparator())));

		} catch (final NoClassDefFoundError e) {
			/**
			 * Spring Boot 2 compat
			 */
		}
	}

	@Test
	public void xToManyMustNotUseEAGER() {
		try {
			final var typeOneToMany = typeFactory.get(OneToMany.class).getReference();
			final var typeManyToMany = typeFactory.get(ManyToMany.class).getReference();

			final var list = launcher.getFactory().Package().getRootPackage().getElements(
					new AbstractFilter<CtTypeReference<?>>() {
						@Override
						public boolean matches(final CtTypeReference<?> element) {
							final var topLevelType = element.getTopLevelType();
							if (typeOneToMany.equals(topLevelType) == false
								&& typeManyToMany.equals(topLevelType) == false
								|| element.getParent() instanceof CtAnnotation<?> == false) {
								return false;
							}
							final var values = ((CtAnnotation<?>) element.getParent()).getValues();
							if (values.containsKey("fetch") == false) {
								return false;
							}
							return values.get("fetch").toString().endsWith("EAGER");
						}
					});
			if (list.isEmpty()) {
				return;
			}
			fail(list.stream().map(l -> "You must set ToMany with not EAGER in " + mapPathElementToString(l
					.getParent()))
					.distinct().collect(Collectors.joining(System.lineSeparator())));
		} catch (final NoClassDefFoundError e) {
			/**
			 * Spring Boot 2 compat
			 */
		}
	}

	@Test
	public void noSuppressWarnings() {
		final var typeSuppressWarnings = typeFactory.get(SuppressWarnings.class).getReference();

		final var list = launcher.getFactory().Package().getRootPackage().getElements(
				new AbstractFilter<CtTypeReference<?>>() {
					@Override
					public boolean matches(final CtTypeReference<?> element) {
						final var topLevelType = element.getTopLevelType();
						return typeSuppressWarnings.equals(topLevelType)
							   && element.getParent() instanceof CtAnnotation<?>;
					}
				});
		if (list.isEmpty()) {
			return;
		}

		fail(list.stream().map(l -> "Never use @SuppressWarnings in " + mapPathElementToString(l.getParent()))
				.distinct().collect(Collectors.joining(System.lineSeparator())));
	}

	@Test
	public void noRuntimeException() {
		final var typeRuntimeException = typeFactory.get(RuntimeException.class).getReference();

		final var list = launcher.getFactory().Package().getRootPackage().getElements(
				new AbstractFilter<CtThrow>() {
					@Override
					public boolean matches(final CtThrow element) {
						return element.getReferencedTypes().contains(typeRuntimeException);
					}
				});
		if (list.isEmpty()) {
			return;
		}
		fail(list.stream().map(
				l -> "Never use RuntimeException (use InternalErrorException instead) in "
					 + mapPathElementToString(l.getParent()))
				.collect(Collectors.joining(System.lineSeparator())));
	}

	@Test
	public void noNullPointerException() {
		final var typeNullPointerException = typeFactory.get(NullPointerException.class).getReference();

		final var list = launcher.getFactory().Package().getRootPackage().getElements(
				new AbstractFilter<CtThrow>() {
					@Override
					public boolean matches(final CtThrow element) {
						return element.getReferencedTypes().contains(typeNullPointerException);
					}
				});
		if (list.isEmpty()) {
			return;
		}
		fail(list.stream().map(
				l -> "Never use NullPointerException (use InternalErrorException instead) in "
					 + mapPathElementToString(l.getParent()))
				.collect(Collectors.joining(System.lineSeparator())));
	}

	@Test
	public void notOldJunitAssert() {
		checkClassNotPresent("org.junit.Assert", "Don't use old Junit Assert");
	}

	@Test
	public void notOldJunitRunner() {
		checkClassNotPresent("org.junit.runner", "Don't use old Junit Runner");
	}

	@Test
	public void notOldJunit() {
		checkClassNotPresent("org.junit.Test", "Don't use old Junit Test");
	}

	@Test
	public void notJunitFramework() {
		checkClassNotPresent("junit.framework", "Don't use Junit internal classes");
	}

	@Test
	public void notSQLDate() {
		checkClassNotPresent("java.sql.Date", "Don't use SQL date");
	}

	@Test
	public void notCommonsLang2Use3() {
		checkClassNotPresent("org.apache.commons.lang.", "Use commons lang 3");
	}

	@Test
	public void notCommonsCollection3Use4() {
		checkClassNotPresent("org.apache.commons.collections.", "Use commons collection 4");
	}

	@Test
	public void notLog4j() {
		checkClassNotPresent("org.apache.logging.log4j.", "Don't use directly log4j");
	}

	public void notFlatJavaMailSenderOutsideTests(final String allowSourceDir) {
		checkClassNotPresent("tv.hd3g.mailkit.utility.FlatJavaMailSender", allowSourceDir,
				"Never use it outside " + allowSourceDir);
	}

	@Test
	public void notFlatJavaMailSenderOutsideTests() {
		notFlatJavaMailSenderOutsideTests(SRC_TEST_JAVA);
	}

	public void notFlatJobKitEngineOutsideTests(final String allowSourceDir) {
		checkClassNotPresent("tv.hd3g.jobkit.engine.FlatJobKitEngine", allowSourceDir,
				"Never use it outside " + allowSourceDir);
	}

	@Test
	public void notFlatJobKitEngineOutsideTests() {
		notFlatJobKitEngineOutsideTests(SRC_TEST_JAVA);
	}

	@Test
	public void springBootControllersInControllerPackage() {
		assertSpringBootStereotypeInItsPackage(CONTROLLER_ANNOTATION_NAME, CONTROLLER_BASE_PKG);
	}

	@Test
	public void springBootEntitiesInEntityPackage() {
		assertSpringBootStereotypeInItsPackage(ENTITY_ANNOTATION_NAME, ENTITY_BASE_PKG);
	}

	@Test
	public void springBootRepositoriesInRepositoryPackage() {
		assertSpringBootStereotypeInItsPackage(REPOSITORY_ANNOTATION_NAME, REPOSITORY_BASE_PKG);
	}

	@Test
	public void springBootComponentInComponentPackage() {
		assertSpringBootStereotypeInItsPackage(COMPONENT_ANNOTATION_NAME, COMPONENT_BASE_PKG);
	}

	@Test
	public void springBootServicesInServicePackage() {
		assertSpringBootStereotypeInItsPackage(SERVICE_ANNOTATION_NAME, SERVICE_BASE_PKG);
	}

	@Test
	public void springBootNotControllerInControllerPackage() {
		Class<?> restControllerAnnotation;
		try {
			restControllerAnnotation = Class.forName(REST_CONTROLLER_ANNOTATION_NAME);
		} catch (final ClassNotFoundException e) {
			return;
		}

		assertClassesByPackageIsAnnotated(CONTROLLER_BASE_PKG, CONTROLLER_ANNOTATION_NAME, CLASS,
				getIsAnnotatedClass(restControllerAnnotation));
	}

	@Test
	public void springBootNotComponentInComponentPackage() {
		Class<?> componentAnnotation;
		try {
			componentAnnotation = Class.forName(COMPONENT_ANNOTATION_NAME);
		} catch (final ClassNotFoundException e) {
			return;
		}

		assertClassesByPackageIsAnnotated(COMPONENT_BASE_PKG, COMPONENT_ANNOTATION_NAME, CLASS,
				getIsAnnotatedClass(componentAnnotation));
	}

	@Test
	public void springBootNotEntityInEntityPackage() {
		try {
			final var mappedSuperclassAnnotation = Class.forName("jakarta.persistence.MappedSuperclass");
			assertClassesByPackageIsAnnotated(ENTITY_BASE_PKG, ENTITY_ANNOTATION_NAME, CLASS,
					getIsAnnotatedClass(mappedSuperclassAnnotation));
		} catch (final ClassNotFoundException e2) {// NOSONAR S108
		}
	}

	@Test
	public void springBootNotRepositoryInRepositoryPackage() {
		assertClassesByPackageIsAnnotated(REPOSITORY_BASE_PKG, REPOSITORY_ANNOTATION_NAME, INTERFACE,
				t -> {
					if (t.getQualifiedName().toLowerCase().endsWith("dao")) {
						return true;
					}
					try {
						final var fullClass = Class.forName(t.getQualifiedName());
						return Stream.of(fullClass.getInterfaces())
								.map(Class::getName)
								.anyMatch(validRepositoriesClassesNames::contains);
					} catch (final ClassNotFoundException e) {
						throw new IllegalCallerException(e);
					}
				});
	}

	@Test
	public void springBootNotServiceInServicePackage() {
		assertClassesByPackageIsAnnotated(SERVICE_BASE_PKG, SERVICE_ANNOTATION_NAME, CLASS);
	}

	@Test
	public void springBootNotClassInControllerPackage() {
		assertClassesByPackageIsRightType(CONTROLLER_BASE_PKG, List.of(CLASS));
	}

	@Test
	public void springBootNotClassInEntityPackage() {
		assertClassesByPackageIsRightType(ENTITY_BASE_PKG, List.of(CLASS), CtModifiable::isAbstract);
	}

	@Test
	public void springBootNotInterfaceInRepositoryPackage() {
		Class<?> repositoryAnnotation;
		try {
			repositoryAnnotation = Class.forName(REPOSITORY_ANNOTATION_NAME);
		} catch (final ClassNotFoundException e) {
			return;
		}
		final var filter = getIsAnnotatedClass(repositoryAnnotation)
				.and(t -> t.getQualifiedName().toLowerCase().endsWith("daoimpl"));
		assertClassesByPackageIsRightType(REPOSITORY_BASE_PKG, List.of(INTERFACE), filter);
	}

	@Test
	public void springBootNotClassOrInterfaceInServicePackage() {
		assertClassesByPackageIsRightType(SERVICE_BASE_PKG, List.of(INTERFACE, CLASS,
				CtTypeCat.ABSTRACT));
	}

	/**
	 * Check services class name "nServiceImpl"
	 */
	@Test
	public void springBootServiceBadName() {
		final var serviceImplList = searchByAnnotationInClass(SERVICE_ANNOTATION_NAME);
		final var badNames = serviceImplList.stream()
				.map(CtTypeInformation::getQualifiedName)
				.filter(s -> s.endsWith("ServiceImpl") == false)
				.collect(toUnmodifiableList());
		if (badNames.isEmpty() == false) {
			fail("Service classes with invalid suffix name (not ends with \"ServiceImpl\"): " + badNames);
		}
	}

	/**
	 * Check services class name implements Interface
	 */
	@Test
	public void springBootServiceDontImplInterface() {
		final var serviceImplList = searchByAnnotationInClass(SERVICE_ANNOTATION_NAME);
		final var noInterfaces = serviceImplList.stream()
				.filter(s -> s.getSuperInterfaces().isEmpty()
							 && Optional.ofNullable(s.getSuperclass())
									 .map(CtTypeReference::getSuperInterfaces)
									 .orElse(Set.of())
									 .isEmpty())
				.map(CtTypeInformation::getQualifiedName)
				.collect(toUnmodifiableList());
		if (noInterfaces.isEmpty() == false) {
			fail("Service classes that do not implement an interface or extends classes do not implement an interface: "
				 + noInterfaces);
		}
	}

	/**
	 * Check services class name if implemented Interface is same Pkg, check I name "nService"
	 */
	@Test
	public void springBootServiceInterfaceNames() {
		final var serviceImplList = searchByAnnotationInClass(SERVICE_ANNOTATION_NAME);
		final var localPackages = serviceImplList.stream()
				.filter(s -> s.getSuperInterfaces().isEmpty() == false)
				.map(CtElement::getParent)
				.filter(Objects::nonNull)
				.filter(CtPackage.class::isInstance)
				.map(CtPackage.class::cast)
				.map(CtPackage::getReference)
				.distinct()
				.collect(toUnmodifiableSet());

		final var badNamedServiceInterfaces = serviceImplList.stream()
				.flatMap(s -> s.getSuperInterfaces().stream())
				.distinct()
				.filter(i -> i.getPackage() != null)
				.filter(i -> localPackages.contains(i.getPackage()))
				.map(CtTypeInformation::getQualifiedName)
				.filter(i -> i.endsWith("Service") == false)
				.collect(toUnmodifiableList());

		if (badNamedServiceInterfaces.isEmpty() == false) {
			fail("Missnamed Service interfaces: " + badNamedServiceInterfaces);
		}
	}

	/**
	 * Good: MyCheckPolicyTest extends CheckPolicy
	 * Bad: MyCheckPolicy extends CheckPolicy
	 */
	@Test
	public void classExtendsCheckPolicyNamesMustEndsByTest() {
		final var typeCheckPolicy = typeFactory.get(CheckPolicy.class);
		final var classesWithBadNames = classesByImported.keySet().stream()
				.filter(cl -> cl.getQualifiedName().equals(typeCheckPolicy.getQualifiedName()))
				.flatMap(cl -> classesByImported.get(cl).stream())
				.filter(cl -> cl.getQualifiedName().equals(typeCheckPolicy.getQualifiedName()) == false)
				.map(CtType::getQualifiedName)
				.filter(name -> name.endsWith("Test") == false)
				.collect(Collectors.toUnmodifiableSet());

		if (classesWithBadNames.isEmpty() == false) {
			throw new MissingTestInClassName(classesWithBadNames.stream().collect(Collectors.joining(", ")));
		}
	}

	@Test
	public void springBootRESTControllerMethodsMustReturnResponseEntity() {
		final var typeObject = typeFactory.get(Object.class);
		final var allObjectMethodNames = typeObject.getAllMethods().stream()
				.map(CtMethod::getSimpleName)
				.distinct()
				.collect(toUnmodifiableSet());

		final var publicMethodByControllerClasses = searchByAnnotationInClass(REST_CONTROLLER_ANNOTATION_NAME).stream()
				.collect(Collectors.toUnmodifiableMap(c -> c,
						c -> c.getAllMethods().stream()
								.filter(CtMethod::isPublic)
								.filter(Predicate.not(CtMethod::isStatic))
								.filter(m -> allObjectMethodNames.contains(m.getSimpleName()) == false)
								.collect(toUnmodifiableList())));

		final var bclList = publicMethodByControllerClasses.entrySet().stream()
				.flatMap(entry -> {
					final var cl = entry.getKey();
					final var methods = entry.getValue();

					return Stream.of(hasNotAnnotationMethod(methods, annotationsControllerRequestNames)
							.map(m -> cl.getSimpleName() + ":" + m.getSimpleName())
							.map(ref -> "public method in RestController \"" + ref + "\" is not a @RequestMapping"),
							methods.stream()
									.filter(m -> (m.getType() == null
												  || m.getType().toString()
														  .startsWith(
																  "org.springframework.http.ResponseEntity") == false))
									.map(m -> cl.getSimpleName() + ":" + m.getSimpleName())
									.map(ref -> "public method in RestController \"" + ref
												+ "\" don't return a ResponseEntity"))
							.flatMap(t -> t);
				})
				.collect(toUnmodifiableList());

		if (bclList.isEmpty() == false) {
			throw new AssertionError(String.join("; ", bclList));
		}
	}

}

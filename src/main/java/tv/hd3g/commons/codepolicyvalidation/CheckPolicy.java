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
import static tv.hd3g.commons.codepolicyvalidation.CheckPolicy.CtTypeCat.ABSTRACT;
import static tv.hd3g.commons.codepolicyvalidation.CheckPolicy.CtTypeCat.CLASS;
import static tv.hd3g.commons.codepolicyvalidation.CheckPolicy.CtTypeCat.INTERFACE;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import spoon.Launcher;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtThrow;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtModifiable;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeInformation;
import spoon.reflect.factory.TypeFactory;
import spoon.reflect.path.CtRole;
import spoon.reflect.path.impl.CtPathImpl;
import spoon.reflect.path.impl.CtRolePathElement;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.AbstractFilter;
import spoon.support.reflect.declaration.CtClassImpl;
import spoon.support.reflect.declaration.CtInterfaceImpl;
import spoon.support.reflect.declaration.CtPackageImpl;

@Disabled
public class CheckPolicy {

	private static final String CONTROLLER_ANNOTATION_NAME = "org.springframework.stereotype.Controller";
	private static final String ENTITY_ANNOTATION_NAME = "javax.persistence.Entity";
	private static final String REPOSITORY_ANNOTATION_NAME = "org.springframework.stereotype.Repository";
	private static final String SERVICE_ANNOTATION_NAME = "org.springframework.stereotype.Service";

	private static final String CONTROLLER_BASE_PKG = "controller";
	private static final String ENTITY_BASE_PKG = "entity";
	private static final String REPOSITORY_BASE_PKG = "repository";
	private static final String SERVICE_BASE_PKG = "service";

	public static final TypeFactory typeFactory = new TypeFactory();

	private static Launcher launcher;
	private static Map<CtTypeReference<?>, Set<? extends CtType<?>>> classesByImported;

	/**
	 * Scan in "this" src/main/java and src/test/java
	 */
	@BeforeAll
	public static void globalInit() {
		globalInit("src/main/java", "src/test/java");
	}

	public static void globalInit(final String... inputResources) {
		launcher = new Launcher();
		for (int i = 0; i < inputResources.length; i++) {
			launcher.addInputResource(inputResources[i]);
		}
		final var allTypes = launcher.buildModel().getAllTypes().stream().collect(Collectors.toUnmodifiableList());

		final Map<CtType<?>, Set<CtTypeReference<?>>> importedByClasses = allTypes.stream()
		        .collect(Collectors.toUnmodifiableMap(
		                k -> k,
		                v -> v.getUsedTypes(true).stream().distinct().collect(Collectors.toUnmodifiableSet())));

		classesByImported = importedByClasses
		        .values().stream()
		        .flatMap(Set::stream)
		        .distinct()
		        .collect(Collectors.toUnmodifiableMap(k -> k,
		                v -> importedByClasses.entrySet().stream()
		                        .filter(entry -> entry.getValue().contains(v))
		                        .map(Entry::getKey)
		                        .distinct()
		                        .collect(Collectors.toSet())));
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
		fail(list.stream().map(l -> "Don't use "
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
	public void xToOneMustToSetOptional() {
		final var typeManyToOne = typeFactory.get(ManyToOne.class).getReference();
		final var typeOneToOne = typeFactory.get(OneToOne.class).getReference();

		final var list = launcher.getFactory().Package().getRootPackage().getElements(
		        new AbstractFilter<CtTypeReference<?>>() {
			        @Override
			        public boolean matches(final CtTypeReference<?> element) {
				        final var topLevelType = element.getTopLevelType();
				        if (typeManyToOne.equals(topLevelType) == false && typeOneToOne.equals(topLevelType) == false
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
	}

	@Test
	public void xToManyMustNotUseEAGER() {
		final var typeOneToMany = typeFactory.get(OneToMany.class).getReference();
		final var typeManyToMany = typeFactory.get(ManyToMany.class).getReference();

		final var list = launcher.getFactory().Package().getRootPackage().getElements(
		        new AbstractFilter<CtTypeReference<?>>() {
			        @Override
			        public boolean matches(final CtTypeReference<?> element) {
				        final var topLevelType = element.getTopLevelType();
				        if (typeOneToMany.equals(topLevelType) == false && typeManyToMany.equals(topLevelType) == false
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
		fail(list.stream().map(l -> "You must set ToMany with not EAGER in " + mapPathElementToString(l.getParent()))
		        .distinct().collect(Collectors.joining(System.lineSeparator())));
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

	public static String mapPathElementToString(final CtElement element) {
		final var path = element.getPath();
		if (path instanceof CtPathImpl == false) {
			return path.toString();
		}
		final var pathElements = ((CtPathImpl) path).getElements();
		return pathElements.stream().map(pE -> {
			if (pE instanceof CtRolePathElement == false) {
				return pE.toString() + " ";
			}

			final var rRPE = (CtRolePathElement) pE;
			if (rRPE.getRole().equals(CtRole.SUB_PACKAGE) || rRPE.getRole().equals(
			        CtRole.CONTAINED_TYPE)) {
				return rRPE.getArguments().get("name") + ".";
			} else if (rRPE.getRole().equals(CtRole.METHOD)) {
				final var pos = element.getPosition();
				return rRPE.getArguments().get("signature")
				       + " " + pos.getFile().getName()
				       + ":" + pos.getLine();
			} else if (rRPE.getRole().equals(CtRole.FIELD)) {
				final var pos = element.getPosition();
				return rRPE.getArguments().get("name")
				       + " (" + pos.getFile().getName() + ":" + pos.getLine() + ")";
			}
			return "";
		}).collect(Collectors.joining());
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

	public static void checkClassNotPresent(final String classBaseName, final String reason) {
		final var classesWithBadImports = classesByImported.keySet().stream()
		        .filter(cl -> cl.getQualifiedName().startsWith(classBaseName))
		        .flatMap(cl -> classesByImported.get(cl).stream()
		                .distinct().map(t -> t.getQualifiedName()
		                                     + " class must not import "
		                                     + cl.getQualifiedName()
		                                     + " class"))
		        .collect(Collectors.toUnmodifiableSet());

		if (classesWithBadImports.isEmpty() == false) {
			throw new BadImportClass(classBaseName,
			        reason,
			        classesWithBadImports.stream().collect(Collectors.joining(", ")));
		}
	}

	public static List<CtType<?>> searchByAnnotationInClass(final Class<?> annotation) {// NOSONAR S1452
		final var annotationType = typeFactory.get(annotation).getReference();
		return launcher.getFactory().Package().getRootPackage().getElements(
		        new AbstractFilter<CtType<?>>() {
			        @Override
			        public boolean matches(final CtType<?> element) {
				        return element.getAnnotations().stream()
				                .anyMatch(a -> a.getType().equals(annotationType));
			        }
		        });
	}

	public static List<CtType<?>> searchByAnnotationInClass(final String annotationName) {// NOSONAR S1452
		Class<?> annotation;
		try {
			annotation = Class.forName(annotationName);
		} catch (final ClassNotFoundException e) {
			return List.of();
		}
		return searchByAnnotationInClass(annotation);
	}

	static boolean ensureContainInPackageName(final String packageName, final String searched) {
		final var dot = packageName.lastIndexOf('.');
		if (dot == -1) {
			return packageName.startsWith(searched) || packageName.endsWith(searched);
		} else {
			final var subpackageName = packageName.substring(dot + 1);
			return subpackageName.startsWith(searched) || subpackageName.endsWith(searched);
		}
	}

	public static void assertInPackageName(final CtType<?> item, final String packageNameContain) {
		final var ctPackage = (CtPackageImpl) item.getParent();
		final var packageName = ctPackage.getQualifiedName();
		if (ensureContainInPackageName(packageName, packageNameContain) == false) {
			throw new BadClassLocation(item.getQualifiedName(), packageName, packageNameContain);
		}
	}

	public static void assertSpringBootStereotypeInItsPackage(final String annotationName,
	                                                          final String packageNameContain) {
		final var bclList = searchByAnnotationInClass(annotationName)
		        .stream()
		        .map(item -> {
			        try {
				        assertInPackageName(item, packageNameContain);
				        return null;
			        } catch (final BadClassLocation bcl) {
				        return bcl;
			        }
		        })
		        .filter(Objects::nonNull)
		        .collect(Collectors.toList());
		if (bclList.isEmpty() == false) {
			throw new BadClassLocation(bclList);
		}
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
	public void springBootServicesInServicePackage() {
		assertSpringBootStereotypeInItsPackage(SERVICE_ANNOTATION_NAME, SERVICE_BASE_PKG);
	}

	public static List<CtPackage> searchPackagesByPackageName(final String packageNameContain) {
		return launcher.getFactory().Package().getRootPackage().getElements(
		        new AbstractFilter<CtPackage>() {
			        @Override
			        public boolean matches(final CtPackage element) {
				        return ensureContainInPackageName(element.getQualifiedName(), packageNameContain);
			        }
		        });
	}

	public static List<CtType<?>> searchClassesByPackages(final List<CtPackage> packages, // NOSONAR S1452
	                                                      final Predicate<CtType<?>> inPackageFilter) {
		final var packagesSet = packages.stream().distinct().collect(toUnmodifiableSet());
		return launcher.getFactory().Package().getRootPackage().getElements(
		        new AbstractFilter<CtType<?>>() {
			        @Override
			        public boolean matches(final CtType<?> element) {
				        return packagesSet.contains(element.getParent())
				               && inPackageFilter.test(element);
			        }
		        });
	}

	public static Predicate<CtType<?>> getIsAnnotatedClass(final Class<?> annotation) { // NOSONAR S1452
		final var annotationType = typeFactory.get(annotation).getReference();
		return element -> element.getAnnotations()
		        .stream()
		        .anyMatch(a -> a.getType().equals(annotationType));

	}

	private static final Predicate<CtType<?>> getClassFilter(final boolean mustAbstract) {
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

	public enum CtTypeCat {
		CLASS(getClassFilter(false)),
		ABSTRACT(getClassFilter(true)),
		INTERFACE(t -> CtInterfaceImpl.class.equals(t.getClass()));

		private final Predicate<CtType<?>> filter;

		private CtTypeCat(final Predicate<CtType<?>> filter) {
			this.filter = filter;
		}
	}

	public static boolean assertClassesByPackageIsAnnotated(final String packageNameContain,
	                                                        final String annotationName,
	                                                        final CtTypeCat typeCat) {
		return assertClassesByPackageIsAnnotated(packageNameContain, annotationName, typeCat, t -> false);
	}

	private static final Predicate<CtType<?>> filterTestAllowed = t -> {
		final var name = t.getQualifiedName();
		return name.toLowerCase().endsWith("test")
		       || name.endsWith("IT")
		       || name.endsWith("UT")
		       || name.endsWith("TT");
	};

	public static boolean assertClassesByPackageIsAnnotated(final String packageNameContain,
	                                                        final String annotationName,
	                                                        final CtTypeCat typeCat,
	                                                        final Predicate<CtType<?>> filterOutAllowed) {
		Class<?> annotation;
		try {
			annotation = Class.forName(annotationName);
		} catch (final ClassNotFoundException e) {
			return true;
		}
		final var packages = searchPackagesByPackageName(packageNameContain);
		final var predicateIsAnnotated = getIsAnnotatedClass(annotation);

		final var noAnnotatedList = searchClassesByPackages(packages,
		        predicateIsAnnotated.negate().and(typeCat.filter))
		                .stream()
		                .filter(filterOutAllowed.negate())
		                .filter(filterTestAllowed.negate())
		                .collect(toUnmodifiableList());

		if (noAnnotatedList.isEmpty()) {
			return true;
		}
		final var bclList = noAnnotatedList.stream()
		        .map(item -> new BadClassAnnotation(item.getQualifiedName(),
		                ((CtPackageImpl) item.getParent()).getQualifiedName(),
		                packageNameContain))
		        .collect(Collectors.toUnmodifiableList());
		throw new BadClassAnnotation(bclList);
	}

	@Test
	public void springBootNotControllerInControllerPackage() {
		Class<?> restControllerAnnotation;
		try {
			restControllerAnnotation = Class.forName("org.springframework.web.bind.annotation.RestController");
		} catch (final ClassNotFoundException e) {
			return;
		}

		assertClassesByPackageIsAnnotated(CONTROLLER_BASE_PKG, CONTROLLER_ANNOTATION_NAME, CLASS,
		        getIsAnnotatedClass(restControllerAnnotation));
	}

	@Test
	public void springBootNotEntityInEntityPackage() {
		Class<?> mappedSuperclassAnnotation;
		try {
			mappedSuperclassAnnotation = Class.forName("javax.persistence.MappedSuperclass");
		} catch (final ClassNotFoundException e) {
			return;
		}
		assertClassesByPackageIsAnnotated(ENTITY_BASE_PKG, ENTITY_ANNOTATION_NAME, CLASS,
		        getIsAnnotatedClass(mappedSuperclassAnnotation));
	}

	@Test
	public void springBootNotRepositoryInRepositoryPackage() {
		assertClassesByPackageIsAnnotated(REPOSITORY_BASE_PKG, REPOSITORY_ANNOTATION_NAME, INTERFACE,
		        t -> t.getQualifiedName().toLowerCase().endsWith("dao"));
	}

	@Test
	public void springBootNotServiceInServicePackage() {
		assertClassesByPackageIsAnnotated(SERVICE_BASE_PKG, SERVICE_ANNOTATION_NAME, CLASS);
	}

	public static boolean assertClassesByPackageIsRightType(final String packageNameContain,
	                                                        final Collection<CtTypeCat> typeCatAllowed) {
		return assertClassesByPackageIsRightType(packageNameContain, typeCatAllowed, t -> false);
	}

	public static boolean assertClassesByPackageIsRightType(final String packageNameContain,
	                                                        final Collection<CtTypeCat> typeCatAllowed,
	                                                        final Predicate<CtType<?>> filterOutAllowed) {
		final var packages = searchPackagesByPackageName(packageNameContain);
		final Predicate<CtType<?>> inPackageFilter = t -> typeCatAllowed.stream()
		        .anyMatch(tC -> tC.filter.test(t));
		final var badTypeList = searchClassesByPackages(packages,
		        inPackageFilter.negate().and(filterTestAllowed.negate()).and(filterOutAllowed.negate()));

		if (badTypeList.isEmpty()) {
			return true;
		}
		final var bclList = badTypeList.stream()
		        .map(CtTypeInformation::getQualifiedName)
		        .collect(toUnmodifiableList());
		throw new AssertionError("Invalid type in package " + packageNameContain + ":" + bclList);
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
		assertClassesByPackageIsRightType(SERVICE_BASE_PKG, List.of(INTERFACE, CLASS, ABSTRACT));
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
		        .filter(p -> p instanceof CtPackage)
		        .map(p -> (CtPackage) p)
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

}

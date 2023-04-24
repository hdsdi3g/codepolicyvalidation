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

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toUnmodifiableList;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static java.util.stream.Collectors.toUnmodifiableSet;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import spoon.Launcher;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeInformation;
import spoon.reflect.factory.TypeFactory;
import spoon.reflect.path.CtRole;
import spoon.reflect.path.impl.CtPathImpl;
import spoon.reflect.path.impl.CtRolePathElement;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.AbstractFilter;
import spoon.support.reflect.declaration.CtPackageImpl;

public class Policies {

	public static final TypeFactory typeFactory = new TypeFactory();
	static Launcher launcher;
	static Map<CtTypeReference<?>, Set<? extends CtType<?>>> classesByImported;

	public static void globalInit(final String... inputResources) {
		launcher = new Launcher();
		for (var i = 0; i < inputResources.length; i++) {
			launcher.addInputResource(inputResources[i]);
		}
		final var allTypes = launcher.buildModel()
				.getAllTypes()
				.stream()
				.collect(toUnmodifiableList());

		final Map<CtType<?>, Set<CtTypeReference<?>>> importedByClasses = allTypes.stream()
				.collect(toUnmodifiableMap(
						k -> k,
						v -> {
							/**
							 * Safe Set transformation.
							 * Sometime, hasNext() here thrown a "Comparison method violates its general contract"
							 */
							final var source = v.getUsedTypes(true);
							final var itr = source.iterator();
							final var intermediate = new HashSet<CtTypeReference<?>>();
							while (true) {// NOSONAR S135
								try {
									if (itr.hasNext() == false) {
										break;
									}
									intermediate.add(itr.next());
								} catch (final IllegalArgumentException e) {
									if (itr.hasNext() == false) {
										break;
									}
								}
							}
							if (intermediate.size() != source.size()) {
								throw new IllegalArgumentException(
										"Invalid Spoon behavior during import collection for "
																   + v.getQualifiedName()
																   + " intermediate Set size "
																   + intermediate.size()
																   + " != source size " + source.size());
							}
							return intermediate;
						}));

		classesByImported = importedByClasses.values()
				.stream()
				.flatMap(Set::stream)
				.distinct()
				.collect(toUnmodifiableMap(k -> k,
						v -> importedByClasses.entrySet().stream()
								.filter(entry -> entry.getValue().contains(v))
								.map(Entry::getKey)
								.collect(toUnmodifiableSet())));
	}

	public String mapPathElementToString(final CtElement element) {
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
		}).collect(joining());
	}

	public void checkClassNotPresent(final String classBaseName, final String reason) {
		final var classesWithBadImports = classesByImported.keySet().stream()
				.filter(cl -> cl.getQualifiedName().startsWith(classBaseName))
				.flatMap(cl -> classesByImported.get(cl).stream()
						.distinct().map(t -> t.getQualifiedName()
											 + " class must not import "
											 + cl.getQualifiedName()
											 + " class"))
				.collect(toUnmodifiableSet());

		if (classesWithBadImports.isEmpty() == false) {
			throw new BadImportClass(classBaseName,
					reason,
					classesWithBadImports.stream().collect(Collectors.joining(", ")));
		}
	}

	public void checkClassNotPresent(final String classBaseName,
									 final String allowSourceDir,
									 final String reason) {
		final var appRootDirSize = new File("").getAbsolutePath().length() + 1;
		final var classesWithBadImports = classesByImported.keySet().stream()
				.filter(cl -> cl.getQualifiedName().startsWith(classBaseName))
				.flatMap(cl -> classesByImported.get(cl).stream()
						.distinct()
						.filter(c -> {
							final var basePath = c.getPosition()
									.getFile()
									.getAbsolutePath()
									.substring(appRootDirSize)
									.replace("\\", "/");
							return basePath.startsWith(allowSourceDir) == false;
						})
						.map(t -> t.getQualifiedName()
								  + " class must not import "
								  + cl.getQualifiedName()
								  + " class"))
				.collect(toUnmodifiableSet());

		if (classesWithBadImports.isEmpty() == false) {
			throw new BadImportClass(classBaseName,
					reason,
					classesWithBadImports.stream().collect(Collectors.joining(", ")));
		}
	}

	public List<CtType<?>> searchByAnnotationInClass(final Class<?> annotation) {// NOSONAR S1452
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

	public List<CtType<?>> searchByAnnotationInClass(final String annotationName) {// NOSONAR S1452
		Class<?> annotation;
		try {
			annotation = Class.forName(annotationName);
		} catch (final ClassNotFoundException e) {
			return List.of();
		}
		return searchByAnnotationInClass(annotation);
	}

	boolean ensureContainInPackageName(final String packageName, final String searched) {
		final var dot = packageName.lastIndexOf('.');
		if (dot == -1) {
			return packageName.startsWith(searched) || packageName.endsWith(searched);
		} else {
			final var subpackageName = packageName.substring(dot + 1);
			return subpackageName.startsWith(searched) || subpackageName.endsWith(searched);
		}
	}

	public void assertInPackageName(final CtType<?> item, final String packageNameContain) {
		final var ctPackage = (CtPackageImpl) item.getParent();
		final var packageName = ctPackage.getQualifiedName();
		if (ensureContainInPackageName(packageName, packageNameContain) == false) {
			throw new BadClassLocation(item.getQualifiedName(), packageName, packageNameContain);
		}
	}

	public void assertSpringBootStereotypeInItsPackage(final String annotationName,
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

	public List<CtPackage> searchPackagesByPackageName(final String packageNameContain) {
		return launcher.getFactory().Package().getRootPackage().getElements(
				new AbstractFilter<CtPackage>() {
					@Override
					public boolean matches(final CtPackage element) {
						return ensureContainInPackageName(element.getQualifiedName(), packageNameContain);
					}
				});
	}

	public List<CtType<?>> searchClassesByPackages(final List<CtPackage> packages, // NOSONAR S1452
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

	public Predicate<CtType<?>> getIsAnnotatedClass(final Class<?> annotation) { // NOSONAR S1452
		final var annotationType = typeFactory.get(annotation).getReference();
		return element -> element.getAnnotations()
				.stream()
				.anyMatch(a -> a.getType().equals(annotationType));

	}

	Stream<CtMethod<?>> hasNotAnnotationMethod(final List<CtMethod<?>> methods, // NOSONAR S1452
											   final Set<String> annotationNames) {
		return methods.stream()
				.filter(m -> m.getAnnotations().stream()
						.noneMatch(d -> annotationNames.stream().anyMatch(aN -> d.toString().contains(aN))));
	}

	public boolean assertClassesByPackageIsAnnotated(final String packageNameContain,
													 final String annotationName,
													 final CtTypeCat typeCat) {
		return assertClassesByPackageIsAnnotated(packageNameContain, annotationName, typeCat, t -> false);
	}

	static final Predicate<CtType<?>> filterTestAllowed = t -> {
		final var name = t.getQualifiedName();
		return name.toLowerCase().endsWith("test")
			   || name.endsWith("IT")
			   || name.endsWith("UT")
			   || name.endsWith("TT");
	};

	public boolean assertClassesByPackageIsAnnotated(final String packageNameContain,
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

	public boolean assertClassesByPackageIsRightType(final String packageNameContain,
													 final Collection<CtTypeCat> typeCatAllowed) {
		return assertClassesByPackageIsRightType(packageNameContain, typeCatAllowed, t -> false);
	}

	public boolean assertClassesByPackageIsRightType(final String packageNameContain,
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

}

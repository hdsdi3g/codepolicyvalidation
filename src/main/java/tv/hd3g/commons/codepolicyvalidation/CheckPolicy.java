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

import static org.junit.jupiter.api.Assertions.fail;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import spoon.Launcher;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtThrow;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.TypeFactory;
import spoon.reflect.path.CtRole;
import spoon.reflect.path.impl.CtPathImpl;
import spoon.reflect.path.impl.CtRolePathElement;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.AbstractFilter;

public class CheckPolicy {

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
	public boolean noIllegalArgumentExceptionWOConstructor() {
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
			return true;
		}

		fail(list.stream().map(l -> "Don't use "
		                            + l.getType().getQualifiedName()
		                            + " without message in "
		                            + mapPathElementToString(l))
		        .collect(Collectors.joining(System.lineSeparator())));
		return false;
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
	public boolean printStackTrace() {
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
			return true;
		}
		fail(list.stream().map(l -> "Don't use printStackTrace in " + mapPathElementToString(l.getParent()))
		        .collect(Collectors.joining(System.lineSeparator())));
		return false;
	}

	@Test
	public boolean xToOneMustToSetOptional() {
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
			return true;
		}
		fail(list.stream().map(l -> "You must set ToOne with optional in " + mapPathElementToString(l.getParent()))
		        .distinct().collect(Collectors.joining(System.lineSeparator())));
		return false;
	}

	@Test
	public boolean xToManyMustNotUseEAGER() {
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
			return true;
		}
		fail(list.stream().map(l -> "You must set ToMany with not EAGER in " + mapPathElementToString(l.getParent()))
		        .distinct().collect(Collectors.joining(System.lineSeparator())));
		return false;
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

}

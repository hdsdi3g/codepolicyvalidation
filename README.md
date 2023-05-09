# CodePolicyValidation

Static Java 17 code validator using Spoon (http://spoon.gforge.inria.fr/).

It's still in alpha.

See pom.xml file for more details.

![Java CI with Maven](https://github.com/hdsdi3g/codepolicyvalidation/workflows/Java%20CI%20with%20Maven/badge.svg)

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=hdsdi3g_codepolicyvalidation&metric=alert_status)](https://sonarcloud.io/dashboard?id=hdsdi3g_codepolicyvalidation)

![CodeQL](https://github.com/hdsdi3g/codepolicyvalidation/workflows/CodeQL/badge.svg)

## Setup

Add in your pom file:

```
<dependency>
    <groupId>tv.hd3g.commons</groupId>
    <artifactId>codepolicyvalidation</artifactId>
    <version>(last current version)</version>
    <scope>test</scope>
</dependency>
```

Use as Junit 5 template. Just create a test Class in your code like this:

```
import tv.hd3g.commons.codepolicyvalidation.CheckPolicy;

public class CodePolicyValidationTest extends CheckPolicy {
}
```

And run tests.

## Contributing / debugging

For run the tests, you juste needs Maven.

Versioning: just use [SemVer](https://semver.org/).

## Author and License

This project is writer by [hdsdi3g](https://github.com/hdsdi3g) and licensed under the LGPL License; see the LICENCE.TXT file for details.

# CodePolicyValidation changelog

## 3.1.0

Switch to Java 17, update deps (Spoon, Jakarta, Commons Compress, Slf4j, Logback).

## 3.0.0

Exclude log4j from use, and replace it by SLF4J and logback #43

## 2.2.0

Fix troubles with Spring Boot 3 #40

## 2.1.0

Remove check Repository annotation presence for repositories class on springBootNotRepositoryInRepositoryPackage #38

## 2.0.0

Switch from Java 11 to Java 17 (LTS)

Migrate from Javax to Jakarta #35

## 1.1.0

Protect too never import FlatJobKit or FlatJavaMailSender outside tests (see https://github.com/hdsdi3g/prodlib) #33


# Changelog

## [Unreleased]

## [5.4.3]
### Changed
- Client version updated on [5.2.15](https://github.com/reportportal/client-java/releases/tag/5.2.15), by @HardNorth
- Format of last error log of test in item description was updated, by @HardNorth
### Removed
- Code reference report on Test NG's "Test" level to address issues with reruns, by @HardNorth

## [5.4.2]
### Added
- `@Description` annotation support, by @oleksandr-fomenko
- `@DisplayName` annotation support, by @oleksandr-fomenko
- Putting last error logs of tests to Items' description, by @utdacc
### Changed
- Client version updated on [5.2.13](https://github.com/reportportal/client-java/releases/tag/5.2.13), by @HardNorth
### Removed
- `TestNG` dependency marked as `compileOnly`, by @HardNorth
- JSR-305 dependency, by @HardNorth

## [5.4.1]
### Changed
- Move Okhttp3 to test dependencies, by @HardNorth
- Client version updated on [5.2.4](https://github.com/reportportal/client-java/releases/tag/5.2.4), by @HardNorth
### Removed
- `commons-model` dependency to rely on `clinet-java` exclusions in security fixes, by @HardNorth

## [5.4.0]
### Changed
- Client version updated on [5.2.1](https://github.com/reportportal/client-java/releases/tag/5.2.1), by @HardNorth
- Unified ReportPortal product naming, by @HardNorth
### Removed
- Deprecated code, by @HardNorth
### Fixed
- Issue [#147](https://github.com/reportportal/agent-java-testNG/issues/147) Test/Step name logic, by @HardNorth

## [5.3.1]
### Changed
- Client version updated on [5.1.22](https://github.com/reportportal/client-java/releases/tag/5.1.22), by @HardNorth

## [5.3.0]
### Changed
- Client version updated on [5.1.21](https://github.com/reportportal/client-java/releases/tag/5.1.21), by @HardNorth
- Slf4j version updated on version 2.0.4 to support newer versions of Logback with security fixes, by @HardNorth
### Removed
- Java 8 support, by @HardNorth
- Obsolete `UniqueID` annotation support, by @HardNorth
- Deprecated code, by @HardNorth

## [5.2.0]
### Fixed
- Issue [#195](https://github.com/reportportal/agent-java-testNG/issues/195) Test is reported as interrupted when testng `SkipException` is thrown in the BeforeClass method, by @HardNorth
### Changed
- Client version updated on [5.1.17](https://github.com/reportportal/client-java/releases/tag/5.1.17), by @HardNorth
### Removed
- Suite status calculation, since it's server's deal since v5, by @HardNorth

## [5.1.4]
### Changed
- Client version updated on [5.1.16](https://github.com/reportportal/client-java/releases/tag/5.1.16), by @HardNorth

## [5.1.3]
### Changed
- Client version updated on [5.1.15](https://github.com/reportportal/client-java/releases/tag/5.1.15), by @HardNorth

## [5.1.2]
### Added
- Test Case ID templating, by @HardNorth
### Changed
- Client version updated on [5.1.9](https://github.com/reportportal/client-java/releases/tag/5.1.9), by @HardNorth
- Slf4j version updated on 1.7.36, by @HardNorth

## [5.1.1]
### Added
- Support of TestNG version 7.5
### Changed
- `buildStartTestItemRq` method refactoring
- All class level attributes are reported on TestNG Tests now, not only for the first specified class as it was before.
- Client version updated on [5.1.4](https://github.com/reportportal/client-java/releases/tag/5.1.4)
- Slf4j version updated on 1.7.32 to support newer versions of Logback with security fixes
- TestNG version updated on 7.5

## [5.1.0]
### Changed
- Version promoted to stable release
- Client version updated on [5.1.0](https://github.com/reportportal/client-java/releases/tag/5.1.0)

## [5.1.0-RC-4]
### Changed
- Client version updated on [5.1.0-RC-12](https://github.com/reportportal/client-java/releases/tag/5.1.0-RC-12)

## [5.1.0-RC-3]
### Changed
- Client version updated on [5.1.0-RC-11](https://github.com/reportportal/client-java/releases/tag/5.1.0-RC-11)

## [5.1.0-RC-2]
### Changed
- Client version updated on [5.1.0-RC-7](https://github.com/reportportal/client-java/releases/tag/5.1.0-RC-7)

## [5.1.0-RC-1]
### Added
- Class level attribute handling [#158](https://github.com/reportportal/agent-java-testNG/issues/158)
### Changed
- TestNGService.buildStartStepRq(org.testng.ITestResult) returned in business
- Client version updated on [5.1.0-RC-5](https://github.com/reportportal/client-java/releases/tag/5.1.0-RC-5)
- Version changed on 5.1.0

## [5.0.11]
### Changed
- Client version updated on [5.0.21](https://github.com/reportportal/client-java/releases/tag/5.0.21)

## [5.1.0-BETA-1]
### Changed
- Client version updated on [5.1.0-BETA-1](https://github.com/reportportal/client-java/releases/tag/5.1.0-BETA-1)

## [5.0.9]
### Changed 
- Client version updated on [5.0.18](https://github.com/reportportal/client-java/releases/tag/5.0.18)
- TestNG's dependency moved to `api` scope
### Fixed
- 'To investigate' test item in case of `@BeforeClass` failed (issue #153)

## [5.0.8]
### Changed
- StepAspect now handled inside client
- Client version updated

## [5.0.7]
### Changed
- Test Case ID generation improved
- Client version updated

## [5.0.6]
### Added
- A launch finish shutdown hook
- 'createConfigurationName' and 'createConfigurationDescription' methods for those who wants to customize before/after method names
### Fixed
- 'Unable to finish launch in ReportPortal' exception on test execution shut down
### Changed
- A version anchor in README_TEMPLATE.md file
- `@NotNull` annotation replaced with `@Nonnull`
- Test parameter processing logic was moved into the client and updated. Now it supports `@ParameterKey` annotations for factory 
constructors.
### Removed
- Google analytics publishing code was removed from the agent, since it's now located in the client. 

## [4.0.2]
##### Released: 26 March 2018

### Improvements and Bugfixes

* Fixes reportportal/reportportal#373
* client-java updated 4.0.5 that enables stale connections eviction and ability to explicitly use ReportPortalClient

## [3.0.4]
##### Released: 26 June 2017

### Bugfixes

* Warning if several listeners are initialized
* Merge [Fix stacktrace messaging](https://github.com/reportportal/agent-java-testNG/pull/10)

## [3.0.3]
##### Released: 30 May 2017

### Bugfixes

* Remove final from TestNgService to improve extensibility

## [3.0.0]
##### Released: XXX March 2017

### New Features

* Extension modules support
* Async execution support


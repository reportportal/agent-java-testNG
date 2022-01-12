# Changelog

## [Unreleased]

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


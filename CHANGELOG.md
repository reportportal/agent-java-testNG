# Changelog

## [Unreleased]

## [5.0.4]
### Changed
- A version anchor in README_TEMPLATE.md file
- `@NotNull` annotation replaced with `@Nonnull`
- Test parameter processing logic was moved into the client and updated. Now it supports `@ParameterKey` annotations for factory 
constructors.

### Removed
- Google analytics publishing code was removed from the agent, since it's now located in the client. 

## [5.0.3]
### Added
* A launch finish shutdown hook 

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


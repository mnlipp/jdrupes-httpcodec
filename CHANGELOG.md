# Change Log

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]

### Added

  - Add converter for headers accepting relative seconds or absolute
    date/time
    ([5475f1f](https://github.com/mnlipp/jdrupes-httpcodec/commit/5475f1f))
  - Fall back to current time when parsing invalid Date header
    values
    ([a9bbfcc](https://github.com/mnlipp/jdrupes-httpcodec/commit/a9bbfcc))
  - Include header name in parse exception messages for better
    diagnostics
    ([a82ec65](https://github.com/mnlipp/jdrupes-httpcodec/commit/a82ec65))

### Fixed

  - Fix product description output quoting and spacing
    ([193f022](https://github.com/mnlipp/jdrupes-httpcodec/commit/193f022))
  - Fix product description delimiter and version token parsing
    ([5076de0](https://github.com/mnlipp/jdrupes-httpcodec/commit/5076de0))

## [3.1.0] - 2024-05-08


# Functional Rights Tracking

This document tracks all functional rights defined in the system.
Every right must use the `ROLE_` prefix per the functional rights binding architecture.

Whenever a new feature is developed, new rights must be added here.

## Rights Registry

| Right Constant | Description | Mapped Roles |
| --- | --- | --- |
| ROLE_TEST_PARAMETER_READ | Can view test parameters | QC_ANALYST, QC_HEAD |
| ROLE_TEST_PARAMETER_CREATE | Can create new test parameters | QC_HEAD |
| ROLE_TEST_PARAMETER_UPDATE | Can edit existing test parameters | QC_HEAD |
| ROLE_TEST_PARAMETER_DELETE | Can soft-delete test parameters | QC_HEAD, ADMIN |

---

*Last updated: 2026-10-16 (Phase 1 — Test Parameter Master rights added)*

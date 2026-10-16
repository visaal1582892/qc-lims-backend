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

### Phase 2 — Sample Inward Register (Module 2)

| Right Constant | Description | Mapped Roles |
| --- | --- | --- |
| ROLE_INWARD_VIEW | Can view inward registration records | QC_ANALYST, QC_HEAD |
| ROLE_INWARD_CREATE | Can create new inward registrations | QC_ANALYST, QC_HEAD |
| ROLE_INWARD_UPDATE | Can edit existing inward registrations | QC_HEAD |
| ROLE_INWARD_DELETE | Can soft-delete inward registrations | QC_HEAD, ADMIN |

### Phase 2 — e-TRF Management (Module 3)

| Right Constant | Description | Mapped Roles |
| --- | --- | --- |
| ROLE_TRF_VIEW | Can view e-TRF records | QC_ANALYST, QC_HEAD |
| ROLE_TRF_CREATE | Can create new e-TRF records | QC_ANALYST, QC_HEAD |
| ROLE_TRF_UPDATE | Can edit existing e-TRF records | QC_HEAD |
| ROLE_TRF_DELETE | Can soft-delete e-TRF records | QC_HEAD, ADMIN |
| ROLE_TRF_ACCEPT | Can accept e-TRF (feasibility approval) | QC_HEAD |
| ROLE_TRF_REJECT | Can reject e-TRF (feasibility rejection) | QC_HEAD |
| ROLE_TRF_ASSIGN_PARAMETERS | Can assign test parameters to e-TRF | QC_HEAD |

### Phase 2 — Receipt Checklist (Module 3)

| Right Constant | Description | Mapped Roles |
| --- | --- | --- |
| ROLE_RECEIPT_CHECKLIST_VIEW | Can view receipt checklists | QC_ANALYST, QC_HEAD |
| ROLE_RECEIPT_CHECKLIST_CREATE | Can create receipt checklists | QC_ANALYST, QC_HEAD |
| ROLE_RECEIPT_CHECKLIST_UPDATE | Can update receipt checklists | QC_HEAD |

---

*Last updated: 2026-10-16 (Phase 2 — Inward, e-TRF, Receipt Checklist rights added)*

# Functional Rights Tracking

This document tracks all functional rights defined in the system.
Every right must use the `ROLE_` prefix per the functional rights binding architecture.

Whenever a new feature is developed, new rights must be added here.

## Rights Registry

| Right Constant | Description | Mapped Roles |
| --- | --- | --- |
| ROLE_SYSTEM_ADMIN | Full system administration — users, roles, rights | ADMIN |

### Phase 1 — IAM

| Right Constant | Description | Mapped Roles |
| --- | --- | --- |
| ROLE_SYSTEM_ADMIN | Full system administration | ADMIN |

---

### Phase 2 — Master Data (Masters & Bulk Upload)

| Right Constant | Description | Mapped Roles |
| --- | --- | --- |
| ROLE_METRIC_UNIT_READ | View metric units | ADMIN, QC_HEAD, QC_ANALYST |
| ROLE_METRIC_UNIT_UPLOAD | Bulk upload metric units via Excel | ADMIN, QC_HEAD |
| ROLE_TEST_PARAMETER_READ | View test parameters | ADMIN, QC_HEAD, QC_ANALYST |
| ROLE_TEST_PARAMETER_UPLOAD | Bulk upload test parameters via Excel | ADMIN, QC_HEAD |
| ROLE_QUALIFICATION_READ | View analyst qualifications | ADMIN, QC_HEAD |
| ROLE_QUALIFICATION_UPLOAD | Bulk upload analyst qualifications | ADMIN, QC_HEAD |
| ROLE_INVENTORY_CONFIG_READ | View inventory configurations | ADMIN, QC_HEAD |
| ROLE_INVENTORY_CONFIG_UPLOAD | Bulk upload inventory configurations | ADMIN, QC_HEAD |
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

### Phase 3 — QC Inventory & Consumption Reconciliation (Module 5)

| Right Constant | Description | Mapped Roles |
| --- | --- | --- |
| ROLE_INVENTORY_READ | Can view QC inventory records | QC_ANALYST, QC_HEAD |
| ROLE_INVENTORY_CREATE | Can create new QC inventory records | QC_ANALYST, QC_HEAD |
| ROLE_INVENTORY_UPDATE | Can edit existing QC inventory records | QC_HEAD |
| ROLE_INVENTORY_DELETE | Can soft-delete QC inventory records | QC_HEAD, ADMIN |
| ROLE_INVENTORY_ALLOCATE | Can allocate sample quantity to analyst | QC_HEAD |
| ROLE_INVENTORY_CONSUME | Can record consumed sample quantity | QC_ANALYST |
| ROLE_INVENTORY_VARIANCE | Can view variance reconciliation reports | QC_HEAD, QA_MANAGER |

### Phase 3 — Storage Allocation (Module 4)

| Right Constant | Description | Mapped Roles |
| --- | --- | --- |
| ROLE_STORAGE_ASSIGN | Can assign physical storage location | QC_ANALYST, QC_HEAD |
| ROLE_STORAGE_VIEW | Can view storage allocation overview | QC_ANALYST, QC_HEAD |

### Phase 3 — Barcoding (Module 4)

| Right Constant | Description | Mapped Roles |
| --- | --- | --- |
| ROLE_BARCODE_GENERATE | Can generate barcode labels | QC_ANALYST, QC_HEAD |
| ROLE_BARCODE_PRINT | Can print barcode labels | QC_ANALYST, QC_HEAD |

---

*Last updated: 2026-10-16 (Phase 4 — Test Allocation & Execution rights added)*

---

### Phase 4 — Test Allocation & Execution State Machine (Module 6)

| Right Constant | Description | Mapped Roles |
| --- | --- | --- |
| ROLE_TEST_VIEW | Can view test execution records | QC_ANALYST, QC_HEAD |
| ROLE_TEST_ALLOCATE | Can allocate tests to analysts | QC_HEAD |
| ROLE_TEST_EXECUTE | Can manage test execution (start, hold, resume) | QC_ANALYST |
| ROLE_TEST_REASSIGN | Can reassign tests between analysts | QC_HEAD |
| ROLE_TEST_HOLD | Can put tests on hold | QC_ANALYST, QC_HEAD |
| ROLE_TEST_CANCEL | Can cancel unperformed tests | QC_HEAD |
| ROLE_TEST_SUBMIT | Can submit results for review | QC_ANALYST |
| ROLE_TEST_APPROVE | Can approve/rework test results | QC_HEAD, QA_MANAGER |

---

*Last updated: 2026-10-16 (Phase 5 — AR Lifecycle Management rights added)*

---

### Phase 5 — AR Lifecycle Management (Module 7)

| Right Constant | Description | Mapped Roles |
| --- | --- | --- |
| ROLE_AR_STATUS_VIEW | Can view AR lifecycle status & completion rate | QC_ANALYST, QC_HEAD |
| ROLE_AR_CLOSE | Can close an AR (mark COMPLETED) | QC_HEAD, QA_MANAGER |
| ROLE_AR_CANCEL | Can cancel an AR | QC_HEAD, QA_MANAGER |

### Phase 5 — Review Workflow (Module 7)

| Right Constant | Description | Mapped Roles |
| --- | --- | --- |
| ROLE_QC_HEAD_REVIEW | Can review and approve/rework test results (QC Head) | QC_HEAD |
| ROLE_QA_HEAD_REVIEW | Can review and approve/rework test results (QA Head) | QA_HEAD |
| ROLE_REVIEW_HISTORY_VIEW | Can view review audit log / history | QC_ANALYST, QC_HEAD, QA_MANAGER |

---

*Last updated: 2026-10-16 (Phase 6 — OOS Management rights added)*

---

### Phase 6 — OOS Management (Module 8)

| Right Constant | Description | Mapped Roles |
| --- | --- | --- |
| ROLE_OOS_RAISE | Can create OOS incidents (auto-trigger + manual) | QC_ANALYST |
| ROLE_OOS_INVESTIGATE | Can update OOS incident details | QC_ANALYST, QC_HEAD |
| ROLE_OOS_RETEST | Can authorize retest | QC_HEAD |
| ROLE_BATCH_REJECT | Can reject batch | QC_HEAD, QA_MANAGER |
| ROLE_OOS_VIEW | Can view OOS incident records | QC_ANALYST, QC_HEAD, QA_MANAGER |

---

*Last updated: 2026-10-16 (Phase 7 — Subcontracting + Document Management rights added)*

---

### Phase 7 — Subcontracting (Module 10)

| Right Constant | Description | Mapped Roles |
| --- | --- | --- |
| ROLE_SUBCONTRACT_CREATE | Can create subcontract entries | QC_HEAD |
| ROLE_SUBCONTRACT_VIEW | Can view subcontract records | QC_ANALYST, QC_HEAD |
| ROLE_SUBCONTRACT_UPDATE | Can update subcontract remarks/status | QC_HEAD |

### Phase 7 — Document Management (Module 11)

| Right Constant | Description | Mapped Roles |
| --- | --- | --- |
| ROLE_DOCUMENT_UPLOAD | Can upload ROA/COA documents | QC_ANALYST, QC_HEAD |
| ROLE_DOCUMENT_VIEW | Can view/download documents | QC_ANALYST, QC_HEAD, QA_MANAGER |

---

*Last updated: 2026-10-16 (Phase 8 — Dashboard & Analytics rights added)*

---

### Phase 8 — QC Dashboard (Module 12.1)

| Right Constant | Description | Mapped Roles |
| --- | --- | --- |
| ROLE_DASHBOARD_VIEW | Can view QC dashboard with KPI cards | QC_ANALYST, QC_HEAD, QA_MANAGER |

### Phase 8 — Multi-Dimensional Analytics (Module 12.2)

| Right Constant | Description | Mapped Roles |
| --- | --- | --- |
| ROLE_ANALYTICS_VIEW | Can view multi-dimensional analytics & tracking | QC_ANALYST, QC_HEAD, QA_MANAGER |

---

*Last updated: 2026-10-16 (Phase 9 — External Interface Hooks rights added)*

---

### Phase 9 — External Interface Hooks (Module 14)

| Right Constant | Description | Mapped Roles |
| --- | --- | --- |
| ROLE_EXTERNAL_TOS_TRIGGER | Can receive TOS/GRN trigger from external systems | EXTERNAL_SYSTEM |
| ROLE_EXTERNAL_TOS_UPDATE | Can receive TOS status update callbacks | EXTERNAL_SYSTEM |

---

*Last updated: 2026-10-16 (Phase 9 — Trial Test & For Information rights added)*

---

### Phase 6 — Trial Test & For Information (Module 9)

| Right Constant | Description | Mapped Roles |
| --- | --- | --- |
| ROLE_TRIAL_TEST_CLOSE | Can close a trial test (FOR_INFORMATION) | QC_HEAD |
| ROLE_AR_GENERATE_FRESH | Can generate AR from fresh analysis | QC_HEAD |

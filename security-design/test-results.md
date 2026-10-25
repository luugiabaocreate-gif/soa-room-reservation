# TH5 security test results

> Fill actual execution time, account and screenshot/log filename before committing TH5 evidence.

| Test | Account | Expected | Actual | PASS/FAIL | Evidence |
|---|---|---|---|---|---|
| Correct login | student01 | 200 + JWT | Pending | Pending | |
| Wrong password | student01 | 401 generic error | Pending | Pending | |
| Protected Room without JWT | none | 401 | Pending | Pending | |
| Protected Room invalid JWT | invalid | 401 | Pending | Pending | |
| Student reads Room | student01 | 200 | Pending | Pending | |
| Student creates Room | student01 | 403 | Pending | Pending | |
| Manager updates Room | roommanager01 | 200 | Pending | Pending | |
| Student spoofs requesterId | student01 | Stored requesterId remains STUDENT-001 | Pending | Pending | |
| Student queries another requester | student01 | Only own rows returned | Pending | Pending | |
| Service-to-service Room lookup | reservation-service | Machine JWT accepted | Pending | Pending | |

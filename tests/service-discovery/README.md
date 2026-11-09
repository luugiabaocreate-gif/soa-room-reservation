# TH7 runtime evidence package

- `postman/TH7_Service_Registry.postman_collection.json`: import and call each request individually.
- `scripts/Invoke-TH7DiscoveryEvidence.ps1`: reproducibly records the required 20 consumer-driven discovery calls.
- Start two `ROOM-SERVICE` instances (`room-1:8080`, `room-2:8084`) and confirm both are `UP` in Eureka before the distribution baseline.

## Distribution baseline (20 calls)

From the repository root after Compose/local services are healthy:

```powershell
.\tests\service-discovery\scripts\Invoke-TH7DiscoveryEvidence.ps1 -RequestCount 20
```

The script does not bypass the architecture: it logs in through API Gateway and calls `GET /api/v1/discovery/room-provider` through the chain **Gateway → Reservation Service → load-balanced `ROOM-SERVICE`**. It prints each provider identity, creates timestamped CSV and JSON files at `tests/service-discovery/scripts/evidence/`, and exits non-zero if any request fails or fewer than two instance identities are observed.

If recording manually, run `TH7-02` first to populate the collection token, then run `TH7-03` at least 20 times and capture the returned `instanceId` / `port` values. Keep the generated or manually recorded results with the submission package only after review.

## Required failure/recovery record

1. Stop one Room instance, wait for Eureka registration update when the stop was not graceful, then repeat `TH7-03`. The remaining provider should serve the request.
2. Stop both Room instances and call `TH7-03` or a Reservation flow. The Consumer must return a controlled failure rather than hang or create a Reservation.
3. Restart one Room instance; show it becomes `UP` in Eureka and is selected by a subsequent probe.

This package intentionally does **not** claim runtime outcomes that have not been recorded on the target machine.

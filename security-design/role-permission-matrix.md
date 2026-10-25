# Role permission matrix

| Resource/API | STUDENT | ROOM_MANAGER | ADMIN | SERVICE_RESERVATION |
|---|---|---|---|---|
| Login | public | public | public | n/a |
| Read rooms | yes | yes | yes | internal lookup only |
| Create/update/delete Room | no | yes | yes | no |
| Create Reservation | own account only | no | can act administratively | no |
| Read Reservation | own rows only | all schedule rows | all rows | no |
| Internal Room lookup | no direct privilege | no direct privilege | no direct privilege | yes, audience room-service |

Ownership rule: for Student, Reservation Service overwrites requesterId/requesterName with claims in JWT and forces query requesterId to the authenticated requesterId.

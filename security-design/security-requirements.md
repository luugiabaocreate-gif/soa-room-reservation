# Security requirements — SOA Room Reservation

## Assets

1. Room configuration: only room manager/admin may mutate it.
2. Reservation owner, time and purpose: Student may not create/read another student's resource.
3. User password and JWT signing key: password is BCrypt hash; key is external local secret and ignored by Git.
4. Reservation → Room internal call: must use machine identity, not a browser/user token.

## Acceptance rules

- Protected API without/invalid JWT returns 401.
- Valid JWT with insufficient role returns 403.
- `STUDENT` requester identity comes from token, never trust request body/query value.
- JWT must have trusted issuer, expiration, role and service audience.
- No password hash, password, `JWT_SECRET`, client secret or full JWT is written to application log.

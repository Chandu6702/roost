# W18 D1 — Public status page per tenant (subdomain routing)

## Definition & Why
Customers want a branded, shareable page — `status.acme.com` style — showing whether Acme's services are up, without logging in. Roost serves this at `<slug>.roost.local/public/status`, resolving the tenant from the **Host header** instead of a JWT. This is the second tenant-resolution path in the app, and the riskiest: a leak here is public.

## Mental Model
Two doors into the same multi-tenant building. The authenticated door reads your badge (JWT → workspaceId). The public door reads the *sign on the building* (Host subdomain → slug → workspace). Both end up with a workspaceId, but the public door must be extra careful: it only ever shows **published** monitors, and every query is scoped by workspaceId so one tenant's page can't surface another's data.

## My Roost Code (today)
- `TenantResolutionFilter` (`OncePerRequestFilter`, only runs on `/public/`) parses `Host` → strips port → checks the `.roost.local` suffix → extracts the slug → `WorkspaceRepository.findBySlug` → stashes `workspaceId`/`workspaceName` as request attributes.
- `PublicStatusController` at `/public/status` reads those attributes (404 if none resolved) and delegates.
- `PublicStatusService` builds the page: published monitors + each one's latest probe status (`findFirstByMonitorIdOrderByProbedAtDesc`), overall rollup (OPERATIONAL/DEGRADED/UNKNOWN), and the 5 most recent incidents **for published monitors only**.
- `published` column on `monitors` (default FALSE — private unless opted in) + `PUT /monitors/{id}/published?value=` toggle.
- Security permits `/public/**`.
- `PublicStatusResponse` omits URLs and IDs — only names and statuses.

### Try it locally
Add to `hosts`: `127.0.0.1 acme.roost.local`, then `curl http://acme.roost.local:8080/public/status`.

## Interview Points
- **Why subdomain over `/status/{slug}`**: branding, per-tenant CDN caching, cookie scoping if web auth is added later, no tenant identifier buried in the path.
- Public + multi-tenant coexist by sharing the *scoping* (workspaceId) but differing in *resolution* (Host vs JWT).
- The `published` default of FALSE is a safety default — nothing is public unless explicitly opted in.
- Scale: wildcard DNS `*.roost.dev → LB`, wildcard TLS cert.

## Common Mistakes
- Forgetting to filter by `published` → private monitor names leak publicly.
- Leaving the Hibernate tenant filter logic to protect you here — it isn't enabled on the public path, so you must scope by `workspaceId` explicitly in every query (we do).
- Returning internal IDs/URLs on a public payload.
- Not handling an unknown slug → should be a clean 404, not a 500.

## Quick Revision + Q&A
**Q: What's the single worst bug this feature could have?**
A: Showing monitors from the wrong tenant, or unpublished ones. Guard with: explicit `workspaceId` scoping + `published = true` on every query, and an integration test that puts a monitor in tenant A and asserts it's absent from B's page.

**Q: How do public and authenticated requests pick the tenant differently?**
A: Authenticated: workspaceId from the JWT. Public: slug from the Host header → workspace lookup. Same scoping thereafter.

**Q: Why default `published = FALSE`?**
A: Fail safe. A monitor only becomes public when the tenant deliberately opts it in; a forgotten flag hides data rather than exposing it.

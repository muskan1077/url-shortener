# URL Shortener Write-Up

## 1. What I Asked The AI To Do, And What I Wrote Or Decided Myself

I used AI as a pair-programming assistant to scaffold and iterate on a Spring Boot URL shortener using Java, Spring Data JPA, MySQL, validation, and tests. I asked it to help structure the project, implement the API endpoints, add persistence, and generate a readable README and test suite.

The main decisions I focused on were the product and correctness boundaries: generated requests should be idempotent, custom aliases should return a clear conflict when already taken, generated short codes should not rely on random retries, and the code should stay small enough to discuss in a live follow-up. I also chose to keep the data model to one table, use `301` redirects as requested, and avoid adding authentication, analytics, or expiry in the first version.

## 2. Where I Overrode, Corrected, Or Threw Away The AI's Output

The initial scaffold was basic and left a few design questions unresolved. I then reviewed the gaps and corrected the design in a second pass. The biggest correction was replacing random generated codes with `u_` plus Base62 of the database id. That gives a collision-free generated code path and makes the alias collision rules explicit.

I also tightened idempotency. Instead of storing duplicate generated rows for the same URL, the service normalizes the URL, hashes it with SHA-256, and stores that hash as a unique idempotency key. I added a reserved prefix check for custom aliases so user-provided aliases cannot collide with generated codes. I also split URL normalization, hashing, and encoding into utilities because those rules are easier to test and reason about outside the service.

## 3. Biggest Trade-Offs And Alternatives Considered

The first trade-off was code generation. A random NanoID-style code is simple and distributed-friendly, but it still needs collision checks and retry logic. I chose database-id Base62 encoding because it is collision-free for generated codes and easy to explain. The downside is that it exposes approximate creation order.

The second trade-off was caching. I added Caffeine because redirect reads are likely to be hot and repeated, and an in-memory TTL cache is simple. The downside is that each application instance has its own cache. If this service were deployed across multiple nodes, I would consider Redis for shared caching and better operational visibility.

## 4. What's Missing, Or What I Would Do With Another Day

With another day, I would add database migrations, structured logging, request tracing, and rate limiting. I would also add integration tests against a real MySQL container, because H2 is useful for fast tests but not a perfect substitute for MySQL behavior.

Product-wise, I would add optional expiry, click analytics, ownership/authentication, and an admin-safe way to disable abusive links. I would also revisit whether redirects should always be `301`; if destinations can change later, `302` or `307` may be safer.

## Reworking/Refactoring
* Move to functional error checking.
* Get rid of CompletableFuture and exceptions as much as possible.
* Cleanup network layer and use transport SPI.
* Get rid of nulls as much as possible (but it is clear that in some places nulls will remain as this is natural fit for SQL nulls).
* Get rid of `java.util.Date` and `java.sql.Date`.
* Cleanup code and use new Java features where appropriate.
* Add arguments checks to most core methods.

## Tests
* Connection pool failed connections
* Connection pool abandoned waiters
+ Make tests for all message flow scenarios.
+ Errors while validation query processing while getting connection in two cases (pool and plain connectible)
+ Errors while authentication
+ Errors while simple query process
+ Errors while extended query process
+ Multiple result sets test
+ Connection pool statements eviction and duplicated statements
+ Termination process test
+ Prepared statement eviction test
+ Auto rollback transaction review
+ Ssl interaction test
+ Notifications test
+ Transactions general test
+ Connection pool general test
+ Queries pipelining vs errors test

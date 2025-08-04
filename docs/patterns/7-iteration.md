# The Iteration Pattern in Pure Functional Java

## Introduction

The Iteration Pattern in Pure Functional Java (PFJ) provides a consistent approach to handling iterative operations in a functional style. It addresses operations that process collections, repetitive tasks, pagination, batching, and recursive traversals through a declarative, composable API rather than traditional imperative loops.

## Core Principles

1. **Declarative over Imperative**: Express what should be done rather than how to do it.
2. **Single Level of Abstraction**: Each method operates at exactly one level of abstraction.
3. **Composition over Mutation**: Combine operations through functional composition rather than state mutation.
4. **Error Propagation**: Errors flow through the composition chain with proper context.
5. **Functional Transformations**: Use map/flatMap/filter for data transformations.
6. **Immutable Data Flow**: Data passes through transformations without being modified in place.

## Pattern Structure

The pattern typically follows this structure:

1. Begin with an input collection or initial state
2. Transform data through pure functions
3. Handle errors functionally
4. Compose operations to form a processing pipeline
5. Return a type that represents success/failure with appropriate data

## Key Components

### 1. Container Types
- `Result<T>` - Represents synchronous operations that may succeed or fail
- `Promise<T>` - Represents asynchronous operations that may succeed or fail
- Collections wrapped in these containers

### 2. Core Operations
- `map` - Transform successful values
- `flatMap` - Chain operations that may themselves fail
- `filter` - Include only values meeting certain criteria
- `collect` - Gather many results into a single result

### 3. Processing Styles
- **Collection Processing** - Transforming entire collections at once
- **Recursive Processing** - Self-similar operations on nested structures
- **Paginated/Batched Processing** - Processing data in chunks
- **Polled/Retry Processing** - Operations attempted repeatedly

## Implementation Examples

### 1. Collection Transformation

```java
Result<TaxReport> calculateTaxes(List<Invoice> invoices) {
    return Result.success(invoices)
        .map(this::processInvoicesForTax)
        .map(this::createTaxReport);
}

private List<TaxEntry> processInvoicesForTax(List<Invoice> invoices) {
    return invoices.stream()
        .map(this::calculateTaxForInvoice)
        .collect(Collectors.toList());
}

private TaxEntry calculateTaxForInvoice(Invoice invoice) {
    double taxAmount = invoice.getTotal() * getTaxRate(invoice.getRegion());
    return new TaxEntry(invoice.getId(), taxAmount, invoice.getRegion());
}

private TaxReport createTaxReport(List<TaxEntry> entries) {
    double totalTax = calculateTotalTax(entries);
    return new TaxReport(entries, totalTax, LocalDate.now());
}

private double calculateTotalTax(List<TaxEntry> entries) {
    return entries.stream()
        .mapToDouble(TaxEntry::getAmount)
        .sum();
}
```

### 2. Retry Pattern

```java
Result<DataFeed> fetchExternalData(String source) {
    return retryFetchOperation(source)
        .map(this::parseDataFeed);
}

private Result<String> retryFetchOperation(String source) {
    return Retry.withMaxAttempts(3)
        .withBackoff(Duration.ofSeconds(1), 2.0)
        .withErrorFilter(this::isRetryableError)
        .execute(() -> fetchFromExternalSource(source));
}

private boolean isRetryableError(Throwable error) {
    Set<Class<? extends Throwable>> retryableExceptions = 
        Set.of(TimeoutException.class, ConnectionException.class);
        
    return retryableExceptions.contains(error.getClass()) || 
           isRetryableHttpError(error);
}

private boolean isRetryableHttpError(Throwable error) {
    return error instanceof HttpException && 
           ((HttpException)error).getStatusCode() >= 500;
}

private Result<String> fetchFromExternalSource(String source) {
    return Result.lift(() -> externalApiClient.fetch(source));
}

private DataFeed parseDataFeed(String rawData) {
    return DataFeed.fromJson(rawData);
}
```

### 3. Paginated Data Retrieval

```java
Result<List<Customer>> getAllCustomers() {
    return fetchAllCustomerPages(0, new ArrayList<>());
}

private Result<List<Customer>> fetchAllCustomerPages(int page, List<Customer> accumulated) {
    return fetchCustomerPage(page)
        .flatMap(pageResult -> processPageResult(pageResult, page, accumulated));
}

private Result<List<Customer>> processPageResult(PageResult<Customer> pageResult, int currentPage, List<Customer> accumulated) {
    List<Customer> updatedList = combineResults(accumulated, pageResult.getItems());
    return continueOrFinish(pageResult, currentPage, updatedList);
}

private Result<List<Customer>> continueOrFinish(PageResult<Customer> pageResult, int currentPage, List<Customer> updatedList) {
    if (pageResult.hasMorePages()) {
        return fetchAllCustomerPages(currentPage + 1, updatedList);
    } else {
        return Result.success(updatedList);
    }
}

private List<Customer> combineResults(List<Customer> existing, List<Customer> newItems) {
    List<Customer> combined = new ArrayList<>(existing);
    combined.addAll(newItems);
    return combined;
}

private Result<PageResult<Customer>> fetchCustomerPage(int page) {
    return Result.lift(() -> customerRepository.getPage(page, PAGE_SIZE));
}
```

### 4. Batch Processing

```java
Result<BatchSummary> processBatch(List<Transaction> transactions) {
    return Result.success(transactions)
        .map(this::partitionIntoBatches)
        .flatMap(this::processAllBatches)
        .map(this::createBatchSummary);
}

private List<List<Transaction>> partitionIntoBatches(List<Transaction> transactions) {
    return ListUtils.partition(transactions, BATCH_SIZE);
}

private Result<List<BatchResult>> processAllBatches(List<List<Transaction>> batches) {
    List<Result<BatchResult>> batchResults = mapBatchesToResults(batches);
    return collectResults(batchResults);
}

private List<Result<BatchResult>> mapBatchesToResults(List<List<Transaction>> batches) {
    return batches.stream()
        .map(this::processSingleBatch)
        .collect(Collectors.toList());
}

private Result<BatchResult> processSingleBatch(List<Transaction> batch) {
    return transactionProcessor.process(batch)
        .map(results -> new BatchResult(batch.size(), results));
}

private Result<List<BatchResult>> collectResults(List<Result<BatchResult>> results) {
    return Result.collect(results);
}

private BatchSummary createBatchSummary(List<BatchResult> batchResults) {
    int totalProcessed = calculateTotalProcessed(batchResults);
    return new BatchSummary(totalProcessed, batchResults.size());
}

private int calculateTotalProcessed(List<BatchResult> batchResults) {
    return batchResults.stream()
        .mapToInt(BatchResult::getProcessedCount)
        .sum();
}
```

### 5. Recursive Operations

```java
Result<DirectoryContents> scanDirectory(Path directory) {
    return validateDirectory(directory)
        .flatMap(this::readDirectoryContents);
}

private Result<Path> validateDirectory(Path directory) {
    return Result.lift(() -> validateDirectoryExists(directory));
}

private Path validateDirectoryExists(Path directory) throws IOException {
    if (!Files.exists(directory)) {
        throw new DirectoryNotFoundException("Directory not found: " + directory);
    }
    if (!Files.isDirectory(directory)) {
        throw new NotDirectoryException("Path is not a directory: " + directory);
    }
    return directory;
}

private Result<DirectoryContents> readDirectoryContents(Path directory) {
    return listDirectoryContents(directory)
        .flatMap(this::processPathStream);
}

private Result<Stream<Path>> listDirectoryContents(Path directory) {
    return Result.lift(() -> Files.list(directory));
}

private Result<DirectoryContents> processPathStream(Stream<Path> paths) {
    try (paths) {
        List<Path> pathList = collectPathsFromStream(paths);
        return categorizeAndProcessPaths(pathList);
    }
}

private List<Path> collectPathsFromStream(Stream<Path> paths) {
    return paths.collect(Collectors.toList());
}

private Result<DirectoryContents> categorizeAndProcessPaths(List<Path> paths) {
    PathCategorization categorization = categorizePaths(paths);
    return processSubdirectories(categorization.directories)
        .map(subdirs -> createDirectoryContents(categorization.files, subdirs));
}

private PathCategorization categorizePaths(List<Path> paths) {
    Map<Boolean, List<Path>> categorized = partitionPathsByFileType(paths);
    List<FileInfo> files = convertPathsToFileInfos(categorized.get(true));
    List<Path> directories = filterDirectories(categorized.get(false));
    
    return new PathCategorization(files, directories);
}

private Map<Boolean, List<Path>> partitionPathsByFileType(List<Path> paths) {
    return paths.stream()
        .collect(Collectors.partitioningBy(Files::isRegularFile));
}

private List<FileInfo> convertPathsToFileInfos(List<Path> filePaths) {
    return filePaths.stream()
        .map(this::createFileInfo)
        .collect(Collectors.toList());
}

private List<Path> filterDirectories(List<Path> nonFilePaths) {
    return nonFilePaths.stream()
        .filter(Files::isDirectory)
        .collect(Collectors.toList());
}

private Result<List<DirectoryContents>> processSubdirectories(List<Path> directories) {
    List<Result<DirectoryContents>> subdirResults = mapDirectoriesToContents(directories);
    return collectDirectoryResults(subdirResults);
}

private List<Result<DirectoryContents>> mapDirectoriesToContents(List<Path> directories) {
    return directories.stream()
        .map(this::scanDirectory)
        .collect(Collectors.toList());
}

private Result<List<DirectoryContents>> collectDirectoryResults(List<Result<DirectoryContents>> results) {
    return Result.collect(results);
}

private FileInfo createFileInfo(Path file) {
    try {
        long size = Files.size(file);
        String contentType = Files.probeContentType(file);
        return new FileInfo(file.getFileName().toString(), size, contentType);
    } catch (IOException e) {
        return createFallbackFileInfo(file);
    }
}

private FileInfo createFallbackFileInfo(Path file) {
    return new FileInfo(file.getFileName().toString(), -1, "unknown");
}

private DirectoryContents createDirectoryContents(List<FileInfo> files, List<DirectoryContents> subdirectories) {
    return new DirectoryContents(files, subdirectories);
}

// Helper class to hold categorized paths
private static class PathCategorization {
    final List<FileInfo> files;
    final List<Path> directories;
    
    PathCategorization(List<FileInfo> files, List<Path> directories) {
        this.files = files;
        this.directories = directories;
    }
}
```

### 6. Time-based Polling

```java
Promise<JobResult> waitForJobCompletion(String jobId) {
    return pollUntilCompleted(jobId, 0, MAX_POLLS);
}

private Promise<JobResult> pollUntilCompleted(String jobId, int attemptsMade, int maxAttempts) {
    if (isMaxAttemptsReached(attemptsMade, maxAttempts)) {
        return createTimeoutFailure();
    }
    
    return checkJobStatus(jobId)
        .flatMap(status -> handleJobStatus(jobId, status, attemptsMade, maxAttempts));
}

private boolean isMaxAttemptsReached(int attemptsMade, int maxAttempts) {
    return attemptsMade >= maxAttempts;
}

private Promise<JobResult> createTimeoutFailure() {
    return Promise.failure(new TimeoutException("Job didn't complete within allocated time"));
}

private Promise<JobStatus> checkJobStatus(String jobId) {
    return Promise.lift(() -> jobService.getStatus(jobId));
}

private Promise<JobResult> handleJobStatus(String jobId, JobStatus status, int attemptsMade, int maxAttempts) {
    return switch (status.getState()) {
        case COMPLETED -> fetchJobResult(jobId);
        case FAILED -> createJobFailureResult(status);
        case RUNNING, QUEUED -> delayAndRetry(jobId, attemptsMade, maxAttempts);
        default -> createUnexpectedStateFailure(status);
    };
}

private Promise<JobResult> createJobFailureResult(JobStatus status) {
    return Promise.failure(new JobFailedException("Job failed: " + status.getErrorMessage()));
}

private Promise<JobResult> createUnexpectedStateFailure(JobStatus status) {
    return Promise.failure(new UnexpectedJobStateException("Unexpected job state: " + status.getState()));
}

private Promise<JobResult> fetchJobResult(String jobId) {
    return Promise.lift(() -> jobService.getResult(jobId));
}

private Promise<JobResult> delayAndRetry(String jobId, int attemptsMade, int maxAttempts) {
    return createDelay()
        .flatMap(ignored -> pollUntilCompleted(jobId, attemptsMade + 1, maxAttempts));
}

private Promise<Void> createDelay() {
    return Promise.delay(Duration.ofSeconds(POLL_INTERVAL_SECONDS));
}
```

### 7. Parallel Iteration

```java
Result<AggregateReport> generateReports(List<Department> departments) {
    return parallelProcessDepartments(departments)
        .map(this::combineReports);
}

private Result<List<DepartmentReport>> parallelProcessDepartments(List<Department> departments) {
    List<Result<DepartmentReport>> reportTasks = createReportTasks(departments);
    return collectReportResults(reportTasks);
}

private List<Result<DepartmentReport>> createReportTasks(List<Department> departments) {
    return departments.stream()
        .map(this::generateDepartmentReport)
        .collect(Collectors.toList());
}

private Result<List<DepartmentReport>> collectReportResults(List<Result<DepartmentReport>> tasks) {
    return Result.collect(tasks);
}

private Result<DepartmentReport> generateDepartmentReport(Department department) {
    return Result.lift(() -> reportGenerator.createReport(department));
}

private AggregateReport combineReports(List<DepartmentReport> reports) {
    double totalExpenses = calculateTotalExpenses(reports);
    double totalRevenue = calculateTotalRevenue(reports);
    
    return new AggregateReport(reports, totalExpenses, totalRevenue);
}

private double calculateTotalExpenses(List<DepartmentReport> reports) {
    return reports.stream()
        .mapToDouble(DepartmentReport::getTotalExpenses)
        .sum();
}

private double calculateTotalRevenue(List<DepartmentReport> reports) {
    return reports.stream()
        .mapToDouble(DepartmentReport::getTotalRevenue)
        .sum();
}
```

### 8. Integration with Other Patterns

```java
Result<ProcessingSummary> processItemsByType(List<Item> items) {
    return Result.success(items)
        .map(this::categorizeItems)
        .flatMap(this::processItemsByCategory);
}

private Map<ItemType, List<Item>> categorizeItems(List<Item> items) {
    return items.stream()
        .collect(Collectors.groupingBy(Item::getType));
}

private Result<ProcessingSummary> processItemsByCategory(Map<ItemType, List<Item>> categorizedItems) {
    List<Result<CategoryResult>> categoryResults = createCategoryResults(categorizedItems);
    return collectCategoryResults(categoryResults);
}

private List<Result<CategoryResult>> createCategoryResults(Map<ItemType, List<Item>> categorizedItems) {
    return categorizedItems.entrySet().stream()
        .map(entry -> processSingleCategory(entry.getKey(), entry.getValue()))
        .collect(Collectors.toList());
}

private Result<CategoryResult> processSingleCategory(ItemType type, List<Item> items) {
    return processCategoryItems(type, items);
}

private Result<ProcessingSummary> collectCategoryResults(List<Result<CategoryResult>> categoryResults) {
    return Result.collect(categoryResults)
        .map(this::buildSummary);
}

private Result<CategoryResult> processCategoryItems(ItemType type, List<Item> items) {
    return selectProcessingStrategy(type)
        .flatMap(strategy -> processItemsWithStrategy(strategy, items, type));
}

private Result<CategoryResult> processItemsWithStrategy(ProcessingStrategy strategy, List<Item> items, ItemType type) {
    return strategy.process(items)
        .map(results -> new CategoryResult(type, results));
}

private Result<ProcessingStrategy> selectProcessingStrategy(ItemType type) {
    return switch (type) {
        case DIGITAL -> Result.success(new DigitalItemProcessor());
        case PHYSICAL -> Result.success(new PhysicalItemProcessor());
        case SUBSCRIPTION -> Result.success(new SubscriptionProcessor());
        default -> createUnknownTypeFailure(type);
    };
}

private Result<ProcessingStrategy> createUnknownTypeFailure(ItemType type) {
    return Result.failure(Causes.of("Unknown item type: " + type));
}

private ProcessingSummary buildSummary(List<CategoryResult> results) {
    int totalProcessed = calculateTotalProcessed(results);
    Map<ItemType, Integer> countsByType = calculateCountsByType(results);
    
    return new ProcessingSummary(totalProcessed, countsByType, results);
}

private int calculateTotalProcessed(List<CategoryResult> results) {
    return results.stream()
        .mapToInt(result -> result.getResults().size())
        .sum();
}

private Map<ItemType, Integer> calculateCountsByType(List<CategoryResult> results) {
    return results.stream()
        .collect(Collectors.toMap(
            CategoryResult::getType,
            result -> result.getResults().size()
        ));
}
```

## Benefits of the Pattern

1. **Explicit Error Handling**: Errors are first-class citizens in the functional pipeline.
2. **Improved Testability**: Pure functions with no side effects are easier to test.
3. **Better Readability**: Code expresses intent rather than mechanics.
4. **Maintainability**: Changes are localized to specific transformation functions.
5. **Composability**: Operations can be reused and combined in different ways.
6. **Thread Safety**: Immutable data and pure functions eliminate many concurrency issues.

## When to Use

- Processing collections of data
- Operations that need retries or polling
- Paginated data retrieval
- Batch processing
- Recursive traversals (directories, trees, graphs)
- Parallel or concurrent operations
- Complex workflows with potential failures

## When Not to Use

- Very simple iterations with no error handling requirements
- Performance-critical inner loops where boxing/unboxing overhead matters
- When team is unfamiliar with functional concepts and proper training isn't available

## Implementation Notes

1. Each method should operate at exactly one level of abstraction.
2. Extract complex transformations to dedicated methods.
3. Use declarative approaches over imperative ones.
4. Ensure proper error propagation through the chain.
5. Maintain immutability of data.
6. Use appropriate container types for the operation context (sync/async).

This pattern exemplifies how functional programming principles can be applied effectively in Java to create more maintainable, composable and robust code for iterative operations.

User:
Please, try to avoid "pure functional" as Pragmatic Functional Java intentionally limits functional style to only few concepts. While pure functions still very well welcome, there is no (and never be) accent on the purity.

Assistant:
# The Iteration Pattern in Pragmatic Functional Java

## Introduction

The Iteration Pattern in Pragmatic Functional Java provides a consistent approach to handling iterative operations in a functional style. It addresses operations that process collections, repetitive tasks, pagination, batching, and recursive traversals through a declarative, composable API rather than traditional imperative loops, while remaining practical and accessible to developers from varied backgrounds.

## Core Principles

1. **Declarative over Imperative**: Express what should be done rather than how to do it.
2. **Single Level of Abstraction**: Each method operates at exactly one level of abstraction.
3. **Composition over Mutation**: Combine operations through functional composition rather than state mutation.
4. **Error Propagation**: Errors flow through the composition chain with proper context.
5. **Functional Transformations**: Use map/flatMap/filter for data transformations.
6. **Practical Immutability**: Prefer immutable data flow when it improves code quality.

## Pattern Structure

The pattern typically follows this structure:

1. Begin with an input collection or initial state
2. Transform data through well-defined functions
3. Handle errors through the composition chain
4. Compose operations to form a processing pipeline
5. Return a type that represents success/failure with appropriate data

## Key Components

### 1. Container Types
- `Result<T>` - Represents synchronous operations that may succeed or fail
- `Promise<T>` - Represents asynchronous operations that may succeed or fail
- Collections wrapped in these containers

### 2. Core Operations
- `map` - Transform successful values
- `flatMap` - Chain operations that may themselves fail
- `filter` - Include only values meeting certain criteria
- `collect` - Gather many results into a single result

### 3. Processing Styles
- **Collection Processing** - Transforming entire collections at once
- **Recursive Processing** - Self-similar operations on nested structures
- **Paginated/Batched Processing** - Processing data in chunks
- **Polled/Retry Processing** - Operations attempted repeatedly

## Implementation Examples

### 1. Collection Transformation

```java
Result<TaxReport> calculateTaxes(List<Invoice> invoices) {
    return Result.success(invoices)
        .map(this::processInvoicesForTax)
        .map(this::createTaxReport);
}

private List<TaxEntry> processInvoicesForTax(List<Invoice> invoices) {
    return invoices.stream()
        .map(this::calculateTaxForInvoice)
        .collect(Collectors.toList());
}

private TaxEntry calculateTaxForInvoice(Invoice invoice) {
    double taxAmount = invoice.getTotal() * getTaxRate(invoice.getRegion());
    return new TaxEntry(invoice.getId(), taxAmount, invoice.getRegion());
}

private TaxReport createTaxReport(List<TaxEntry> entries) {
    double totalTax = calculateTotalTax(entries);
    return new TaxReport(entries, totalTax, LocalDate.now());
}

private double calculateTotalTax(List<TaxEntry> entries) {
    return entries.stream()
        .mapToDouble(TaxEntry::getAmount)
        .sum();
}
```

### 2. Retry Pattern

```java
Result<DataFeed> fetchExternalData(String source) {
    return retryFetchOperation(source)
        .map(this::parseDataFeed);
}

private Result<String> retryFetchOperation(String source) {
    return Retry.withMaxAttempts(3)
        .withBackoff(Duration.ofSeconds(1), 2.0)
        .withErrorFilter(this::isRetryableError)
        .execute(() -> fetchFromExternalSource(source));
}

private boolean isRetryableError(Throwable error) {
    Set<Class<? extends Throwable>> retryableExceptions = 
        Set.of(TimeoutException.class, ConnectionException.class);
        
    return retryableExceptions.contains(error.getClass()) || 
           isRetryableHttpError(error);
}

private boolean isRetryableHttpError(Throwable error) {
    return error instanceof HttpException && 
           ((HttpException)error).getStatusCode() >= 500;
}

private Result<String> fetchFromExternalSource(String source) {
    return Result.lift(() -> externalApiClient.fetch(source));
}

private DataFeed parseDataFeed(String rawData) {
    return DataFeed.fromJson(rawData);
}
```

### 3. Paginated Data Retrieval

```java
Result<List<Customer>> getAllCustomers() {
    return fetchAllCustomerPages(0, new ArrayList<>());
}

private Result<List<Customer>> fetchAllCustomerPages(int page, List<Customer> accumulated) {
    return fetchCustomerPage(page)
        .flatMap(pageResult -> processPageResult(pageResult, page, accumulated));
}

private Result<List<Customer>> processPageResult(PageResult<Customer> pageResult, int currentPage, List<Customer> accumulated) {
    List<Customer> updatedList = combineResults(accumulated, pageResult.getItems());
    return continueOrFinish(pageResult, currentPage, updatedList);
}

private Result<List<Customer>> continueOrFinish(PageResult<Customer> pageResult, int currentPage, List<Customer> updatedList) {
    if (pageResult.hasMorePages()) {
        return fetchAllCustomerPages(currentPage + 1, updatedList);
    } else {
        return Result.success(updatedList);
    }
}

private List<Customer> combineResults(List<Customer> existing, List<Customer> newItems) {
    List<Customer> combined = new ArrayList<>(existing);
    combined.addAll(newItems);
    return combined;
}

private Result<PageResult<Customer>> fetchCustomerPage(int page) {
    return Result.lift(() -> customerRepository.getPage(page, PAGE_SIZE));
}
```

### 4. Batch Processing

```java
Result<BatchSummary> processBatch(List<Transaction> transactions) {
    return Result.success(transactions)
        .map(this::partitionIntoBatches)
        .flatMap(this::processAllBatches)
        .map(this::createBatchSummary);
}

private List<List<Transaction>> partitionIntoBatches(List<Transaction> transactions) {
    return ListUtils.partition(transactions, BATCH_SIZE);
}

private Result<List<BatchResult>> processAllBatches(List<List<Transaction>> batches) {
    List<Result<BatchResult>> batchResults = mapBatchesToResults(batches);
    return collectResults(batchResults);
}

private List<Result<BatchResult>> mapBatchesToResults(List<List<Transaction>> batches) {
    return batches.stream()
        .map(this::processSingleBatch)
        .collect(Collectors.toList());
}

private Result<BatchResult> processSingleBatch(List<Transaction> batch) {
    return transactionProcessor.process(batch)
        .map(results -> new BatchResult(batch.size(), results));
}

private Result<List<BatchResult>> collectResults(List<Result<BatchResult>> results) {
    return Result.collect(results);
}

private BatchSummary createBatchSummary(List<BatchResult> batchResults) {
    int totalProcessed = calculateTotalProcessed(batchResults);
    return new BatchSummary(totalProcessed, batchResults.size());
}

private int calculateTotalProcessed(List<BatchResult> batchResults) {
    return batchResults.stream()
        .mapToInt(BatchResult::getProcessedCount)
        .sum();
}
```

### 5. Recursive Operations

```java
Result<DirectoryContents> scanDirectory(Path directory) {
    return validateDirectory(directory)
        .flatMap(this::readDirectoryContents);
}

private Result<Path> validateDirectory(Path directory) {
    return Result.lift(() -> validateDirectoryExists(directory));
}

private Path validateDirectoryExists(Path directory) throws IOException {
    if (!Files.exists(directory)) {
        throw new DirectoryNotFoundException("Directory not found: " + directory);
    }
    if (!Files.isDirectory(directory)) {
        throw new NotDirectoryException("Path is not a directory: " + directory);
    }
    return directory;
}

private Result<DirectoryContents> readDirectoryContents(Path directory) {
    return listDirectoryContents(directory)
        .flatMap(this::processPathStream);
}

private Result<Stream<Path>> listDirectoryContents(Path directory) {
    return Result.lift(() -> Files.list(directory));
}

private Result<DirectoryContents> processPathStream(Stream<Path> paths) {
    try (paths) {
        List<Path> pathList = collectPathsFromStream(paths);
        return categorizeAndProcessPaths(pathList);
    }
}

private List<Path> collectPathsFromStream(Stream<Path> paths) {
    return paths.collect(Collectors.toList());
}

private Result<DirectoryContents> categorizeAndProcessPaths(List<Path> paths) {
    PathCategorization categorization = categorizePaths(paths);
    return processSubdirectories(categorization.directories)
        .map(subdirs -> createDirectoryContents(categorization.files, subdirs));
}

private PathCategorization categorizePaths(List<Path> paths) {
    Map<Boolean, List<Path>> categorized = partitionPathsByFileType(paths);
    List<FileInfo> files = convertPathsToFileInfos(categorized.get(true));
    List<Path> directories = filterDirectories(categorized.get(false));
    
    return new PathCategorization(files, directories);
}

private Map<Boolean, List<Path>> partitionPathsByFileType(List<Path> paths) {
    return paths.stream()
        .collect(Collectors.partitioningBy(Files::isRegularFile));
}

private List<FileInfo> convertPathsToFileInfos(List<Path> filePaths) {
    return filePaths.stream()
        .map(this::createFileInfo)
        .collect(Collectors.toList());
}

private List<Path> filterDirectories(List<Path> nonFilePaths) {
    return nonFilePaths.stream()
        .filter(Files::isDirectory)
        .collect(Collectors.toList());
}

private Result<List<DirectoryContents>> processSubdirectories(List<Path> directories) {
    List<Result<DirectoryContents>> subdirResults = mapDirectoriesToContents(directories);
    return collectDirectoryResults(subdirResults);
}

private List<Result<DirectoryContents>> mapDirectoriesToContents(List<Path> directories) {
    return directories.stream()
        .map(this::scanDirectory)
        .collect(Collectors.toList());
}

private Result<List<DirectoryContents>> collectDirectoryResults(List<Result<DirectoryContents>> results) {
    return Result.collect(results);
}

private FileInfo createFileInfo(Path file) {
    try {
        long size = Files.size(file);
        String contentType = Files.probeContentType(file);
        return new FileInfo(file.getFileName().toString(), size, contentType);
    } catch (IOException e) {
        return createFallbackFileInfo(file);
    }
}

private FileInfo createFallbackFileInfo(Path file) {
    return new FileInfo(file.getFileName().toString(), -1, "unknown");
}

private DirectoryContents createDirectoryContents(List<FileInfo> files, List<DirectoryContents> subdirectories) {
    return new DirectoryContents(files, subdirectories);
}

// Helper class to hold categorized paths
private static class PathCategorization {
    final List<FileInfo> files;
    final List<Path> directories;
    
    PathCategorization(List<FileInfo> files, List<Path> directories) {
        this.files = files;
        this.directories = directories;
    }
}
```

### 6. Time-based Polling

```java
Promise<JobResult> waitForJobCompletion(String jobId) {
    return pollUntilCompleted(jobId, 0, MAX_POLLS);
}

private Promise<JobResult> pollUntilCompleted(String jobId, int attemptsMade, int maxAttempts) {
    if (isMaxAttemptsReached(attemptsMade, maxAttempts)) {
        return createTimeoutFailure();
    }
    
    return checkJobStatus(jobId)
        .flatMap(status -> handleJobStatus(jobId, status, attemptsMade, maxAttempts));
}

private boolean isMaxAttemptsReached(int attemptsMade, int maxAttempts) {
    return attemptsMade >= maxAttempts;
}

private Promise<JobResult> createTimeoutFailure() {
    return Promise.failure(new TimeoutException("Job didn't complete within allocated time"));
}

private Promise<JobStatus> checkJobStatus(String jobId) {
    return Promise.lift(() -> jobService.getStatus(jobId));
}

private Promise<JobResult> handleJobStatus(String jobId, JobStatus status, int attemptsMade, int maxAttempts) {
    return switch (status.getState()) {
        case COMPLETED -> fetchJobResult(jobId);
        case FAILED -> createJobFailureResult(status);
        case RUNNING, QUEUED -> delayAndRetry(jobId, attemptsMade, maxAttempts);
        default -> createUnexpectedStateFailure(status);
    };
}

private Promise<JobResult> createJobFailureResult(JobStatus status) {
    return Promise.failure(new JobFailedException("Job failed: " + status.getErrorMessage()));
}

private Promise<JobResult> createUnexpectedStateFailure(JobStatus status) {
    return Promise.failure(new UnexpectedJobStateException("Unexpected job state: " + status.getState()));
}

private Promise<JobResult> fetchJobResult(String jobId) {
    return Promise.lift(() -> jobService.getResult(jobId));
}

private Promise<JobResult> delayAndRetry(String jobId, int attemptsMade, int maxAttempts) {
    return createDelay()
        .flatMap(ignored -> pollUntilCompleted(jobId, attemptsMade + 1, maxAttempts));
}

private Promise<Void> createDelay() {
    return Promise.delay(Duration.ofSeconds(POLL_INTERVAL_SECONDS));
}
```

### 7. Parallel Iteration

```java
Result<AggregateReport> generateReports(List<Department> departments) {
    return parallelProcessDepartments(departments)
        .map(this::combineReports);
}

private Result<List<DepartmentReport>> parallelProcessDepartments(List<Department> departments) {
    List<Result<DepartmentReport>> reportTasks = createReportTasks(departments);
    return collectReportResults(reportTasks);
}

private List<Result<DepartmentReport>> createReportTasks(List<Department> departments) {
    return departments.stream()
        .map(this::generateDepartmentReport)
        .collect(Collectors.toList());
}

private Result<List<DepartmentReport>> collectReportResults(List<Result<DepartmentReport>> tasks) {
    return Result.collect(tasks);
}

private Result<DepartmentReport> generateDepartmentReport(Department department) {
    return Result.lift(() -> reportGenerator.createReport(department));
}

private AggregateReport combineReports(List<DepartmentReport> reports) {
    double totalExpenses = calculateTotalExpenses(reports);
    double totalRevenue = calculateTotalRevenue(reports);
    
    return new AggregateReport(reports, totalExpenses, totalRevenue);
}

private double calculateTotalExpenses(List<DepartmentReport> reports) {
    return reports.stream()
        .mapToDouble(DepartmentReport::getTotalExpenses)
        .sum();
}

private double calculateTotalRevenue(List<DepartmentReport> reports) {
    return reports.stream()
        .mapToDouble(DepartmentReport::getTotalRevenue)
        .sum();
}
```

### 8. Integration with Other Patterns

```java
Result<ProcessingSummary> processItemsByType(List<Item> items) {
    return Result.success(items)
        .map(this::categorizeItems)
        .flatMap(this::processItemsByCategory);
}

private Map<ItemType, List<Item>> categorizeItems(List<Item> items) {
    return items.stream()
        .collect(Collectors.groupingBy(Item::getType));
}

private Result<ProcessingSummary> processItemsByCategory(Map<ItemType, List<Item>> categorizedItems) {
    List<Result<CategoryResult>> categoryResults = createCategoryResults(categorizedItems);
    return collectCategoryResults(categoryResults);
}

private List<Result<CategoryResult>> createCategoryResults(Map<ItemType, List<Item>> categorizedItems) {
    return categorizedItems.entrySet().stream()
        .map(entry -> processSingleCategory(entry.getKey(), entry.getValue()))
        .collect(Collectors.toList());
}

private Result<CategoryResult> processSingleCategory(ItemType type, List<Item> items) {
    return processCategoryItems(type, items);
}

private Result<ProcessingSummary> collectCategoryResults(List<Result<CategoryResult>> categoryResults) {
    return Result.collect(categoryResults)
        .map(this::buildSummary);
}

private Result<CategoryResult> processCategoryItems(ItemType type, List<Item> items) {
    return selectProcessingStrategy(type)
        .flatMap(strategy -> processItemsWithStrategy(strategy, items, type));
}

private Result<CategoryResult> processItemsWithStrategy(ProcessingStrategy strategy, List<Item> items, ItemType type) {
    return strategy.process(items)
        .map(results -> new CategoryResult(type, results));
}

private Result<ProcessingStrategy> selectProcessingStrategy(ItemType type) {
    return switch (type) {
        case DIGITAL -> Result.success(new DigitalItemProcessor());
        case PHYSICAL -> Result.success(new PhysicalItemProcessor());
        case SUBSCRIPTION -> Result.success(new SubscriptionProcessor());
        default -> createUnknownTypeFailure(type);
    };
}

private Result<ProcessingStrategy> createUnknownTypeFailure(ItemType type) {
    return Result.failure(Causes.of("Unknown item type: " + type));
}

private ProcessingSummary buildSummary(List<CategoryResult> results) {
    int totalProcessed = calculateTotalProcessed(results);
    Map<ItemType, Integer> countsByType = calculateCountsByType(results);
    
    return new ProcessingSummary(totalProcessed, countsByType, results);
}

private int calculateTotalProcessed(List<CategoryResult> results) {
    return results.stream()
        .mapToInt(result -> result.getResults().size())
        .sum();
}

private Map<ItemType, Integer> calculateCountsByType(List<CategoryResult> results) {
    return results.stream()
        .collect(Collectors.toMap(
            CategoryResult::getType,
            result -> result.getResults().size()
        ));
}
```

## Benefits of the Pattern

1. **Explicit Error Handling**: Errors are first-class citizens in the functional pipeline.
2. **Improved Testability**: Functions with clear inputs and outputs are easier to test.
3. **Better Readability**: Code expresses intent rather than mechanics.
4. **Maintainability**: Changes are localized to specific transformation functions.
5. **Composability**: Operations can be reused and combined in different ways.
6. **Thread Safety**: Reduced mutation leads to fewer concurrency issues.

## When to Use

- Processing collections of data
- Operations that need retries or polling
- Paginated data retrieval
- Batch processing
- Recursive traversals (directories, trees, graphs)
- Parallel or concurrent operations
- Complex workflows with potential failures

## When Not to Use

- Very simple iterations with no error handling requirements
- Performance-critical inner loops where Stream API overhead matters
- When team is unfamiliar with functional concepts and proper training isn't available

## Implementation Notes

1. Each method should operate at exactly one level of abstraction.
2. Extract complex transformations to dedicated methods.
3. Use declarative approaches over imperative ones.
4. Ensure proper error propagation through the chain.
5. Prefer immutable data flow where practical.
6. Use appropriate container types for the operation context (sync/async).

This pattern exemplifies how pragmatic functional programming principles can be applied effectively in Java to create more maintainable, composable and robust code for iterative operations, without requiring a fully functional paradigm.

User:
Please, add note, that 'collect' belongs to Java 'Stream' class.

Assistant:
# The Iteration Pattern in Pragmatic Functional Java

## Introduction

The Iteration Pattern in Pragmatic Functional Java provides a consistent approach to handling iterative operations in a functional style. It addresses operations that process collections, repetitive tasks, pagination, batching, and recursive traversals through a declarative, composable API rather than traditional imperative loops, while remaining practical and accessible to developers from varied backgrounds.

## Core Principles

1. **Declarative over Imperative**: Express what should be done rather than how to do it.
2. **Single Level of Abstraction**: Each method operates at exactly one level of abstraction.
3. **Composition over Mutation**: Combine operations through functional composition rather than state mutation.
4. **Error Propagation**: Errors flow through the composition chain with proper context.
5. **Functional Transformations**: Use map/flatMap/filter for data transformations.
6. **Practical Immutability**: Prefer immutable data flow when it improves code quality.

## Pattern Structure

The pattern typically follows this structure:

1. Begin with an input collection or initial state
2. Transform data through well-defined functions
3. Handle errors through the composition chain
4. Compose operations to form a processing pipeline
5. Return a type that represents success/failure with appropriate data

## Key Components

### 1. Container Types
- `Result<T>` - Represents synchronous operations that may succeed or fail
- `Promise<T>` - Represents asynchronous operations that may succeed or fail
- Collections wrapped in these containers

### 2. Core Operations
- `map` - Transform successful values
- `flatMap` - Chain operations that may themselves fail
- `filter` - Include only values meeting certain criteria
- `collect` - A method belonging to Java's `Stream` class that gathers stream elements into a collection or other result container
- `Result.collect` - A custom method that gathers many results into a single result

### 3. Processing Styles
- **Collection Processing** - Transforming entire collections at once
- **Recursive Processing** - Self-similar operations on nested structures
- **Paginated/Batched Processing** - Processing data in chunks
- **Polled/Retry Processing** - Operations attempted repeatedly

## Implementation Examples

### 1. Collection Transformation

```java
Result<TaxReport> calculateTaxes(List<Invoice> invoices) {
    return Result.success(invoices)
        .map(this::processInvoicesForTax)
        .map(this::createTaxReport);
}

private List<TaxEntry> processInvoicesForTax(List<Invoice> invoices) {
    return invoices.stream()
        .map(this::calculateTaxForInvoice)
        .collect(Collectors.toList());
}

private TaxEntry calculateTaxForInvoice(Invoice invoice) {
    double taxAmount = invoice.getTotal() * getTaxRate(invoice.getRegion());
    return new TaxEntry(invoice.getId(), taxAmount, invoice.getRegion());
}

private TaxReport createTaxReport(List<TaxEntry> entries) {
    double totalTax = calculateTotalTax(entries);
    return new TaxReport(entries, totalTax, LocalDate.now());
}

private double calculateTotalTax(List<TaxEntry> entries) {
    return entries.stream()
        .mapToDouble(TaxEntry::getAmount)
        .sum();
}
```

### 2. Retry Pattern

```java
Result<DataFeed> fetchExternalData(String source) {
    return retryFetchOperation(source)
        .map(this::parseDataFeed);
}

private Result<String> retryFetchOperation(String source) {
    return Retry.withMaxAttempts(3)
        .withBackoff(Duration.ofSeconds(1), 2.0)
        .withErrorFilter(this::isRetryableError)
        .execute(() -> fetchFromExternalSource(source));
}

private boolean isRetryableError(Throwable error) {
    Set<Class<? extends Throwable>> retryableExceptions = 
        Set.of(TimeoutException.class, ConnectionException.class);
        
    return retryableExceptions.contains(error.getClass()) || 
           isRetryableHttpError(error);
}

private boolean isRetryableHttpError(Throwable error) {
    return error instanceof HttpException && 
           ((HttpException)error).getStatusCode() >= 500;
}

private Result<String> fetchFromExternalSource(String source) {
    return Result.lift(() -> externalApiClient.fetch(source));
}

private DataFeed parseDataFeed(String rawData) {
    return DataFeed.fromJson(rawData);
}
```

### 3. Paginated Data Retrieval

```java
Result<List<Customer>> getAllCustomers() {
    return fetchAllCustomerPages(0, new ArrayList<>());
}

private Result<List<Customer>> fetchAllCustomerPages(int page, List<Customer> accumulated) {
    return fetchCustomerPage(page)
        .flatMap(pageResult -> processPageResult(pageResult, page, accumulated));
}

private Result<List<Customer>> processPageResult(PageResult<Customer> pageResult, int currentPage, List<Customer> accumulated) {
    List<Customer> updatedList = combineResults(accumulated, pageResult.getItems());
    return continueOrFinish(pageResult, currentPage, updatedList);
}

private Result<List<Customer>> continueOrFinish(PageResult<Customer> pageResult, int currentPage, List<Customer> updatedList) {
    if (pageResult.hasMorePages()) {
        return fetchAllCustomerPages(currentPage + 1, updatedList);
    } else {
        return Result.success(updatedList);
    }
}

private List<Customer> combineResults(List<Customer> existing, List<Customer> newItems) {
    List<Customer> combined = new ArrayList<>(existing);
    combined.addAll(newItems);
    return combined;
}

private Result<PageResult<Customer>> fetchCustomerPage(int page) {
    return Result.lift(() -> customerRepository.getPage(page, PAGE_SIZE));
}
```

### 4. Batch Processing

```java
Result<BatchSummary> processBatch(List<Transaction> transactions) {
    return Result.success(transactions)
        .map(this::partitionIntoBatches)
        .flatMap(this::processAllBatches)
        .map(this::createBatchSummary);
}

private List<List<Transaction>> partitionIntoBatches(List<Transaction> transactions) {
    return ListUtils.partition(transactions, BATCH_SIZE);
}

private Result<List<BatchResult>> processAllBatches(List<List<Transaction>> batches) {
    List<Result<BatchResult>> batchResults = mapBatchesToResults(batches);
    return collectResults(batchResults);
}

private List<Result<BatchResult>> mapBatchesToResults(List<List<Transaction>> batches) {
    return batches.stream()
        .map(this::processSingleBatch)
        .collect(Collectors.toList());
}

private Result<BatchResult> processSingleBatch(List<Transaction> batch) {
    return transactionProcessor.process(batch)
        .map(results -> new BatchResult(batch.size(), results));
}

private Result<List<BatchResult>> collectResults(List<Result<BatchResult>> results) {
    return Result.collect(results);
}

private BatchSummary createBatchSummary(List<BatchResult> batchResults) {
    int totalProcessed = calculateTotalProcessed(batchResults);
    return new BatchSummary(totalProcessed, batchResults.size());
}

private int calculateTotalProcessed(List<BatchResult> batchResults) {
    return batchResults.stream()
        .mapToInt(BatchResult::getProcessedCount)
        .sum();
}
```

### 5. Recursive Operations

```java
Result<DirectoryContents> scanDirectory(Path directory) {
    return validateDirectory(directory)
        .flatMap(this::readDirectoryContents);
}

private Result<Path> validateDirectory(Path directory) {
    return Result.lift(() -> validateDirectoryExists(directory));
}

private Path validateDirectoryExists(Path directory) throws IOException {
    if (!Files.exists(directory)) {
        throw new DirectoryNotFoundException("Directory not found: " + directory);
    }
    if (!Files.isDirectory(directory)) {
        throw new NotDirectoryException("Path is not a directory: " + directory);
    }
    return directory;
}

private Result<DirectoryContents> readDirectoryContents(Path directory) {
    return listDirectoryContents(directory)
        .flatMap(this::processPathStream);
}

private Result<Stream<Path>> listDirectoryContents(Path directory) {
    return Result.lift(() -> Files.list(directory));
}

private Result<DirectoryContents> processPathStream(Stream<Path> paths) {
    try (paths) {
        List<Path> pathList = collectPathsFromStream(paths);
        return categorizeAndProcessPaths(pathList);
    }
}

private List<Path> collectPathsFromStream(Stream<Path> paths) {
    return paths.collect(Collectors.toList());
}

private Result<DirectoryContents> categorizeAndProcessPaths(List<Path> paths) {
    PathCategorization categorization = categorizePaths(paths);
    return processSubdirectories(categorization.directories)
        .map(subdirs -> createDirectoryContents(categorization.files, subdirs));
}

private PathCategorization categorizePaths(List<Path> paths) {
    Map<Boolean, List<Path>> categorized = partitionPathsByFileType(paths);
    List<FileInfo> files = convertPathsToFileInfos(categorized.get(true));
    List<Path> directories = filterDirectories(categorized.get(false));
    
    return new PathCategorization(files, directories);
}

private Map<Boolean, List<Path>> partitionPathsByFileType(List<Path> paths) {
    return paths.stream()
        .collect(Collectors.partitioningBy(Files::isRegularFile));
}

private List<FileInfo> convertPathsToFileInfos(List<Path> filePaths) {
    return filePaths.stream()
        .map(this::createFileInfo)
        .collect(Collectors.toList());
}

private List<Path> filterDirectories(List<Path> nonFilePaths) {
    return nonFilePaths.stream()
        .filter(Files::isDirectory)
        .collect(Collectors.toList());
}

private Result<List<DirectoryContents>> processSubdirectories(List<Path> directories) {
    List<Result<DirectoryContents>> subdirResults = mapDirectoriesToContents(directories);
    return collectDirectoryResults(subdirResults);
}

private List<Result<DirectoryContents>> mapDirectoriesToContents(List<Path> directories) {
    return directories.stream()
        .map(this::scanDirectory)
        .collect(Collectors.toList());
}

private Result<List<DirectoryContents>> collectDirectoryResults(List<Result<DirectoryContents>> results) {
    return Result.collect(results);
}

private FileInfo createFileInfo(Path file) {
    try {
        long size = Files.size(file);
        String contentType = Files.probeContentType(file);
        return new FileInfo(file.getFileName().toString(), size, contentType);
    } catch (IOException e) {
        return createFallbackFileInfo(file);
    }
}

private FileInfo createFallbackFileInfo(Path file) {
    return new FileInfo(file.getFileName().toString(), -1, "unknown");
}

private DirectoryContents createDirectoryContents(List<FileInfo> files, List<DirectoryContents> subdirectories) {
    return new DirectoryContents(files, subdirectories);
}

// Helper class to hold categorized paths
private static class PathCategorization {
    final List<FileInfo> files;
    final List<Path> directories;
    
    PathCategorization(List<FileInfo> files, List<Path> directories) {
        this.files = files;
        this.directories = directories;
    }
}
```

### 6. Time-based Polling

```java
Promise<JobResult> waitForJobCompletion(String jobId) {
    return pollUntilCompleted(jobId, 0, MAX_POLLS);
}

private Promise<JobResult> pollUntilCompleted(String jobId, int attemptsMade, int maxAttempts) {
    if (isMaxAttemptsReached(attemptsMade, maxAttempts)) {
        return createTimeoutFailure();
    }
    
    return checkJobStatus(jobId)
        .flatMap(status -> handleJobStatus(jobId, status, attemptsMade, maxAttempts));
}

private boolean isMaxAttemptsReached(int attemptsMade, int maxAttempts) {
    return attemptsMade >= maxAttempts;
}

private Promise<JobResult> createTimeoutFailure() {
    return Promise.failure(new TimeoutException("Job didn't complete within allocated time"));
}

private Promise<JobStatus> checkJobStatus(String jobId) {
    return Promise.lift(() -> jobService.getStatus(jobId));
}

private Promise<JobResult> handleJobStatus(String jobId, JobStatus status, int attemptsMade, int maxAttempts) {
    return switch (status.getState()) {
        case COMPLETED -> fetchJobResult(jobId);
        case FAILED -> createJobFailureResult(status);
        case RUNNING, QUEUED -> delayAndRetry(jobId, attemptsMade, maxAttempts);
        default -> createUnexpectedStateFailure(status);
    };
}

private Promise<JobResult> createJobFailureResult(JobStatus status) {
    return Promise.failure(new JobFailedException("Job failed: " + status.getErrorMessage()));
}

private Promise<JobResult> createUnexpectedStateFailure(JobStatus status) {
    return Promise.failure(new UnexpectedJobStateException("Unexpected job state: " + status.getState()));
}

private Promise<JobResult> fetchJobResult(String jobId) {
    return Promise.lift(() -> jobService.getResult(jobId));
}

private Promise<JobResult> delayAndRetry(String jobId, int attemptsMade, int maxAttempts) {
    return createDelay()
        .flatMap(ignored -> pollUntilCompleted(jobId, attemptsMade + 1, maxAttempts));
}

private Promise<Void> createDelay() {
    return Promise.delay(Duration.ofSeconds(POLL_INTERVAL_SECONDS));
}
```

### 7. Parallel Iteration

```java
Result<AggregateReport> generateReports(List<Department> departments) {
    return parallelProcessDepartments(departments)
        .map(this::combineReports);
}

private Result<List<DepartmentReport>> parallelProcessDepartments(List<Department> departments) {
    List<Result<DepartmentReport>> reportTasks = createReportTasks(departments);
    return collectReportResults(reportTasks);
}

private List<Result<DepartmentReport>> createReportTasks(List<Department> departments) {
    return departments.stream()
        .map(this::generateDepartmentReport)
        .collect(Collectors.toList());
}

private Result<List<DepartmentReport>> collectReportResults(List<Result<DepartmentReport>> tasks) {
    return Result.collect(tasks);
}

private Result<DepartmentReport> generateDepartmentReport(Department department) {
    return Result.lift(() -> reportGenerator.createReport(department));
}

private AggregateReport combineReports(List<DepartmentReport> reports) {
    double totalExpenses = calculateTotalExpenses(reports);
    double totalRevenue = calculateTotalRevenue(reports);
    
    return new AggregateReport(reports, totalExpenses, totalRevenue);
}

private double calculateTotalExpenses(List<DepartmentReport> reports) {
    return reports.stream()
        .mapToDouble(DepartmentReport::getTotalExpenses)
        .sum();
}

private double calculateTotalRevenue(List<DepartmentReport> reports) {
    return reports.stream()
        .mapToDouble(DepartmentReport::getTotalRevenue)
        .sum();
}
```

### 8. Integration with Other Patterns

```java
Result<ProcessingSummary> processItemsByType(List<Item> items) {
    return Result.success(items)
        .map(this::categorizeItems)
        .flatMap(this::processItemsByCategory);
}

private Map<ItemType, List<Item>> categorizeItems(List<Item> items) {
    return items.stream()
        .collect(Collectors.groupingBy(Item::getType));
}

private Result<ProcessingSummary> processItemsByCategory(Map<ItemType, List<Item>> categorizedItems) {
    List<Result<CategoryResult>> categoryResults = createCategoryResults(categorizedItems);
    return collectCategoryResults(categoryResults);
}

private List<Result<CategoryResult>> createCategoryResults(Map<ItemType, List<Item>> categorizedItems) {
    return categorizedItems.entrySet().stream()
        .map(entry -> processSingleCategory(entry.getKey(), entry.getValue()))
        .collect(Collectors.toList());
}

private Result<CategoryResult> processSingleCategory(ItemType type, List<Item> items) {
    return processCategoryItems(type, items);
}

private Result<ProcessingSummary> collectCategoryResults(List<Result<CategoryResult>> categoryResults) {
    return Result.collect(categoryResults)
        .map(this::buildSummary);
}

private Result<CategoryResult> processCategoryItems(ItemType type, List<Item> items) {
    return selectProcessingStrategy(type)
        .flatMap(strategy -> processItemsWithStrategy(strategy, items, type));
}

private Result<CategoryResult> processItemsWithStrategy(ProcessingStrategy strategy, List<Item> items, ItemType type) {
    return strategy.process(items)
        .map(results -> new CategoryResult(type, results));
}

private Result<ProcessingStrategy> selectProcessingStrategy(ItemType type) {
    return switch (type) {
        case DIGITAL -> Result.success(new DigitalItemProcessor());
        case PHYSICAL -> Result.success(new PhysicalItemProcessor());
        case SUBSCRIPTION -> Result.success(new SubscriptionProcessor());
        default -> createUnknownTypeFailure(type);
    };
}

private Result<ProcessingStrategy> createUnknownTypeFailure(ItemType type) {
    return Result.failure(Causes.of("Unknown item type: " + type));
}

private ProcessingSummary buildSummary(List<CategoryResult> results) {
    int totalProcessed = calculateTotalProcessed(results);
    Map<ItemType, Integer> countsByType = calculateCountsByType(results);
    
    return new ProcessingSummary(totalProcessed, countsByType, results);
}

private int calculateTotalProcessed(List<CategoryResult> results) {
    return results.stream()
        .mapToInt(result -> result.getResults().size())
        .sum();
}

private Map<ItemType, Integer> calculateCountsByType(List<CategoryResult> results) {
    return results.stream()
        .collect(Collectors.toMap(
            CategoryResult::getType,
            result -> result.getResults().size()
        ));
}
```

## Benefits of the Pattern

1. **Explicit Error Handling**: Errors are first-class citizens in the functional pipeline.
2. **Improved Testability**: Functions with clear inputs and outputs are easier to test.
3. **Better Readability**: Code expresses intent rather than mechanics.
4. **Maintainability**: Changes are localized to specific transformation functions.
5. **Composability**: Operations can be reused and combined in different ways.
6. **Thread Safety**: Reduced mutation leads to fewer concurrency issues.

## When to Use

- Processing collections of data
- Operations that need retries or polling
- Paginated data retrieval
- Batch processing
- Recursive traversals (directories, trees, graphs)
- Parallel or concurrent operations
- Complex workflows with potential failures

## When Not to Use

- Very simple iterations with no error handling requirements
- Performance-critical inner loops where Stream API overhead matters
- When team is unfamiliar with functional concepts and proper training isn't available

## Implementation Notes

1. Each method should operate at exactly one level of abstraction.
2. Extract complex transformations to dedicated methods.
3. Use declarative approaches over imperative ones.
4. Ensure proper error propagation through the chain.
5. Prefer immutable data flow where practical.
6. Use appropriate container types for the operation context (sync/async).

This pattern exemplifies how pragmatic functional programming principles can be applied effectively in Java to create more maintainable, composable and robust code for iterative operations, without requiring a fully functional paradigm.

User:
Example for recursive operations is overcomplicated. Simplify it, please.

Assistant:
# Simplified Recursive Operations Example

Let me provide a simplified example of recursive operations using the Iteration Pattern in Pragmatic Functional Java.

```java
Result<DirectoryContents> scanDirectory(Path directory) {
    return validateDirectory(directory)
        .flatMap(this::readDirectoryContents);
}

private Result<Path> validateDirectory(Path directory) {
    return Result.lift(() -> {
        if (!Files.exists(directory)) {
            throw new IOException("Directory not found: " + directory);
        }
        if (!Files.isDirectory(directory)) {
            throw new IOException("Path is not a directory: " + directory);
        }
        return directory;
    });
}

private Result<DirectoryContents> readDirectoryContents(Path directory) {
    return Result.lift(() -> Files.list(directory))
        .flatMap(this::processDirectoryContents);
}

private Result<DirectoryContents> processDirectoryContents(Stream<Path> paths) {
    try (paths) {
        List<Path> pathList = paths.collect(Collectors.toList());
        
        // Separate files and directories
        List<Path> files = pathList.stream()
            .filter(Files::isRegularFile)
            .collect(Collectors.toList());
            
        List<Path> directories = pathList.stream()
            .filter(Files::isDirectory)
            .collect(Collectors.toList());
        
        // Process files
        List<FileInfo> fileInfos = files.stream()
            .map(this::createFileInfo)
            .collect(Collectors.toList());
        
        // Recursively process subdirectories
        return processSubdirectories(directories)
            .map(subdirs -> new DirectoryContents(fileInfos, subdirs));
    }
}

private FileInfo createFileInfo(Path file) {
    try {
        long size = Files.size(file);
        String name = file.getFileName().toString();
        return new FileInfo(name, size);
    } catch (IOException e) {
        // Fallback if we can't get the size
        return new FileInfo(file.getFileName().toString(), -1);
    }
}

private Result<List<DirectoryContents>> processSubdirectories(List<Path> directories) {
    // Map each directory to its contents and collect results
    List<Result<DirectoryContents>> results = directories.stream()
        .map(this::scanDirectory)
        .collect(Collectors.toList());
    
    // Combine all results into a single Result
    return Result.collect(results);
}

// Simple value classes
class FileInfo {
    private final String name;
    private final long size;
    
    FileInfo(String name, long size) {
        this.name = name;
        this.size = size;
    }
    
    // Getters
    public String getName() { return name; }
    public long getSize() { return size; }
}

class DirectoryContents {
    private final List<FileInfo> files;
    private final List<DirectoryContents> subdirectories;
    
    DirectoryContents(List<FileInfo> files, List<DirectoryContents> subdirectories) {
        this.files = files;
        this.subdirectories = subdirectories;
    }
    
    // Getters
    public List<FileInfo> getFiles() { return files; }
    public List<DirectoryContents> getSubdirectories() { return subdirectories; }
}
```

## Key Aspects of this Simplified Example

1. **Clear Recursion Point**: The `scanDirectory` method calls itself indirectly through `processSubdirectories`.

2. **Functional Composition**: Operations are chained using `flatMap` and `map`.

3. **Error Handling**: Errors are propagated through the `Result` type.

4. **Collecting Results**: The `collect` method from Java's `Stream` class is used to gather elements into collections.

5. **Clear Separation of Concerns**:
    - Directory validation
    - Reading directory contents
    - Processing files
    - Recursively processing subdirectories

6. **Resource Management**: The stream is properly closed using try-with-resources.

This example demonstrates the core principles of the Iteration Pattern in a more concise way while maintaining its functional approach to recursive directory traversal.

User:
Rewrite it from scratch, use different problem to illustrate recursion.

Assistant:
# Recursive Operations Example: Parsing JSON Objects

Here's a new example illustrating recursion using the Iteration Pattern to parse and transform a nested JSON structure into a domain model.

```java
/**
 * Parses a JSON string into a hierarchical Category structure
 */
Result<Category> parseCategories(String json) {
    return parseJsonToMap(json)
        .flatMap(this::buildCategoryTree);
}

/**
 * Safely converts the raw JSON string to a Map
 */
private Result<Map<String, Object>> parseJsonToMap(String json) {
    return Result.lift(() -> {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    });
}

/**
 * Builds the root Category from the parsed JSON map
 */
private Result<Category> buildCategoryTree(Map<String, Object> rootMap) {
    return extractRequiredString(rootMap, "id")
        .flatMap(id -> extractRequiredString(rootMap, "name")
            .flatMap(name -> processSubcategories(rootMap)
                .map(subcategories -> new Category(id, name, subcategories))));
}

/**
 * Safely extracts a required string value from a map
 */
private Result<String> extractRequiredString(Map<String, Object> map, String key) {
    Object value = map.get(key);
    if (value == null) {
        return Result.failure(new ParseException("Missing required field: " + key));
    }
    if (!(value instanceof String)) {
        return Result.failure(new ParseException("Field is not a string: " + key));
    }
    return Result.success((String) value);
}

/**
 * Processes the subcategories array from the map
 */
private Result<List<Category>> processSubcategories(Map<String, Object> parentMap) {
    // Return empty list if no subcategories
    if (!parentMap.containsKey("subcategories")) {
        return Result.success(Collections.emptyList());
    }
    
    Object subcategoriesObj = parentMap.get("subcategories");
    if (!(subcategoriesObj instanceof List)) {
        return Result.failure(new ParseException("Subcategories is not an array"));
    }
    
    List<?> subcategoriesList = (List<?>) subcategoriesObj;
    return parseSubcategories(subcategoriesList);
}

/**
 * Recursively parses each subcategory
 */
private Result<List<Category>> parseSubcategories(List<?> subcategoriesList) {
    // Convert each item to a Category and collect the results
    List<Result<Category>> categoryResults = subcategoriesList.stream()
        .map(this::parseSubcategory)
        .collect(Collectors.toList());
    
    // Combine the list of Results into a single Result of a list
    return Result.collect(categoryResults);
}

/**
 * Parse a single subcategory
 */
private Result<Category> parseSubcategory(Object item) {
    if (!(item instanceof Map)) {
        return Result.failure(new ParseException("Subcategory is not an object"));
    }
    
    // Recursive call to build each subcategory
    Map<String, Object> subcategoryMap = (Map<String, Object>) item;
    return buildCategoryTree(subcategoryMap);
}

/**
 * Domain model for categories
 */
class Category {
    private final String id;
    private final String name;
    private final List<Category> subcategories;
    
    Category(String id, String name, List<Category> subcategories) {
        this.id = id;
        this.name = name;
        this.subcategories = subcategories;
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public List<Category> getSubcategories() { return subcategories; }
    
    // Helper method to find a category by ID anywhere in the tree
    public Optional<Category> findById(String searchId) {
        if (id.equals(searchId)) {
            return Optional.of(this);
        }
        
        // Search through subcategories (depth-first)
        return subcategories.stream()
            .map(sub -> sub.findById(searchId))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst();
    }
    
    // Calculate the total number of categories (including this one)
    public int getTotalCount() {
        return 1 + subcategories.stream()
            .mapToInt(Category::getTotalCount)
            .sum();
    }
}

/**
 * Custom exception for parse errors
 */
class ParseException extends Exception {
    ParseException(String message) {
        super(message);
    }
}
```

## Example JSON Input

```json
{
  "id": "electronics",
  "name": "Electronics",
  "subcategories": [
    {
      "id": "computers",
      "name": "Computers",
      "subcategories": [
        {
          "id": "laptops",
          "name": "Laptops",
          "subcategories": []
        },
        {
          "id": "desktops",
          "name": "Desktop Computers",
          "subcategories": []
        }
      ]
    },
    {
      "id": "phones",
      "name": "Mobile Phones",
      "subcategories": [
        {
          "id": "smartphones",
          "name": "Smartphones",
          "subcategories": []
        },
        {
          "id": "accessories",
          "name": "Phone Accessories",
          "subcategories": []
        }
      ]
    }
  ]
}
```

## How to Use the Parser

```java
String jsonInput = "..."; // JSON content from file or API
Result<Category> result = categoryParser.parseCategories(jsonInput);

result.onSuccess(categoryTree -> {
    System.out.println("Successfully parsed " + categoryTree.getTotalCount() + " categories");
    
    // Find a specific category
    categoryTree.findById("smartphones").ifPresent(smartphone -> {
        System.out.println("Found category: " + smartphone.getName());
    });
}).onFailure(error -> {
    System.err.println("Failed to parse categories: " + error.getMessage());
});
```

## Key Aspects of this Recursive Example

1. **Recursive Structure**: The category tree is built by recursively parsing subcategories.

2. **Functional Approach**: Uses `flatMap` and `map` to chain operations and build the result.

3. **Error Handling**: Uses the `Result` type to propagate errors through the processing chain.

4. **Collection Processing**: The `collect` method from Java's `Stream` class gathers elements into collections.

5. **Result Aggregation**: `Result.collect()` combines multiple Result objects into a single Result.

6. **Domain Model**: The `Category` class itself has recursive methods (`findById` and `getTotalCount`).

7. **Type Safety**: Uses safe type checking and conversion when working with the untyped JSON data.

This example demonstrates the power of the Iteration Pattern when dealing with hierarchical data structures, showing how recursion can be implemented in a functional style while maintaining proper error handling.

User:
Please, check last examples for single level of abstraction.

Assistant:
# Revised JSON Category Parser with Single Level of Abstraction

After reviewing your code, I notice it violates the Single Level of Abstraction Principle (SLAP), which states that functions should operate at only one level of abstraction. I'll rewrite it with cleaner separation of concerns.

```java
/**
 * Class responsible for parsing JSON into a Category hierarchy
 */
public class CategoryParser {
    private final ObjectMapper mapper = new ObjectMapper();
    
    /**
     * Main entry point to parse a JSON string into a Category hierarchy
     */
    public Result<Category> parseCategories(String json) {
        return parseJsonToMap(json)
            .flatMap(this::buildCategoryTree);
    }
    
    /**
     * Parses raw JSON into a Map
     */
    private Result<Map<String, Object>> parseJsonToMap(String json) {
        return Result.lift(() -> 
            mapper.readValue(json, new TypeReference<Map<String, Object>>() {}));
    }
    
    /**
     * Builds a category tree from the root map
     */
    private Result<Category> buildCategoryTree(Map<String, Object> rootMap) {
        return extractCategoryData(rootMap)
            .flatMap(this::createCategory);
    }
    
    /**
     * Extracts required data for a category from a map
     */
    private Result<CategoryData> extractCategoryData(Map<String, Object> map) {
        Result<String> idResult = extractRequiredString(map, "id");
        Result<String> nameResult = extractRequiredString(map, "name");
        Result<List<?>> subcategoriesResult = extractSubcategoriesList(map);
        
        return Result.combine(idResult, nameResult, subcategoriesResult, CategoryData::new);
    }
    
    /**
     * Extracts subcategories list from a map
     */
    private Result<List<?>> extractSubcategoriesList(Map<String, Object> map) {
        if (!map.containsKey("subcategories")) {
            return Result.success(Collections.emptyList());
        }
        
        Object subcategoriesObj = map.get("subcategories");
        if (!(subcategoriesObj instanceof List)) {
            return Result.failure(new ParseException("Subcategories is not an array"));
        }
        
        return Result.success((List<?>) subcategoriesObj);
    }
    
    /**
     * Creates a Category from extracted data
     */
    private Result<Category> createCategory(CategoryData data) {
        return parseSubcategories(data.subcategoriesList)
            .map(subcategories -> new Category(data.id, data.name, subcategories));
    }
    
    /**
     * Extracts a required string from a map
     */
    private Result<String> extractRequiredString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return Result.failure(new ParseException("Missing required field: " + key));
        }
        if (!(value instanceof String)) {
            return Result.failure(new ParseException("Field is not a string: " + key));
        }
        return Result.success((String) value);
    }
    
    /**
     * Parses subcategories from a list
     */
    private Result<List<Category>> parseSubcategories(List<?> subcategoriesList) {
        List<Result<Category>> categoryResults = new ArrayList<>();
        
        for (Object item : subcategoriesList) {
            categoryResults.add(parseSubcategory(item));
        }
        
        return Result.collect(categoryResults);
    }
    
    /**
     * Parses a single subcategory from an object
     */
    private Result<Category> parseSubcategory(Object item) {
        if (!(item instanceof Map)) {
            return Result.failure(new ParseException("Subcategory is not an object"));
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> subcategoryMap = (Map<String, Object>) item;
        return buildCategoryTree(subcategoryMap);
    }
    
    /**
     * Data class to hold temporary category data during parsing
     */
    private static class CategoryData {
        final String id;
        final String name;
        final List<?> subcategoriesList;
        
        CategoryData(String id, String name, List<?> subcategoriesList) {
            this.id = id;
            this.name = name;
            this.subcategoriesList = subcategoriesList;
        }
    }
}

/**
 * Domain model for categories
 */
class Category {
    private final String id;
    private final String name;
    private final List<Category> subcategories;
    
    Category(String id, String name, List<Category> subcategories) {
        this.id = id;
        this.name = name;
        this.subcategories = Collections.unmodifiableList(subcategories);
    }
    
    public String getId() { 
        return id; 
    }
    
    public String getName() { 
        return name; 
    }
    
    public List<Category> getSubcategories() { 
        return subcategories; 
    }
    
    public Optional<Category> findById(String searchId) {
        if (id.equals(searchId)) {
            return Optional.of(this);
        }
        
        return subcategories.stream()
            .map(sub -> sub.findById(searchId))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst();
    }
    
    public int getTotalCount() {
        return 1 + subcategories.stream()
            .mapToInt(Category::getTotalCount)
            .sum();
    }
}

/**
 * Custom exception for parse errors
 */
class ParseException extends Exception {
    ParseException(String message) {
        super(message);
    }
}

/**
 * Result type for error handling
 */
interface Result<T> {
    // Methods for Result implementation
    // ...
}
```

## Key Improvements for Single Level of Abstraction

1. **Smaller, Focused Methods**: Each method does exactly one thing at one level of abstraction.

2. **Data Transfer Object**: Added a `CategoryData` class to hold intermediate parsing results.

3. **Separated Concerns**:
    - JSON parsing
    - Data extraction
    - Data validation
    - Object creation

4. **Result Combination**: Used `Result.combine()` to merge multiple result operations.

5. **Clear Method Names**: Each method's name clearly describes what it does.

6. **Consistent Error Handling**: The `Result` type is used consistently throughout.

7. **Immutability**: The `Category` class is immutable with defensive copying.

8. **Clear Class Responsibilities**:
    - `CategoryParser`: Handles parsing JSON into domain objects
    - `Category`: Domain model with its own business logic
    - `ParseException`: Specific exception for parsing errors

This revised example demonstrates the same recursive parsing of nested categories but with proper adherence to the Single Level of Abstraction Principle, making the code more maintainable and easier to understand.

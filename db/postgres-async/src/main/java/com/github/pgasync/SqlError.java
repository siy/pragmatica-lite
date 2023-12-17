package com.github.pgasync;

import org.pragmatica.lang.Result;

@SuppressWarnings("unused")
public sealed interface SqlError extends Result.Cause {
    @SuppressWarnings("unused")
    record ChannelClosed(String message) implements SqlError {}
    @SuppressWarnings("unused")
    record SimultaneousUseDetected(String message) implements SqlError {}
    @SuppressWarnings("unused")
    record ConnectionPoolClosed(String message) implements SqlError {}
    @SuppressWarnings("unused")
    record BadAuthenticationSequence(String message) implements SqlError {}
    @SuppressWarnings("unused")
    record CommunicationError(String message) implements SqlError {}

    @SuppressWarnings("unused")
    record ServerResponse(String code, String level, String message) {}

    @SuppressWarnings("unused")
    sealed interface ServerError extends SqlError {
        ServerResponse response();
        String readableCode();

        default String message() {
            return STR."\{response().level()}: SQLSTATE=\{response().code()}, MESSAGE=\{response().message()}";
        }
    }

    record ServerWarning(ServerResponse response, String readableCode) implements ServerError {}
    record ServerErrorNoData(ServerResponse response, String readableCode) implements ServerError {}
    record ServerErrorSQLStatementNotYetComplete(ServerResponse response, String readableCode) implements ServerError {}
    record ServerConnectionException(ServerResponse response, String readableCode) implements ServerError {}
    record ServerTriggeredActionException(ServerResponse response, String readableCode) implements ServerError {}
    record ServerErrorFeatureNotSupported(ServerResponse response, String readableCode) implements ServerError {}
    record ServerErrorInvalidTransactionInitiation(ServerResponse response, String readableCode) implements ServerError {}
    record ServerLocatorException(ServerResponse response, String readableCode) implements ServerError {}
    record ServerErrorInvalidGrantor(ServerResponse response, String readableCode) implements ServerError {}
    record ServerErrorInvalidRoleSpecification(ServerResponse response, String readableCode) implements ServerError {}
    record ServerDiagnosticsException(ServerResponse response, String readableCode) implements ServerError {}
    record ServerErrorCaseNotFound(ServerResponse response, String readableCode) implements ServerError {}
    record ServerErrorCardinalityViolation(ServerResponse response, String readableCode) implements ServerError {}
    record ServerDataException(ServerResponse response, String readableCode) implements ServerError {}
    record ServerErrorIntegrityConstraintViolation(ServerResponse response, String readableCode) implements ServerError {}
    record ServerErrorInvalidCursorState(ServerResponse response, String readableCode) implements ServerError {}
    record ServerErrorInvalidTransactionState(ServerResponse response, String readableCode) implements ServerError {}
    record ServerErrorInvalidSQLStatementName(ServerResponse response, String readableCode) implements ServerError {}
    record ServerErrorTriggeredDataChangeViolation(ServerResponse response, String readableCode) implements ServerError {}
    record ServerErrorInvalidAuthorizationSpecification(ServerResponse response, String readableCode) implements ServerError {}
    record ServerErrorDependentPrivilegeDescriptorsStillExist(ServerResponse response, String readableCode) implements ServerError {}
    record ServerErrorInvalidTransactionTermination(ServerResponse response, String readableCode) implements ServerError {}
    record ServerSQLRoutineException(ServerResponse response, String readableCode) implements ServerError {}
    record ServerErrorInvalidCursorName(ServerResponse response, String readableCode) implements ServerError {}
    record ServerExternalRoutineException(ServerResponse response, String readableCode) implements ServerError {}
    record ServerExternalRoutineInvocationException(ServerResponse response, String readableCode) implements ServerError {}
    record ServerSavepointException(ServerResponse response, String readableCode) implements ServerError {}
    record ServerErrorInvalidCatalogName(ServerResponse response, String readableCode) implements ServerError {}
    record ServerErrorInvalidSchemaName(ServerResponse response, String readableCode) implements ServerError {}
    record ServerErrorTransactionRollback(ServerResponse response, String readableCode) implements ServerError {}
    record ServerSyntaxErrorOrAccessRuleViolation(ServerResponse response, String readableCode) implements ServerError {}
    record ServerErrorWithCheckOptionViolation(ServerResponse response, String readableCode) implements ServerError {}
    record ServerErrorInsufficientResources(ServerResponse response, String readableCode) implements ServerError {}
    record ServerErrorProgramLimitExceeded(ServerResponse response, String readableCode) implements ServerError {}
    record ServerErrorObjectNotInPrerequisiteState(ServerResponse response, String readableCode) implements ServerError {}
    record ServerErrorOperatorIntervention(ServerResponse response, String readableCode) implements ServerError {}
    record ServerSystemError(ServerResponse response, String readableCode) implements ServerError {}
    record ServerSnapshotFailure(ServerResponse response, String readableCode) implements ServerError {}
    record ServerConfigurationFileError(ServerResponse response, String readableCode) implements ServerError {}
    record ServerForeignDataWrapperError(ServerResponse response, String readableCode) implements ServerError {}
    record ServerPlPgSQLError(ServerResponse response, String readableCode) implements ServerError {}
    record ServerInternalError(ServerResponse response, String readableCode) implements ServerError {}
    record ServerUnknownError(ServerResponse response, String readableCode) implements ServerError {}


    static SqlError fromThrowable(Throwable th) {
        return new CommunicationError(th.getMessage());
    }
}


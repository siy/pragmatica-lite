/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.pgasync;

import com.github.pgasync.SqlError.ServerConfigurationFileError;
import com.github.pgasync.SqlError.ServerConnectionException;
import com.github.pgasync.SqlError.ServerDataException;
import com.github.pgasync.SqlError.ServerDiagnosticsException;
import com.github.pgasync.SqlError.ServerError;
import com.github.pgasync.SqlError.ServerErrorCardinalityViolation;
import com.github.pgasync.SqlError.ServerErrorCaseNotFound;
import com.github.pgasync.SqlError.ServerErrorDependentPrivilegeDescriptorsStillExist;
import com.github.pgasync.SqlError.ServerErrorFeatureNotSupported;
import com.github.pgasync.SqlError.ServerErrorInsufficientResources;
import com.github.pgasync.SqlError.ServerErrorIntegrityConstraintViolation;
import com.github.pgasync.SqlError.ServerErrorInvalidAuthorizationSpecification;
import com.github.pgasync.SqlError.ServerErrorInvalidCatalogName;
import com.github.pgasync.SqlError.ServerErrorInvalidCursorName;
import com.github.pgasync.SqlError.ServerErrorInvalidCursorState;
import com.github.pgasync.SqlError.ServerErrorInvalidGrantor;
import com.github.pgasync.SqlError.ServerErrorInvalidRoleSpecification;
import com.github.pgasync.SqlError.ServerErrorInvalidSQLStatementName;
import com.github.pgasync.SqlError.ServerErrorInvalidSchemaName;
import com.github.pgasync.SqlError.ServerErrorInvalidTransactionInitiation;
import com.github.pgasync.SqlError.ServerErrorInvalidTransactionState;
import com.github.pgasync.SqlError.ServerErrorInvalidTransactionTermination;
import com.github.pgasync.SqlError.ServerErrorNoData;
import com.github.pgasync.SqlError.ServerErrorObjectNotInPrerequisiteState;
import com.github.pgasync.SqlError.ServerErrorOperatorIntervention;
import com.github.pgasync.SqlError.ServerErrorProgramLimitExceeded;
import com.github.pgasync.SqlError.ServerErrorSQLStatementNotYetComplete;
import com.github.pgasync.SqlError.ServerErrorTransactionRollback;
import com.github.pgasync.SqlError.ServerErrorTriggeredDataChangeViolation;
import com.github.pgasync.SqlError.ServerErrorWithCheckOptionViolation;
import com.github.pgasync.SqlError.ServerExternalRoutineException;
import com.github.pgasync.SqlError.ServerExternalRoutineInvocationException;
import com.github.pgasync.SqlError.ServerForeignDataWrapperError;
import com.github.pgasync.SqlError.ServerInternalError;
import com.github.pgasync.SqlError.ServerLocatorException;
import com.github.pgasync.SqlError.ServerPlPgSQLError;
import com.github.pgasync.SqlError.ServerResponse;
import com.github.pgasync.SqlError.ServerSQLRoutineException;
import com.github.pgasync.SqlError.ServerSavepointException;
import com.github.pgasync.SqlError.ServerSnapshotFailure;
import com.github.pgasync.SqlError.ServerSyntaxErrorOrAccessRuleViolation;
import com.github.pgasync.SqlError.ServerSystemError;
import com.github.pgasync.SqlError.ServerTriggeredActionException;
import com.github.pgasync.SqlError.ServerWarning;
import com.github.pgasync.async.IntermediatePromise;
import com.github.pgasync.message.ExtendedQueryMessage;
import com.github.pgasync.message.Message;
import com.github.pgasync.message.backend.Authentication;
import com.github.pgasync.message.backend.BIndicators;
import com.github.pgasync.message.backend.BackendKeyData;
import com.github.pgasync.message.backend.CommandComplete;
import com.github.pgasync.message.backend.DataRow;
import com.github.pgasync.message.backend.ErrorResponse;
import com.github.pgasync.message.backend.NoticeResponse;
import com.github.pgasync.message.backend.NotificationResponse;
import com.github.pgasync.message.backend.ParameterStatus;
import com.github.pgasync.message.backend.ReadyForQuery;
import com.github.pgasync.message.backend.RowDescription;
import com.github.pgasync.message.backend.UnknownMessage;
import com.github.pgasync.message.frontend.Bind;
import com.github.pgasync.message.frontend.Describe;
import com.github.pgasync.message.frontend.Execute;
import com.github.pgasync.message.frontend.FIndicators;
import com.github.pgasync.message.frontend.PasswordMessage;
import com.github.pgasync.message.frontend.Query;
import com.github.pgasync.message.frontend.SASLInitialResponse;
import com.github.pgasync.message.frontend.SASLResponse;
import com.github.pgasync.net.SqlException;
import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Messages stream to Postgres backend.
 *
 * @author Marat Gainullin
 */
public abstract class PgProtocolStream implements ProtocolStream {
    private static final Logger log = LoggerFactory.getLogger(PgProtocolStream.class);

    protected final Charset encoding;

    private IntermediatePromise<? super Message> onResponse;
    private final Map<String, Set<Consumer<String>>> subscriptions = new HashMap<>();

    private Consumer<RowDescription.ColumnDescription[]> onColumns;
    private Consumer<DataRow> onRow;
    private Consumer<CommandComplete> onAffected;

    private boolean seenReadyForQuery;
    private Message readyForQueryPendingMessage;
    private Message lastSentMessage;

    public PgProtocolStream(Charset encoding) {
        this.encoding = encoding;
    }

    private IntermediatePromise<? super Message> consumeOnResponse() {
        var wasOnResponse = onResponse;
        onResponse = null;
        return wasOnResponse;
    }

    @Override
    public IntermediatePromise<Message> authenticate(String userName, String password, Authentication authRequired) {
        if (authRequired.saslScramSha256()) {
            var clientNonce = UUID.randomUUID().toString();
            var saslInitialResponse = new SASLInitialResponse(Authentication.SUPPORTED_SASL,
                                                              null, ""/*SaslPrep.asQueryString(userName) - Postgres requires an empty string here*/,
                                                              clientNonce);
            return send(saslInitialResponse)
                .map(message -> {
                    if (message instanceof Authentication authentication) {
                        var serverFirstMessage = authentication.saslContinueData();
                        if (serverFirstMessage != null) {
                            return send(SASLResponse.of(password,
                                                        serverFirstMessage,
                                                        clientNonce,
                                                        saslInitialResponse.gs2Header(),
                                                        saslInitialResponse.clientFirstMessageBare()));
                        } else {
                            throw new IllegalStateException("Bad SASL authentication sequence message detected on 'server-first-message' step");
                        }
                    } else {
                        throw new IllegalStateException("Bad SASL authentication sequence detected on 'server-first-message' step");
                    }
                })
                .flatMap(Fn1.id());
        } else {
            return send(PasswordMessage.passwordMessage(userName, password, authRequired.md5salt(), encoding));
        }
    }

    protected abstract void write(Message... messages);

    @Override
    public IntermediatePromise<Message> send(Message message) {
        return offerRoundTrip(() -> {
            lastSentMessage = message;
            write(message);
            if (message instanceof ExtendedQueryMessage) {
                write(FIndicators.SYNC);
            }
        });
    }

    @Override
    public IntermediatePromise<Unit> send(Query query,
                                          Consumer<RowDescription.ColumnDescription[]> onColumns,
                                          Consumer<DataRow> onRow,
                                          Consumer<CommandComplete> onAffected) {
        this.onColumns = onColumns;
        this.onRow = onRow;
        this.onAffected = onAffected;
        return send(query).onSuccess(_ -> {});
    }

    @Override
    public IntermediatePromise<Integer> send(Bind bind,
                                             Describe describe,
                                             Consumer<RowDescription.ColumnDescription[]> onColumns,
                                             Consumer<DataRow> onRow) {
        this.onColumns = onColumns;
        this.onRow = onRow;
        this.onAffected = null;
        return offerRoundTrip(() -> {
            Execute execute;
            lastSentMessage = execute = new Execute();
            write(bind, describe, execute, FIndicators.SYNC);
        }).map(commandComplete -> ((CommandComplete) commandComplete).affectedRows());
    }

    @Override
    public IntermediatePromise<Integer> send(Bind bind, Consumer<DataRow> onRow) {
        this.onColumns = null;
        this.onRow = onRow;
        this.onAffected = null;
        return offerRoundTrip(() -> {
            Execute execute;
            lastSentMessage = execute = new Execute();
            write(bind, execute, FIndicators.SYNC);
        }).map(commandComplete -> ((CommandComplete) commandComplete).affectedRows());
    }

    @Override
    public Runnable subscribe(String channel, Consumer<String> onNotification) {
        subscriptions
            .computeIfAbsent(channel, _ -> new HashSet<>())
            .add(onNotification);

        return () -> subscriptions.computeIfPresent(channel, (_, subscription) -> {
            subscription.remove(onNotification);
            return subscription.isEmpty() ? null : subscription;
        });
    }

    protected void gotException(Throwable th) {
        onColumns = null;
        onRow = null;
        onAffected = null;
        readyForQueryPendingMessage = null;
        lastSentMessage = null;

        if (onResponse != null) {
            var uponResponse = consumeOnResponse();
            Promise.runAsync(() -> uponResponse.fail(th));
        }
    }

    protected void gotMessage(Message message) {
        switch (message) {
            case NotificationResponse notification -> publish(notification);
            case BIndicators.BindComplete _ -> {}                 // op op since bulk message sequence
            case BIndicators.ParseComplete _, BIndicators.CloseComplete _ -> readyForQueryPendingMessage = message;
            case RowDescription rowDescription -> onColumns.accept(rowDescription.getColumns());
            case BIndicators.NoData _ -> onColumns.accept(new RowDescription.ColumnDescription[]{});
            case DataRow dataRow -> onRow.accept(dataRow);
            case ErrorResponse errorResponse -> {
                if (seenReadyForQuery) {
                    readyForQueryPendingMessage = message;
                } else {
                    gotException(toSqlException(errorResponse));
                }
            }
            case CommandComplete commandComplete -> {
                if (isSimpleQueryInProgress()) {
                    onAffected.accept(commandComplete);
                } else {
                    // assert !isSimpleQueryInProgress() :
                    // "During simple query message flow, CommandComplete message should be consumed only by dedicated callback,
                    // due to possibility of multiple CommandComplete messages, one per sql clause.";
                    readyForQueryPendingMessage = message;
                }
            }
            case Authentication authentication -> {
                if (authentication.authenticationOk() || authentication.saslServerFinalResponse()) {
                    readyForQueryPendingMessage = message;
                } else {
                    consumeOnResponse().resolveAsync(() -> message);
                }
            }
            case ReadyForQuery _ -> {
                seenReadyForQuery = true;
                if (readyForQueryPendingMessage instanceof ErrorResponse errorResponse) {
                    gotException(toSqlException(errorResponse));
                } else {
                    onColumns = null;
                    onRow = null;
                    onAffected = null;
                    var response = readyForQueryPendingMessage != null ? readyForQueryPendingMessage : message;
                    consumeOnResponse().resolveAsync(() -> response);
                }
                readyForQueryPendingMessage = null;
            }
            case ParameterStatus _, BackendKeyData _, UnknownMessage _ -> log.trace(message.toString());
            case NoticeResponse _ -> log.warn(message.toString());
            case null, default -> consumeOnResponse().resolveAsync(() -> message);
        }
    }

    private IntermediatePromise<Message> offerRoundTrip(Runnable requestAction) {
        return offerRoundTrip(requestAction, true);
    }

    protected IntermediatePromise<Message> offerRoundTrip(Runnable requestAction, boolean assumeConnected) {
        var uponResponse = IntermediatePromise.<Message>create();

        if (!assumeConnected || isConnected()) {
            if (onResponse == null) {
                onResponse = uponResponse;
                try {
                    requestAction.run();
                } catch (Throwable th) {
                    gotException(th);
                }
            } else {
                Promise.runAsync(() -> uponResponse.fail(new IllegalStateException(
                    "Postgres messages stream simultaneous use detected")));
            }
        } else {
            Promise.runAsync(() -> uponResponse.fail(new IllegalStateException("Channel is closed")));
        }
        return uponResponse;
    }

    private void publish(NotificationResponse notification) {
        Set<Consumer<String>> consumers = subscriptions.get(notification.getChannel());
        if (consumers != null) {
            consumers.forEach(c -> c.accept(notification.getPayload()));
        }
    }

    private boolean isSimpleQueryInProgress() {
        return lastSentMessage instanceof Query;
    }

    private boolean isExtendedQueryInProgress() {
        return lastSentMessage instanceof ExtendedQueryMessage;
    }

    private static SqlException toSqlException(ErrorResponse error) {
        return new SqlException(toSqlError(error));
    }

    private static ServerError toSqlError(ErrorResponse error) {
        return ERROR_MAPPER.getOrDefault(error.code(),
                                         response -> new SqlError.ServerUnknownError(response, "unknown_error"))
                           .apply(error.asServerResponse());
    }

    private static final Map<String, Fn1<ServerError, ServerResponse>> ERROR_MAPPER = new HashMap<>();

    static {
        ERROR_MAPPER.put("01000", response -> new ServerWarning(response, "warning"));
        ERROR_MAPPER.put("0100C", response -> new ServerWarning(response, "dynamic_result_sets_returned"));
        ERROR_MAPPER.put("01008", response -> new ServerWarning(response, "implicit_zero_bit_padding"));
        ERROR_MAPPER.put("01003", response -> new ServerWarning(response, "null_value_eliminated_in_set_function"));
        ERROR_MAPPER.put("01007", response -> new ServerWarning(response, "privilege_not_granted"));
        ERROR_MAPPER.put("01006", response -> new ServerWarning(response, "privilege_not_revoked"));
        ERROR_MAPPER.put("01004", response -> new ServerWarning(response, "string_data_right_truncation"));
        ERROR_MAPPER.put("01P01", response -> new ServerWarning(response, "deprecated_feature"));
        ERROR_MAPPER.put("02000", response -> new ServerErrorNoData(response, "no_data"));
        ERROR_MAPPER.put("02001", response -> new ServerErrorNoData(response, "no_additional_dynamic_result_sets_returned"));
        ERROR_MAPPER.put("03000", response -> new ServerErrorSQLStatementNotYetComplete(response, "sql_statement_not_yet_complete"));
        ERROR_MAPPER.put("08000", response -> new ServerConnectionException(response, "connection_exception"));
        ERROR_MAPPER.put("08003", response -> new ServerConnectionException(response, "connection_does_not_exist"));
        ERROR_MAPPER.put("08006", response -> new ServerConnectionException(response, "connection_failure"));
        ERROR_MAPPER.put("08001", response -> new ServerConnectionException(response, "sqlclient_unable_to_establish_sqlconnection"));
        ERROR_MAPPER.put("08004", response -> new ServerConnectionException(response, "sqlserver_rejected_establishment_of_sqlconnection"));
        ERROR_MAPPER.put("08007", response -> new ServerConnectionException(response, "transaction_resolution_unknown"));
        ERROR_MAPPER.put("08P01", response -> new ServerConnectionException(response, "protocol_violation"));
        ERROR_MAPPER.put("09000", response -> new ServerTriggeredActionException(response, "triggered_action_exception"));
        ERROR_MAPPER.put("0A000", response -> new ServerErrorFeatureNotSupported(response, "feature_not_supported"));
        ERROR_MAPPER.put("0B000", response -> new ServerErrorInvalidTransactionInitiation(response, "invalid_transaction_initiation"));
        ERROR_MAPPER.put("0F000", response -> new ServerLocatorException(response, "locator_exception"));
        ERROR_MAPPER.put("0F001", response -> new ServerLocatorException(response, "invalid_locator_specification"));
        ERROR_MAPPER.put("0L000", response -> new ServerErrorInvalidGrantor(response, "invalid_grantor"));
        ERROR_MAPPER.put("0LP01", response -> new ServerErrorInvalidGrantor(response, "invalid_grant_operation"));
        ERROR_MAPPER.put("0P000", response -> new ServerErrorInvalidRoleSpecification(response, "invalid_role_specification"));
        ERROR_MAPPER.put("0Z000", response -> new ServerDiagnosticsException(response, "diagnostics_exception"));
        ERROR_MAPPER.put("0Z002", response -> new ServerDiagnosticsException(response, "stacked_diagnostics_accessed_without_active_handler"));
        ERROR_MAPPER.put("20000", response -> new ServerErrorCaseNotFound(response, "case_not_found"));
        ERROR_MAPPER.put("21000", response -> new ServerErrorCardinalityViolation(response, "cardinality_violation"));
        ERROR_MAPPER.put("22000", response -> new ServerDataException(response, "data_exception"));
        ERROR_MAPPER.put("2202E", response -> new ServerDataException(response, "array_subscript_error"));
        ERROR_MAPPER.put("22021", response -> new ServerDataException(response, "character_not_in_repertoire"));
        ERROR_MAPPER.put("22008", response -> new ServerDataException(response, "datetime_field_overflow"));
        ERROR_MAPPER.put("22012", response -> new ServerDataException(response, "division_by_zero"));
        ERROR_MAPPER.put("22005", response -> new ServerDataException(response, "error_in_assignment"));
        ERROR_MAPPER.put("2200B", response -> new ServerDataException(response, "escape_character_conflict"));
        ERROR_MAPPER.put("22022", response -> new ServerDataException(response, "indicator_overflow"));
        ERROR_MAPPER.put("22015", response -> new ServerDataException(response, "interval_field_overflow"));
        ERROR_MAPPER.put("2201E", response -> new ServerDataException(response, "invalid_argument_for_logarithm"));
        ERROR_MAPPER.put("22014", response -> new ServerDataException(response, "invalid_argument_for_ntile_function"));
        ERROR_MAPPER.put("22016", response -> new ServerDataException(response, "invalid_argument_for_nth_value_function"));
        ERROR_MAPPER.put("2201F", response -> new ServerDataException(response, "invalid_argument_for_power_function"));
        ERROR_MAPPER.put("2201G", response -> new ServerDataException(response, "invalid_argument_for_width_bucket_function"));
        ERROR_MAPPER.put("22018", response -> new ServerDataException(response, "invalid_character_value_for_cast"));
        ERROR_MAPPER.put("22007", response -> new ServerDataException(response, "invalid_datetime_format"));
        ERROR_MAPPER.put("22019", response -> new ServerDataException(response, "invalid_escape_character"));
        ERROR_MAPPER.put("2200D", response -> new ServerDataException(response, "invalid_escape_octet"));
        ERROR_MAPPER.put("22025", response -> new ServerDataException(response, "invalid_escape_sequence"));
        ERROR_MAPPER.put("22P06", response -> new ServerDataException(response, "nonstandard_use_of_escape_character"));
        ERROR_MAPPER.put("22010", response -> new ServerDataException(response, "invalid_indicator_parameter_value"));
        ERROR_MAPPER.put("22023", response -> new ServerDataException(response, "invalid_parameter_value"));
        ERROR_MAPPER.put("22013", response -> new ServerDataException(response, "invalid_preceding_or_following_size"));
        ERROR_MAPPER.put("2201B", response -> new ServerDataException(response, "invalid_regular_expression"));
        ERROR_MAPPER.put("2201W", response -> new ServerDataException(response, "invalid_row_count_in_limit_clause"));
        ERROR_MAPPER.put("2201X", response -> new ServerDataException(response, "invalid_row_count_in_result_offset_clause"));
        ERROR_MAPPER.put("2202H", response -> new ServerDataException(response, "invalid_tablesample_argument"));
        ERROR_MAPPER.put("2202G", response -> new ServerDataException(response, "invalid_tablesample_repeat"));
        ERROR_MAPPER.put("22009", response -> new ServerDataException(response, "invalid_time_zone_displacement_value"));
        ERROR_MAPPER.put("2200C", response -> new ServerDataException(response, "invalid_use_of_escape_character"));
        ERROR_MAPPER.put("2200G", response -> new ServerDataException(response, "most_specific_type_mismatch"));
        ERROR_MAPPER.put("22004", response -> new ServerDataException(response, "null_value_not_allowed"));
        ERROR_MAPPER.put("22002", response -> new ServerDataException(response, "null_value_no_indicator_parameter"));
        ERROR_MAPPER.put("22003", response -> new ServerDataException(response, "numeric_value_out_of_range"));
        ERROR_MAPPER.put("2200H", response -> new ServerDataException(response, "sequence_generator_limit_exceeded"));
        ERROR_MAPPER.put("22026", response -> new ServerDataException(response, "string_data_length_mismatch"));
        ERROR_MAPPER.put("22001", response -> new ServerDataException(response, "string_data_right_truncation"));
        ERROR_MAPPER.put("22011", response -> new ServerDataException(response, "substring_error"));
        ERROR_MAPPER.put("22027", response -> new ServerDataException(response, "trim_error"));
        ERROR_MAPPER.put("22024", response -> new ServerDataException(response, "unterminated_c_string"));
        ERROR_MAPPER.put("2200F", response -> new ServerDataException(response, "zero_length_character_string"));
        ERROR_MAPPER.put("22P01", response -> new ServerDataException(response, "floating_point_exception"));
        ERROR_MAPPER.put("22P02", response -> new ServerDataException(response, "invalid_text_representation"));
        ERROR_MAPPER.put("22P03", response -> new ServerDataException(response, "invalid_binary_representation"));
        ERROR_MAPPER.put("22P04", response -> new ServerDataException(response, "bad_copy_file_format"));
        ERROR_MAPPER.put("22P05", response -> new ServerDataException(response, "untranslatable_character"));
        ERROR_MAPPER.put("2200L", response -> new ServerDataException(response, "not_an_xml_document"));
        ERROR_MAPPER.put("2200M", response -> new ServerDataException(response, "invalid_xml_document"));
        ERROR_MAPPER.put("2200N", response -> new ServerDataException(response, "invalid_xml_content"));
        ERROR_MAPPER.put("2200S", response -> new ServerDataException(response, "invalid_xml_comment"));
        ERROR_MAPPER.put("2200T", response -> new ServerDataException(response, "invalid_xml_processing_instruction"));
        ERROR_MAPPER.put("22030", response -> new ServerDataException(response, "duplicate_json_object_key_value"));
        ERROR_MAPPER.put("22031", response -> new ServerDataException(response, "invalid_argument_for_sql_json_datetime_function"));
        ERROR_MAPPER.put("22032", response -> new ServerDataException(response, "invalid_json_text"));
        ERROR_MAPPER.put("22033", response -> new ServerDataException(response, "invalid_sql_json_subscript"));
        ERROR_MAPPER.put("22034", response -> new ServerDataException(response, "more_than_one_sql_json_item"));
        ERROR_MAPPER.put("22035", response -> new ServerDataException(response, "no_sql_json_item"));
        ERROR_MAPPER.put("22036", response -> new ServerDataException(response, "non_numeric_sql_json_item"));
        ERROR_MAPPER.put("22037", response -> new ServerDataException(response, "non_unique_keys_in_a_json_object"));
        ERROR_MAPPER.put("22038", response -> new ServerDataException(response, "singleton_sql_json_item_required"));
        ERROR_MAPPER.put("22039", response -> new ServerDataException(response, "sql_json_array_not_found"));
        ERROR_MAPPER.put("2203A", response -> new ServerDataException(response, "sql_json_member_not_found"));
        ERROR_MAPPER.put("2203B", response -> new ServerDataException(response, "sql_json_number_not_found"));
        ERROR_MAPPER.put("2203C", response -> new ServerDataException(response, "sql_json_object_not_found"));
        ERROR_MAPPER.put("2203D", response -> new ServerDataException(response, "too_many_json_array_elements"));
        ERROR_MAPPER.put("2203E", response -> new ServerDataException(response, "too_many_json_object_members"));
        ERROR_MAPPER.put("2203F", response -> new ServerDataException(response, "sql_json_scalar_required"));
        ERROR_MAPPER.put("2203G", response -> new ServerDataException(response, "sql_json_item_cannot_be_cast_to_target_type"));
        ERROR_MAPPER.put("23000", response -> new ServerErrorIntegrityConstraintViolation(response, "integrity_constraint_violation"));
        ERROR_MAPPER.put("23001", response -> new ServerErrorIntegrityConstraintViolation(response, "restrict_violation"));
        ERROR_MAPPER.put("23502", response -> new ServerErrorIntegrityConstraintViolation(response, "not_null_violation"));
        ERROR_MAPPER.put("23503", response -> new ServerErrorIntegrityConstraintViolation(response, "foreign_key_violation"));
        ERROR_MAPPER.put("23505", response -> new ServerErrorIntegrityConstraintViolation(response, "unique_violation"));
        ERROR_MAPPER.put("23514", response -> new ServerErrorIntegrityConstraintViolation(response, "check_violation"));
        ERROR_MAPPER.put("23P01", response -> new ServerErrorIntegrityConstraintViolation(response, "exclusion_violation"));
        ERROR_MAPPER.put("24000", response -> new ServerErrorInvalidCursorState(response, "invalid_cursor_state"));
        ERROR_MAPPER.put("25000", response -> new ServerErrorInvalidTransactionState(response, "invalid_transaction_state"));
        ERROR_MAPPER.put("25001", response -> new ServerErrorInvalidTransactionState(response, "active_sql_transaction"));
        ERROR_MAPPER.put("25002", response -> new ServerErrorInvalidTransactionState(response, "branch_transaction_already_active"));
        ERROR_MAPPER.put("25008", response -> new ServerErrorInvalidTransactionState(response, "held_cursor_requires_same_isolation_level"));
        ERROR_MAPPER.put("25003", response -> new ServerErrorInvalidTransactionState(response, "inappropriate_access_mode_for_branch_transaction"));
        ERROR_MAPPER.put("25004",
                         response -> new ServerErrorInvalidTransactionState(response, "inappropriate_isolation_level_for_branch_transaction"));
        ERROR_MAPPER.put("25005", response -> new ServerErrorInvalidTransactionState(response, "no_active_sql_transaction_for_branch_transaction"));
        ERROR_MAPPER.put("25006", response -> new ServerErrorInvalidTransactionState(response, "read_only_sql_transaction"));
        ERROR_MAPPER.put("25007", response -> new ServerErrorInvalidTransactionState(response, "schema_and_data_statement_mixing_not_supported"));
        ERROR_MAPPER.put("25P01", response -> new ServerErrorInvalidTransactionState(response, "no_active_sql_transaction"));
        ERROR_MAPPER.put("25P02", response -> new ServerErrorInvalidTransactionState(response, "in_failed_sql_transaction"));
        ERROR_MAPPER.put("25P03", response -> new ServerErrorInvalidTransactionState(response, "idle_in_transaction_session_timeout"));
        ERROR_MAPPER.put("26000", response -> new ServerErrorInvalidSQLStatementName(response, "invalid_sql_statement_name"));
        ERROR_MAPPER.put("27000", response -> new ServerErrorTriggeredDataChangeViolation(response, "triggered_data_change_violation"));
        ERROR_MAPPER.put("28000", response -> new ServerErrorInvalidAuthorizationSpecification(response, "invalid_authorization_specification"));
        ERROR_MAPPER.put("28P01", response -> new ServerErrorInvalidAuthorizationSpecification(response, "invalid_password"));
        ERROR_MAPPER.put("2B000",
                         response -> new ServerErrorDependentPrivilegeDescriptorsStillExist(response, "dependent_privilege_descriptors_still_exist"));
        ERROR_MAPPER.put("2BP01", response -> new ServerErrorDependentPrivilegeDescriptorsStillExist(response, "dependent_objects_still_exist"));
        ERROR_MAPPER.put("2D000", response -> new ServerErrorInvalidTransactionTermination(response, "invalid_transaction_termination"));
        ERROR_MAPPER.put("2F000", response -> new ServerSQLRoutineException(response, "sql_routine_exception"));
        ERROR_MAPPER.put("2F005", response -> new ServerSQLRoutineException(response, "function_executed_no_return_statement"));
        ERROR_MAPPER.put("2F002", response -> new ServerSQLRoutineException(response, "modifying_sql_data_not_permitted"));
        ERROR_MAPPER.put("2F003", response -> new ServerSQLRoutineException(response, "prohibited_sql_statement_attempted"));
        ERROR_MAPPER.put("2F004", response -> new ServerSQLRoutineException(response, "reading_sql_data_not_permitted"));
        ERROR_MAPPER.put("34000", response -> new ServerErrorInvalidCursorName(response, "invalid_cursor_name"));
        ERROR_MAPPER.put("38000", response -> new ServerExternalRoutineException(response, "external_routine_exception"));
        ERROR_MAPPER.put("38001", response -> new ServerExternalRoutineException(response, "containing_sql_not_permitted"));
        ERROR_MAPPER.put("38002", response -> new ServerExternalRoutineException(response, "modifying_sql_data_not_permitted"));
        ERROR_MAPPER.put("38003", response -> new ServerExternalRoutineException(response, "prohibited_sql_statement_attempted"));
        ERROR_MAPPER.put("38004", response -> new ServerExternalRoutineException(response, "reading_sql_data_not_permitted"));
        ERROR_MAPPER.put("39000", response -> new ServerExternalRoutineInvocationException(response, "external_routine_invocation_exception"));
        ERROR_MAPPER.put("39001", response -> new ServerExternalRoutineInvocationException(response, "invalid_sqlstate_returned"));
        ERROR_MAPPER.put("39004", response -> new ServerExternalRoutineInvocationException(response, "null_value_not_allowed"));
        ERROR_MAPPER.put("39P01", response -> new ServerExternalRoutineInvocationException(response, "trigger_protocol_violated"));
        ERROR_MAPPER.put("39P02", response -> new ServerExternalRoutineInvocationException(response, "srf_protocol_violated"));
        ERROR_MAPPER.put("39P03", response -> new ServerExternalRoutineInvocationException(response, "event_trigger_protocol_violated"));
        ERROR_MAPPER.put("3B000", response -> new ServerSavepointException(response, "savepoint_exception"));
        ERROR_MAPPER.put("3B001", response -> new ServerSavepointException(response, "invalid_savepoint_specification"));
        ERROR_MAPPER.put("3D000", response -> new ServerErrorInvalidCatalogName(response, "invalid_catalog_name"));
        ERROR_MAPPER.put("3F000", response -> new ServerErrorInvalidSchemaName(response, "invalid_schema_name"));
        ERROR_MAPPER.put("40000", response -> new ServerErrorTransactionRollback(response, "transaction_rollback"));
        ERROR_MAPPER.put("40002", response -> new ServerErrorTransactionRollback(response, "transaction_integrity_constraint_violation"));
        ERROR_MAPPER.put("40001", response -> new ServerErrorTransactionRollback(response, "serialization_failure"));
        ERROR_MAPPER.put("40003", response -> new ServerErrorTransactionRollback(response, "statement_completion_unknown"));
        ERROR_MAPPER.put("40P01", response -> new ServerErrorTransactionRollback(response, "deadlock_detected"));
        ERROR_MAPPER.put("42000", response -> new ServerSyntaxErrorOrAccessRuleViolation(response, "syntax_error_or_access_rule_violation"));
        ERROR_MAPPER.put("42601", response -> new ServerSyntaxErrorOrAccessRuleViolation(response, "syntax_error"));
        ERROR_MAPPER.put("42501", response -> new ServerSyntaxErrorOrAccessRuleViolation(response, "insufficient_privilege"));
        ERROR_MAPPER.put("42846", response -> new ServerSyntaxErrorOrAccessRuleViolation(response, "cannot_coerce"));
        ERROR_MAPPER.put("42803", response -> new ServerSyntaxErrorOrAccessRuleViolation(response, "grouping_error"));
        ERROR_MAPPER.put("42P20", response -> new ServerSyntaxErrorOrAccessRuleViolation(response, "windowing_error"));
        ERROR_MAPPER.put("42P19", response -> new ServerSyntaxErrorOrAccessRuleViolation(response, "invalid_recursion"));
        ERROR_MAPPER.put("42830", response -> new ServerSyntaxErrorOrAccessRuleViolation(response, "invalid_foreign_key"));
        ERROR_MAPPER.put("42602", response -> new ServerSyntaxErrorOrAccessRuleViolation(response, "invalid_name"));
        ERROR_MAPPER.put("42622", response -> new ServerSyntaxErrorOrAccessRuleViolation(response, "name_too_long"));
        ERROR_MAPPER.put("42939", response -> new ServerSyntaxErrorOrAccessRuleViolation(response, "reserved_name"));
        ERROR_MAPPER.put("42804", response -> new ServerSyntaxErrorOrAccessRuleViolation(response, "datatype_mismatch"));
        ERROR_MAPPER.put("42P18", response -> new ServerSyntaxErrorOrAccessRuleViolation(response, "indeterminate_datatype"));
        ERROR_MAPPER.put("42P21", response -> new ServerSyntaxErrorOrAccessRuleViolation(response, "collation_mismatch"));
        ERROR_MAPPER.put("42P22", response -> new ServerSyntaxErrorOrAccessRuleViolation(response, "indeterminate_collation"));
        ERROR_MAPPER.put("42809", response -> new ServerSyntaxErrorOrAccessRuleViolation(response, "wrong_object_type"));
        ERROR_MAPPER.put("428C9", response -> new ServerSyntaxErrorOrAccessRuleViolation(response, "generated_always"));
        ERROR_MAPPER.put("42703", response -> new ServerSyntaxErrorOrAccessRuleViolation(response, "undefined_column"));
        ERROR_MAPPER.put("42883", response -> new ServerSyntaxErrorOrAccessRuleViolation(response, "undefined_function"));
        ERROR_MAPPER.put("42P01", response -> new ServerSyntaxErrorOrAccessRuleViolation(response, "undefined_table"));
        ERROR_MAPPER.put("42P02", response -> new ServerSyntaxErrorOrAccessRuleViolation(response, "undefined_parameter"));
        ERROR_MAPPER.put("42704", response -> new ServerSyntaxErrorOrAccessRuleViolation(response, "undefined_object"));
        ERROR_MAPPER.put("42701", response -> new ServerSyntaxErrorOrAccessRuleViolation(response, "duplicate_column"));
        ERROR_MAPPER.put("42P03", response -> new ServerSyntaxErrorOrAccessRuleViolation(response, "duplicate_cursor"));
        ERROR_MAPPER.put("42P04", response -> new ServerSyntaxErrorOrAccessRuleViolation(response, "duplicate_database"));
        ERROR_MAPPER.put("42723", response -> new ServerSyntaxErrorOrAccessRuleViolation(response, "duplicate_function"));
        ERROR_MAPPER.put("42P05", response -> new ServerSyntaxErrorOrAccessRuleViolation(response, "duplicate_prepared_statement"));
        ERROR_MAPPER.put("42P06", response -> new ServerSyntaxErrorOrAccessRuleViolation(response, "duplicate_schema"));
        ERROR_MAPPER.put("42P07", response -> new ServerSyntaxErrorOrAccessRuleViolation(response, "duplicate_table"));
        ERROR_MAPPER.put("42712", response -> new ServerSyntaxErrorOrAccessRuleViolation(response, "duplicate_alias"));
        ERROR_MAPPER.put("42710", response -> new ServerSyntaxErrorOrAccessRuleViolation(response, "duplicate_object"));
        ERROR_MAPPER.put("42702", response -> new ServerSyntaxErrorOrAccessRuleViolation(response, "ambiguous_column"));
        ERROR_MAPPER.put("42725", response -> new ServerSyntaxErrorOrAccessRuleViolation(response, "ambiguous_function"));
        ERROR_MAPPER.put("42P08", response -> new ServerSyntaxErrorOrAccessRuleViolation(response, "ambiguous_parameter"));
        ERROR_MAPPER.put("42P09", response -> new ServerSyntaxErrorOrAccessRuleViolation(response, "ambiguous_alias"));
        ERROR_MAPPER.put("42P10", response -> new ServerSyntaxErrorOrAccessRuleViolation(response, "invalid_column_reference"));
        ERROR_MAPPER.put("42611", response -> new ServerSyntaxErrorOrAccessRuleViolation(response, "invalid_column_definition"));
        ERROR_MAPPER.put("42P11", response -> new ServerSyntaxErrorOrAccessRuleViolation(response, "invalid_cursor_definition"));
        ERROR_MAPPER.put("42P12", response -> new ServerSyntaxErrorOrAccessRuleViolation(response, "invalid_database_definition"));
        ERROR_MAPPER.put("42P13", response -> new ServerSyntaxErrorOrAccessRuleViolation(response, "invalid_function_definition"));
        ERROR_MAPPER.put("42P14", response -> new ServerSyntaxErrorOrAccessRuleViolation(response, "invalid_prepared_statement_definition"));
        ERROR_MAPPER.put("42P15", response -> new ServerSyntaxErrorOrAccessRuleViolation(response, "invalid_schema_definition"));
        ERROR_MAPPER.put("42P16", response -> new ServerSyntaxErrorOrAccessRuleViolation(response, "invalid_table_definition"));
        ERROR_MAPPER.put("42P17", response -> new ServerSyntaxErrorOrAccessRuleViolation(response, "invalid_object_definition"));
        ERROR_MAPPER.put("44000", response -> new ServerErrorWithCheckOptionViolation(response, "with_check_option_violation"));
        ERROR_MAPPER.put("53000", response -> new ServerErrorInsufficientResources(response, "insufficient_resources"));
        ERROR_MAPPER.put("53100", response -> new ServerErrorInsufficientResources(response, "disk_full"));
        ERROR_MAPPER.put("53200", response -> new ServerErrorInsufficientResources(response, "out_of_memory"));
        ERROR_MAPPER.put("53300", response -> new ServerErrorInsufficientResources(response, "too_many_connections"));
        ERROR_MAPPER.put("53400", response -> new ServerErrorInsufficientResources(response, "configuration_limit_exceeded"));
        ERROR_MAPPER.put("54000", response -> new ServerErrorProgramLimitExceeded(response, "program_limit_exceeded"));
        ERROR_MAPPER.put("54001", response -> new ServerErrorProgramLimitExceeded(response, "statement_too_complex"));
        ERROR_MAPPER.put("54011", response -> new ServerErrorProgramLimitExceeded(response, "too_many_columns"));
        ERROR_MAPPER.put("54023", response -> new ServerErrorProgramLimitExceeded(response, "too_many_arguments"));
        ERROR_MAPPER.put("55000", response -> new ServerErrorObjectNotInPrerequisiteState(response, "object_not_in_prerequisite_state"));
        ERROR_MAPPER.put("55006", response -> new ServerErrorObjectNotInPrerequisiteState(response, "object_in_use"));
        ERROR_MAPPER.put("55P02", response -> new ServerErrorObjectNotInPrerequisiteState(response, "cant_change_runtime_param"));
        ERROR_MAPPER.put("55P03", response -> new ServerErrorObjectNotInPrerequisiteState(response, "lock_not_available"));
        ERROR_MAPPER.put("55P04", response -> new ServerErrorObjectNotInPrerequisiteState(response, "unsafe_new_enum_value_usage"));
        ERROR_MAPPER.put("57000", response -> new ServerErrorOperatorIntervention(response, "operator_intervention"));
        ERROR_MAPPER.put("57014", response -> new ServerErrorOperatorIntervention(response, "query_canceled"));
        ERROR_MAPPER.put("57P01", response -> new ServerErrorOperatorIntervention(response, "admin_shutdown"));
        ERROR_MAPPER.put("57P02", response -> new ServerErrorOperatorIntervention(response, "crash_shutdown"));
        ERROR_MAPPER.put("57P03", response -> new ServerErrorOperatorIntervention(response, "cannot_connect_now"));
        ERROR_MAPPER.put("57P04", response -> new ServerErrorOperatorIntervention(response, "database_dropped"));
        ERROR_MAPPER.put("57P05", response -> new ServerErrorOperatorIntervention(response, "idle_session_timeout"));
        ERROR_MAPPER.put("58000", response -> new ServerSystemError(response, "system_error"));
        ERROR_MAPPER.put("58030", response -> new ServerSystemError(response, "io_error"));
        ERROR_MAPPER.put("58P01", response -> new ServerSystemError(response, "undefined_file"));
        ERROR_MAPPER.put("58P02", response -> new ServerSystemError(response, "duplicate_file"));
        ERROR_MAPPER.put("72000", response -> new ServerSnapshotFailure(response, "snapshot_too_old"));
        ERROR_MAPPER.put("F0000", response -> new ServerConfigurationFileError(response, "config_file_error"));
        ERROR_MAPPER.put("F0001", response -> new ServerConfigurationFileError(response, "lock_file_exists"));
        ERROR_MAPPER.put("HV000", response -> new ServerForeignDataWrapperError(response, "fdw_error"));
        ERROR_MAPPER.put("HV005", response -> new ServerForeignDataWrapperError(response, "fdw_column_name_not_found"));
        ERROR_MAPPER.put("HV002", response -> new ServerForeignDataWrapperError(response, "fdw_dynamic_parameter_value_needed"));
        ERROR_MAPPER.put("HV010", response -> new ServerForeignDataWrapperError(response, "fdw_function_sequence_error"));
        ERROR_MAPPER.put("HV021", response -> new ServerForeignDataWrapperError(response, "fdw_inconsistent_descriptor_information"));
        ERROR_MAPPER.put("HV024", response -> new ServerForeignDataWrapperError(response, "fdw_invalid_attribute_value"));
        ERROR_MAPPER.put("HV007", response -> new ServerForeignDataWrapperError(response, "fdw_invalid_column_name"));
        ERROR_MAPPER.put("HV008", response -> new ServerForeignDataWrapperError(response, "fdw_invalid_column_number"));
        ERROR_MAPPER.put("HV004", response -> new ServerForeignDataWrapperError(response, "fdw_invalid_data_type"));
        ERROR_MAPPER.put("HV006", response -> new ServerForeignDataWrapperError(response, "fdw_invalid_data_type_descriptors"));
        ERROR_MAPPER.put("HV091", response -> new ServerForeignDataWrapperError(response, "fdw_invalid_descriptor_field_identifier"));
        ERROR_MAPPER.put("HV00B", response -> new ServerForeignDataWrapperError(response, "fdw_invalid_handle"));
        ERROR_MAPPER.put("HV00C", response -> new ServerForeignDataWrapperError(response, "fdw_invalid_option_index"));
        ERROR_MAPPER.put("HV00D", response -> new ServerForeignDataWrapperError(response, "fdw_invalid_option_name"));
        ERROR_MAPPER.put("HV090", response -> new ServerForeignDataWrapperError(response, "fdw_invalid_string_length_or_buffer_length"));
        ERROR_MAPPER.put("HV00A", response -> new ServerForeignDataWrapperError(response, "fdw_invalid_string_format"));
        ERROR_MAPPER.put("HV009", response -> new ServerForeignDataWrapperError(response, "fdw_invalid_use_of_null_pointer"));
        ERROR_MAPPER.put("HV014", response -> new ServerForeignDataWrapperError(response, "fdw_too_many_handles"));
        ERROR_MAPPER.put("HV001", response -> new ServerForeignDataWrapperError(response, "fdw_out_of_memory"));
        ERROR_MAPPER.put("HV00P", response -> new ServerForeignDataWrapperError(response, "fdw_no_schemas"));
        ERROR_MAPPER.put("HV00J", response -> new ServerForeignDataWrapperError(response, "fdw_option_name_not_found"));
        ERROR_MAPPER.put("HV00K", response -> new ServerForeignDataWrapperError(response, "fdw_reply_handle"));
        ERROR_MAPPER.put("HV00Q", response -> new ServerForeignDataWrapperError(response, "fdw_schema_not_found"));
        ERROR_MAPPER.put("HV00R", response -> new ServerForeignDataWrapperError(response, "fdw_table_not_found"));
        ERROR_MAPPER.put("HV00L", response -> new ServerForeignDataWrapperError(response, "fdw_unable_to_create_execution"));
        ERROR_MAPPER.put("HV00M", response -> new ServerForeignDataWrapperError(response, "fdw_unable_to_create_reply"));
        ERROR_MAPPER.put("HV00N", response -> new ServerForeignDataWrapperError(response, "fdw_unable_to_establish_connection"));
        ERROR_MAPPER.put("P0000", response -> new ServerPlPgSQLError(response, "plpgsql_error"));
        ERROR_MAPPER.put("P0001", response -> new ServerPlPgSQLError(response, "raise_exception"));
        ERROR_MAPPER.put("P0002", response -> new ServerPlPgSQLError(response, "no_data_found"));
        ERROR_MAPPER.put("P0003", response -> new ServerPlPgSQLError(response, "too_many_rows"));
        ERROR_MAPPER.put("P0004", response -> new ServerPlPgSQLError(response, "assert_failure"));
        ERROR_MAPPER.put("XX000", response -> new ServerInternalError(response, "internal_error"));
        ERROR_MAPPER.put("XX001", response -> new ServerInternalError(response, "data_corrupted"));
        ERROR_MAPPER.put("XX002", response -> new ServerInternalError(response, "index_corrupted"));
    }
}

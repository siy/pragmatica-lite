package org.pragmatica.json;

import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Verify;
import org.pragmatica.lang.parse.Number;

public interface InternalUseCase {

    // No raw strings/integers/etc. Business logic operates in some types - express them explicitly.

    // Field validation lives in value objects.
    record Value1(String value) {
        public static Result<Value1> value1(String value) {
            return Verify.ensure(ServiceError.MISSING_VALUE1, value, Verify.Is::notNull)
                         .map(Value1::new);
        }
    }

    record Value2(int value) {
        public static Result<Value2> value2(String value) {
            return Verify.ensure(ServiceError.MISSING_VALUE2, value, Verify.Is::notNull)
                         .flatMap(Number::parseInt)
                         .mapError(_ -> ServiceError.VALUE2_MUST_BE_DIGIT)
                         .map(Value2::new);
        }
    }

    // Cross-field validation (i.e. business rules) lives in the request object.
    record Request(Value1 value1, Value2 value2) {
        public static Result<Request> request(String value1, String value2) {

        }
    }

    private static final ObjectMapper jsonMapper = new ObjectMapper();

    enum ServiceError implements Cause {
        JSON_PARSE_ERROR("Invalid request body", 1),
        MISSING_VALUE1("Missing value1", 3),
        MISSING_VALUE2("Missing value2", 4),
        VALUE2_MUST_BE_DIGIT("Value2 must be digit", 5);

        private final String message;
        private final int errorCode;

        ServiceError(String message, int errorCode) {
            this.message = message;
            this.errorCode = errorCode;
        }

        public int errorCode() {
            return errorCode;
        }

        @Override
        public String message() {
            return message;
        }
//
//        record JsonParseError(String message, int errorCode) implements ServiceError {}
//
//        record InvalidUserInputError(String message, int errorCode) implements ServiceError {}
//
//        record BusinessRuleError(String message, int errorCode) implements ServiceError {}
    }

    public record InputDTO(String value1, String value2) {}

    public record OutputDTO(String one, String two) {}

    public static Result<ServiceError, OutputDTO> create(String requestBody) {

        return Result.of(parseRequestBody(requestBody))

                     .flatMap(InternalUseCase::validateInput)

                     .flatMap(InternalUseCase::validateBusinessRules)

                     .onOk(InternalUseCase::writeToDatabase)

                     .onOk(InternalUseCase::produceAuditLogKafkaMessage)

                     .map(_ -> new OutputDTO("Thank", "you"));

    }

    private static Result<ServiceError, InputDTO> parseRequestBody(String requestBody) {

        try {

            return Result.ok(jsonMapper.readValue(requestBody.getBytes(), InputDTO.class));

        } catch (Exception ex) {

            return Result.err(new JsonParseError("Invalid request body", 1));

        }

    }

    private static Result<ServiceError, InputDTO> validateInput(InputDTO inputDTO) {

        return Result.<ServiceError, InputDTO>of(inputDTO)

                     .filter(Objects::nonNull, _ -> new InvalidUserInputError("Missing input", 2))

                     .filter(dto -> dto.value1 != null, _ -> new InvalidUserInputError("Missing value1", 3))

                     .filter(dto -> dto.value2 != null, _ -> new InvalidUserInputError("Missing value2", 4))

                     .tryOf(dto -> Integer.parseInt(dto.value2),
                            _ -> new InvalidUserInputError("Value2 must be digit", 5))

                     .fold(Result::err, _ -> Result.ok(inputDTO));

    }

    private static Result<ServiceError, InputDTO> validateBusinessRules(InputDTO inputDTO) {

        if (inputDTO.value1.contains("business") || inputDTO.value2.contains("2")) {
            return Result.err(new BusinessRuleError("Illegal values", 6));
        } else {
            return Result.ok(inputDTO);
        }

    }

    private static void writeToDatabase(InputDTO inputDTO) {

// Write to database, performing a sideeffect.

// Throws exception on technical errors like connection problems etc.

// No functional error handling on errors you can't do anything about.

    }

    private static void produceAuditLogKafkaMessage(InputDTO inputDTO) {

        Thread.ofVirtual().start(() -> {

// Produce Kafka message, performing a sideeffect.

        });

    }

}
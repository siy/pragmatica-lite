package org.pragmatica.json;

public class InternalService {

    private static final ObjectMapper jsonMapper = new ObjectMapper();

    public sealed interface ServiceError extends Result.Err {

        String message();

        int errorCode();

        record JsonParseError(String message, int errorCode) implements ServiceError {}

        record InvalidUserInputError(String message, int errorCode) implements ServiceError {}

        record BusinessRuleError(String message, int errorCode) implements ServiceError {}

    }

    public record InputDTO(String value1, String value2) {}

    public record OutputDTO(String one, String two) {}

    public static Result<ServiceError, OutputDTO> create(String requestBody) {

        return Result.of(parseRequestBody(requestBody))

                     .flatMap(InternalService::validateInput)

                     .flatMap(InternalService::validateBusinessRules)

                     .onOk(InternalService::writeToDatabase)

                     .onOk(InternalService::produceAuditLogKafkaMessage)

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
package org.pragmatica.examples.promise;

import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.io.TimeSpan;
import org.pragmatica.lang.utils.Causes;
import org.pragmatica.lang.utils.Retry;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

import static org.pragmatica.lang.io.TimeSpan.timeSpan;
import static org.pragmatica.lang.utils.Retry.BackoffStrategy.*;
import static org.pragmatica.lang.utils.Retry.create;

class SequentialPromiseOperationExample {
    private final UserRepository userRepository = _ -> Causes.cause("User not found").promise();
    private final OrderRepository orderRepository = _ -> Causes.cause("Order not found").promise();
    private final InvoiceService invoiceService = _ -> Causes.cause("Invoice generation failed").promise();
    private final EmailService emailService = _ -> {
    };
    private final LogService logService = (_, _) -> {
    };

    record UserId(String id) {}

    record User(UserId id, String name) {}

    record Order(UserId userId, String description) {}

    record Invoice(List<Order> orders) {}

    interface UserRepository {
        Promise<User> findUserById(UserId userId);
    }

    interface OrderRepository {
        Promise<List<Order>> findOrdersByUser(User user);
    }

    interface InvoiceService {
        Promise<Invoice> createInvoice(List<Order> orders);
    }

    interface EmailService {
        void sendInvoice(Invoice invoice);
    }

    interface LogService {
        void logError(String message, Cause cause);
    }

    // Example of sequential asynchronous operations with Promise
    Promise<Invoice> processUserOrders(UserId userId) {
        // Chain the operations sequentially
        return userRepository.findUserById(userId)
                             .flatMap(orderRepository::findOrdersByUser)
                             .flatMap(invoiceService::createInvoice)
                             .onSuccess(emailService::sendInvoice)
                             .onFailure(cause -> logService.logError("Invoice generation failed", cause));
    }

    private final PaymentService paymentService = _ -> Causes.cause("Payment failed").promise();

    record Amount(BigDecimal value) {}

    record Payment(UserId userId, Amount amount, Currency currency) {}

    record PaymentConfirmation(String message) {}

    interface PaymentService {
        Promise<PaymentConfirmation> processPayment(Payment payment);
    }

    private Retry retry = Retry.create()
                               .attempts(5)
                               .strategy(fixed().interval(timeSpan(2).seconds()));

    Promise<PaymentConfirmation> processPayment(Payment payment) {
        return retry.execute(() -> paymentService.processPayment(payment));
    }

    void configureRetry() {
        var linear = Retry.create()
                          .attempts(5)
                          .strategy(linear().initialDelay(timeSpan(50L).millis())
                                            .increment(timeSpan(50L).millis())
                                            .maxDelay(timeSpan(1).seconds()));

    }
}
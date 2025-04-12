package org.pragmatica.examples.promise;

import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.utils.Causes;

import java.util.List;

class PromiseAnyOperationExample {
    private final WeatherService openWeatherMapService = _ -> Causes.cause("Not implemented").promise();
    private final WeatherService weatherstackService = _ -> Causes.cause("Not implemented").promise();
    private final WeatherService accuWeatherService = _ -> Causes.cause("Not implemented").promise();
    private final WeatherService NWService = _ -> Causes.cause("Not implemented").promise();

    record WeatherInfo(String city, String temperature) {}

    interface WeatherService {
        Promise<WeatherInfo> fetchWeatherInfo(String city);
    }

    Promise<WeatherInfo> fetchWeatherInfo(String city) {
        return Promise.any(openWeatherMapService.fetchWeatherInfo(city),
                           weatherstackService.fetchWeatherInfo(city),
                           accuWeatherService.fetchWeatherInfo(city),
                           NWService.fetchWeatherInfo(city));
    }


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
}
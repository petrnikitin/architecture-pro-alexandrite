package com.alexandrite.servicea;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private Tracer tracer;

    @GetMapping("/")
    public String createOrder() {
        logger.info("Service A: Creating order");

        Span span = tracer.spanBuilder("createOrder").startSpan();
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("service", "service-a");
            span.setAttribute("order.id", "ORD-12345");

            // Вызов service-b для расчёта цены
            logger.info("Service A: Calling Service B for price calculation");
            String serviceBUrl = "http://service-b:8080/calculate-price";
            String price = restTemplate.getForObject(serviceBUrl, String.class);

            span.setAttribute("price", price);
            logger.info("Service A: Received price from Service B: {}", price);

            return "Order created successfully! Order ID: ORD-12345, Price: " + price;
        } catch (Exception e) {
            span.recordException(e);
            logger.error("Service A: Error creating order", e);
            return "Error creating order: " + e.getMessage();
        } finally {
            span.end();
        }
    }
}

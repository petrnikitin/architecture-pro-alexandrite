package com.alexandrite.serviceb;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;

@RestController
public class PriceController {

    private static final Logger logger = LoggerFactory.getLogger(PriceController.class);

    @Autowired
    private Tracer tracer;

    private final Random random = new Random();

    @GetMapping("/calculate-price")
    public String calculatePrice() {
        logger.info("Service B: Calculating price");

        Span span = tracer.spanBuilder("calculatePrice").startSpan();
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("service", "service-b");

            // Симуляция расчёта цены (задержка 100-500ms)
            int delay = 100 + random.nextInt(400);
            Thread.sleep(delay);

            int price = 1000 + random.nextInt(9000); // 1000-10000
            String priceStr = "$" + price;

            span.setAttribute("price", priceStr);
            span.setAttribute("calculation.time.ms", delay);
            logger.info("Service B: Price calculated: {}, delay: {}ms", priceStr, delay);

            return priceStr;
        } catch (Exception e) {
            span.recordException(e);
            logger.error("Service B: Error calculating price", e);
            return "Error calculating price: " + e.getMessage();
        } finally {
            span.end();
        }
    }
}

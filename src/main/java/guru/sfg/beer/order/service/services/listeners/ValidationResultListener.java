package guru.sfg.beer.order.service.services.listeners;

import java.util.UUID;
import javax.validation.Validation;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import com.mysql.cj.log.Log;
import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.services.BeerOrderManager;
import guru.sfg.brewery.model.events.ValidateOrderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by jt on 12/2/19.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class ValidationResultListener {

    private final BeerOrderManager beerOrderManager;

    @JmsListener(destination = JmsConfig.VALIDATE_ORDER_QUEUE)
    public void listen(ValidateOrderResult validateOrderResult){

        Boolean isValid = validateOrderResult.getIsValid();
        final UUID orderId = validateOrderResult.getOrderId();

        log.debug("Validation Result for orderId id : " + orderId);
        beerOrderManager.processValidationResult(orderId,isValid);

    }
}

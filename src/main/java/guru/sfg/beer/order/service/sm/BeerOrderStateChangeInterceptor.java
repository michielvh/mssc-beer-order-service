package guru.sfg.beer.order.service.sm;

import java.util.Optional;
import java.util.UUID;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.support.StateMachineInterceptorAdapter;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Component;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BeerOrderStateChangeInterceptor extends StateMachineInterceptorAdapter<BeerOrderStatusEnum, BeerOrderEventEnum> {

    private final BeerOrderRepository beerOrderRepository;

    @Override
    public void preStateChange(State<BeerOrderStatusEnum, BeerOrderEventEnum> state,
            Message<BeerOrderEventEnum> message, Transition<BeerOrderStatusEnum, BeerOrderEventEnum> transition,
            StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachine) {

        // Haal ID uit header van de state
        // haal object uit database
        // pas status aan (is de state.getId())
        // Persist to database


        Optional.ofNullable(message).ifPresent(msg -> {
            Optional.ofNullable(UUID.class.cast(msg.getHeaders().getOrDefault(
                            "beer_object_id" ,-1)))
                    .ifPresent(beerOrderId -> {

                        log.debug("Saving state for beer order id: " + beerOrderId + "With state : " +state.getId());
                        BeerOrder beerOrder = beerOrderRepository.getOne(beerOrderId);
                        beerOrder.setOrderStatus(state.getId());

                        //saveAndFlush to send to DB directly
                        beerOrderRepository.saveAndFlush(beerOrder);

                    });
    });
}
    }
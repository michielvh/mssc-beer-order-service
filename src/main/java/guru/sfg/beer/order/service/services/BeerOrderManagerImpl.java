package guru.sfg.beer.order.service.services;

import java.util.UUID;
import javax.transaction.Transactional;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import guru.sfg.beer.order.service.sm.BeerOrderStateChangeInterceptor;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BeerOrderManagerImpl implements BeerOrderManager {

    private final StateMachineFactory<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachineFactory;
    private final BeerOrderRepository             beerOrderRepository;
private final     BeerOrderStateChangeInterceptor beerOrderStateChangeInterceptor;
    @Transactional
    @Override
    public BeerOrder newBeerOrder(BeerOrder beerOrder) {

        //defensive coding
        beerOrder.setId(null);
        beerOrder.setOrderStatus(BeerOrderStatusEnum.NEW);

        //Id will be set by hibernate while saving
        BeerOrder savedBeerOrder = beerOrderRepository.save(beerOrder);

        sendBeerOrderEvent(savedBeerOrder, BeerOrderEventEnum.VALIDATE_ORDER);





        return savedBeerOrder;
    }

    @Override
    public void processValidationResult(UUID orderId, Boolean isValid) {
        BeerOrder beerOrder = beerOrderRepository.getOne(orderId);
        if (isValid){
            sendBeerOrderEvent(beerOrder,BeerOrderEventEnum.VALIDATION_PASSED);


            BeerOrder validatedOrder = beerOrderRepository.findOneById(orderId);

            sendBeerOrderEvent(validatedOrder, BeerOrderEventEnum.ALLOCATE_ORDER);

        } else {
            sendBeerOrderEvent(beerOrder,BeerOrderEventEnum.VALIDATION_FAILED);
        }
    }

    private void sendBeerOrderEvent(BeerOrder beerOrder, BeerOrderEventEnum eventEnum){

        StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachine = build(beerOrder);

        // maar 2 dingen  nodig :
        //  - event
        //  - id van object
        // set Message payload met het event dat gebeurd
        Message msg = MessageBuilder.withPayload(eventEnum)
                .setHeader("beer_order_id", beerOrder.getId())
                .build();

        // bij sendEvent wordt de BeerOrderStateMachineConfig aangeroepen
        //  - indien er door de configuratie (van de transitie) vb NEW->VALIDATED
        //  een state change gebeurd DAN PAS komt de interceptor/listener

        stateMachine.sendEvent(msg);
    }

    private StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> build(BeerOrder beerOrder){
// Ophalen van statemachine uit factory == "stateMachine DataBase" met Id van object waarvan state aangepast wordt
// id voor chaching
        StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachine =
                stateMachineFactory.getStateMachine(beerOrder.getId());

        stateMachine.stop();

        // reset machine config
        // zet status naar objectStatus
        // add interceptor/listener

        stateMachine.getStateMachineAccessor()
                .doWithAllRegions(stateMachineAccess ->{
                    stateMachineAccess.resetStateMachine(
                            new DefaultStateMachineContext<>(beerOrder.getOrderStatus(), null,null,null));
                    stateMachineAccess.addStateMachineInterceptor(beerOrderStateChangeInterceptor);
                });

        stateMachine.start();
        return stateMachine;
    }
}

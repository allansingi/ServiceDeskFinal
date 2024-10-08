package pt.allanborges.orderserviceapi.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import models.exceptions.ResourceNotFoundException;
import models.requests.CreateOrderRequest;
import models.requests.UpdateOrderRequest;
import models.responses.OrderResponse;
import models.responses.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import pt.allanborges.orderserviceapi.clients.UserServiceFeignClient;
import pt.allanborges.orderserviceapi.entities.Order;
import pt.allanborges.orderserviceapi.mapper.OrderMapper;
import pt.allanborges.orderserviceapi.repositories.OrderRepository;
import pt.allanborges.orderserviceapi.services.OrderService;

import java.util.List;

import static java.time.LocalDateTime.now;
import static models.enums.OrderStatusEnum.CLOSED;

@Log4j2
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository repository;
    private final OrderMapper mapper;
    private final UserServiceFeignClient userServiceFeignClient;

    @Override
    public Order findById(final Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(
                "Object not found. Id: " + id + ", Type: " + Order.class.getSimpleName()
        ));
    }

    @Override
    public void save(CreateOrderRequest request) {
        final var requester = validateUserId(request.requesterId());
        final var customer = validateUserId(request.customerId());

        log.info("Requester: {}", requester);
        log.info("Customer: {}", customer);

        final var entity = repository.save(mapper.fromRequest(request));
        log.info("Order created: {}", entity);
    }

    @Override
    public OrderResponse update(final Long id, UpdateOrderRequest request) {
        validateUsers(request);
        
        Order entity = findById(id);
        entity = mapper.fromRequest(entity, request);

        if (entity.getStatus().equals(CLOSED))
            entity.setClosedAt(now());

        return mapper.fromEntity(repository.save(entity));
    }

    private void validateUsers(UpdateOrderRequest request) {
        if (request.requesterId() != null) validateUserId(request.requesterId());
        if (request.customerId() != null) validateUserId(request.customerId());
    }

    @Override
    public void deleteById(final Long id) {
        repository.delete(findById(id));
    }

    @Override
    public List<Order> findAll() {
        return repository.findAll();
    }

    @Override
    public Page<Order> findAllPaginated(Integer page, Integer linesPerPage, String direction, String orderBy) {
        PageRequest pageRequest = PageRequest.of(
                page,
                linesPerPage,
                Sort.Direction.valueOf(direction),
                orderBy
        );
        return repository.findAll(pageRequest);
    }

    UserResponse validateUserId(final String userId) {
        return userServiceFeignClient.findById(userId).getBody();
    }

}
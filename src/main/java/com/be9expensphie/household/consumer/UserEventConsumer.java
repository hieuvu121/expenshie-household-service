package com.be9expensphie.household.consumer;
import com.be9expensphie.common.event.UserEvent;
import com.be9expensphie.household.entity.UserSummary;
import com.be9expensphie.household.repository.UserSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

    private final UserSummaryRepository userSummaryRepository;

    @KafkaListener(topics = "user-events", containerFactory = "userEventKafkaListenerContainerFactory")
    public void consume(UserEvent event) {
        if ("USER_REGISTERED".equals(event.getEventType())) {
            userSummaryRepository.findByUserId(event.getUserId()).ifPresentOrElse(
                    existing -> log.info("UserSummary already exists for userId={}", event.getUserId()),
                    () -> {
                        userSummaryRepository.save(UserSummary.builder()
                                .userId(event.getUserId())
                                .email(event.getEmail())
                                .fullName(event.getFullName())
                                .build());
                        log.info("Saved UserSummary for userId={}", event.getUserId());
                    }
            );
        }
    }
}

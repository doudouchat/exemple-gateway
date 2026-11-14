package com.exemple.gateway.core.session;

import org.springframework.session.ReactiveSessionRepository;

import com.hazelcast.spring.session.HazelcastIndexedSessionRepository;
import com.hazelcast.spring.session.HazelcastIndexedSessionRepository.HazelcastSession;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public class HazelcastSessionRepository implements ReactiveSessionRepository<HazelcastSession> {

    private final HazelcastIndexedSessionRepository sessionRepository;

    @Override
    public Mono<HazelcastSession> createSession() {
        return Mono.just(sessionRepository.createSession());
    }

    @Override
    public Mono<Void> save(HazelcastSession session) {
        sessionRepository.save(session);
        LOG.debug("Session saved: {}", session.getId());
        return Mono.empty();
    }

    @Override
    public Mono<HazelcastSession> findById(String id) {
        return Mono.justOrEmpty(sessionRepository.findById(id));
    }

    @Override
    public Mono<Void> deleteById(String id) {
        sessionRepository.deleteById(id);
        LOG.debug("Session deleted: {}", id);
        return Mono.empty();
    }

}

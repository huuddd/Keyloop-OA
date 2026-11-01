package com.keyloop.scheduler.application.cqrs;

public interface QueryHandler<Q extends Query<R>, R> {
    R handle(Q query);
}

package com.keyloop.scheduler.application.cqrs;

import com.keyloop.scheduler.domain.shared.Result;

public interface CommandHandler<C extends Command<R>, R> {
    Result<R> handle(C command);
}

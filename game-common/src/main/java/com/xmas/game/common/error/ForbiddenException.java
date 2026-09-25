package com.xmas.game.common.error;

public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(403, message);
    }
}

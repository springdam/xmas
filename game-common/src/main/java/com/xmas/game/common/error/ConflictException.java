package com.xmas.game.common.error;

public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(409, message);
    }
}

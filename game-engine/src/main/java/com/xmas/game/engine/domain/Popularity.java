package com.xmas.game.engine.domain;

public final class Popularity {

    public static final int MIN = 1;
    public static final int MAX = 100;

    private Popularity() {
    }

    public static int clamp(int value) {
        return Math.max(MIN, Math.min(MAX, value));
    }
}

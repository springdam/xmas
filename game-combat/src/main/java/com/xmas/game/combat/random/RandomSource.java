package com.xmas.game.combat.random;

public interface RandomSource {

    /** Uniformly distributed value in [0, 1). */
    double nextDouble();
}

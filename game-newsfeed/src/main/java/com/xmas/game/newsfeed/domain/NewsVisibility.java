package com.xmas.game.newsfeed.domain;

public enum NewsVisibility {
    PUBLIC,
    INVOLVED_ONLY,
    /** Visible only to the acting player (e.g. a private intelligence report). */
    ACTOR_ONLY,
    HIDDEN
}

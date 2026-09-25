package com.xmas.game.app.web;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiErrorBody {

    private int status;
    private String message;
}

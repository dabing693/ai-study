package com.lyh.finance.reactive.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/flux")
public class FluxController {
    @GetMapping("/hello")
    public Mono<?> hello() {
        System.out.println(Thread.currentThread().getName());
        return Mono.just(Map.of("data", "hello"));
    }
}

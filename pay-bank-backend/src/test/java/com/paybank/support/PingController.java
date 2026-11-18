package com.paybank.support;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Protected endpoint that only exists in tests, to verify API protection. */
@RestController
public class PingController {

    @GetMapping("/api/test/ping")
    public String ping() {
        return "pong";
    }
}

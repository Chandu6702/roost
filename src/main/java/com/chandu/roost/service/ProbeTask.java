package com.chandu.roost.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class ProbeTask {

    private static final Logger log =
            LoggerFactory.getLogger(ProbeTask.class);

    public ProbeTask() {
        log.info("ProbeTask instance created");
    }
}
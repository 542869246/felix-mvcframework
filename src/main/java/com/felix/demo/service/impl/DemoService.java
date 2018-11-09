package com.felix.demo.service.impl;

import com.felix.demo.service.IDemoService;
import com.felix.mvcframework.annotation.FFService;

@FFService
public class DemoService implements IDemoService{

    @Override
    public String get(String name) {
        return "Hello," + name;
    }
}

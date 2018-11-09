package com.felix.demo.controller;

import com.felix.demo.service.IDemoService;
import com.felix.mvcframework.annotation.FFAutowired;
import com.felix.mvcframework.annotation.FFController;
import com.felix.mvcframework.annotation.FFRequestMapping;
import com.felix.mvcframework.annotation.FFRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@FFController()
@FFRequestMapping("/demo")
public class DemoController {

    @FFAutowired
    private IDemoService demoService;

    @FFRequestMapping("/query.json")
    public void query(HttpServletRequest request, HttpServletResponse response,
                      @FFRequestParam("name") String name) {
        String result = demoService.get(name);

        try {
            response.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FFRequestMapping("/add")
    public void add(HttpServletRequest request, HttpServletResponse response,
                    @FFRequestParam("a") String a, @FFRequestParam("b") String b) {
        try {
            response.getWriter().write(a + b);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FFRequestMapping("/remove")
    public void remove(HttpServletRequest request, HttpServletResponse response,
                       @FFRequestParam("id") String id) {
        try {
            response.getWriter().write(id);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

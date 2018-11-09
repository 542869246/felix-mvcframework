package com.felix.test;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class TestOne {

    @Test
    public void name() {
    }


    @Test
    public void t1() {
        System.out.println("test");
        System.out.println();
        List l = new ArrayList();
        for (Object o : l) {

        }

        String a = creates("Asafdas", 12, l);

        System.out.println(a);
    }

    private String creates(String asd, int i, List l) {

        char[] chars = asd.toCharArray();
        if (chars[0] >= 'A' && chars[0] <= 'Z') {
            chars[0] += 32;
        }

        return String.valueOf(chars);
    }


}

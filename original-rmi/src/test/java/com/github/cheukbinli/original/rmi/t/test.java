package com.github.cheukbinli.original.rmi.t;

import com.github.cheukbinli.original.common.annotation.rmi.RmiClient;

@RmiClient(serviceImplementation = "ABCDEFG")
public class test {

    public int a(String a, int b, char c, long d, Boolean e) {
        return 0;
    }

}

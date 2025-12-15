package com.peosu.fn;

import com.peosu.fn.ds.JsonQ;

import java.io.File;
import java.util.List;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        JsonQ jsonQ= JsonQ.fromIO(new File("/home/n2/Downloads/sample.json"));
        List<String> val=jsonQ.get("..features[*].properties[*]")//.getStrings();
                .select("reg_name","dist_name","ward_name").getStrings();
        System.out.println(val);


    }
}
package com.peosu.fn.utils;

import com.peosu.fn.ds.JsonQ;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Log {
    private static final Logger logger = Logger.getLogger(JsonQ.class.getName());

    public static void log(Exception e) {logger.log(Level.SEVERE,e.toString(),e);}
    public static void log(String s,Object ... args) {logger.log(Level.SEVERE,String.format(s,args));}
    public static void warn(String s,Object ... args) {logger.log(Level.WARNING,String.format(s,args));}
}

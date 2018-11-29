package com.molian.web;

public class UserContext {
    private static String user = "";

    public static String getUser() {
        return user;
    }

    public static void setUser(String user) {
        UserContext.user = user;
    }
}

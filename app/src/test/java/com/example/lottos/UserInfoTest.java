package com.example.lottos;

import static org.junit.Assert.*;

import com.example.lottos.entities.UserInfo;

import org.junit.Test;


public class UserInfoTest {
    @Test
    public void testUserInfoConstructorAndGetters() {
        // 1. This will now correctly use YOUR UserInfo class from your project
        UserInfo info = new UserInfo("Jane", "1234", "jane@email.com", "555-999");

        // 2. These assertions will now work because they are calling methods on YOUR class
        assertEquals("Jane", info.getDisplayName());
        assertEquals("1234", info.getPassword());
        assertEquals("jane@email.com", info.getEmail());
        assertEquals("555-999", info.getPhoneNumber());
    }
}

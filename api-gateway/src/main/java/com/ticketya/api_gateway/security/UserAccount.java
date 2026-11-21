package com.ticketya.api_gateway.security;

public record UserAccount(String id, String username, String passwordHash, Role role) {}
package com.sybyl.trace.exception;

public class UsernameAlreadyInUseException extends RuntimeException {
	public UsernameAlreadyInUseException() {
		super("Username already in use");
	}
}

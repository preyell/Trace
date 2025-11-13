package com.sybyl.trace.exception;

public class EmailAlreadyInUseException extends RuntimeException {
	public EmailAlreadyInUseException() {
		super("Email already in use");
	}
}

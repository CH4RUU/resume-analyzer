package com.charu.resumeanalyzer.web;

/** Thrown when the caller's input can't be processed (bad file, empty JD, etc). */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}

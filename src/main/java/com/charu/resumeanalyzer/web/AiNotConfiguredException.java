package com.charu.resumeanalyzer.web;

/** Thrown when AI feedback is requested but no Anthropic API key is configured. */
public class AiNotConfiguredException extends RuntimeException {

    public AiNotConfiguredException(String message) {
        super(message);
    }
}

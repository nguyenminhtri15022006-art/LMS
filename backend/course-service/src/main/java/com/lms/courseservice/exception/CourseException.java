package com.lms.courseservice.exception;

import lombok.Getter;

@Getter
public class CourseException extends RuntimeException {
    private final ErrorCode errorCode;

    public CourseException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}

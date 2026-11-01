package com.keyloop.scheduler.domain;

import com.keyloop.scheduler.domain.shared.Error;
import com.keyloop.scheduler.domain.shared.Result;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResultTest {

    // --- Success ---

    @Test
    void success_getValue_returnsValue() {
        Result<String> result = Result.success("hello");
        assertThat(result.getValue()).isEqualTo("hello");
    }

    @Test
    void success_isSuccess_returnsTrue() {
        Result<String> result = Result.success("hello");
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void success_isFailure_returnsFalse() {
        Result<String> result = Result.success("hello");
        assertThat(result.isFailure()).isFalse();
    }

    @Test
    void success_getError_throwsException() {
        Result<String> result = Result.success("hello");
        assertThatThrownBy(result::getError)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No error on success result");
    }

    // --- Failure ---

    @Test
    void failure_getError_returnsError() {
        Error error = new Error("Test.Error", "something went wrong");
        Result<String> result = Result.failure(error);
        assertThat(result.getError()).isEqualTo(error);
    }

    @Test
    void failure_isFailure_returnsTrue() {
        Error error = new Error("Test.Error", "something went wrong");
        Result<String> result = Result.failure(error);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void failure_isSuccess_returnsFalse() {
        Error error = new Error("Test.Error", "something went wrong");
        Result<String> result = Result.failure(error);
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void failure_getValue_throwsException() {
        Error error = new Error("Test.Error", "something went wrong");
        Result<String> result = Result.failure(error);
        assertThatThrownBy(result::getValue)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No value on failure result");
    }

    // --- fold ---

    @Test
    void fold_onSuccess_appliesSuccessFunction() {
        Result<String> result = Result.success("hello");
        String folded = result.fold(
                value -> "success: " + value,
                error -> "failure: " + error.code()
        );
        assertThat(folded).isEqualTo("success: hello");
    }

    @Test
    void fold_onFailure_appliesFailureFunction() {
        Error error = new Error("Test.Error", "something went wrong");
        Result<String> result = Result.failure(error);
        String folded = result.fold(
                value -> "success: " + value,
                err -> "failure: " + err.code()
        );
        assertThat(folded).isEqualTo("failure: Test.Error");
    }

    // --- Void success ---

    @Test
    void voidSuccess_isSuccess() {
        Result<Void> result = Result.success();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isNull();
    }
}

package com.Group2.Ecommerce.Common.Exception;

import com.Group2.Ecommerce.Common.ApiResponse;
import com.Group2.Ecommerce.Product.Dto.ProductRequest;
import com.Group2.Ecommerce.Product.ProductController;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleValidation_includesFieldReasonsInMessage_andReturnsFieldMap() throws Exception {
        Method method = ProductController.class.getMethod("create", ProductRequest.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new ProductRequest(), "productRequest");
        bindingResult.addError(new FieldError("productRequest", "categoryId", "Category is required"));
        bindingResult.addError(new FieldError("productRequest", "name", "Name is required"));

        ResponseEntity<ApiResponse<Map<String, String>>> response = handler.handleValidation(
                new MethodArgumentNotValidException(parameter, bindingResult));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("Validation failed: Category is required, Name is required");
        assertThat(response.getBody().getData()).containsEntry("categoryId", "Category is required");
    }

    @Test
    void handleDuplicate_tooLong_includesMaxLengthFromPostgresMessage() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "value too long for type character varying(255)");

        ResponseEntity<ApiResponse<Void>> response = handler.handleDuplicate(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getMessage())
                .isEqualTo("A value is too long for one of the fields (max 255 characters).");
    }

    @Test
    void handleDuplicate_tooLong_withoutLengthStaysGeneric() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("value too long");

        ResponseEntity<ApiResponse<Void>> response = handler.handleDuplicate(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getMessage())
                .isEqualTo("A value is too long for one of the fields.");
    }
}

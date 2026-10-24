package com.nemall.product;

import com.nemall.product.controller.AdminProductController;
import com.nemall.product.exception.ProductBusinessException;
import com.nemall.product.exception.ProductExceptionHandler;
import com.nemall.product.service.AdminProductService;
import com.nemall.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminProductControllerTests {

    private MockMvc mvc;
    private AdminProductService adminProductService;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        adminProductService = mock(AdminProductService.class);
        productService = mock(ProductService.class);
        mvc = MockMvcBuilders.standaloneSetup(
                        new AdminProductController(adminProductService, productService))
                .setControllerAdvice(new ProductExceptionHandler())
                .build();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void returnsSuccessAfterServiceCompletes(int targetStatus) throws Exception {
        mvc.perform(post("/api/merchant/products/19/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":" + targetStatus + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"));

        verify(adminProductService).updateProductStatus(19L, targetStatus);
        verifyNoInteractions(productService);
    }

    @Test
    void returnsServiceFailureWithoutReportingSuccess() throws Exception {
        doThrow(new ProductBusinessException(501, "商品状态更新尚未实现"))
                .when(adminProductService).updateProductStatus(19L, 2);

        mvc.perform(post("/api/merchant/products/19/status")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":2}"))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.code").value(501));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"status\":null}", "{\"status\":0}", "{\"status\":3}"})
    void leavesStatusValidationToService(String body) throws Exception {
        mvc.perform(post("/api/merchant/products/19/status")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    void leavesProductIdValidationToService(String id) throws Exception {
        mvc.perform(post("/api/merchant/products/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":2}"))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsMissingBody() throws Exception {
        mvc.perform(post("/api/merchant/products/19/status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}

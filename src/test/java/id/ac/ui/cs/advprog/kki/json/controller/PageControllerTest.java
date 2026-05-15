package id.ac.ui.cs.advprog.kki.json.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PageControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PageController()).build();
    }

    @Test
    void authRouteForwardsToLoginPage() throws Exception {
        mockMvc.perform(get("/auth"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/login.html"));
    }

    @Test
    void loginRouteForwardsToLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/login.html"));
    }

    @Test
    void registerRouteForwardsToRegisterPage() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/register.html"));
    }

    @Test
    void profileRouteForwardsToProfilePage() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/profile.html"));
    }

    @Test
    void catalogRouteForwardsToCatalogPage() throws Exception {
        mockMvc.perform(get("/catalog"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/catalog.html"));
    }

    @Test
    void ordersRouteForwardsToOrdersPage() throws Exception {
        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/orders.html"));
    }

    @Test
    void vouchersRouteForwardsToVouchersPage() throws Exception {
        mockMvc.perform(get("/vouchers"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/vouchers.html"));
    }

    @Test
    void walletRouteForwardsToWalletPage() throws Exception {
        mockMvc.perform(get("/wallet"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/wallet.html"));
    }

    @Test
    void transactionsRouteForwardsToTransactionsPage() throws Exception {
        mockMvc.perform(get("/transactions"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/transactions.html"));
    }
}

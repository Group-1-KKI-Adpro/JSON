package id.ac.ui.cs.advprog.kki.json.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/auth")
    public String authPage() {
        return "forward:/login.html";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "forward:/login.html";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "forward:/register.html";
    }

    @GetMapping("/profile")
    public String profilePage() {
        return "forward:/profile.html";
    }

    @GetMapping("/catalog")
    public String catalogPage() {
        return "forward:/catalog.html";
    }

    @GetMapping("/orders")
    public String ordersPage() {
        return "forward:/orders.html";
    }

    @GetMapping("/vouchers")
    public String vouchersPage() {
        return "forward:/vouchers.html";
    }

    @GetMapping("/wallet")
    public String walletPage() {
        return "forward:/wallet.html";
    }

    @GetMapping("/transactions")
    public String transactionsPage() {
        return "forward:/transactions.html";
    }
}

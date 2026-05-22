package id.ac.ui.cs.advprog.kki.json.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping({"/auth", "/profile", "/catalog", "/orders", "/vouchers"})
    public String moduleEntryPage() {
        return "forward:/index.html";
    }

    @GetMapping("/wallet")
    public String walletPage() {
        return "redirect:/wallet.html";
    }

    @GetMapping("/transactions")
    public String transactionsPage() {
        return "redirect:/transactions.html";
    }
}

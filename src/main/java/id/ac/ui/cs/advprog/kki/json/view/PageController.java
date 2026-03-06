package id.ac.ui.cs.advprog.kki.json.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/wallet")
    public String walletPage() {
        return "Transaction/wallet";
    }

    @GetMapping("/transactions")
    public String transactionsPage() {
        return "Transaction/transaction";
    }
}


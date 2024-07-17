package com.scoreme.newname;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/bankname")
public class BankNameController {

    private final BankNameService bankNameService;

    @Autowired
    public BankNameController(BankNameService bankNameService) {
        this.bankNameService = bankNameService;
    }

    @PostMapping("/extract")
    public ResponseEntity<String> extractBankName(@RequestParam("file") MultipartFile file) {
        try {
            String bankName = bankNameService.getBankNameFromPdf(file);
            return new ResponseEntity<>(bankName, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error processing PDF: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

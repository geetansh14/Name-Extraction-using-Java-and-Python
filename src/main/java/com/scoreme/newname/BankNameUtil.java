package com.scoreme.newname;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class BankNameUtil {
    public static Set<String> loadBankNames(String csvFilePath) throws IOException {
        Set<String> bankNames = new HashSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String bankName = line.trim().replace("\"", "");
                
                bankNames.add(bankName);
                // System.out.println("Read bank name: " + bankName);
            }
        }
        return bankNames;
    }
}

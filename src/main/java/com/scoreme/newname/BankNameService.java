package com.scoreme.newname;

import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@Service
public class BankNameService {

    private static final Logger logger = LoggerFactory.getLogger(BankNameService.class);
    private final Set<String> bankNames;
    private final Set<String> ifscList;
    private final Map<String, String> actualBankNames;
    private final Map<String, String> lowerToOriginalBankNames; // Map to store lowercase to original names

    public BankNameService() {
        Set<String> tempBankNames;
        Set<String> tempIfscList;
        Map<String, String> tempActualBankNames = new HashMap<>();
        Map<String, String> tempLowerToOriginalBankNames = new HashMap<>(); // Initialize the map

        try {
            logger.info("Loading bank names from CSV");
            Set<String> loadedBankNames = BankNameUtil.loadBankNames("D:\\VS Folders\\jproject\\n" + //
                    "ewname\\src\\main\\resources\\static\\bankName.csv");
            tempBankNames = loadedBankNames.stream().map(String::toLowerCase).collect(Collectors.toSet());
            for (String name : loadedBankNames) {
                tempLowerToOriginalBankNames.put(name.toLowerCase(), name); // Populate the map
            }
        } catch (IOException e) {
            logger.error("Failed to load bank names from CSV", e);
            tempBankNames = Set.of(); // Assign an empty set to avoid null pointer issues
        }

        try {
            logger.info("Loading IFSC codes from JSON");
            ObjectMapper mapper = new ObjectMapper();
            tempIfscList = mapper.readValue(Files.readAllBytes(Paths.get("D:\\VS Folders\\jproject\\n" + //
                    "ewname\\src\\main\\resources\\static\\IFSC-list.json")),
                    new TypeReference<Set<String>>() {
                    });
        } catch (IOException e) {
            logger.error("Failed to load IFSC codes from JSON", e);
            tempIfscList = Set.of(); // Assign an empty set to avoid null pointer issues
        }

        try {
            logger.info("Loading bank abbreviations from CSV");
            List<String[]> lines = Files
                    .readAllLines(Paths.get("D:\\VS Folders\\jproject\\n" + //
                            "ewname\\src\\main\\resources\\static\\bank_abbreviations.csv"))
                    .stream().map(line -> line.split(",")).collect(Collectors.toList());
            for (String[] line : lines) {
                if (line.length == 2) {
                    tempActualBankNames.put(line[0].toLowerCase().trim(), line[1].toLowerCase().trim());
                }
            }
        } catch (IOException e) {
            logger.error("Failed to load bank abbreviations from CSV", e);
            tempActualBankNames = Map.of(); // Assign an empty map to avoid null pointer issues
        }

        this.actualBankNames = tempActualBankNames;
        this.bankNames = tempBankNames;
        this.ifscList = tempIfscList;
        this.lowerToOriginalBankNames = tempLowerToOriginalBankNames; // Assign the map
    }

    public String getBankNameFromPdf(MultipartFile file) throws IOException, TesseractException {
        byte[] pdfBytes = file.getBytes();
        String text = PdfUtil.extractText(pdfBytes);
        logger.info("Processing PDF file: {}, size: {} bytes", file.getOriginalFilename(), pdfBytes.length);

        logger.info("Extracted Text : {}", text);
        logger.info("Searching for bank names in extracted text");

        // Check for IFSC code in extracted text
        String bankNameFromIfsc = findBankNameFromIfsc(text);
        if (bankNameFromIfsc != null) {
            return bankNameFromIfsc;
        }

        // Call Python API to extract text from the first page image

        // Existing OCR processing steps
        PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes));
        StringBuilder ocrTextBuilder = new StringBuilder();
        int pagesToProcess = Math.min(document.getNumberOfPages(), 3);
        for (int i = 0; i < pagesToProcess; i++) {
            PDPage page = document.getPage(i);
            String ocrText = PdfUtil.extractOcrTextFromPage(page);
            ocrTextBuilder.append(ocrText).append("\n");
        }

        String ocrText = ocrTextBuilder.toString();
        logger.info("Combined OCR Text from first {} pages: {}", pagesToProcess, ocrText);

        // Check for IFSC code in combined OCR text
        bankNameFromIfsc = findBankNameFromIfsc(ocrText);
        if (bankNameFromIfsc != null) {
            return bankNameFromIfsc;
        }

        String pythonExtractedText = extractTextFromPythonApi(pdfBytes);
        if (pythonExtractedText != null) {
            logger.info("Finding Bank name from PYTHON API.");
            String bankNameFromPythonText = findBankName(pythonExtractedText);
            if (bankNameFromPythonText != null) {
                logger.info("Got name from Python API.");
                return bankNameFromPythonText;
            }
        }

        String bankName = findBankName(ocrText);
        if (bankName != null) {
            return bankName;
        }

        String bankName_2 = findBankName(text);
        if (bankName_2 != null) {
            logger.info("Found bank name in text: {}", bankName_2);
            return bankName_2;
        }

        logger.info("No bank name found in PDF");
        return "No Name";
    }

    private String extractTextFromPythonApi(byte[] pdfBytes) {
        try {
            String apiUrl = "http://localhost:5000/extract_text";
            RestTemplate restTemplate = new RestTemplate();

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // Create the multipart request
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(pdfBytes) {
                @Override
                public String getFilename() {
                    return "file.pdf"; // Set a default filename
                }
            });

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            // Send request to Python API
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                @SuppressWarnings("unchecked")
                Map<String, String> responseBody = response.getBody();
                if (responseBody != null && responseBody.containsKey("text")) {
                    return responseBody.get("text");
                }
            }
        } catch (Exception e) {
            logger.error("Error while calling Python API for text extraction", e);
        }
        return null;
    }

    private String findBankName(String text) {
        if (text == null || text.isEmpty()) {
            logger.info("Text is null or empty");
            return null;
        }

        String lowerText = text.toLowerCase();
        // Create a pattern to match the word "bank"
        Pattern pattern = Pattern.compile("\\bbank\\b");
        Matcher matcher = pattern.matcher(lowerText);

        while (matcher.find()) {
            int bankStart = matcher.start();
            int bankEnd = matcher.end();

            // Define a window around the "bank" word
            int start = Math.max(bankStart - 75, 0);
            int end = Math.min(bankEnd + 75, lowerText.length());
            String surroundingText = lowerText.substring(start, end);
            logger.info("Surrounding text: {}", surroundingText);

            // Split the surrounding text into words and remove punctuation
            List<String> words = Arrays.stream(surroundingText.split("\\s+"))
                    .map(word -> word.replaceAll("[\\p{Punct}&&[^&]]", ""))
                    .collect(Collectors.toList());
            logger.info("Words are : {}", words);

            // Find the position of the word "bank" in the words list
            int bankWordIndex = -1;
            for (int i = 0; i < words.size(); i++) {
                if (words.get(i).equals("bank")) {
                    bankWordIndex = i;
                    break;
                }
            }

            // Generate potential names by keeping the "bank" word in the same position
            if (bankWordIndex != -1) {
                logger.info("Making names");
                for (int i = 0; i <= bankWordIndex; i++) {
                    for (int j = bankWordIndex + 1; j <= words.size(); j++) {
                        List<String> subList = words.subList(i, j);
                        if (subList.contains("bank")) {
                            // Replace "coop" / "co-op" with "co-operative"
                            subList = subList.stream()
                                    .map(word -> word.replaceAll("coop|co-op", "co-operative"))
                                    .collect(Collectors.toList());
                            String potentialName = String.join(" ", subList);
                            logger.info("Potential name: {}", potentialName);

                            String matchedName = getMatchingBankName(potentialName.toLowerCase().trim());
                            if (matchedName != null) {
                                logger.info("Matched bank name with 90% similarity: {}", matchedName);
                                return matchedName;
                            }
                        }
                    }
                }
            }
        }
        logger.info("No bank name found in text");
        return null;
    }

    private String getMatchingBankName(String potentialName) {
        LevenshteinDistance levenshtein = new LevenshteinDistance();
        String found = null;

        for (String bankName : bankNames) {
            int distance = levenshtein.apply(bankName, potentialName);
            int maxLen = Math.max(bankName.length(), potentialName.length());
            double similarity = 1.0 - (double) distance / maxLen;

            if (similarity >= 0.9) {
                logger.info("Found match: '{}' with similarity: {}", bankName, similarity);
                found = bankName;

                break;
            }
        }

        if (found != null) {
            String originalName = lowerToOriginalBankNames.get(found);
            logger.info("Matched bank name from bankName.csv: {}", originalName);
            return originalName; // Use the map to return the original name
        }

        return null;
    }

    private String findBankNameFromIfsc(String text) {
        if (text == null || text.isEmpty()) {
            logger.info("Text is null or empty");
            return null;
        }

        String lowerText = text.toLowerCase();
        LevenshteinDistance levenshtein = new LevenshteinDistance();

        // Create a pattern to match words around "ifs" or "ifsc"
        Pattern pattern = Pattern.compile("\\b\\w{3,4}\\b");
        Matcher matcher = pattern.matcher(lowerText);

        boolean ifscFoundNearby = false;

        while (matcher.find()) {
            String foundWord = matcher.group();
            int similarityIfsc = levenshtein.apply("ifsc", foundWord);
            int similarityIfs = levenshtein.apply("ifs", foundWord);

            int maxLen = Math.max(foundWord.length(), 4);
            double similarityIfscRatio = 1.0 - (double) similarityIfsc / maxLen;
            double similarityIfsRatio = 1.0 - (double) similarityIfs / maxLen;

            if (similarityIfscRatio >= 0.7 || similarityIfsRatio >= 0.7) {
                logger.info("Word '{}' is similar to 'ifs' or 'ifsc' with similarity ratio >= 0.7", foundWord);
                int start = Math.max(matcher.start() - 200, 0);
                int end = Math.min(matcher.start() + 200, lowerText.length());
                String surroundingText = lowerText.substring(start, end);
                logger.info("Surrounding text for IFSC code: {}", surroundingText);

                // Extract all 11-length words
                Matcher ifscMatcher = Pattern.compile("\\b\\w{11}\\b").matcher(surroundingText);
                while (ifscMatcher.find()) {
                    String potentialIfsc = ifscMatcher.group();
                    // logger.info("Found 11-length word: {}", potentialIfsc);

                    // Check if the 11-length word is in the IFSC list
                    if (ifscList.contains(potentialIfsc.toUpperCase())) {
                        logger.info("Found IFSC code in IFSC list: {}", potentialIfsc);
                        return getBankNameFromIfscApi(potentialIfsc);
                    }
                }
                ifscFoundNearby = true;
            } else {
                logger.info("Word '{}' is not similar enough to 'ifs' or 'ifsc'", foundWord);
            }
        }

        // If "ifs" or "ifsc" was found but no IFSC code was found nearby, search the
        // entire text
        if (ifscFoundNearby) {
            logger.info("No IFSC code found near 'ifs' or 'ifsc', searching entire text");
            Matcher ifscMatcher = Pattern.compile("\\b\\w{11}\\b").matcher(lowerText);
            while (ifscMatcher.find()) {
                String potentialIfsc = ifscMatcher.group();
                logger.info("Found 11-length word in entire text: {}", potentialIfsc);

                // Check if the 11-length word is in the IFSC list
                if (ifscList.contains(potentialIfsc.toUpperCase())) {
                    logger.info("Found IFSC code in IFSC list: {}", potentialIfsc);
                    return getBankNameFromIfscApi(potentialIfsc);
                }
            }
        }

        logger.info("No IFSC code found in text");
        return null;
    }

    private String getBankNameFromIfscApi(String ifscCode) {
        try {
            String apiUrl = "https://ifsc.razorpay.com/" + ifscCode.toUpperCase();
            RestTemplate restTemplate = new RestTemplate();

            // Set the headers to accept JSON content type
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                String responseBody = response.getBody();
                if (responseBody != null) {
                    // Parse the JSON response
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode rootNode = objectMapper.readTree(responseBody);
                    JsonNode bankNode = rootNode.path("BANK");

                    if (!bankNode.isMissingNode()) {
                        String bankNameFromApi = bankNode.asText();
                        logger.info("Bank name from API: {}", bankNameFromApi);

                        // Check bank name from API against bankName.csv
                        String matchedBankName = getMatchingBankName(bankNameFromApi.toLowerCase().trim());
                        if (matchedBankName != null) {
                            logger.info("Matched bank name from bankName.csv x2 : {}", matchedBankName);
                            return matchedBankName;
                        }

                        // Check bank name from API against bank_abbreviations.csv
                        String expandedBankName = getExpandedBankName(bankNameFromApi.toLowerCase().trim());
                        if (expandedBankName != null) {
                            String finalMatchedBankName = getMatchingBankName(expandedBankName.toLowerCase().trim());
                            if (finalMatchedBankName != null) {
                                logger.info("Matched bank name from expanded abbreviation: {}", finalMatchedBankName);
                                return finalMatchedBankName;
                            }
                        }
                    } else {
                        logger.warn("Bank name element not found in the JSON response for IFSC code {}", ifscCode);
                    }
                }
            }
        } catch (HttpClientErrorException.NotFound e) {
            logger.warn("IFSC code {} not found in API", ifscCode);
        } catch (Exception e) {
            logger.error("Error while fetching bank name from IFSC API for code {}", ifscCode, e);
        }
        return null;
    }

    private String getExpandedBankName(String bankName) {
        LevenshteinDistance levenshtein = new LevenshteinDistance();

        for (Map.Entry<String, String> entry : actualBankNames.entrySet()) {
            String key = entry.getKey().toLowerCase();
            String value = entry.getValue().toLowerCase();

            int distanceToKey = levenshtein.apply(key, bankName);
            int distanceToValue = levenshtein.apply(value, bankName);

            int maxLenKey = Math.max(key.length(), bankName.length());
            int maxLenValue = Math.max(value.length(), bankName.length());

            double similarityToKey = 1.0 - (double) distanceToKey / maxLenKey;
            double similarityToValue = 1.0 - (double) distanceToValue / maxLenValue;
            // logger.info("Similarity to key is : {} and to value is : {}",
            // similarityToKey, similarityToValue);

            if (similarityToKey >= 0.9) {
                return value;
            }

            if (similarityToValue >= 0.9) {
                return key;
            }
        }
        return null;
    }

}

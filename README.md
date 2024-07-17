# **Extracting Bank Name from PDF using Tesseract OCR with Spring Boot and Flask**

# _Project Overview_

This project focuses on extracting bank names from PDF documents using optical character recognition (OCR). It employs both Tesseract OCR and a custom Flask API for OCR operations. The project combines Java (Spring Boot) and Python (Flask) to offer a robust solution for extracting and verifying bank names from PDFs.

# _Key Features_

**Spring Boot**: Handles PDF processing and manages the backend services.
**Flask API**: Interfaces with pytesseract to perform OCR operations in Python.
**Tesseract OCR**: Utilized for text extraction from PDFs and images.
**IFSC Code Verification**: Uses the IFSC Razorpay API to verify bank names based on IFSC codes.
**High Accuracy**: Achieved 99.3% accuracy after testing on 900 files.

# _Prerequisites_

**Java Development Kit (JDK) 11 or above**: Required for running the Spring Boot application.
**Python 3.x**: Necessary for the Flask API.
**Tesseract OCR**: Install the OCR engine for text extraction.
**Install Tesseract OCR via the following link**: Tesseract OCR Installer.
Make sure to add the Tesseract installation path to your system environment variables.

# _Setup and Installation_

1. Clone the Repository :
git clone https://github.com/geetansh14/Name-Extraction-using-Java-and-Python.git
2. Spring Boot Application : 
Configure pom.xml:
Ensure that the pom.xml file is configured with the required dependencies for Spring Boot, PDF processing (e.g., Apache PDFBox), and RESTful communication (e.g., RestTemplate). Update the dependencies as per your project's needs.

Configure application.properties:
Adjust application.properties for your environment settings, including the Flask API URL and any other relevant configurations.

Build and run the Spring Boot application:
mvn clean install
mvn spring-boot:run

3. Flask API : 
Install the required Python packages:
pip install pytesseract Flask

Ensure pytesseract can find the Tesseract executable. Add the path to your system environment variables if not done automatically. 
import pytesseract
from flask import Flask, request, jsonify

app = Flask(__name__)

_Update this path to your Tesseract installation directory_
pytesseract.pytesseract.tesseract_cmd = r'C:\Program Files\Tesseract-OCR\tesseract.exe'

Run the Flask API server:
python app.py

4. Configure API Endpoints : 
Use Postman or another API client to interact with the Flask API and test OCR extraction.
Ensure your Spring Boot application correctly integrates with the Flask API for OCR tasks.

5. IFSC Code Verification : 
The application uses the IFSC Razorpay API to verify bank names. Implement API requests as needed to fetch and validate bank information based on IFSC codes.

# _Usage_

**Upload PDFs**: Uses the Spring Boot service to upload and process PDF files.
**Extract Bank Names**: The application will use the Flask API to perform OCR on the uploaded PDFs.
**Verify Bank Names**: Checks the bank names using the IFSC Razorpay API.
**Fallback Mechanism**: If IFSC code verification fails, extract bank names from the OCR results.

# _Accuracy_

The solution has been tested with 900 PDF files, achieving an accuracy rate of 99.3% in bank name extraction.

# _Troubleshooting_

Tesseract Not Found: Ensure Tesseract OCR is correctly installed and the path is added to system variables.
API Errors: Check API keys and endpoint configurations for both Flask and IFSC Razorpay.

# _Contributing_

Feel free to fork the repository and submit pull requests. For major changes or new features, please open an issue to discuss the modifications.

_License_
This project is licensed under the MIT License. See the LICENSE file for details.

_Contact_
For any questions or support, please contact geetansh1425@gmail.com .

# pdf_text_extractor.py

import fitz  # PyMuPDF
import pytesseract
from PIL import Image
import io
import flask
from flask import request, jsonify

app = flask.Flask(__name__)

pytesseract.pytesseract.tesseract_cmd = r'C:\\Program Files\\Tesseract-OCR\\tesseract.exe'  #Change as per system

@app.route('/extract_text', methods=['POST'])
def extract_text():
    try:
        if 'file' not in request.files:
            return jsonify({"error": "No file part"}), 400
        
        file = request.files['file']
        pdf_document = fitz.open(stream=file.read(), filetype="pdf")
        
        first_page = pdf_document.load_page(0)
        pix = first_page.get_pixmap()
        img = Image.open(io.BytesIO(pix.tobytes()))
        
        text = pytesseract.image_to_string(img)
     #    print("Extracted text : ", text)
        return jsonify({"text": text}), 200
    
    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)

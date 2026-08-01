package com.charu.resumeanalyzer.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.charu.resumeanalyzer.web.BadRequestException;

/**
 * Pulls plain text out of an uploaded resume. Supports PDF, DOCX and plain text.
 */
@Service
public class TextExtractor {

    public String extract(MultipartFile file) {
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String extension = extensionOf(name);
        try {
            String text = switch (extension) {
                case "pdf" -> readPdf(file);
                case "docx" -> readDocx(file);
                case "txt", "md" -> new String(file.getBytes(), StandardCharsets.UTF_8);
                default -> throw new BadRequestException(
                        "Unsupported file type '" + extension + "'. Upload a PDF, DOCX or TXT resume.");
            };
            if (text.isBlank()) {
                throw new BadRequestException("No readable text found in " + name
                        + ". If it is a scanned image, export a text-based PDF instead.");
            }
            return text;
        } catch (IOException e) {
            throw new BadRequestException("Could not read " + name + ": " + e.getMessage());
        }
    }

    public String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String readPdf(MultipartFile file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }

    private String readDocx(MultipartFile file) throws IOException {
        try (InputStream in = file.getInputStream();
                XWPFDocument document = new XWPFDocument(in);
                XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }
}

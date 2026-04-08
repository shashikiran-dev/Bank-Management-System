package com.omkaar.bank.service;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.omkaar.bank.entity.AccountEntity;
import com.omkaar.bank.entity.TransactionEntity;
import com.omkaar.bank.entity.UserEntity;
import com.omkaar.bank.model.TransactionType;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PdfStatementService {

    // ── Brand colours ───────────────────────────────────────────────────
    private static final DeviceRgb NAVY       = new DeviceRgb(10,  22,  40);
    private static final DeviceRgb BLUE       = new DeviceRgb(30,  95,  212);
    private static final DeviceRgb GOLD       = new DeviceRgb(201, 168, 76);
    private static final DeviceRgb GREEN      = new DeviceRgb(62,  207, 142);
    private static final DeviceRgb RED        = new DeviceRgb(224, 82,  82);
    private static final DeviceRgb WHITE      = new DeviceRgb(255, 255, 255);
    private static final DeviceRgb LIGHT_GRAY = new DeviceRgb(240, 244, 255);
    private static final DeviceRgb MID_GRAY   = new DeviceRgb(136, 153, 187);

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATETIME_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    /**
     * Generate a PDF bank statement for the given month.
     *
     * @param user         the account holder
     * @param account      their account entity
     * @param transactions ALL transactions for this account (we filter by month)
     * @param yearMonth    e.g. "2024-03"
     * @return raw PDF bytes ready to stream to the browser
     */
    public byte[] generate(UserEntity user,
                           AccountEntity account,
                           List<TransactionEntity> transactions,
                           String yearMonth) throws IOException {

        // ── Parse month ──────────────────────────────────────────────────
        YearMonth ym        = YearMonth.parse(yearMonth);
        Instant   monthStart = ym.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant   monthEnd   = ym.atEndOfMonth().plusDays(1)
                                  .atStartOfDay(ZoneId.systemDefault()).toInstant();

        // ── Filter transactions for this month ───────────────────────────
        UUID accountId = account.getId();
        List<TransactionEntity> monthTx = transactions.stream()
                .filter(tx -> !tx.getTimestamp().isBefore(monthStart)
                           &&  tx.getTimestamp().isBefore(monthEnd))
                .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
                .collect(Collectors.toList());

        // ── Build PDF in memory ──────────────────────────────────────────
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter   writer   = new PdfWriter(baos);
        PdfDocument pdfDoc   = new PdfDocument(writer);
        Document    document = new Document(pdfDoc, PageSize.A4);
        document.setMargins(40, 50, 40, 50);

        PdfFont bold    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont mono    = PdfFontFactory.createFont(StandardFonts.COURIER);

        // ══════════════════════════════════════════════════════════════════
        // HEADER BAND
        // ══════════════════════════════════════════════════════════════════
        Table header = new Table(UnitValue.createPercentArray(new float[]{60, 40}))
                .setWidth(UnitValue.createPercentValue(100));

        // Left — bank name
        Cell leftCell = new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph("SecureBank")
                        .setFont(bold).setFontSize(22).setFontColor(NAVY))
                .add(new Paragraph("Official Account Statement")
                        .setFont(regular).setFontSize(9).setFontColor(MID_GRAY));
        header.addCell(leftCell);

        // Right — statement period
        String period = ym.getMonth().getDisplayName(
                java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH)
                + " " + ym.getYear();
        Cell rightCell = new Cell().setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT)
                .add(new Paragraph("Statement Period")
                        .setFont(bold).setFontSize(9).setFontColor(MID_GRAY))
                .add(new Paragraph(period)
                        .setFont(bold).setFontSize(13).setFontColor(BLUE));
        header.addCell(rightCell);

        document.add(header);
        document.add(new LineSeparator(new SolidLine(1f))
                .setStrokeColor(GOLD).setMarginTop(8).setMarginBottom(16));

        // ══════════════════════════════════════════════════════════════════
        // ACCOUNT SUMMARY BOX
        // ══════════════════════════════════════════════════════════════════
        Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{25, 25, 25, 25}))
                .setWidth(UnitValue.createPercentValue(100))
                .setBackgroundColor(LIGHT_GRAY)
                .setBorderRadius(new com.itextpdf.layout.properties.BorderRadius(6));

        addSummaryCell(summaryTable, "Account Holder", user.getName(), bold, regular);
        addSummaryCell(summaryTable, "Account Number",
                shortAccNo(account.getId()), bold, regular);
        addSummaryCell(summaryTable, "Account Type",
                account.getType().name(), bold, regular);
        addSummaryCell(summaryTable, "Current Balance",
                formatAmount(account.getBalance()), bold, regular);

        document.add(summaryTable);
        document.add(new Paragraph(" ").setFontSize(8)); // spacer

        // ══════════════════════════════════════════════════════════════════
        // STATISTICS ROW
        // ══════════════════════════════════════════════════════════════════
        BigDecimal totalCredits  = BigDecimal.ZERO;
        BigDecimal totalDebits   = BigDecimal.ZERO;

        for (TransactionEntity tx : monthTx) {
            boolean isCredit = isCredit(tx, accountId);
            if (isCredit) totalCredits  = totalCredits.add(tx.getAmount());
            else           totalDebits  = totalDebits.add(tx.getAmount());
        }

        Table statsTable = new Table(UnitValue.createPercentArray(new float[]{33, 33, 34}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(16);

        addStatCell(statsTable, "Total Credits",  formatAmount(totalCredits),  GREEN, bold, regular);
        addStatCell(statsTable, "Total Debits",   formatAmount(totalDebits),   RED,   bold, regular);
        addStatCell(statsTable, "Transactions",   String.valueOf(monthTx.size()), BLUE, bold, regular);

        document.add(statsTable);

        // ══════════════════════════════════════════════════════════════════
        // TRANSACTIONS TABLE
        // ══════════════════════════════════════════════════════════════════
        document.add(new Paragraph("Transaction Details")
                .setFont(bold).setFontSize(11).setFontColor(NAVY).setMarginBottom(8));

        if (monthTx.isEmpty()) {
            document.add(new Paragraph("No transactions found for this period.")
                    .setFont(regular).setFontSize(10).setFontColor(MID_GRAY)
                    .setTextAlignment(TextAlignment.CENTER).setMarginTop(20));
        } else {
            Table txTable = new Table(
                    UnitValue.createPercentArray(new float[]{28, 20, 32, 20}))
                    .setWidth(UnitValue.createPercentValue(100));

            // Header row
            String[] headers = {"Date & Time", "Type", "Description", "Amount"};
            for (String h : headers) {
                txTable.addHeaderCell(new Cell()
                        .setBackgroundColor(NAVY)
                        .setBorder(Border.NO_BORDER)
                        .setPadding(8)
                        .add(new Paragraph(h)
                                .setFont(bold).setFontSize(9)
                                .setFontColor(WHITE)));
            }

            // Data rows
            boolean alternate = false;
            for (TransactionEntity tx : monthTx) {
                Color rowBg = alternate ? LIGHT_GRAY : WHITE;
                alternate = !alternate;

                boolean credit    = isCredit(tx, accountId);
                String  typeLabel = typeLabel(tx, accountId);
                String  desc      = description(tx, accountId);
                String  amtStr    = (credit ? "+" : "-") + formatAmount(tx.getAmount());
                DeviceRgb amtColor = credit ? GREEN : RED;

                LocalDateTime ldt = tx.getTimestamp()
                        .atZone(ZoneId.systemDefault()).toLocalDateTime();

                txTable.addCell(dataCell(DATETIME_FMT.format(ldt), rowBg, regular, 8.5f, TextAlignment.LEFT));
                txTable.addCell(dataCell(typeLabel, rowBg, bold, 8.5f, TextAlignment.LEFT));
                txTable.addCell(dataCell(desc, rowBg, regular, 8.5f, TextAlignment.LEFT));

                // Amount cell — coloured
                Cell amtCell = new Cell()
                        .setBackgroundColor(rowBg)
                        .setBorder(Border.NO_BORDER)
                        .setBorderBottom(new SolidBorder(LIGHT_GRAY, 0.5f))
                        .setPadding(7)
                        .add(new Paragraph(amtStr)
                                .setFont(bold).setFontSize(8.5f)
                                .setFontColor(amtColor)
                                .setTextAlignment(TextAlignment.RIGHT));
                txTable.addCell(amtCell);
            }

            document.add(txTable);
        }

        // ══════════════════════════════════════════════════════════════════
        // FOOTER
        // ══════════════════════════════════════════════════════════════════
        document.add(new LineSeparator(new SolidLine(0.5f))
                .setStrokeColor(MID_GRAY).setMarginTop(20).setMarginBottom(8));

        String generated = "Generated on " +
                DATE_FMT.format(LocalDate.now(ZoneId.systemDefault())) +
                "  |  SecureBank — Confidential";
        document.add(new Paragraph(generated)
                .setFont(regular).setFontSize(8).setFontColor(MID_GRAY)
                .setTextAlignment(TextAlignment.CENTER));

        document.close();
        return baos.toByteArray();
    }

    /* ── helpers ── */

    private static void addSummaryCell(Table t, String label, String value,
                                       PdfFont bold, PdfFont regular) {
        t.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(12)
                .add(new Paragraph(label)
                        .setFont(regular).setFontSize(8).setFontColor(MID_GRAY))
                .add(new Paragraph(value)
                        .setFont(bold).setFontSize(10).setFontColor(NAVY)));
    }

    private static void addStatCell(Table t, String label, String value,
                                    DeviceRgb colour, PdfFont bold, PdfFont regular) {
        t.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(colour, 3f))
                .setPadding(10)
                .add(new Paragraph(value)
                        .setFont(bold).setFontSize(14).setFontColor(colour))
                .add(new Paragraph(label)
                        .setFont(regular).setFontSize(8).setFontColor(MID_GRAY)));
    }

    private static Cell dataCell(String text, Color bg,
                                 PdfFont font, float size,
                                 TextAlignment align) {
        return new Cell()
                .setBackgroundColor(bg)
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(LIGHT_GRAY, 0.5f))
                .setPadding(7)
                .add(new Paragraph(text)
                        .setFont(font).setFontSize(size)
                        .setFontColor(NAVY)
                        .setTextAlignment(align));
    }

    private static boolean isCredit(TransactionEntity tx, UUID accountId) {
        return switch (tx.getType()) {
            case DEPOSIT  -> true;
            case WITHDRAWAL -> false;
            case TRANSFER -> accountId.equals(tx.getToAccountId());
        };
    }

    private static String typeLabel(TransactionEntity tx, UUID accountId) {
        return switch (tx.getType()) {
            case DEPOSIT    -> "Deposit";
            case WITHDRAWAL -> "Withdrawal";
            case TRANSFER   -> accountId.equals(tx.getToAccountId())
                               ? "Transfer In" : "Transfer Out";
        };
    }

    private static String description(TransactionEntity tx, UUID accountId) {
        return switch (tx.getType()) {
            case DEPOSIT    -> "Cash / Bank Deposit";
            case WITHDRAWAL -> "Cash Withdrawal";
            case TRANSFER   -> accountId.equals(tx.getToAccountId())
                               ? "Received from " + shortAccNo(tx.getFromAccountId())
                               : "Sent to "       + shortAccNo(tx.getToAccountId());
        };
    }

    private static String shortAccNo(UUID id) {
        if (id == null) return "—";
        return id.toString().replace("-", "").substring(22).toUpperCase();
    }

    private static String formatAmount(BigDecimal amount) {
        return "Rs." + String.format("%,.2f", amount);
    }
}

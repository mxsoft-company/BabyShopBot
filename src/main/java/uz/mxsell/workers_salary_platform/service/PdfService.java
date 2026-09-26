package uz.mxsell.workers_salary_platform.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.mxsell.workers_salary_platform.dto.onec.OneCReconciliationDto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;

@Slf4j
@Service
public class PdfService {

    private static final String FONT_RESOURCE = "/fonts/DejaVuSans.ttf";
    private static final String FONT_FAMILY = "DejaVu Sans";

    public byte[] generateReconciliationPdf(OneCReconciliationDto data) throws IOException {
        String html = buildReconciliationHtml(data);

        try {
            return render(html, true);
        } catch (Exception e) {
            log.error("PDF rendering with '{}' font failed for reconciliation act: {}. "
                            + "Retrying without the custom font; Cyrillic characters may be rendered incorrectly.",
                    FONT_FAMILY, e.getMessage(), e);
        }

        try {
            return render(html, false);
        } catch (Exception e) {
            log.error("PDF rendering failed even without the custom font: {}", e.getMessage(), e);
            throw new IOException("PDF yaratishda xatolik yuz berdi", e);
        }
    }

    private byte[] render(String html, boolean withFont) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();

        if (withFont) {
            try {
                // useFont() MUST be called before withHtmlContent()
                builder.useFont(() -> getClass().getResourceAsStream(FONT_RESOURCE), FONT_FAMILY);
            } catch (Exception e) {
                log.error("Failed to register '{}' font from {}: {}. "
                                + "The PDF will be generated with the default font; "
                                + "Cyrillic characters may be rendered incorrectly.",
                        FONT_FAMILY, FONT_RESOURCE, e.getMessage(), e);
            }
        }

        builder.withHtmlContent(html, null);
        builder.toStream(out);
        builder.run();
        return out.toByteArray();
    }

    private String buildReconciliationHtml(OneCReconciliationDto data) {
        StringBuilder sb = new StringBuilder(2048);
        sb.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\"/>");
        sb.append("<style>");
        sb.append("body { font-family: '").append(FONT_FAMILY).append("', sans-serif; padding: 20px; color: #222222; }");
        sb.append("h1 { font-size: 20px; text-align: center; margin: 0 0 4px 0; }");
        sb.append(".subtitle { text-align: center; font-size: 12px; color: #666666; margin: 0 0 16px 0; }");
        sb.append(".employee { font-size: 14px; margin: 0 0 4px 0; }");
        sb.append(".period { font-size: 12px; color: #444444; margin: 0 0 14px 0; }");
        sb.append(".section-title { font-size: 14px; margin: 18px 0 6px 0; }");
        sb.append("table { width: 100%; border-collapse: collapse; margin-bottom: 8px; }");
        sb.append("th { background-color: #f0f0f0; border: 1px solid #cccccc; padding: 6px 8px; ");
        sb.append("font-size: 12px; text-align: left; }");
        sb.append("td { border: 1px solid #cccccc; padding: 6px 8px; font-size: 12px; }");
        sb.append("td.number, th.number { text-align: right; }");
        sb.append(".total-row td { font-weight: bold; background-color: #f7f7f7; }");
        sb.append(".footer { margin-top: 18px; font-size: 10px; color: #888888; text-align: center; }");
        sb.append("</style></head><body>");

        sb.append("<h1>Akt sverka</h1>");
        sb.append("<p class=\"subtitle\">Xodim bilan o'zaro hisob-kitob dalolatnomasi</p>");

        sb.append("<p class=\"employee\">👤 Xodim: ")
                .append(escape(data.getName() != null ? data.getName() : "—")).append("</p>");
        sb.append("<p class=\"period\">📅 Davr: ")
                .append(escape(data.getDateFrom() != null ? data.getDateFrom() : "—"))
                .append(" — ")
                .append(escape(data.getDateTo() != null ? data.getDateTo() : "—"))
                .append("</p>");

        sb.append("<p class=\"section-title\">💰 Jami ko'rsatkichlar</p>");
        sb.append("<table><tbody>");
        appendSummaryRow(sb, "Hisoblangan", data.getAccrued(), false);
        appendSummaryRow(sb, "To'langan", data.getPaid(), false);
        appendSummaryRow(sb, "Soliq", data.getTax(), false);
        appendSummaryRow(sb, "Jarima", data.getPenalty(), false);
        appendSummaryRow(sb, "Qoldiq", data.getBalance(), true);
        sb.append("</tbody></table>");

        sb.append("<p class=\"section-title\">📋 Tafsilotlar</p>");
        if (data.getRows() == null || data.getRows().isEmpty()) {
            sb.append("<p>Tafsilotlar mavjud emas.</p>");
        } else {
            sb.append("<table><thead><tr>");
            sb.append("<th>Sana</th><th>Turi</th><th>Tafsilot</th>");
            sb.append("</tr></thead><tbody>");
            for (OneCReconciliationDto.OneCReconciliationRowDto row : data.getRows()) {
                sb.append("<tr>");
                sb.append("<td>").append(escape(row.getDate() != null ? row.getDate() : "—")).append("</td>");
                sb.append("<td>").append(escape(typeLabel(row.getType()))).append("</td>");
                sb.append("<td>").append(escape(rowDetail(row))).append("</td>");
                sb.append("</tr>");
            }
            sb.append("</tbody></table>");
        }

        sb.append("<p class=\"footer\">Yaratilgan sana: ")
                .append(escape(LocalDate.now().toString()))
                .append("</p>");

        sb.append("</body></html>");
        return sb.toString();
    }

    private void appendSummaryRow(StringBuilder sb, String label, Double value, boolean total) {
        sb.append("<tr");
        if (total) {
            sb.append(" class=\"total-row\"");
        }
        sb.append("><td>").append(escape(label)).append("</td>");
        sb.append("<td class=\"number\">").append(escape(formatAmount(value))).append("</td></tr>");
    }

    /**
     * Mirrors the row formatting logic of
     * {@code UpdateDispatcher.showReconciliation()} (Telegram text version),
     * with null-safety for every optional field.
     */
    private String rowDetail(OneCReconciliationDto.OneCReconciliationRowDto row) {
        String type = row.getType() != null ? row.getType() : "";
        return switch (type) {
            case "WORK" -> {
                StringBuilder d = new StringBuilder();
                if (row.getProduct() != null) {
                    d.append(row.getProduct());
                }
                appendQtyAndPrice(d, row);
                d.append(" = ").append(formatAmount(row.getAmount()));
                yield d.toString();
            }
            case "PAYMENT" -> {
                StringBuilder d = new StringBuilder("To'lov: ").append(formatAmount(row.getAmount()));
                if (row.getCurrency() != null) {
                    d.append(" ").append(row.getCurrency());
                }
                yield d.toString();
            }
            case "PRODUCT" -> {
                StringBuilder d = new StringBuilder();
                if (row.getProduct() != null) {
                    d.append(row.getProduct());
                }
                appendQtyAndPrice(d, row);
                d.append(" = ").append(formatAmount(row.getAmount()));
                yield d.toString();
            }
            case "PENALTY" -> "Jarima: " + formatAmount(row.getAmount());
            default -> {
                StringBuilder d = new StringBuilder();
                if (row.getDescription() != null) {
                    d.append(row.getDescription()).append(": ");
                }
                d.append(formatAmount(row.getAmount()));
                yield d.toString();
            }
        };
    }

    private void appendQtyAndPrice(StringBuilder sb, OneCReconciliationDto.OneCReconciliationRowDto row) {
        if (row.getQty() != null && row.getPrice() != null) {
            sb.append(" (").append(String.format("%,.2f", row.getQty()))
                    .append(" x ").append(String.format("%,.2f", row.getPrice())).append(")");
        }
    }

    private String typeLabel(String type) {
        String value = type != null ? type : "";
        return switch (value) {
            case "WORK" -> "Ish";
            case "PAYMENT" -> "To'lov";
            case "PRODUCT" -> "Mahsulot";
            case "PENALTY" -> "Jarima";
            default -> value.isEmpty() ? "Boshqa" : value;
        };
    }

    private String formatAmount(Double val) {
        return val != null ? String.format("%,.2f so'm", val) : "0.00 so'm";
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}

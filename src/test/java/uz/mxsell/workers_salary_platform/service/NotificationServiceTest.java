package uz.mxsell.workers_salary_platform.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import uz.mxsell.workers_salary_platform.dto.api.NotificationRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationServiceTest {

    private final NotificationService service = new NotificationService(null, null, null, null);

    @Test
    void buildDetailBlock_withAllFields_containsEveryEscapedField() {
        NotificationRequest r = new NotificationRequest();
        r.setAdditional(true);
        r.setOrder("ORD-12.5");
        r.setWork("Kesish");
        r.setSubwork("Chet");
        r.setProduct("Futbolka (qizil)");
        r.setBatch("B-1");
        r.setColor("Qizil");
        r.setQty(12.5);
        r.setMachine("Stanok-3");
        r.setPrice(1500.0);
        r.setSum(18750.0);
        r.setDefect(2.0);
        r.setAccrued(18000.0);
        r.setTax(2340.0);
        r.setNet(15660.0);
        r.setPenalty(500.0);
        r.setComment("Izoh (test).");

        String result = ReflectionTestUtils.invokeMethod(service, "buildDetailBlock", r);

        assertNotNull(result);
        assertTrue(result.contains("🔥 *Qo'shimcha ish*"));
        assertTrue(result.contains("📦 Buyurtma: ORD\\-12\\.5"));
        assertTrue(result.contains("🔧 Ish: Kesish / Chet"));
        assertTrue(result.contains("🏷 Mahsulot: Futbolka \\(qizil\\)"));
        assertTrue(result.contains("📋 Partiya: B\\-1"));
        assertTrue(result.contains("🎨 Rang: Qizil"));
        assertTrue(result.contains("🔢 Miqdor: 12\\.50"));
        assertTrue(result.contains("⚙️ Stanok: Stanok\\-3"));
        assertTrue(result.contains("💵 Narx: 1,500\\.00 so'm"));
        assertTrue(result.contains("⚠️ Brak: 2\\.00"));
        assertTrue(result.contains("💰 Summa: 18,750\\.00 so'm"));
        assertTrue(result.contains("💰 Hisoblangan: 18,000\\.00 so'm"));
        assertTrue(result.contains("🏛 Soliq: 2,340\\.00 so'm"));
        assertTrue(result.contains("💵 Qo'lga: 15,660\\.00 so'm"));
        assertTrue(result.contains("⚠️ Jarima: 500\\.00 so'm"));
        assertTrue(result.contains("💬 Izoh: Izoh \\(test\\)\\."));
    }

    @Test
    void buildDetailBlock_withoutOptionalFields_returnsEmptyString() {
        NotificationRequest r = new NotificationRequest();
        r.setType("SYSTEM");
        r.setTitle("Eslatma");
        r.setMessage("Oddiy xabar");

        String result = ReflectionTestUtils.invokeMethod(service, "buildDetailBlock", r);

        assertEquals("", result);
    }
}

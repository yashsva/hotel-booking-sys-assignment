package com.rupeek.hotelbooking.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Drives the real HTTP API through onboard -> search -> book -> pay -> cancel -> search again. */
@SpringBootTest
@AutoConfigureMockMvc
class BookingFlowIntegrationTest {

    private static final LocalDate CHECK_IN = LocalDate.now(ZoneId.of("Asia/Kolkata")).plusDays(10);
    private static final LocalDate CHECK_OUT = CHECK_IN.plusDays(2);

    @Autowired
    private MockMvc mvc;

    @Test
    void fullBookingLifecycleOverHttp() throws Exception {
        String onboarding = postJson("/api/v1/owners", """
                {"name": "Flow Test Owner",
                 "properties": [{
                   "name": "Flow Test Inn",
                   "address": {"line": "1 MG Road", "locality": "Fort", "city": "Flowtown"},
                   "starRating": 4,
                   "amenities": ["WIFI", "POOL"],
                   "roomTypes": [{"name": "Deluxe", "maxOccupancy": 2, "totalUnits": 1, "pricePerNight": 3000}]
                 }]}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.owner.multiProperty").value(false))
                .andReturn().getResponse().getContentAsString();
        String propertyId = JsonPath.read(onboarding, "$.properties[0].id");
        String roomTypeId = JsonPath.read(onboarding, "$.properties[0].roomTypes[0].id");

        search("&amenities=POOL&minRating=4&maxPrice=3000")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].availableRooms[0].totalPrice.amount").value(6000.0));

        String bookingJson = """
                {"propertyId": "%s", "roomTypeId": "%s", "guestName": "Asha", "guests": 2,
                 "checkIn": "%s", "checkOut": "%s"}""".formatted(propertyId, roomTypeId, CHECK_IN, CHECK_OUT);
        String booking = postJson("/api/v1/bookings", bookingJson)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andReturn().getResponse().getContentAsString();
        String bookingId = JsonPath.read(booking, "$.id");

        postJson("/api/v1/bookings", bookingJson)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("RoomUnavailableException"));
        search("").andExpect(jsonPath("$", hasSize(0)));

        String payment = """
                {"method": "UPI", "details": {"vpa": "asha@okbank"}}""";
        mvc.perform(post("/api/v1/bookings/{id}/payments", bookingId)
                        .contentType(MediaType.APPLICATION_JSON).content(payment))
                .andExpect(status().isBadRequest()); // Idempotency-Key header is mandatory
        pay(bookingId, "key-1", payment)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("SUCCEEDED"))
                .andExpect(jsonPath("$.bookingStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.replayed").value(false));
        pay(bookingId, "key-1", payment)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replayed").value(true));

        mvc.perform(post("/api/v1/bookings/{id}/cancellation", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.booking.status").value("CANCELLED"))
                .andExpect(jsonPath("$.refundAmount.amount").value(6000.0));
        mvc.perform(post("/api/v1/bookings/{id}/cancellation", bookingId))
                .andExpect(status().isConflict());

        search("").andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/v1/bookings/{id}", bookingId))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void invalidInputIsReportedAsProblemDetails() throws Exception {
        postJson("/api/v1/owners", """
                {"name": "", "properties": []}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.properties").exists());
        postJson("/api/v1/bookings", """
                {"propertyId": "p", "roomTypeId": "r", "guestName": "A", "guests": 1,
                 "checkIn": "%s", "checkOut": "%s"}""".formatted(CHECK_OUT, CHECK_IN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("checkOut must be after checkIn"));
        mvc.perform(get("/api/v1/bookings/{id}", "missing"))
                .andExpect(status().isNotFound());
        pay("missing", "k", """
                {"method": "CRYPTO", "details": {}}""")
                .andExpect(status().isNotFound());
    }

    private ResultActions postJson(String url, String body) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions pay(String bookingId, String key, String body) throws Exception {
        return mvc.perform(post("/api/v1/bookings/{id}/payments", bookingId)
                .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions search(String extraQuery) throws Exception {
        return mvc.perform(get("/api/v1/properties/search?city=Flowtown&guests=2&checkIn=" + CHECK_IN
                + "&checkOut=" + CHECK_OUT + extraQuery));
    }
}

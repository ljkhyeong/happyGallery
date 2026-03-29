package com.personal.happygallery.app.pass;

import com.personal.happygallery.app.booking.port.out.ClassStorePort;
import com.personal.happygallery.app.booking.port.out.SlotStorePort;
import com.personal.happygallery.app.customer.port.out.UserReaderPort;
import com.personal.happygallery.app.pass.port.out.PassPurchaseStorePort;
import com.personal.happygallery.app.web.CustomerAuthFilter;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.pass.PassPurchase;
import com.personal.happygallery.support.BookingTestHelper;
import com.personal.happygallery.support.TestCleanupSupport;
import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static com.personal.happygallery.support.BookingTestHelper.FUTURE;
import static com.personal.happygallery.support.TestFixtures.defaultBookingClass;
import static com.personal.happygallery.support.TestFixtures.passPurchase;
import static com.personal.happygallery.support.TestFixtures.slot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

final class PassCreditUsageFixture {

    private final WebApplicationContext context;
    private final CustomerAuthFilter customerAuthFilter;
    private final Filter springSessionRepositoryFilter;
    private final ClassStorePort classStorePort;
    private final SlotStorePort slotStorePort;
    private final UserReaderPort userReaderPort;
    private final PassPurchaseStorePort passPurchaseStorePort;
    private final TestCleanupSupport cleanupSupport;
    private final Clock clock;

    private MockMvc mockMvc;
    private BookingClass bookingClass;
    private PassPurchase pass;
    private Cookie sessionCookie;

    PassCreditUsageFixture(WebApplicationContext context,
                           CustomerAuthFilter customerAuthFilter,
                           Filter springSessionRepositoryFilter,
                           ClassStorePort classStorePort,
                           SlotStorePort slotStorePort,
                           UserReaderPort userReaderPort,
                           PassPurchaseStorePort passPurchaseStorePort,
                           TestCleanupSupport cleanupSupport,
                           Clock clock) {
        this.context = context;
        this.customerAuthFilter = customerAuthFilter;
        this.springSessionRepositoryFilter = springSessionRepositoryFilter;
        this.classStorePort = classStorePort;
        this.slotStorePort = slotStorePort;
        this.userReaderPort = userReaderPort;
        this.passPurchaseStorePort = passPurchaseStorePort;
        this.cleanupSupport = cleanupSupport;
        this.clock = clock;
    }

    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSessionRepositoryFilter, customerAuthFilter)
                .build();
        cleanupSupport.clearBookingWithPassAndRefundData();
        cleanupSupport.clearUsers();

        bookingClass = classStorePort.save(defaultBookingClass());
        sessionCookie = signupAndGetSessionCookie("pass-member@example.com", "01099990001");
        Long userId = userReaderPort.findByEmail("pass-member@example.com").orElseThrow().getId();
        pass = passPurchaseStorePort.save(passPurchase(userId, FUTURE.plusDays(90), 320_000L));
    }

    MockMvc mockMvc() {
        return mockMvc;
    }

    PassPurchase pass() {
        return pass;
    }

    Cookie sessionCookie() {
        return sessionCookie;
    }

    Clock clock() {
        return clock;
    }

    Slot createFutureSlot() {
        return slotStorePort.save(slot(bookingClass, FUTURE, FUTURE.plusHours(2)));
    }

    Slot createFutureSlot(LocalDateTime startAt) {
        return slotStorePort.save(slot(bookingClass, startAt, startAt.plusHours(2)));
    }

    Long createPassBooking(Long slotId) throws Exception {
        String response = mockMvc.perform(post("/api/v1/me/bookings")
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "slotId": %d,
                                  "passId": %d
                                }
                                """.formatted(slotId, pass.getId())))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return BookingTestHelper.extractBookingId(response);
    }

    private Cookie signupAndGetSessionCookie(String email, String phone) throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "password123",
                                  "name": "회원",
                                  "phone": "%s"
                                }
                                """.formatted(email, phone)))
                .andExpect(status().isCreated())
                .andReturn();
        Cookie cookie = result.getResponse().getCookie("HG_SESSION");
        assertThat(cookie).isNotNull();
        return cookie;
    }
}

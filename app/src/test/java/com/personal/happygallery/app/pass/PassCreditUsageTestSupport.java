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
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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

abstract class PassCreditUsageTestSupport {

    @Autowired protected WebApplicationContext context;
    @Autowired protected CustomerAuthFilter customerAuthFilter;
    @Autowired @Qualifier("springSessionRepositoryFilter") protected Filter springSessionRepositoryFilter;
    @Autowired protected ClassStorePort classStorePort;
    @Autowired protected SlotStorePort slotStorePort;
    @Autowired protected UserReaderPort userReaderPort;
    @Autowired protected PassPurchaseStorePort passPurchaseStorePort;
    @Autowired protected TestCleanupSupport cleanupSupport;
    @Autowired protected Clock clock;

    protected MockMvc mockMvc;
    protected BookingClass bookingClass;
    protected PassPurchase pass;
    protected Cookie sessionCookie;

    @BeforeEach
    void setUpPassCreditUsageContext() throws Exception {
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

    protected Slot createFutureSlot() {
        return slotStorePort.save(slot(bookingClass, FUTURE, FUTURE.plusHours(2)));
    }

    protected Slot createFutureSlot(LocalDateTime startAt) {
        return slotStorePort.save(slot(bookingClass, startAt, startAt.plusHours(2)));
    }

    protected Long createPassBooking(Long slotId) throws Exception {
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

    protected Cookie signupAndGetSessionCookie(String email, String phone) throws Exception {
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

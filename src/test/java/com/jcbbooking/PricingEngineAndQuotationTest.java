package com.jcbbooking;

import com.jcbbooking.dto.QuotationRequest;
import com.jcbbooking.dto.QuotationResponse;
import com.jcbbooking.exception.ResourceNotFoundException;
import com.jcbbooking.model.Pricing;
import com.jcbbooking.model.Product;
import com.jcbbooking.model.RentalPackage;
import com.jcbbooking.repository.PricingRepository;
import com.jcbbooking.repository.ProductRepository;
import com.jcbbooking.repository.RentalPackageRepository;
import com.jcbbooking.service.impl.PricingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PricingEngineAndQuotationTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private PricingRepository pricingRepository;

    @Mock
    private RentalPackageRepository rentalPackageRepository;

    @InjectMocks
    private PricingServiceImpl pricingService;

    private Product testCarProduct;
    private Product testJcbProduct;
    private Pricing testRidePricing;
    private Pricing testJcbPricing;

    @BeforeEach
    void setUp() {
        testCarProduct = Product.builder()
                .id(1L)
                .name("Sedan Prime")
                .productType("RIDE")
                .category("SEDAN")
                .build();

        testJcbProduct = Product.builder()
                .id(2L)
                .name("JCB 3DX Excavator")
                .productType("HEAVY_EQUIPMENT")
                .category("EXCAVATOR")
                .build();

        testRidePricing = Pricing.builder()
                .id(1L)
                .productId(1L)
                .pricingModel("RIDE")
                .basePrice(50.0)
                .perKmPrice(14.0)
                .perMinutePrice(1.5)
                .bookingFee(18.0)
                .minimumFare(100.0)
                .driverCharge(400.0)
                .nightAllowance(300.0)
                .nightAllowanceStartHour(23)
                .nightAllowanceEndHour(6)
                .minOutstationKmOneWay(50.0)
                .minOutstationKmRoundTrip(250.0)
                .taxPercentage(5.0)
                .surgeMultiplier(1.0)
                .active(true)
                .build();

        testJcbPricing = Pricing.builder()
                .id(2L)
                .productId(2L)
                .pricingModel("HEAVY_EQUIPMENT")
                .basePrice(1000.0)
                .perHourPrice(850.0)
                .minimumHours(4)
                .operatorCharge(500.0)
                .driverCharge(300.0)
                .taxPercentage(5.0)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("1. Local booking quotation and fare calculation")
    void testLocalBookingQuotation() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testCarProduct));
        when(pricingRepository.findByProductIdAndActiveTrue(1L)).thenReturn(Optional.of(testRidePricing));

        QuotationRequest req = QuotationRequest.builder()
                .productId(1L)
                .bookingType("LOCAL_RIDE")
                .pickupLatitude(12.9716)
                .pickupLongitude(77.5946)
                .dropLatitude(12.9352)
                .dropLongitude(77.6245)
                .durationHours(0.5)
                .build();

        QuotationResponse res = pricingService.generateAuthoritativeQuotation(req);

        assertNotNull(res);
        assertNotNull(res.getQuotationId());
        assertTrue(res.getTotalEstimatedFare() > 0);
        assertEquals("INR", res.getCurrency());
        assertEquals("Sedan Prime", res.getProductName());
        assertTrue(res.getEstimatedDistanceKm() > 0);
    }

    @Test
    @DisplayName("2. Scheduled booking with valid future date")
    void testScheduledBookingQuotation() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testCarProduct));
        when(pricingRepository.findByProductIdAndActiveTrue(1L)).thenReturn(Optional.of(testRidePricing));

        QuotationRequest req = QuotationRequest.builder()
                .productId(1L)
                .bookingType("SCHEDULED")
                .scheduledPickupAt(LocalDateTime.now().plusDays(2))
                .pickupLatitude(12.9716)
                .pickupLongitude(77.5946)
                .dropLatitude(12.9352)
                .dropLongitude(77.6245)
                .build();

        QuotationResponse res = pricingService.generateAuthoritativeQuotation(req);
        assertNotNull(res);
        assertEquals("SCHEDULED", res.getBookingType());
        assertTrue(res.getTotalEstimatedFare() > 0);
    }

    @Test
    @DisplayName("3. Rental package pricing calculation with extra hours/km")
    void testRentalPackagePricing() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testCarProduct));
        when(pricingRepository.findByProductIdAndActiveTrue(1L)).thenReturn(Optional.of(testRidePricing));

        RentalPackage pkg4hr = RentalPackage.builder()
                .id(3L)
                .name("4 hr 40 km")
                .includedHours(4.0)
                .includedKm(40.0)
                .basePackageFare(1099.0)
                .extraKmPrice(11.0)
                .extraHourPrice(90.0)
                .bookingFee(18.0)
                .taxPercentage(5.0)
                .active(true)
                .build();

        when(rentalPackageRepository.findById(3L)).thenReturn(Optional.of(pkg4hr));
        when(rentalPackageRepository.findByNameIgnoreCase("4 hr 40 km")).thenReturn(Optional.of(pkg4hr));

        QuotationRequest req = QuotationRequest.builder()
                .productId(1L)
                .bookingType("RENTAL")
                .rentalPackageId(3L)
                .distanceKm(50.0) // 10 km extra
                .durationHours(5.0) // 1 hr extra
                .build();

        QuotationResponse res = pricingService.generateAuthoritativeQuotation(req);

        assertNotNull(res);
        // Base: 1099 + Extra KM (10 * 11 = 110) + Extra Hour (1 * 90 = 90) + Booking Fee (18) = 1317. Tax 5% = 65.85. Total = 1382.85
        assertTrue(res.getTotalEstimatedFare() >= 1380.0);
    }

    @Test
    @DisplayName("4. Outstation one-way pricing with minimum chargeable km")
    void testOutstationOneWayPricing() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testCarProduct));
        when(pricingRepository.findByProductIdAndActiveTrue(1L)).thenReturn(Optional.of(testRidePricing));

        QuotationRequest req = QuotationRequest.builder()
                .productId(1L)
                .bookingType("OUTSTATION")
                .outstationTripType("ONE_WAY")
                .distanceKm(30.0) // Below min 50 km -> must charge for 50 km
                .durationHours(2.0)
                .build();

        QuotationResponse res = pricingService.generateAuthoritativeQuotation(req);
        assertNotNull(res);
        // Min 50km * 14 = 700 + 400 driver + 18 booking fee = 1118 + 5% tax = 1173.9
        assertTrue(res.getTotalEstimatedFare() >= 1100.0);
    }

    @Test
    @DisplayName("5. Outstation round-trip pricing with multi-day allowance")
    void testOutstationRoundTripPricing() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testCarProduct));
        when(pricingRepository.findByProductIdAndActiveTrue(1L)).thenReturn(Optional.of(testRidePricing));

        LocalDateTime pickup = LocalDateTime.now().plusDays(1);
        LocalDateTime returnAt = pickup.plusHours(48); // 2 days

        QuotationRequest req = QuotationRequest.builder()
                .productId(1L)
                .bookingType("OUTSTATION")
                .outstationTripType("ROUND_TRIP")
                .scheduledPickupAt(pickup)
                .returnAt(returnAt)
                .distanceKm(150.0) // 150 * 2 = 300 km (above min 250 km)
                .durationHours(48.0)
                .build();

        QuotationResponse res = pricingService.generateAuthoritativeQuotation(req);
        assertNotNull(res);
        // Effective 300 km * 14 = 4200 + 2 days driver (800) + night allowance (600) + fee (18) + tax
        assertTrue(res.getTotalEstimatedFare() >= 5000.0);
    }

    @Test
    @DisplayName("6. Heavy equipment pricing calculation")
    void testHeavyEquipmentPricing() {
        when(productRepository.findById(2L)).thenReturn(Optional.of(testJcbProduct));
        when(pricingRepository.findByProductIdAndActiveTrue(2L)).thenReturn(Optional.of(testJcbPricing));

        QuotationRequest req = QuotationRequest.builder()
                .productId(2L)
                .bookingType("HEAVY_EQUIPMENT")
                .durationHours(6.0) // 6 hours (above min 4 hours)
                .distanceKm(10.0)
                .build();

        QuotationResponse res = pricingService.generateAuthoritativeQuotation(req);
        assertNotNull(res);
        // Base 1000 + (6 * 850 = 5100) + 500 operator + 300 driver = 6900 + 5% tax = 7245.0
        assertTrue(res.getTotalEstimatedFare() >= 7000.0);
    }

    @Test
    @DisplayName("7. Invalid vehicle/model combination throws exception")
    void testInvalidVehicleThrowsException() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        QuotationRequest req = QuotationRequest.builder()
                .productId(999L)
                .bookingType("LOCAL_RIDE")
                .build();

        assertThrows(ResourceNotFoundException.class, () -> pricingService.generateAuthoritativeQuotation(req));
    }

    @Test
    @DisplayName("8. Past scheduled pickup throws exception")
    void testPastScheduledPickupThrowsException() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testCarProduct));

        QuotationRequest req = QuotationRequest.builder()
                .productId(1L)
                .bookingType("SCHEDULED")
                .scheduledPickupAt(LocalDateTime.now().minusHours(2)) // in past
                .build();

        assertThrows(IllegalArgumentException.class, () -> pricingService.generateAuthoritativeQuotation(req));
    }

    @Test
    @DisplayName("9. Invalid GPS coordinates throw exception")
    void testInvalidCoordinatesThrowsException() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testCarProduct));

        QuotationRequest req = QuotationRequest.builder()
                .productId(1L)
                .pickupLatitude(95.0) // Invalid: > 90
                .pickupLongitude(77.0)
                .build();

        assertThrows(IllegalArgumentException.class, () -> pricingService.generateAuthoritativeQuotation(req));
    }
}

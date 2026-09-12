package com.personal.happygallery.adapter.out.external.shipping;

import com.personal.happygallery.application.order.port.in.ShipmentTrackingWebhookUseCase.TrackingEvent;
import com.personal.happygallery.application.order.port.in.ShipmentTrackingWebhookUseCase.TrackingUpdate;
import com.personal.happygallery.application.order.port.out.KoreaPostTrackingLookup;
import com.personal.happygallery.domain.order.ShipmentTrackingStatus;
import com.personal.happygallery.domain.order.ShippingCarrier;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.helpers.DefaultHandler;

@Component
class KoreaPostShipmentTrackingLookup implements KoreaPostTrackingLookup {
    static final String PATH = "/trace/retrieveLongitudinalService/retrieveLongitudinalService/getLongitudinalDomesticList";
    private static final Logger log = LoggerFactory.getLogger(KoreaPostShipmentTrackingLookup.class);
    private final KoreaPostProperties properties;
    private final RestClient restClient;

    KoreaPostShipmentTrackingLookup(KoreaPostProperties properties,
            @Qualifier("koreaPostRestClient") RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    @Override
    public boolean isEnabled() {
        return properties.enabled();
    }

    @Override
    public Optional<TrackingUpdate> lookup(Long orderId, String trackingNumber) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        String registeredNumber = trackingNumber.replaceAll("[-\\s]", "");
        if (!registeredNumber.matches("[0-9]{13}")) {
            throw new IllegalArgumentException("우체국 운송장 번호는 숫자 13자리여야 합니다.");
        }
        try {
            byte[] body = restClient.get().uri(uri -> uri.path(PATH)
                            .queryParam("serviceKey", "{key}").queryParam("rgist", "{number}")
                            .build(properties.serviceKey(), registeredNumber))
                    .retrieve().body(byte[].class);
            return parse(orderId, trackingNumber, body);
        } catch (Exception exception) {
            // URL에 인증키와 운송장이 있으므로 원인 예외·본문을 로그에 남기지 않는다.
            log.warn("우체국 배송조회 실패 [orderId={} type={}]", orderId, exception.getClass().getSimpleName());
            throw new IllegalStateException("우체국 배송조회 응답을 처리하지 못했습니다.");
        }
    }

    private Optional<TrackingUpdate> parse(Long orderId, String trackingNumber, byte[] body) throws Exception {
        if (body == null) {
            throw new IllegalArgumentException("빈 배송조회 응답");
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newDefaultInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        var builder = factory.newDocumentBuilder();
        builder.setErrorHandler(new DefaultHandler());
        Element root = builder.parse(new ByteArrayInputStream(body)).getDocumentElement();
        if (!"LongitudinalDomesticListResponse".equals(root.getTagName())
                || !"Y".equals(text(root, "successYN")) || !"00".equals(text(root, "returnCode"))) {
            throw new IllegalArgumentException("실패한 배송조회 응답");
        }
        NodeList items = root.getElementsByTagName("longitudinalDomesticList");
        List<TrackingEvent> events = new ArrayList<>();
        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);
            String statusText = text(item, "processSttus");
            events.add(new TrackingEvent(
                    LocalDate.parse(text(item, "dlvyDate").replace('.', '-'))
                            .atTime(LocalTime.parse(text(item, "dlvyTime"))),
                    status(statusText), statusText, text(item, "nowLc"), null));
        }
        if (events.isEmpty()) {
            return Optional.empty();
        }
        events.sort(Comparator.comparing(TrackingEvent::occurredAt));
        String statusText = text(root, "dlvySttus");
        if (statusText.isBlank()) {
            statusText = events.getLast().statusText();
        }
        return Optional.of(new TrackingUpdate(orderId, ShippingCarrier.KOREA_POST, trackingNumber,
                status(statusText), statusText, events));
    }

    private static String text(Element element, String name) {
        NodeList nodes = element.getElementsByTagName(name);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().strip();
    }

    private static ShipmentTrackingStatus status(String text) {
        return switch (text) {
            case "접수" -> ShipmentTrackingStatus.PICKED_UP;
            case "발송", "도착" -> ShipmentTrackingStatus.IN_TRANSIT;
            case "배달준비" -> ShipmentTrackingStatus.OUT_FOR_DELIVERY;
            case "배달완료" -> ShipmentTrackingStatus.DELIVERED;
            default -> ShipmentTrackingStatus.UNKNOWN;
        };
    }
}

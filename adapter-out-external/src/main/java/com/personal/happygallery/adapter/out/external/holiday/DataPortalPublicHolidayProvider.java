package com.personal.happygallery.adapter.out.external.holiday;

import com.personal.happygallery.application.booking.port.out.PublicHolidayProvider;
import com.personal.happygallery.application.booking.port.out.PublicHolidaySnapshotPort.PublicHoliday;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

@Component
class DataPortalPublicHolidayProvider implements PublicHolidayProvider {

    private static final Logger log = LoggerFactory.getLogger(DataPortalPublicHolidayProvider.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final PublicHolidayApiProperties properties;
    private final RestClient restClient;

    DataPortalPublicHolidayProvider(
            PublicHolidayApiProperties properties,
            RestClient publicHolidayRestClient) {
        this.properties = properties;
        this.restClient = publicHolidayRestClient;
    }

    @Override
    public boolean isEnabled() {
        return properties.enabled();
    }

    @Override
    public Optional<List<PublicHoliday>> fetch(int year) {
        if (!properties.enabled()) {
            return Optional.empty();
        }

        try {
            byte[] xml = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/B090041/openapi/service/SpcdeInfoService/getRestDeInfo")
                            .queryParam("ServiceKey", properties.serviceKey())
                            .queryParam("pageNo", 1)
                            .queryParam("numOfRows", 100)
                            .queryParam("solYear", year)
                            .build())
                    .retrieve()
                    .body(byte[].class);
            return Optional.of(parse(xml));
        } catch (RestClientException | IllegalArgumentException exception) {
            log.warn("공식 공휴일 조회에 실패했습니다. year={}, type={}",
                    year, exception.getClass().getSimpleName());
            return Optional.empty();
        } catch (Exception exception) {
            log.warn("공식 공휴일 응답을 읽지 못했습니다. year={}, type={}",
                    year, exception.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private static List<PublicHoliday> parse(byte[] xml) throws Exception {
        if (xml == null || xml.length == 0) {
            throw new IllegalArgumentException("공휴일 응답이 비어 있습니다.");
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newDefaultInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        Element root = factory.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml))
                .getDocumentElement();
        if (!"00".equals(text(root, "resultCode"))) {
            throw new IllegalArgumentException("공휴일 API가 실패 응답을 반환했습니다.");
        }

        NodeList items = root.getElementsByTagName("item");
        Map<LocalDate, String> namesByDate = new LinkedHashMap<>();
        for (int index = 0; index < items.getLength(); index++) {
            Element item = (Element) items.item(index);
            LocalDate date = LocalDate.parse(text(item, "locdate"), DATE_FORMAT);
            String name = text(item, "dateName");
            namesByDate.merge(date, name, (left, right) -> left.equals(right)
                    ? left
                    : left + " · " + right);
        }
        return namesByDate.entrySet().stream()
                .map(entry -> new PublicHoliday(entry.getKey(), entry.getValue()))
                .toList();
    }

    private static String text(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            throw new IllegalArgumentException("공휴일 응답 필드가 없습니다: " + tagName);
        }
        return nodes.item(0).getTextContent();
    }
}

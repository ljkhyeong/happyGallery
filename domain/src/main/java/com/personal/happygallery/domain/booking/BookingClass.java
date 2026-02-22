package com.personal.happygallery.domain.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** 체험 클래스 — classes 테이블 */
@Entity
@Table(name = "classes")
public class BookingClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** PERFUME | WOOD | KNIT | POP | ... */
    @Column(nullable = false, length = 30)
    private String category;

    @Column(name = "duration_min", nullable = false)
    private int durationMin;

    /** 원(KRW) 단위 */
    @Column(nullable = false)
    private long price;

    /** 뒤쪽 버퍼(분), 기본 30분 */
    @Column(name = "buffer_min", nullable = false)
    private int bufferMin = 30;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected BookingClass() {}

    public BookingClass(String name, String category, int durationMin, long price, int bufferMin) {
        this.name = name;
        this.category = category;
        this.durationMin = durationMin;
        this.price = price;
        this.bufferMin = bufferMin;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public int getDurationMin() { return durationMin; }
    public long getPrice() { return price; }
    public int getBufferMin() { return bufferMin; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

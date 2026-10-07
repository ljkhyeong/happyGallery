-- 2026-10-07에 V1~V186을 합친 스키마 기준선이다.
-- 보존할 운영 데이터가 없어 기존 이력을 지우고 빈 DB에 이 파일부터 적용한다. 이후 변경은 새 migration으로 추가한다.
-- 테이블은 참조 대상이 먼저 오도록 정렬했고, 문자셋·정렬 규칙은 DB 기본값을 따른다.

CREATE TABLE `admin_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `credential_version` bigint NOT NULL DEFAULT '0',
  `totp_secret_enc` varchar(1024) DEFAULT NULL,
  `mfa_enabled` tinyint(1) NOT NULL DEFAULT '0',
  `last_accepted_totp_step` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_user_username` (`username`)
) ENGINE=InnoDB;

CREATE TABLE `admin_auth_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `admin_user_id` bigint DEFAULT NULL,
  `subject_hmac` char(64) DEFAULT NULL,
  `hmac_key_id` varchar(32) DEFAULT NULL,
  `outcome` varchar(30) NOT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_admin_auth_history_user_created` (`admin_user_id`,`created_at`),
  KEY `idx_admin_auth_history_subject_created` (`subject_hmac`,`created_at`),
  KEY `idx_admin_auth_history_created_id` (`created_at`,`id`),
  CONSTRAINT `fk_admin_auth_history_user` FOREIGN KEY (`admin_user_id`) REFERENCES `admin_user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE `admin_mfa_challenge` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `admin_user_id` bigint NOT NULL,
  `token_hmac` char(64) NOT NULL,
  `expires_at` datetime NOT NULL,
  `consumed_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_mfa_challenge_token` (`token_hmac`),
  KEY `idx_admin_mfa_challenge_user_created` (`admin_user_id`,`created_at`),
  CONSTRAINT `fk_admin_mfa_challenge_user` FOREIGN KEY (`admin_user_id`) REFERENCES `admin_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `admin_mfa_recovery_code` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `admin_user_id` bigint NOT NULL,
  `code_hash` varchar(255) NOT NULL,
  `used_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_admin_mfa_recovery_code_user_used` (`admin_user_id`,`used_at`),
  CONSTRAINT `fk_admin_mfa_recovery_code_user` FOREIGN KEY (`admin_user_id`) REFERENCES `admin_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `admin_setup_lock` (
  `id` tinyint NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `chk_admin_setup_lock_singleton` CHECK ((`id` = 1))
) ENGINE=InnoDB;

CREATE TABLE `booking_calendar_settings` (
  `id` bigint NOT NULL,
  `open_time` time NOT NULL,
  `close_time` time NOT NULL,
  `slot_interval_min` int NOT NULL,
  `block_public_holidays` tinyint(1) NOT NULL DEFAULT '1',
  `version` bigint NOT NULL DEFAULT '0',
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  CONSTRAINT `chk_booking_calendar_hours` CHECK ((`open_time` < `close_time`)),
  CONSTRAINT `chk_booking_calendar_interval` CHECK ((`slot_interval_min` between 10 and 120)),
  CONSTRAINT `chk_booking_calendar_singleton` CHECK ((`id` = 1))
) ENGINE=InnoDB;

CREATE TABLE `classes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `category` varchar(30) NOT NULL COMMENT 'PERFUME | WOOD | KNIT | POP | ...',
  `duration_min` int NOT NULL,
  `price` bigint NOT NULL COMMENT '원(KRW) 단위',
  `buffer_min` int NOT NULL DEFAULT '30' COMMENT '뒤쪽 버퍼(분)',
  `capacity` int NOT NULL DEFAULT '8',
  `description` text,
  `image_url` varchar(500) DEFAULT NULL,
  `preparation_info` varchar(2000) DEFAULT NULL,
  `target_audience` varchar(1000) DEFAULT NULL,
  `pass_eligible` tinyint(1) NOT NULL,
  `status` varchar(10) NOT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `situation_tags` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `chk_classes_capacity` CHECK ((`capacity` >= 1)),
  CONSTRAINT `chk_classes_price_range` CHECK ((`price` between 10 and 9007199254740991))
) ENGINE=InnoDB;

CREATE TABLE `guests` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name_enc` varchar(1024) NOT NULL,
  `name_hmac` char(64) NOT NULL,
  `phone_enc` varchar(255) NOT NULL,
  `phone_hmac` char(64) NOT NULL,
  `phone_verified` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_guests_phone_hmac` (`phone_hmac`),
  KEY `idx_guests_name_hmac` (`name_hmac`)
) ENGINE=InnoDB;

CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `email_enc` varchar(512) DEFAULT NULL,
  `email_hmac` char(64) DEFAULT NULL,
  `password_hash` varchar(255) DEFAULT NULL,
  `credential_version` bigint NOT NULL DEFAULT '0',
  `version` bigint NOT NULL DEFAULT '0',
  `name_enc` varchar(1024) NOT NULL,
  `name_hmac` char(64) NOT NULL,
  `phone_enc` varchar(255) DEFAULT NULL,
  `phone_hmac` char(64) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `phone_verified` tinyint(1) NOT NULL DEFAULT '0',
  `last_login_at` datetime(6) DEFAULT NULL,
  `withdrawn_at` datetime(6) DEFAULT NULL,
  `default_shipping_address_enc` text,
  `shipping_address_version` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_users_email_hmac` (`email_hmac`),
  UNIQUE KEY `uq_users_phone_hmac` (`phone_hmac`),
  KEY `idx_users_name_hmac` (`name_hmac`)
) ENGINE=InnoDB;

CREATE TABLE `pass_purchases` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `purchased_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `expires_at` datetime(6) NOT NULL COMMENT 'purchased_at + 90일',
  `plan_code` varchar(30) NOT NULL COMMENT '구매 시점 이용권 계획 코드',
  `total_credits` int NOT NULL COMMENT '구매 계획의 총 이용 횟수',
  `remaining_credits` int NOT NULL,
  `total_price` bigint NOT NULL DEFAULT '0' COMMENT '이용권 총 결제금액 (KRW)',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '낙관적 락 버전',
  `payment_key` varchar(200) DEFAULT NULL COMMENT 'Toss paymentKey (이용권 결제 확정 시)',
  PRIMARY KEY (`id`),
  KEY `idx_pass_purchases_expires_credits` (`expires_at`,`remaining_credits`),
  KEY `idx_pass_purchases_purchased_at` (`purchased_at`),
  KEY `idx_pass_purchases_user_purchased_id` (`user_id`,`purchased_at` DESC,`id` DESC),
  CONSTRAINT `fk_pass_user_v105` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB;

CREATE TABLE `slots` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `class_id` bigint NOT NULL,
  `start_at` datetime(6) NOT NULL,
  `end_at` datetime(6) NOT NULL,
  `capacity` int NOT NULL DEFAULT '8',
  `booked_count` int NOT NULL DEFAULT '0',
  `admin_active` tinyint(1) NOT NULL DEFAULT '1',
  `calendar_active` tinyint(1) NOT NULL DEFAULT '1',
  `buffer_block_count` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_slot_class_start` (`class_id`,`start_at`),
  KEY `idx_slots_class_availability_start` (`class_id`,`admin_active`,`calendar_active`,`buffer_block_count`,`start_at`),
  CONSTRAINT `fk_slot_class` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`),
  CONSTRAINT `chk_slots_booked_count_capacity` CHECK (((`booked_count` >= 0) and (`booked_count` <= `capacity`))),
  CONSTRAINT `chk_slots_buffer_block_count_non_negative` CHECK ((`buffer_block_count` >= 0)),
  CONSTRAINT `chk_slots_time_range` CHECK ((`start_at` < `end_at`))
) ENGINE=InnoDB;

CREATE TABLE `bookings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `guest_id` bigint DEFAULT NULL,
  `owner_phone_hmac` char(64) DEFAULT NULL,
  `class_id` bigint NOT NULL,
  `slot_id` bigint NOT NULL,
  `status` varchar(15) NOT NULL COMMENT 'BookingStatus: BOOKED | CANCELED | NO_SHOW | COMPLETED',
  `source` varchar(20) NOT NULL DEFAULT 'WEB',
  `deposit_amount` bigint NOT NULL COMMENT '원(KRW) 단위',
  `deposit_paid_at` datetime(6) DEFAULT NULL,
  `balance_amount` bigint NOT NULL DEFAULT '0',
  `participant_count` int NOT NULL DEFAULT '1',
  `balance_status` varchar(10) NOT NULL DEFAULT 'UNPAID' COMMENT 'BalanceStatus: UNPAID | PAID',
  `balance_paid_at` datetime(6) DEFAULT NULL,
  `arrears_flag` tinyint(1) NOT NULL DEFAULT '0' COMMENT '미수금 플래그',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '낙관적 락',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `access_token` varchar(64) DEFAULT NULL COMMENT '비회원 예약 조회용 토큰',
  `payment_method` varchar(15) DEFAULT NULL COMMENT 'DepositPaymentMethod: CARD | EASY_PAY | BANK_TRANSFER',
  `pass_purchase_id` bigint DEFAULT NULL COMMENT '8회권 결제 시 pass_purchases FK',
  `payment_key` varchar(200) DEFAULT NULL COMMENT 'Toss paymentKey (예약금 결제 확정 시)',
  `active_user_id` bigint GENERATED ALWAYS AS ((case when (`status` = 'BOOKED') then `user_id` else NULL end)) STORED,
  `active_guest_id` bigint GENERATED ALWAYS AS ((case when (`status` = 'BOOKED') then `guest_id` else NULL end)) STORED,
  `active_owner_phone_hmac` char(64) GENERATED ALWAYS AS ((case when (`status` = 'BOOKED') then `owner_phone_hmac` else NULL end)) STORED,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_bookings_review_source` (`id`,`class_id`),
  UNIQUE KEY `uq_bookings_active_user_slot` (`slot_id`,`active_user_id`),
  UNIQUE KEY `uq_bookings_active_guest_slot` (`slot_id`,`active_guest_id`),
  UNIQUE KEY `uq_bookings_active_phone_slot` (`slot_id`,`active_owner_phone_hmac`),
  KEY `fk_booking_class` (`class_id`),
  KEY `idx_bookings_guest` (`guest_id`),
  KEY `fk_booking_pass` (`pass_purchase_id`),
  KEY `idx_bookings_deposit_paid_at` (`deposit_paid_at`),
  KEY `idx_bookings_balance_paid_at` (`balance_paid_at`),
  KEY `idx_bookings_access_token_created` (`access_token`,`created_at` DESC,`id` DESC),
  KEY `idx_bookings_user_created_id` (`user_id`,`created_at` DESC,`id` DESC),
  CONSTRAINT `fk_booking_class` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`),
  CONSTRAINT `fk_booking_guest` FOREIGN KEY (`guest_id`) REFERENCES `guests` (`id`),
  CONSTRAINT `fk_booking_pass` FOREIGN KEY (`pass_purchase_id`) REFERENCES `pass_purchases` (`id`),
  CONSTRAINT `fk_booking_slot` FOREIGN KEY (`slot_id`) REFERENCES `slots` (`id`),
  CONSTRAINT `fk_booking_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `chk_bookings_arrears_unpaid` CHECK (((`arrears_flag` = false) or (`balance_status` = 'UNPAID'))),
  CONSTRAINT `chk_bookings_balance_payment` CHECK ((((`balance_status` = 'UNPAID') and (`balance_paid_at` is null)) or ((`balance_status` = 'PAID') and ((`balance_amount` = 0) or (`balance_paid_at` is not null))))),
  CONSTRAINT `chk_bookings_exactly_one_owner` CHECK ((((`user_id` is not null) and (`guest_id` is null)) or ((`user_id` is null) and (`guest_id` is not null)))),
  CONSTRAINT `chk_bookings_owner_phone_hmac_lifecycle` CHECK ((((`status` = 'BOOKED') and (`owner_phone_hmac` is not null)) or ((`status` <> 'BOOKED') and (`owner_phone_hmac` is null)))),
  CONSTRAINT `chk_bookings_participant_count` CHECK ((`participant_count` >= 1)),
  CONSTRAINT `chk_bookings_source` CHECK ((`source` in ('WEB','PHONE','NAVER_TALK','KAKAO','VISIT')))
) ENGINE=InnoDB;

CREATE TABLE `booking_cancellation_tasks` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `booking_id` bigint NOT NULL,
  `task_type` varchar(30) NOT NULL,
  `status` varchar(15) NOT NULL,
  `reason` varchar(500) NOT NULL,
  `completed_by_admin_id` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `completed_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_booking_cancellation_task_booking_type` (`booking_id`,`task_type`),
  KEY `idx_booking_cancellation_task_pending` (`status`,`created_at`,`id`),
  CONSTRAINT `fk_booking_cancellation_task_booking` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`)
) ENGINE=InnoDB;

CREATE TABLE `booking_day_overrides` (
  `calendar_date` date NOT NULL,
  `availability` varchar(10) NOT NULL,
  `reason` varchar(200) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`calendar_date`),
  CONSTRAINT `chk_booking_day_override_availability` CHECK ((`availability` in ('OPEN','CLOSED')))
) ENGINE=InnoDB;

CREATE TABLE `booking_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `booking_id` bigint NOT NULL,
  `action` varchar(30) NOT NULL,
  `from_slot_id` bigint DEFAULT NULL,
  `to_slot_id` bigint DEFAULT NULL,
  `actor` varchar(10) NOT NULL COMMENT 'CUSTOMER | ADMIN',
  `admin_user_id` bigint DEFAULT NULL,
  `reason` varchar(500) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `fk_bh_booking` (`booking_id`),
  KEY `idx_booking_history_admin_user_id_created_at` (`admin_user_id`,`created_at`),
  CONSTRAINT `fk_bh_booking` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`)
) ENGINE=InnoDB;

CREATE TABLE `booking_time_blocks` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `calendar_date` date NOT NULL,
  `start_time` time NOT NULL,
  `end_time` time NOT NULL,
  `reason` varchar(200) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_booking_time_block` (`calendar_date`,`start_time`,`end_time`),
  CONSTRAINT `chk_booking_time_block_range` CHECK ((`start_time` < `end_time`))
) ENGINE=InnoDB;

CREATE TABLE `booking_vacancy_alerts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `slot_id` bigint NOT NULL,
  `guest_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `access_token_hash` varchar(64) DEFAULT NULL,
  `active_key` varchar(100) DEFAULT NULL,
  `status` varchar(20) NOT NULL,
  `notified_at` datetime(6) DEFAULT NULL,
  `canceled_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `version` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_booking_vacancy_alert_active` (`active_key`),
  KEY `fk_booking_vacancy_alerts_guest` (`guest_id`),
  KEY `fk_booking_vacancy_alerts_user` (`user_id`),
  KEY `idx_booking_vacancy_alert_slot_status` (`slot_id`,`status`,`id`),
  CONSTRAINT `fk_booking_vacancy_alerts_guest` FOREIGN KEY (`guest_id`) REFERENCES `guests` (`id`),
  CONSTRAINT `fk_booking_vacancy_alerts_slot` FOREIGN KEY (`slot_id`) REFERENCES `slots` (`id`),
  CONSTRAINT `fk_booking_vacancy_alerts_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `chk_booking_vacancy_alert_owner` CHECK ((((`guest_id` is not null) and (`user_id` is null) and (`access_token_hash` is not null)) or ((`guest_id` is null) and (`user_id` is not null) and (`access_token_hash` is null))))
) ENGINE=InnoDB;

CREATE TABLE `products` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `type` varchar(20) NOT NULL COMMENT 'ProductType: READY_STOCK | MADE_TO_ORDER',
  `price` bigint NOT NULL COMMENT '원(KRW) 단위',
  `description` text,
  `image_url` varchar(500) DEFAULT NULL,
  `specification` varchar(2000) DEFAULT NULL COMMENT '재료, 크기, 고정 사양',
  `care_instructions` varchar(2000) DEFAULT NULL COMMENT '사용 및 관리 방법',
  `production_lead_days` int DEFAULT NULL COMMENT '주문제작 예상 제작 기간(일)',
  `status` varchar(10) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ProductStatus: ACTIVE | INACTIVE',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `category` varchar(50) DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0' COMMENT '낙관적 락',
  PRIMARY KEY (`id`),
  KEY `idx_products_status_created` (`status`,`created_at` DESC),
  KEY `idx_products_status_category` (`status`,`category`),
  CONSTRAINT `chk_products_purchase_terms` CHECK ((((`type` = 'MADE_TO_ORDER') and (`specification` is not null) and (char_length(trim(`specification`)) > 0) and (`production_lead_days` is not null) and (`production_lead_days` between 1 and 180)) or ((`type` = 'READY_STOCK') and (`production_lead_days` is null))))
) ENGINE=InnoDB;

CREATE TABLE `product_variants` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `combination_key` varchar(512) NOT NULL,
  `price_adjustment` bigint NOT NULL DEFAULT '0',
  `quantity` int NOT NULL DEFAULT '0',
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `minimum_stock` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_product_variant_combination` (`product_id`,`combination_key`),
  UNIQUE KEY `uq_variant_product_restock` (`id`,`product_id`),
  KEY `idx_product_variants_product` (`product_id`,`active`,`id`),
  CONSTRAINT `fk_product_variant_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  CONSTRAINT `chk_product_variant_quantity` CHECK ((`quantity` >= 0)),
  CONSTRAINT `ck_variant_minimum_stock` CHECK (((`minimum_stock` is null) or (`minimum_stock` >= 0)))
) ENGINE=InnoDB;

CREATE TABLE `cart_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `product_variant_id` bigint DEFAULT NULL,
  `line_key` varchar(64) NOT NULL,
  `qty` int NOT NULL DEFAULT '1',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_cart_user_line` (`user_id`,`line_key`),
  KEY `fk_cart_item_product` (`product_id`),
  KEY `idx_cart_items_product_variant` (`product_variant_id`),
  CONSTRAINT `fk_cart_item_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  CONSTRAINT `fk_cart_item_product_variant` FOREIGN KEY (`product_variant_id`) REFERENCES `product_variants` (`id`),
  CONSTRAINT `fk_cart_item_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB;

CREATE TABLE `product_option_groups` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `option_key` varchar(64) NOT NULL,
  `option_type` varchar(10) NOT NULL COMMENT 'SELECT | TEXT',
  `name` varchar(25) NOT NULL,
  `required` tinyint(1) NOT NULL,
  `sort_order` int NOT NULL,
  `input_placeholder` varchar(100) DEFAULT NULL,
  `input_max_length` int DEFAULT NULL,
  `input_price_adjustment` bigint DEFAULT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_product_option_group_key` (`product_id`,`option_key`),
  KEY `idx_product_option_groups_product` (`product_id`,`active`,`sort_order`,`id`),
  CONSTRAINT `fk_product_option_group_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  CONSTRAINT `chk_product_option_group_sort` CHECK ((`sort_order` >= 0)),
  CONSTRAINT `chk_product_option_group_type_fields` CHECK ((((`option_type` = 'SELECT') and (`input_placeholder` is null) and (`input_max_length` is null) and (`input_price_adjustment` is null)) or ((`option_type` = 'TEXT') and (`input_max_length` between 1 and 200) and (`input_price_adjustment` >= 0))))
) ENGINE=InnoDB;

CREATE TABLE `cart_item_text_inputs` (
  `cart_item_id` bigint NOT NULL,
  `option_group_id` bigint NOT NULL,
  `option_key` varchar(64) NOT NULL,
  `value` varchar(200) NOT NULL,
  `sort_order` int NOT NULL,
  PRIMARY KEY (`cart_item_id`,`option_group_id`),
  KEY `fk_cart_item_text_input_group` (`option_group_id`),
  CONSTRAINT `fk_cart_item_text_input_cart` FOREIGN KEY (`cart_item_id`) REFERENCES `cart_items` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_cart_item_text_input_group` FOREIGN KEY (`option_group_id`) REFERENCES `product_option_groups` (`id`),
  CONSTRAINT `chk_cart_item_text_input_sort` CHECK ((`sort_order` >= 0))
) ENGINE=InnoDB;

CREATE TABLE `cart_merge_requests` (
  `user_id` bigint NOT NULL,
  `idempotency_key` char(36) NOT NULL,
  `payload_hash` char(64) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`user_id`,`idempotency_key`),
  KEY `idx_cart_merge_requests_created` (`created_at`,`user_id`,`idempotency_key`),
  CONSTRAINT `fk_cart_merge_requests_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB;

CREATE TABLE `coupon_definitions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `discount_type` varchar(20) NOT NULL,
  `discount_value` bigint NOT NULL,
  `min_order_amount` bigint NOT NULL,
  `max_discount_amount` bigint DEFAULT NULL,
  `valid_from` datetime(6) NOT NULL,
  `valid_until` datetime(6) NOT NULL,
  `active` tinyint(1) NOT NULL,
  `publicly_claimable` tinyint(1) NOT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  CONSTRAINT `chk_coupon_definition_discount` CHECK ((((`discount_type` = 'FIXED') and (`discount_value` between 1 and 9007199254740991) and (`max_discount_amount` is null)) or ((`discount_type` = 'PERCENT') and (`discount_value` between 1 and 100) and (`max_discount_amount` between 1 and 9007199254740991)))),
  CONSTRAINT `chk_coupon_definition_flags` CHECK (((`active` in (0,1)) and (`publicly_claimable` in (0,1)))),
  CONSTRAINT `chk_coupon_definition_min_order` CHECK ((`min_order_amount` between 0 and 9007199254740991)),
  CONSTRAINT `chk_coupon_definition_validity` CHECK ((`valid_from` < `valid_until`))
) ENGINE=InnoDB;

CREATE TABLE `data_key_rotation_lock` (
  `id` tinyint NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `chk_data_key_rotation_lock_singleton` CHECK ((`id` = 1))
) ENGINE=InnoDB;

CREATE TABLE `email_verifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `credential_version` bigint NOT NULL,
  `email_hmac` char(64) NOT NULL,
  `code_hmac` char(64) NOT NULL,
  `code_enc` varchar(255) NOT NULL,
  `delivered` tinyint(1) NOT NULL DEFAULT '0',
  `verified` tinyint(1) NOT NULL DEFAULT '0',
  `expires_at` datetime(6) NOT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_email_verifications_user_email_id` (`user_id`,`email_hmac`,`id`),
  KEY `idx_email_verifications_expiry` (`expires_at`,`id`),
  CONSTRAINT `fk_email_verifications_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB;

CREATE TABLE `events` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(200) NOT NULL,
  `summary` varchar(500) NOT NULL,
  `content` text NOT NULL,
  `image_url` varchar(500) DEFAULT NULL,
  `start_at` datetime NOT NULL,
  `end_at` datetime NOT NULL,
  `published` tinyint(1) NOT NULL DEFAULT '0',
  `featured` tinyint(1) NOT NULL DEFAULT '0',
  `coupon_definition_id` bigint DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_events_public` (`published`,`end_at`,`start_at`),
  KEY `idx_events_featured` (`published`,`featured`,`end_at`,`start_at`),
  KEY `idx_events_coupon_definition` (`coupon_definition_id`),
  CONSTRAINT `fk_events_coupon_definition` FOREIGN KEY (`coupon_definition_id`) REFERENCES `coupon_definitions` (`id`),
  CONSTRAINT `chk_events_period` CHECK ((`start_at` < `end_at`))
) ENGINE=InnoDB;

CREATE TABLE `event_products` (
  `event_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  PRIMARY KEY (`event_id`,`product_id`),
  KEY `idx_event_products_product` (`product_id`),
  CONSTRAINT `fk_event_products_event` FOREIGN KEY (`event_id`) REFERENCES `events` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_event_products_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE `payment_attempt` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id_external` varchar(64) NOT NULL COMMENT 'Toss에 넘기는 외부 주문 식별자 (UUID)',
  `context` varchar(20) NOT NULL COMMENT 'PaymentContext: ORDER | BOOKING | PASS',
  `amount` bigint NOT NULL COMMENT '원(KRW) 단위, prepare 시 확정',
  `status` varchar(30) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING | PROCESSING | RETRYABLE | APPROVED | CONFIRMED | FAILED | RECONCILIATION_REQUIRED | COMPENSATION_REQUESTED | COMPENSATION_FAILED | COMPENSATED | CANCELED',
  `processing_at` datetime(6) DEFAULT NULL,
  `processing_token` varchar(64) DEFAULT NULL COMMENT '현재 confirm 실행권 선점 토큰',
  `payment_key` varchar(200) DEFAULT NULL COMMENT 'Toss confirm 요청 paymentKey',
  `confirmed_payment_key` varchar(200) DEFAULT NULL COMMENT 'Toss confirm 승인 응답 paymentKey',
  `confirmed_payment_method` varchar(30) DEFAULT NULL,
  `confirmed_receipt_url` varchar(500) DEFAULT NULL,
  `fail_reason` varchar(500) DEFAULT NULL,
  `payload_enc` mediumtext COMMENT 'prepare 시점 context별 payload 암호문; 만료된 PENDING은 개인정보 제거를 위해 NULL',
  `fulfilled_domain_id` bigint DEFAULT NULL COMMENT 'CONFIRMED 결제로 생성된 context별 도메인 ID',
  `owner_user_id` bigint DEFAULT NULL COMMENT '결제 상태를 조회할 수 있는 회원 ID',
  `owner_phone_hmac` varchar(64) DEFAULT NULL COMMENT '비회원 결제 상태 복구용 정규화 휴대폰 HMAC',
  `owner_phone_hmac_key_id` varchar(32) DEFAULT NULL COMMENT '비회원 결제 휴대폰 HMAC 생성 키 ID',
  `status_access_token_hash` varchar(64) DEFAULT NULL COMMENT '비회원 결제 상태 조회 서명 토큰 전체의 SHA-256 해시',
  `fulfilled_access_token_enc` mediumtext COMMENT '게스트 조회용 raw access token 암호문',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `confirmed_at` datetime(6) DEFAULT NULL,
  `confirm_recovery_attempted_at` datetime(6) DEFAULT NULL COMMENT '마지막 confirm 자동 복구 시도 시각',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '낙관적 락',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_payment_attempt_order_id_external` (`order_id_external`),
  KEY `idx_payment_attempt_status_created` (`status`,`created_at`),
  KEY `idx_payment_attempt_confirm_recovery` (`status`,`confirm_recovery_attempted_at`,`created_at`),
  KEY `idx_payment_attempt_guest_recovery` (`owner_phone_hmac`,`id`),
  KEY `idx_payment_attempt_status_id_created` (`status`,`id`,`created_at`),
  KEY `idx_payment_attempt_receipt` (`context`,`fulfilled_domain_id`,`status`)
) ENGINE=InnoDB;

CREATE TABLE `issued_coupons` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `definition_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `status` varchar(20) NOT NULL,
  `payment_attempt_id` bigint DEFAULT NULL,
  `used_order_id` bigint DEFAULT NULL,
  `claimed_at` datetime(6) NOT NULL,
  `reserved_at` datetime(6) DEFAULT NULL,
  `used_at` datetime(6) DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_issued_coupons_user_definition` (`user_id`,`definition_id`),
  UNIQUE KEY `uq_issued_coupons_payment_attempt` (`payment_attempt_id`),
  UNIQUE KEY `uq_issued_coupons_used_order` (`used_order_id`),
  KEY `idx_issued_coupons_user_claimed` (`user_id`,`claimed_at` DESC,`id` DESC),
  KEY `idx_issued_coupons_definition` (`definition_id`),
  CONSTRAINT `fk_issued_coupon_definition` FOREIGN KEY (`definition_id`) REFERENCES `coupon_definitions` (`id`),
  CONSTRAINT `fk_issued_coupon_payment_attempt` FOREIGN KEY (`payment_attempt_id`) REFERENCES `payment_attempt` (`id`),
  CONSTRAINT `fk_issued_coupon_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `chk_issued_coupon_state` CHECK ((((`status` = 'AVAILABLE') and (`payment_attempt_id` is null) and (`used_order_id` is null) and (`reserved_at` is null) and (`used_at` is null)) or ((`status` = 'RESERVED') and (`payment_attempt_id` is not null) and (`used_order_id` is null) and (`reserved_at` is not null) and (`used_at` is null)) or ((`status` = 'REDEEMED') and (`payment_attempt_id` is not null) and (`used_order_id` is not null) and (`reserved_at` is not null) and (`used_at` is not null)) or ((`status` = 'EXPIRED') and (((`payment_attempt_id` is null) and (`used_order_id` is null) and (`reserved_at` is null) and (`used_at` is null)) or ((`payment_attempt_id` is not null) and (`used_order_id` is not null) and (`reserved_at` is not null) and (`used_at` is not null)))) or ((`status` = 'CANCELED') and (`payment_attempt_id` is null) and (`used_order_id` is null) and (`reserved_at` is null) and (`used_at` is null))))
) ENGINE=InnoDB;

CREATE TABLE `orders` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `guest_id` bigint DEFAULT NULL,
  `access_token` varchar(64) DEFAULT NULL,
  `status` varchar(30) NOT NULL COMMENT 'OrderStatus enum',
  `total_amount` bigint NOT NULL COMMENT '원(KRW) 단위',
  `product_amount` bigint NOT NULL DEFAULT '0',
  `shipping_fee` bigint NOT NULL DEFAULT '0',
  `coupon_discount_amount` bigint NOT NULL DEFAULT '0',
  `reward_used_amount` bigint NOT NULL DEFAULT '0',
  `pg_paid_amount` bigint NOT NULL DEFAULT '0',
  `reward_earn_base` bigint NOT NULL DEFAULT '0',
  `issued_coupon_id` bigint DEFAULT NULL,
  `paid_at` datetime(6) DEFAULT NULL,
  `approval_deadline_at` datetime(6) DEFAULT NULL COMMENT '결제 후 +24h, SLA 배치 기준',
  `made_to_order_consent_version` varchar(30) DEFAULT NULL,
  `made_to_order_consent_disclosure` varchar(1000) DEFAULT NULL,
  `made_to_order_consent_at` datetime(6) DEFAULT NULL,
  `bundle_id` bigint DEFAULT NULL COMMENT '번들 결제 그룹 ID',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `version` bigint NOT NULL DEFAULT '0' COMMENT '낙관적 락 버전',
  `payment_key` varchar(200) DEFAULT NULL COMMENT 'Toss paymentKey (결제 확정 시)',
  PRIMARY KEY (`id`),
  KEY `idx_orders_status_deadline` (`status`,`approval_deadline_at`),
  KEY `idx_orders_guest_created` (`guest_id`,`created_at` DESC),
  KEY `idx_orders_paid_at` (`paid_at`),
  KEY `idx_orders_created_id` (`created_at` DESC,`id` DESC),
  KEY `idx_orders_status_created_id` (`status`,`created_at` DESC,`id` DESC),
  KEY `idx_orders_access_token_created` (`access_token`,`created_at` DESC,`id` DESC),
  KEY `idx_orders_user_created_id` (`user_id`,`created_at` DESC,`id` DESC),
  KEY `fk_orders_issued_coupon` (`issued_coupon_id`),
  CONSTRAINT `fk_order_guest` FOREIGN KEY (`guest_id`) REFERENCES `guests` (`id`),
  CONSTRAINT `fk_order_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_orders_issued_coupon` FOREIGN KEY (`issued_coupon_id`) REFERENCES `issued_coupons` (`id`),
  CONSTRAINT `chk_orders_benefit_amounts` CHECK (((`product_amount` between 1 and 9007199254740991) and (`coupon_discount_amount` between 0 and `product_amount`) and (`reward_used_amount` between 0 and (`product_amount` - `coupon_discount_amount`)) and (`total_amount` = ((`product_amount` + `shipping_fee`) - `coupon_discount_amount`)) and (`pg_paid_amount` = (`total_amount` - `reward_used_amount`)) and (`reward_earn_base` = ((`product_amount` - `coupon_discount_amount`) - `reward_used_amount`)) and (((`issued_coupon_id` is null) and (`coupon_discount_amount` = 0)) or ((`issued_coupon_id` is not null) and (`coupon_discount_amount` > 0))))),
  CONSTRAINT `chk_orders_exactly_one_owner` CHECK ((((`user_id` is not null) and (`guest_id` is null)) or ((`user_id` is null) and (`guest_id` is not null)))),
  CONSTRAINT `chk_orders_shipping_fee` CHECK (((`shipping_fee` >= 0) and (`shipping_fee` <= `total_amount`)))
) ENGINE=InnoDB;

CREATE TABLE `fulfillments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `type` varchar(10) NOT NULL COMMENT 'FulfillmentType: SHIPPING | PICKUP',
  `expected_ship_date` date DEFAULT NULL,
  `pickup_deadline_at` datetime(6) DEFAULT NULL,
  `shipping_address_enc` varchar(4096) DEFAULT NULL COMMENT '주문 시점 배송지 JSON AES-GCM 암호문',
  `carrier` varchar(50) DEFAULT NULL COMMENT '택배사',
  `tracking_number` varchar(100) DEFAULT NULL COMMENT '운송장 번호',
  `carrier_code` varchar(30) DEFAULT NULL,
  `tracking_registration_status` varchar(20) DEFAULT NULL,
  `tracking_request_id` varchar(100) DEFAULT NULL,
  `tracking_registration_attempts` int NOT NULL DEFAULT '0',
  `tracking_next_attempt_at` datetime(6) DEFAULT NULL,
  `tracking_registration_started_at` datetime(6) DEFAULT NULL,
  `tracking_last_error` varchar(500) DEFAULT NULL,
  `tracking_status` varchar(30) DEFAULT NULL,
  `tracking_status_text` varchar(100) DEFAULT NULL,
  `tracking_updated_at` datetime(6) DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0' COMMENT '낙관적 락 버전',
  `tracking_checked_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_fulfillments_order_id` (`order_id`),
  KEY `idx_fulfillment_tracking_registration` (`tracking_registration_status`,`tracking_next_attempt_at`,`id`),
  CONSTRAINT `fk_fulfillment_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
  CONSTRAINT `chk_fulfillment_tracking_pair` CHECK ((((`type` = 'SHIPPING') and (((`carrier` is null) and (`tracking_number` is null)) or ((`carrier` is not null) and (`tracking_number` is not null)))) or ((`type` = 'PICKUP') and (`carrier` is null) and (`tracking_number` is null))))
) ENGINE=InnoDB;

CREATE TABLE `group_inquiries` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `source` varchar(20) NOT NULL,
  `status` varchar(20) NOT NULL,
  `details_enc` text NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `next_contact_on` date DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_group_inquiry_user` (`user_id`,`created_at`,`id`),
  KEY `idx_group_inquiry_status` (`status`,`created_at`,`id`),
  KEY `idx_group_inquiry_created` (`created_at`,`id`),
  KEY `idx_group_inquiries_next_contact` (`next_contact_on`,`id`,`status`),
  CONSTRAINT `fk_group_inquiry_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `chk_group_inquiry_source` CHECK ((`source` in ('WEBSITE','EXTERNAL'))),
  CONSTRAINT `chk_group_inquiry_status` CHECK ((`status` in ('RECEIVED','CONSULTING','CONFIRMED','CLOSED','CANCELED')))
) ENGINE=InnoDB;

CREATE TABLE `group_inquiry_activities` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `inquiry_id` bigint NOT NULL,
  `admin_id` bigint DEFAULT NULL,
  `from_status` varchar(20) DEFAULT NULL,
  `to_status` varchar(20) NOT NULL,
  `note_enc` text NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `member_action` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_group_activity_inquiry` (`inquiry_id`,`id`),
  CONSTRAINT `fk_group_activity_inquiry` FOREIGN KEY (`inquiry_id`) REFERENCES `group_inquiries` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `image_media_reference_lock` (
  `id` tinyint NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `chk_image_media_reference_lock_singleton` CHECK ((`id` = 1))
) ENGINE=InnoDB;

CREATE TABLE `inquiry` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `title` varchar(200) NOT NULL,
  `content` text NOT NULL,
  `reply_content` text,
  `replied_at` datetime(6) DEFAULT NULL,
  `replied_by` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_inquiry_user_created_id` (`user_id`,`created_at` DESC,`id` DESC),
  KEY `idx_inquiry_created_id` (`created_at` DESC,`id` DESC),
  CONSTRAINT `fk_inquiry_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB;

CREATE TABLE `inventory` (
  `product_id` bigint NOT NULL,
  `quantity` int NOT NULL DEFAULT '1',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '낙관적 락',
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `minimum_stock` int DEFAULT NULL,
  PRIMARY KEY (`product_id`),
  CONSTRAINT `fk_inventory_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  CONSTRAINT `chk_inventory_quantity_nonnegative` CHECK ((`quantity` >= 0)),
  CONSTRAINT `ck_inventory_minimum_stock` CHECK (((`minimum_stock` is null) or (`minimum_stock` >= 0)))
) ENGINE=InnoDB;

CREATE TABLE `inventory_adjustments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `product_variant_id` bigint DEFAULT NULL,
  `type` varchar(10) NOT NULL COMMENT 'INCREASE | DECREASE',
  `quantity` int NOT NULL,
  `quantity_before` int NOT NULL,
  `quantity_after` int NOT NULL,
  `reason` varchar(500) NOT NULL,
  `adjusted_by_admin_id` bigint DEFAULT NULL,
  `adjusted_by` varchar(100) NOT NULL,
  `adjusted_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_inventory_adjustment_product_time` (`product_id`,`adjusted_at`,`id`),
  KEY `idx_inventory_adjustments_product_variant` (`product_variant_id`,`adjusted_at` DESC,`id` DESC),
  CONSTRAINT `fk_inventory_adjustment_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  CONSTRAINT `fk_inventory_adjustment_product_variant` FOREIGN KEY (`product_variant_id`) REFERENCES `product_variants` (`id`),
  CONSTRAINT `chk_inventory_adjustment_delta` CHECK ((((`type` = 'INCREASE') and (`quantity_after` = (`quantity_before` + `quantity`))) or ((`type` = 'DECREASE') and (`quantity_after` = (`quantity_before` - `quantity`))))),
  CONSTRAINT `chk_inventory_adjustment_quantity` CHECK ((`quantity` > 0)),
  CONSTRAINT `chk_inventory_adjustment_quantity_after` CHECK ((`quantity_after` >= 0)),
  CONSTRAINT `chk_inventory_adjustment_quantity_before` CHECK ((`quantity_before` >= 0)),
  CONSTRAINT `chk_inventory_adjustment_reason` CHECK ((char_length(trim(`reason`)) > 0))
) ENGINE=InnoDB;

CREATE TABLE `member_favorites` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `product_id` bigint DEFAULT NULL,
  `class_id` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_favorite_product` (`user_id`,`product_id`),
  UNIQUE KEY `uk_favorite_class` (`user_id`,`class_id`),
  KEY `fk_favorite_product` (`product_id`),
  KEY `fk_favorite_class` (`class_id`),
  KEY `idx_favorite_user_created` (`user_id`,`created_at`,`id`),
  CONSTRAINT `fk_favorite_class` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_favorite_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_favorite_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `chk_favorite_target` CHECK ((((`product_id` is not null) + (`class_id` is not null)) = 1))
) ENGINE=InnoDB;

CREATE TABLE `notices` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(200) NOT NULL,
  `content` text NOT NULL,
  `pinned` tinyint(1) NOT NULL DEFAULT '0',
  `view_count` int NOT NULL DEFAULT '0',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '낙관적 락 버전',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_notices_pinned_created` (`pinned` DESC,`created_at` DESC)
) ENGINE=InnoDB;

CREATE TABLE `notification_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `guest_id` bigint DEFAULT NULL,
  `channel` varchar(10) NOT NULL COMMENT 'KAKAO | SMS | EMAIL | PUSH',
  `event_type` varchar(50) NOT NULL COMMENT '예: BOOKING_CONFIRMED, DEPOSIT_REFUNDED, REMINDER_D1',
  `status` varchar(10) NOT NULL COMMENT 'SUCCESS | FAILED',
  `fail_reason` varchar(500) DEFAULT NULL,
  `provider_request_id` varchar(100) DEFAULT NULL,
  `provider_recipient_seq` bigint DEFAULT NULL,
  `sent_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notification_log_provider_delivery` (`channel`,`provider_request_id`,`provider_recipient_seq`),
  KEY `idx_noti_log_guest_event` (`guest_id`,`event_type`,`status`,`sent_at`),
  KEY `idx_noti_log_user_event` (`user_id`,`event_type`,`status`,`sent_at`),
  KEY `idx_notification_log_retention` (`sent_at`,`id`),
  CONSTRAINT `chk_notification_log_exactly_one_recipient` CHECK ((((`guest_id` is not null) and (`user_id` is null)) or ((`guest_id` is null) and (`user_id` is not null))))
) ENGINE=InnoDB;

CREATE TABLE `notification_outbox` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `recipient_type` varchar(10) NOT NULL COMMENT 'GUEST | USER',
  `guest_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `event_type` varchar(50) NOT NULL,
  `aggregate_type` varchar(40) DEFAULT NULL,
  `aggregate_id` bigint DEFAULT NULL,
  `idempotency_key` varchar(200) NOT NULL,
  `status` varchar(20) NOT NULL COMMENT 'PENDING | PROCESSING | DELIVERY_PENDING | DELIVERY_CHECKING | SENT | OBSOLETE | FAILED',
  `attempt_count` int NOT NULL DEFAULT '0',
  `next_attempt_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `locked_at` datetime(6) DEFAULT NULL,
  `processing_token` varchar(64) DEFAULT NULL,
  `processed_at` datetime(6) DEFAULT NULL,
  `read_at` datetime(6) DEFAULT NULL,
  `last_error` varchar(500) DEFAULT NULL,
  `provider_channel` varchar(10) DEFAULT NULL,
  `provider_request_id` varchar(100) DEFAULT NULL,
  `provider_recipient_seq` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `version` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notification_outbox_idempotency` (`idempotency_key`),
  KEY `idx_notification_outbox_dispatch` (`status`,`next_attempt_at`,`created_at`),
  KEY `idx_notification_outbox_processing` (`status`,`locked_at`),
  KEY `idx_notification_outbox_user_inbox` (`user_id`,`status`,`processed_at` DESC,`id` DESC),
  KEY `idx_notification_outbox_guest_inbox` (`guest_id`,`status`,`processed_at` DESC,`id` DESC),
  KEY `idx_notification_outbox_retention` (`status`,`processed_at`,`id`),
  KEY `idx_notification_outbox_event_aggregate` (`event_type`,`aggregate_type`,`aggregate_id`),
  KEY `idx_notification_outbox_delivery_result` (`status`,`next_attempt_at`,`locked_at`,`id`),
  CONSTRAINT `chk_notification_outbox_recipient` CHECK ((((`recipient_type` = 'GUEST') and (`guest_id` is not null) and (`user_id` is null)) or ((`recipient_type` = 'USER') and (`user_id` is not null) and (`guest_id` is null))))
) ENGINE=InnoDB;

CREATE TABLE `order_approvals` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `decided_by_admin_id` bigint DEFAULT NULL,
  `decision` varchar(20) NOT NULL COMMENT 'APPROVE | REJECT | DELAY | AUTO_REFUND | PRODUCTION_COMPLETE | RESUME_PRODUCTION',
  `reason` varchar(500) DEFAULT NULL,
  `decided_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `fk_approval_order` (`order_id`),
  CONSTRAINT `fk_approval_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
) ENGINE=InnoDB;

CREATE TABLE `order_claims` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `claim_type` varchar(30) NOT NULL,
  `requested_resolution` varchar(30) NOT NULL,
  `status` varchar(30) NOT NULL,
  `customer_reason` varchar(1000) NOT NULL,
  `admin_note` varchar(1000) DEFAULT NULL,
  `resolved_by_admin_id` bigint DEFAULT NULL,
  `completed_by_admin_id` bigint DEFAULT NULL,
  `replacement_carrier` varchar(100) DEFAULT NULL,
  `replacement_tracking_number` varchar(100) DEFAULT NULL,
  `requested_at` datetime(6) NOT NULL,
  `resolved_at` datetime(6) DEFAULT NULL,
  `completed_at` datetime(6) DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_order_claim_id_order` (`id`,`order_id`),
  KEY `idx_order_claims_status_requested` (`status`,`requested_at` DESC,`id` DESC),
  KEY `idx_order_claims_order` (`order_id`,`requested_at` DESC,`id` DESC),
  KEY `idx_order_claims_requested` (`requested_at` DESC,`id` DESC),
  CONSTRAINT `fk_order_claim_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
) ENGINE=InnoDB;

CREATE TABLE `order_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `product_variant_id` bigint DEFAULT NULL,
  `product_name` varchar(255) NOT NULL COMMENT '결제 준비 시점 상품명 스냅샷',
  `product_type` varchar(20) NOT NULL COMMENT '결제 준비 시점 상품 유형 스냅샷',
  `specification` varchar(2000) DEFAULT NULL COMMENT '결제 준비 시점 상품 고정 사양 스냅샷',
  `care_instructions` varchar(2000) DEFAULT NULL COMMENT '결제 준비 시점 관리 방법 스냅샷',
  `production_lead_days` int DEFAULT NULL COMMENT '결제 준비 시점 제작 기간(일) 스냅샷',
  `qty` int NOT NULL DEFAULT '1',
  `unit_price` bigint NOT NULL,
  `base_price` bigint NOT NULL,
  `variant_price_adjustment` bigint NOT NULL DEFAULT '0',
  `text_option_price_adjustment` bigint NOT NULL DEFAULT '0',
  `gross_amount` bigint NOT NULL DEFAULT '0',
  `coupon_discount_amount` bigint NOT NULL DEFAULT '0',
  `reward_used_amount` bigint NOT NULL DEFAULT '0',
  `net_paid_amount` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_order_items_id_order` (`id`,`order_id`),
  UNIQUE KEY `uq_order_items_review_source` (`id`,`product_id`),
  KEY `fk_item_product` (`product_id`),
  KEY `idx_order_items_order_product` (`order_id`,`product_id`,`unit_price`,`qty`),
  KEY `idx_order_items_product_variant` (`product_variant_id`),
  CONSTRAINT `fk_item_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
  CONSTRAINT `fk_item_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  CONSTRAINT `fk_order_item_product_variant` FOREIGN KEY (`product_variant_id`) REFERENCES `product_variants` (`id`),
  CONSTRAINT `chk_order_item_option_price_parts` CHECK (((`base_price` >= 1) and (`text_option_price_adjustment` >= 0) and (`unit_price` = ((`base_price` + `variant_price_adjustment`) + `text_option_price_adjustment`)))),
  CONSTRAINT `chk_order_items_benefit_amounts` CHECK (((`gross_amount` between 1 and 9007199254740991) and (`coupon_discount_amount` between 0 and `gross_amount`) and (`reward_used_amount` between 0 and (`gross_amount` - `coupon_discount_amount`)) and (`net_paid_amount` = ((`gross_amount` - `coupon_discount_amount`) - `reward_used_amount`)))),
  CONSTRAINT `chk_order_items_price_arithmetic` CHECK (((`qty` between 1 and 99) and (`unit_price` between 1 and 9007199254740991) and (`gross_amount` = (`qty` * `unit_price`)))),
  CONSTRAINT `chk_order_items_purchase_terms` CHECK ((((`product_type` = 'MADE_TO_ORDER') and (`specification` is not null) and (char_length(trim(`specification`)) > 0) and (`production_lead_days` is not null) and (`production_lead_days` between 1 and 180)) or ((`product_type` = 'READY_STOCK') and (`production_lead_days` is null))))
) ENGINE=InnoDB;

CREATE TABLE `order_claim_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `claim_id` bigint NOT NULL,
  `order_id` bigint NOT NULL,
  `order_item_id` bigint NOT NULL,
  `quantity` int NOT NULL,
  `approved_refund_amount` bigint DEFAULT NULL,
  `approved_reward_restore_amount` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_order_claim_item` (`claim_id`,`order_item_id`),
  KEY `fk_order_claim_item_claim_order` (`claim_id`,`order_id`),
  KEY `fk_order_claim_item_item_order` (`order_item_id`,`order_id`),
  KEY `idx_order_claim_items_order_item` (`order_item_id`),
  CONSTRAINT `fk_order_claim_item_claim_order` FOREIGN KEY (`claim_id`, `order_id`) REFERENCES `order_claims` (`id`, `order_id`),
  CONSTRAINT `fk_order_claim_item_item_order` FOREIGN KEY (`order_item_id`, `order_id`) REFERENCES `order_items` (`id`, `order_id`),
  CONSTRAINT `chk_order_claim_item_quantity` CHECK ((`quantity` > 0)),
  CONSTRAINT `chk_order_claim_item_refund_amount` CHECK (((`approved_refund_amount` is null) or (`approved_refund_amount` >= 0))),
  CONSTRAINT `chk_order_claim_item_reward_restore_amount` CHECK ((((`approved_refund_amount` is null) and (`approved_reward_restore_amount` is null)) or ((`approved_refund_amount` is not null) and (`approved_reward_restore_amount` is not null) and (`approved_reward_restore_amount` between 0 and `approved_refund_amount`))))
) ENGINE=InnoDB;

CREATE TABLE `order_item_option_snapshots` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_item_id` bigint NOT NULL,
  `option_type` varchar(10) NOT NULL COMMENT 'SELECT | TEXT',
  `group_name` varchar(25) NOT NULL,
  `value` varchar(200) NOT NULL,
  `price_adjustment` bigint NOT NULL DEFAULT '0',
  `sort_order` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_order_item_option_snapshot_order` (`order_item_id`,`sort_order`),
  CONSTRAINT `fk_order_item_option_snapshot_item` FOREIGN KEY (`order_item_id`) REFERENCES `order_items` (`id`) ON DELETE CASCADE,
  CONSTRAINT `chk_order_item_option_snapshot_price` CHECK ((`price_adjustment` >= 0)),
  CONSTRAINT `chk_order_item_option_snapshot_sort` CHECK ((`sort_order` >= 0))
) ENGINE=InnoDB;

CREATE TABLE `pass_ledger` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `pass_purchase_id` bigint NOT NULL,
  `type` varchar(10) NOT NULL COMMENT 'PassLedgerType: EARN | USE | REFUND | EXPIRE',
  `amount` int NOT NULL,
  `related_booking_id` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_pass_ledger_booking_type` (`related_booking_id`,`type`),
  KEY `fk_ledger_pass` (`pass_purchase_id`),
  CONSTRAINT `fk_ledger_booking` FOREIGN KEY (`related_booking_id`) REFERENCES `bookings` (`id`),
  CONSTRAINT `fk_ledger_pass` FOREIGN KEY (`pass_purchase_id`) REFERENCES `pass_purchases` (`id`)
) ENGINE=InnoDB;

CREATE TABLE `payment_settlements` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `transaction_key` varchar(64) NOT NULL,
  `payment_key` varchar(200) NOT NULL,
  `order_id_external` varchar(64) DEFAULT NULL,
  `payment_method` varchar(30) DEFAULT NULL,
  `amount` bigint NOT NULL,
  `fee_amount` bigint NOT NULL,
  `supply_amount` bigint NOT NULL,
  `vat` bigint NOT NULL,
  `pay_out_amount` bigint NOT NULL,
  `approved_at` varchar(40) DEFAULT NULL,
  `sold_date` date NOT NULL,
  `paid_out_date` date DEFAULT NULL,
  `cancel_transaction` tinyint(1) NOT NULL,
  `reconciliation_status` varchar(30) NOT NULL,
  `reconciliation_reason` varchar(500) DEFAULT NULL,
  `fetched_at` datetime(6) NOT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_settlement_transaction_key` (`transaction_key`),
  KEY `idx_payment_settlement_issue` (`reconciliation_status`,`sold_date`,`id`)
) ENGINE=InnoDB;

CREATE TABLE `payment_webhook_receipts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `transmission_id` varchar(100) NOT NULL,
  `payment_attempt_id` bigint NOT NULL,
  `event_type` varchar(50) NOT NULL,
  `received_at` datetime(6) NOT NULL,
  `processing_at` datetime(6) DEFAULT NULL,
  `processed_at` datetime(6) DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_payment_webhook_receipts_transmission` (`transmission_id`),
  KEY `fk_payment_webhook_receipts_attempt` (`payment_attempt_id`),
  KEY `idx_payment_webhook_receipts_pending` (`processed_at`,`processing_at`,`id`),
  CONSTRAINT `fk_payment_webhook_receipts_attempt` FOREIGN KEY (`payment_attempt_id`) REFERENCES `payment_attempt` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE `phone_verifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `phone_hmac` char(64) NOT NULL,
  `purpose` varchar(40) NOT NULL,
  `code_hmac` char(64) NOT NULL,
  `code_enc` varchar(255) NOT NULL,
  `delivered` tinyint(1) NOT NULL DEFAULT '0',
  `verified` tinyint(1) NOT NULL DEFAULT '0',
  `expires_at` datetime(6) NOT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_pv_phone_created` (`phone_hmac`,`id`),
  KEY `idx_pv_expires_id` (`expires_at`,`id`),
  KEY `idx_phone_verifications_phone_purpose_id` (`phone_hmac`,`purpose`,`id`)
) ENGINE=InnoDB;

CREATE TABLE `policy_consents` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `payment_attempt_id` bigint DEFAULT NULL,
  `consent_type` varchar(30) NOT NULL,
  `purpose` varchar(40) NOT NULL,
  `policy_version` varchar(40) NOT NULL,
  `accepted_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_policy_consents_user` (`user_id`,`accepted_at` DESC,`id` DESC),
  KEY `idx_policy_consents_payment_attempt` (`payment_attempt_id`,`accepted_at` DESC,`id` DESC),
  CONSTRAINT `fk_policy_consent_payment_attempt` FOREIGN KEY (`payment_attempt_id`) REFERENCES `payment_attempt` (`id`),
  CONSTRAINT `fk_policy_consent_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `chk_policy_consent_subject` CHECK ((((`user_id` is not null) and (`payment_attempt_id` is null)) or ((`user_id` is null) and (`payment_attempt_id` is not null))))
) ENGINE=InnoDB;

CREATE TABLE `product_option_values` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `group_id` bigint NOT NULL,
  `option_key` varchar(64) NOT NULL,
  `name` varchar(25) NOT NULL,
  `sort_order` int NOT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_product_option_value_key` (`group_id`,`option_key`),
  KEY `idx_product_option_values_group` (`group_id`,`active`,`sort_order`,`id`),
  CONSTRAINT `fk_product_option_value_group` FOREIGN KEY (`group_id`) REFERENCES `product_option_groups` (`id`),
  CONSTRAINT `chk_product_option_value_sort` CHECK ((`sort_order` >= 0))
) ENGINE=InnoDB;

CREATE TABLE `product_qna` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `title` varchar(200) NOT NULL,
  `content` text NOT NULL,
  `secret` tinyint(1) NOT NULL DEFAULT '0',
  `reply_content` text,
  `replied_at` datetime(6) DEFAULT NULL,
  `replied_by` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_product_qna_user` (`user_id`),
  KEY `idx_product_qna_unanswered_created` (`replied_at`,`created_at` DESC,`id` DESC),
  KEY `idx_product_qna_product_created_id` (`product_id`,`created_at` DESC,`id` DESC),
  KEY `idx_product_qna_product_user_created_id` (`product_id`,`user_id`,`created_at` DESC,`id` DESC),
  CONSTRAINT `fk_product_qna_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  CONSTRAINT `fk_product_qna_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB;

CREATE TABLE `product_restock_alerts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `product_variant_id` bigint DEFAULT NULL,
  `option_label` varchar(1000) NOT NULL,
  `status` varchar(20) NOT NULL,
  `active_key` varchar(100) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `notified_at` datetime(6) DEFAULT NULL,
  `canceled_at` datetime(6) DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_restock_active` (`active_key`),
  KEY `fk_restock_product` (`product_id`),
  KEY `fk_restock_variant_product` (`product_variant_id`,`product_id`),
  KEY `idx_restock_user` (`user_id`,`id`),
  KEY `idx_restock_status` (`status`,`id`),
  CONSTRAINT `fk_restock_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_restock_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_restock_variant_product` FOREIGN KEY (`product_variant_id`, `product_id`) REFERENCES `product_variants` (`id`, `product_id`) ON DELETE CASCADE,
  CONSTRAINT `ck_restock_status` CHECK ((`status` in ('WAITING','QUEUED','NOTIFIED','CANCELED')))
) ENGINE=InnoDB;

CREATE TABLE `product_variant_selections` (
  `variant_id` bigint NOT NULL,
  `option_group_id` bigint NOT NULL,
  `option_value_id` bigint NOT NULL,
  `sort_order` int NOT NULL,
  PRIMARY KEY (`variant_id`,`option_group_id`),
  KEY `fk_product_variant_selection_group` (`option_group_id`),
  KEY `fk_product_variant_selection_value` (`option_value_id`),
  CONSTRAINT `fk_product_variant_selection_group` FOREIGN KEY (`option_group_id`) REFERENCES `product_option_groups` (`id`),
  CONSTRAINT `fk_product_variant_selection_value` FOREIGN KEY (`option_value_id`) REFERENCES `product_option_values` (`id`),
  CONSTRAINT `fk_product_variant_selection_variant` FOREIGN KEY (`variant_id`) REFERENCES `product_variants` (`id`),
  CONSTRAINT `chk_product_variant_selection_sort` CHECK ((`sort_order` >= 0))
) ENGINE=InnoDB;

CREATE TABLE `public_holiday_snapshot` (
  `holiday_date` date NOT NULL,
  `name` varchar(100) NOT NULL,
  `synced_at` datetime(6) NOT NULL,
  PRIMARY KEY (`holiday_date`)
) ENGINE=InnoDB;

CREATE TABLE `refunds` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint DEFAULT NULL COMMENT '주문 환불',
  `order_claim_id` bigint DEFAULT NULL,
  `booking_id` bigint DEFAULT NULL COMMENT '예약금 환불',
  `pass_purchase_id` bigint DEFAULT NULL COMMENT '8회권 환불',
  `payment_attempt_id` bigint DEFAULT NULL COMMENT '도메인 생성 실패 결제의 보상 환불',
  `amount` bigint NOT NULL,
  `customer_refund_amount` bigint NOT NULL DEFAULT '0',
  `reward_restore_amount` bigint NOT NULL DEFAULT '0',
  `reward_revoke_amount` bigint NOT NULL DEFAULT '0',
  `restore_coupon` tinyint(1) NOT NULL DEFAULT '0',
  `status` varchar(30) NOT NULL COMMENT 'REQUESTED | PROCESSING | RETRYABLE | RECONCILIATION_REQUIRED | SUCCEEDED | FAILED',
  `processing_at` datetime(6) DEFAULT NULL,
  `processing_token` varchar(64) DEFAULT NULL,
  `attempt_count` int NOT NULL DEFAULT '0',
  `next_attempt_at` datetime(6) DEFAULT NULL,
  `last_recovery_at` datetime(6) DEFAULT NULL,
  `payment_key` varchar(255) DEFAULT NULL COMMENT 'Toss paymentKey (원결제 식별자)',
  `refund_transaction_key` varchar(255) DEFAULT NULL COMMENT 'Toss cancel transactionKey (환불 거래 식별자)',
  `succeeded_at` datetime(6) DEFAULT NULL,
  `idempotency_key` varchar(64) NOT NULL COMMENT 'PG 환불 요청 멱등키',
  `fail_reason` varchar(500) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `version` bigint NOT NULL DEFAULT '0',
  `direct_order_id` bigint GENERATED ALWAYS AS ((case when (`order_claim_id` is null) then `order_id` else NULL end)) STORED,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_refunds_idempotency_key` (`idempotency_key`),
  UNIQUE KEY `uq_refunds_pass_purchase` (`pass_purchase_id`),
  UNIQUE KEY `uq_refunds_payment_attempt` (`payment_attempt_id`),
  UNIQUE KEY `uq_refunds_direct_order` (`direct_order_id`),
  UNIQUE KEY `uq_refunds_order_claim` (`order_claim_id`),
  UNIQUE KEY `uq_refunds_refund_transaction_key` (`refund_transaction_key`),
  KEY `idx_refunds_status_created` (`status`,`created_at`),
  KEY `idx_refunds_status_next_attempt` (`status`,`next_attempt_at`,`created_at`),
  KEY `idx_refunds_status_processing` (`status`,`processing_at`,`created_at`),
  KEY `idx_refunds_status_succeeded_at` (`status`,`succeeded_at`),
  KEY `idx_refunds_order` (`order_id`),
  KEY `fk_refund_order_claim_source` (`order_claim_id`,`order_id`),
  KEY `idx_refunds_last_recovery` (`last_recovery_at`,`created_at`,`id`),
  KEY `idx_refunds_booking_created` (`booking_id`,`created_at` DESC,`id` DESC),
  CONSTRAINT `fk_refund_booking` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`),
  CONSTRAINT `fk_refund_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
  CONSTRAINT `fk_refund_order_claim_source` FOREIGN KEY (`order_claim_id`, `order_id`) REFERENCES `order_claims` (`id`, `order_id`),
  CONSTRAINT `fk_refund_pass_purchase` FOREIGN KEY (`pass_purchase_id`) REFERENCES `pass_purchases` (`id`),
  CONSTRAINT `fk_refund_payment_attempt` FOREIGN KEY (`payment_attempt_id`) REFERENCES `payment_attempt` (`id`),
  CONSTRAINT `chk_refund_order_claim_source` CHECK (((`order_claim_id` is null) or (`order_id` is not null))),
  CONSTRAINT `chk_refunds_exactly_one_source` CHECK ((((((`booking_id` is not null) + (`order_id` is not null)) + (`pass_purchase_id` is not null)) + (`payment_attempt_id` is not null)) = 1)),
  CONSTRAINT `chk_refunds_mixed_benefit_amounts` CHECK (((`amount` >= 0) and (`customer_refund_amount` >= 0) and (`reward_restore_amount` >= 0) and (`reward_revoke_amount` >= 0) and (`customer_refund_amount` = (`amount` + `reward_restore_amount`)) and ((`customer_refund_amount` > 0) or (`reward_revoke_amount` > 0) or (0 <> `restore_coupon`)) and ((`order_id` is not null) or ((`amount` > 0) and (`customer_refund_amount` = `amount`) and (`reward_restore_amount` = 0) and (`reward_revoke_amount` = 0) and (`restore_coupon` = false))) and ((`order_claim_id` is null) or (`restore_coupon` = false)) and ((`restore_coupon` = false) or ((`order_id` is not null) and (`order_claim_id` is null)))))
) ENGINE=InnoDB;

CREATE TABLE `reviews` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `order_item_id` bigint DEFAULT NULL,
  `product_id` bigint DEFAULT NULL,
  `booking_id` bigint DEFAULT NULL,
  `booking_class_id` bigint DEFAULT NULL,
  `rating` int DEFAULT NULL,
  `content` text,
  `status` varchar(10) NOT NULL DEFAULT 'PUBLISHED',
  `hidden_reason` varchar(500) DEFAULT NULL,
  `hidden_at` datetime(6) DEFAULT NULL,
  `hidden_by_admin_id` bigint DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `recreation_blocked` tinyint(1) NOT NULL DEFAULT '0',
  `edited_at` datetime(6) DEFAULT NULL,
  `content_revision` bigint NOT NULL DEFAULT '1',
  `reply_content` text,
  `reply_admin_id` bigint DEFAULT NULL,
  `reply_created_at` datetime(6) DEFAULT NULL,
  `reply_edited_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `version` bigint NOT NULL DEFAULT '0',
  `reserved_order_item_id` bigint GENERATED ALWAYS AS ((case when ((`deleted_at` is null) or (`recreation_blocked` = true)) then `order_item_id` else NULL end)) STORED,
  `reserved_booking_id` bigint GENERATED ALWAYS AS ((case when ((`deleted_at` is null) or (`recreation_blocked` = true)) then `booking_id` else NULL end)) STORED,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_reviews_reserved_order_item` (`reserved_order_item_id`),
  UNIQUE KEY `uq_reviews_reserved_booking` (`reserved_booking_id`),
  KEY `fk_reviews_hidden_admin` (`hidden_by_admin_id`),
  KEY `fk_reviews_reply_admin` (`reply_admin_id`),
  KEY `idx_reviews_order_item` (`order_item_id`,`product_id`),
  KEY `idx_reviews_booking` (`booking_id`,`booking_class_id`),
  KEY `idx_reviews_product_public` (`product_id`,`status`,`deleted_at`,`created_at`,`id`),
  KEY `idx_reviews_product_rating_public` (`product_id`,`status`,`deleted_at`,`rating`,`created_at`,`id`),
  KEY `idx_reviews_class_public` (`booking_class_id`,`status`,`deleted_at`,`created_at`,`id`),
  KEY `idx_reviews_class_rating_public` (`booking_class_id`,`status`,`deleted_at`,`rating`,`created_at`,`id`),
  KEY `idx_reviews_user_created` (`user_id`,`deleted_at`,`created_at`,`id`),
  KEY `idx_reviews_admin_status_created` (`deleted_at`,`status`,`created_at`,`id`),
  KEY `idx_reviews_created` (`deleted_at`,`created_at`,`id`),
  KEY `idx_reviews_tombstone_retention` (`recreation_blocked`,`deleted_at`,`id`),
  KEY `idx_reviews_product_rating_low_public` (`product_id`,`status`,`deleted_at`,`rating`,`created_at` DESC,`id` DESC),
  KEY `idx_reviews_class_rating_low_public` (`booking_class_id`,`status`,`deleted_at`,`rating`,`created_at` DESC,`id` DESC),
  CONSTRAINT `fk_reviews_booking_class` FOREIGN KEY (`booking_id`, `booking_class_id`) REFERENCES `bookings` (`id`, `class_id`),
  CONSTRAINT `fk_reviews_hidden_admin` FOREIGN KEY (`hidden_by_admin_id`) REFERENCES `admin_user` (`id`),
  CONSTRAINT `fk_reviews_order_item_product` FOREIGN KEY (`order_item_id`, `product_id`) REFERENCES `order_items` (`id`, `product_id`),
  CONSTRAINT `fk_reviews_reply_admin` FOREIGN KEY (`reply_admin_id`) REFERENCES `admin_user` (`id`),
  CONSTRAINT `fk_reviews_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `chk_reviews_content_revision` CHECK ((`content_revision` >= 1)),
  CONSTRAINT `chk_reviews_deleted_content` CHECK ((((`deleted_at` is null) and (`rating` is not null) and (`content` is not null)) or ((`deleted_at` is not null) and (`rating` is null) and (`content` is null)))),
  CONSTRAINT `chk_reviews_moderation` CHECK ((((`deleted_at` is not null) and (`hidden_reason` is null) and (`hidden_at` is null) and (`hidden_by_admin_id` is null)) or ((`deleted_at` is null) and (`status` = 'PUBLISHED') and (`hidden_reason` is null) and (`hidden_at` is null) and (`hidden_by_admin_id` is null)) or ((`deleted_at` is null) and (`status` = 'HIDDEN') and (`hidden_reason` is not null) and (`hidden_at` is not null) and (`hidden_by_admin_id` is not null)))),
  CONSTRAINT `chk_reviews_rating` CHECK ((`rating` between 1 and 5)),
  CONSTRAINT `chk_reviews_reply` CHECK ((((`reply_content` is null) and (`reply_admin_id` is null) and (`reply_created_at` is null) and (`reply_edited_at` is null)) or ((`reply_content` is not null) and (`reply_admin_id` is not null) and (`reply_created_at` is not null)))),
  CONSTRAINT `chk_reviews_source_pair` CHECK ((((`order_item_id` is not null) and (`product_id` is not null) and (`booking_id` is null) and (`booking_class_id` is null)) or ((`order_item_id` is null) and (`product_id` is null) and (`booking_id` is not null) and (`booking_class_id` is not null)))),
  CONSTRAINT `chk_reviews_status` CHECK ((`status` in ('PUBLISHED','HIDDEN')))
) ENGINE=InnoDB;

CREATE TABLE `review_evidence_snapshots` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `review_id` bigint NOT NULL,
  `content_revision` bigint NOT NULL,
  `rating` int NOT NULL,
  `content` text NOT NULL,
  `edited_at` datetime(6) DEFAULT NULL,
  `provenance` varchar(20) NOT NULL,
  `images_complete` tinyint(1) NOT NULL,
  `captured_at` datetime(6) NOT NULL,
  `retention_until` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_review_evidence_review_revision` (`review_id`,`content_revision`,`id`),
  KEY `idx_review_evidence_retention` (`retention_until`,`id`),
  CONSTRAINT `fk_review_evidence_review_restrict` FOREIGN KEY (`review_id`) REFERENCES `reviews` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_review_evidence_provenance` CHECK ((`provenance` in ('LIVE','LEGACY_REPORT'))),
  CONSTRAINT `chk_review_evidence_rating` CHECK ((`rating` between 1 and 5)),
  CONSTRAINT `chk_review_evidence_revision` CHECK ((`content_revision` >= 1))
) ENGINE=InnoDB;

CREATE TABLE `review_evidence_snapshot_images` (
  `snapshot_id` bigint NOT NULL,
  `sort_order` int NOT NULL,
  `image_url` varchar(512) NOT NULL,
  PRIMARY KEY (`snapshot_id`,`sort_order`),
  KEY `idx_review_evidence_image_url` (`image_url`),
  CONSTRAINT `fk_review_evidence_image_snapshot` FOREIGN KEY (`snapshot_id`) REFERENCES `review_evidence_snapshots` (`id`) ON DELETE CASCADE,
  CONSTRAINT `chk_review_evidence_image_order` CHECK ((`sort_order` between 0 and 4))
) ENGINE=InnoDB;

CREATE TABLE `review_helpful_votes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `review_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_review_helpful_review_user` (`review_id`,`user_id`),
  KEY `idx_review_helpful_user_review` (`user_id`,`review_id`),
  CONSTRAINT `fk_review_helpful_review` FOREIGN KEY (`review_id`) REFERENCES `reviews` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_review_helpful_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB;

CREATE TABLE `review_images` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `review_id` bigint NOT NULL,
  `image_url` varchar(512) NOT NULL,
  `sort_order` int NOT NULL,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_review_images_order` (`review_id`,`sort_order`),
  UNIQUE KEY `uq_review_images_url` (`image_url`),
  KEY `idx_review_images_review` (`review_id`,`sort_order`,`id`),
  CONSTRAINT `fk_review_images_review` FOREIGN KEY (`review_id`) REFERENCES `reviews` (`id`) ON DELETE CASCADE,
  CONSTRAINT `chk_review_images_order` CHECK ((`sort_order` between 0 and 4))
) ENGINE=InnoDB;

CREATE TABLE `review_moderation_actions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `review_id` bigint NOT NULL,
  `action` varchar(15) NOT NULL,
  `previous_status` varchar(10) NOT NULL,
  `new_status` varchar(10) NOT NULL,
  `reason` varchar(500) DEFAULT NULL,
  `admin_user_id` bigint NOT NULL,
  `evidence_snapshot_id` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_review_moderation_admin` (`admin_user_id`),
  KEY `idx_review_moderation_review_created` (`review_id`,`created_at`,`id`),
  KEY `fk_review_moderation_evidence` (`evidence_snapshot_id`),
  KEY `idx_review_moderation_created` (`created_at`,`id`),
  CONSTRAINT `fk_review_moderation_admin` FOREIGN KEY (`admin_user_id`) REFERENCES `admin_user` (`id`),
  CONSTRAINT `fk_review_moderation_evidence` FOREIGN KEY (`evidence_snapshot_id`) REFERENCES `review_evidence_snapshots` (`id`),
  CONSTRAINT `fk_review_moderation_review_restrict` FOREIGN KEY (`review_id`) REFERENCES `reviews` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_review_moderation_action` CHECK ((((`action` = 'HIDE') and (`previous_status` = 'PUBLISHED') and (`new_status` = 'HIDDEN') and (`reason` is not null)) or ((`action` = 'REPUBLISH') and (`previous_status` = 'HIDDEN') and (`new_status` = 'PUBLISHED') and (`reason` is null))))
) ENGINE=InnoDB;

CREATE TABLE `review_reports` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `review_id` bigint NOT NULL,
  `reporter_user_id` bigint NOT NULL,
  `reason` varchar(30) NOT NULL,
  `detail` varchar(1000) DEFAULT NULL,
  `snapshot_status` varchar(10) NOT NULL,
  `evidence_snapshot_id` bigint NOT NULL,
  `status` varchar(10) NOT NULL DEFAULT 'PENDING',
  `decision_note` varchar(1000) DEFAULT NULL,
  `decided_by_admin_id` bigint DEFAULT NULL,
  `decided_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_review_reports_review_reporter` (`review_id`,`reporter_user_id`),
  KEY `fk_review_reports_reporter` (`reporter_user_id`),
  KEY `fk_review_reports_admin` (`decided_by_admin_id`),
  KEY `idx_review_reports_status_created` (`status`,`created_at`,`id`),
  KEY `idx_review_reports_created` (`created_at`,`id`),
  KEY `fk_review_reports_evidence` (`evidence_snapshot_id`),
  KEY `idx_review_reports_decided` (`decided_at`,`id`),
  CONSTRAINT `fk_review_reports_admin` FOREIGN KEY (`decided_by_admin_id`) REFERENCES `admin_user` (`id`),
  CONSTRAINT `fk_review_reports_evidence` FOREIGN KEY (`evidence_snapshot_id`) REFERENCES `review_evidence_snapshots` (`id`),
  CONSTRAINT `fk_review_reports_reporter` FOREIGN KEY (`reporter_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_review_reports_review_restrict` FOREIGN KEY (`review_id`) REFERENCES `reviews` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_review_report_decision` CHECK ((((`status` = 'PENDING') and (`decision_note` is null) and (`decided_by_admin_id` is null) and (`decided_at` is null)) or ((`status` in ('ACCEPTED','REJECTED')) and (`decided_by_admin_id` is not null) and (`decided_at` is not null)))),
  CONSTRAINT `chk_review_report_reason` CHECK ((`reason` in ('SPAM','ABUSIVE','PRIVACY','FALSE_INFORMATION','OTHER'))),
  CONSTRAINT `chk_review_report_snapshot_status` CHECK ((`snapshot_status` in ('PUBLISHED','HIDDEN'))),
  CONSTRAINT `chk_review_report_status` CHECK ((`status` in ('PENDING','ACCEPTED','REJECTED')))
) ENGINE=InnoDB;

CREATE TABLE `reward_accounts` (
  `user_id` bigint NOT NULL,
  `available_balance` bigint NOT NULL DEFAULT '0',
  `reserved_balance` bigint NOT NULL DEFAULT '0',
  `debt_balance` bigint NOT NULL DEFAULT '0',
  `version` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`user_id`),
  CONSTRAINT `fk_reward_account_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `chk_reward_account_available` CHECK ((`available_balance` >= 0)),
  CONSTRAINT `chk_reward_account_debt` CHECK ((`debt_balance` >= 0)),
  CONSTRAINT `chk_reward_account_reserved` CHECK ((`reserved_balance` >= 0))
) ENGINE=InnoDB;

CREATE TABLE `reward_ledger` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `type` varchar(20) NOT NULL,
  `amount` bigint NOT NULL,
  `available_after` bigint NOT NULL,
  `reserved_after` bigint NOT NULL,
  `debt_after` bigint NOT NULL,
  `payment_attempt_id` bigint DEFAULT NULL,
  `order_id` bigint DEFAULT NULL,
  `idempotency_key` varchar(160) NOT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_reward_ledger_idempotency` (`idempotency_key`),
  KEY `fk_reward_ledger_attempt` (`payment_attempt_id`),
  KEY `fk_reward_ledger_order` (`order_id`),
  KEY `idx_reward_ledger_user_created` (`user_id`,`created_at` DESC,`id` DESC),
  CONSTRAINT `fk_reward_ledger_attempt` FOREIGN KEY (`payment_attempt_id`) REFERENCES `payment_attempt` (`id`),
  CONSTRAINT `fk_reward_ledger_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
  CONSTRAINT `fk_reward_ledger_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `chk_reward_ledger_amount` CHECK ((`amount` > 0)),
  CONSTRAINT `chk_reward_ledger_available` CHECK ((`available_after` >= 0)),
  CONSTRAINT `chk_reward_ledger_debt` CHECK ((`debt_after` >= 0)),
  CONSTRAINT `chk_reward_ledger_reserved` CHECK ((`reserved_after` >= 0))
) ENGINE=InnoDB;

CREATE TABLE `reward_lots` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `source_order_id` bigint DEFAULT NULL,
  `earned_amount` bigint NOT NULL,
  `remaining_amount` bigint NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_reward_lot_spendable` (`user_id`,`expires_at`,`id`),
  KEY `idx_reward_lot_source_order` (`source_order_id`,`id`),
  CONSTRAINT `fk_reward_lot_order` FOREIGN KEY (`source_order_id`) REFERENCES `orders` (`id`),
  CONSTRAINT `fk_reward_lot_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `chk_reward_lot_earned_positive` CHECK ((`earned_amount` > 0)),
  CONSTRAINT `chk_reward_lot_remaining` CHECK (((`remaining_amount` >= 0) and (`remaining_amount` <= `earned_amount`)))
) ENGINE=InnoDB;

CREATE TABLE `reward_reservations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `payment_attempt_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `order_id` bigint DEFAULT NULL,
  `amount` bigint NOT NULL,
  `restored_amount` bigint NOT NULL DEFAULT '0',
  `status` varchar(20) NOT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `resolved_at` datetime(6) DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_reward_reservation_attempt` (`payment_attempt_id`),
  UNIQUE KEY `uq_reward_reservation_order` (`order_id`),
  KEY `idx_reward_reservation_user_status` (`user_id`,`status`,`id`),
  CONSTRAINT `fk_reward_reservation_attempt` FOREIGN KEY (`payment_attempt_id`) REFERENCES `payment_attempt` (`id`),
  CONSTRAINT `fk_reward_reservation_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
  CONSTRAINT `fk_reward_reservation_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `chk_reward_reservation_amount` CHECK ((`amount` > 0)),
  CONSTRAINT `chk_reward_reservation_restored` CHECK (((`restored_amount` >= 0) and (`restored_amount` <= `amount`))),
  CONSTRAINT `chk_reward_reservation_state` CHECK ((((`status` = 'RESERVED') and (`order_id` is null) and (`resolved_at` is null) and (`restored_amount` = 0)) or ((`status` = 'USED') and (`order_id` is not null) and (`resolved_at` is not null) and (`restored_amount` between 0 and `amount`)) or ((`status` = 'RELEASED') and (`order_id` is null) and (`resolved_at` is not null) and (`restored_amount` = 0))))
) ENGINE=InnoDB;

CREATE TABLE `reward_reservation_allocations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `reservation_id` bigint NOT NULL,
  `reward_lot_id` bigint NOT NULL,
  `amount` bigint NOT NULL,
  `restored_amount` bigint NOT NULL DEFAULT '0',
  `original_expiry` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_reward_reservation_lot` (`reservation_id`,`reward_lot_id`),
  KEY `fk_reward_allocation_lot` (`reward_lot_id`),
  CONSTRAINT `fk_reward_allocation_lot` FOREIGN KEY (`reward_lot_id`) REFERENCES `reward_lots` (`id`),
  CONSTRAINT `fk_reward_allocation_reservation` FOREIGN KEY (`reservation_id`) REFERENCES `reward_reservations` (`id`),
  CONSTRAINT `chk_reward_allocation_amount` CHECK ((`amount` > 0)),
  CONSTRAINT `chk_reward_allocation_restored` CHECK (((`restored_amount` >= 0) and (`restored_amount` <= `amount`)))
) ENGINE=InnoDB;

CREATE TABLE `shipment_tracking_events` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `occurred_at` datetime(6) NOT NULL,
  `status` varchar(30) NOT NULL,
  `status_text` varchar(100) NOT NULL,
  `location` varchar(200) DEFAULT NULL,
  `description` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_shipment_tracking_event_order_time` (`order_id`,`occurred_at`,`id`),
  CONSTRAINT `fk_shipment_tracking_event_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `shipping_address_changes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `guest_id` bigint DEFAULT NULL,
  `before_address_enc` varchar(4096) NOT NULL,
  `after_address_enc` varchar(4096) NOT NULL,
  `changed_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_address_change_user` (`user_id`),
  KEY `fk_address_change_guest` (`guest_id`),
  KEY `idx_address_change_order` (`order_id`,`changed_at`,`id`),
  CONSTRAINT `fk_address_change_guest` FOREIGN KEY (`guest_id`) REFERENCES `guests` (`id`),
  CONSTRAINT `fk_address_change_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_address_change_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_address_change_owner` CHECK (((`user_id` is not null) <> (`guest_id` is not null)))
) ENGINE=InnoDB;

CREATE TABLE `smartstore_inventory_mapping_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `action` varchar(20) NOT NULL,
  `previous_origin_product_no` bigint DEFAULT NULL,
  `next_origin_product_no` bigint DEFAULT NULL,
  `previous_enabled` tinyint(1) DEFAULT NULL,
  `next_enabled` tinyint(1) DEFAULT NULL,
  `previous_option_mappings` text,
  `next_option_mappings` text,
  `previous_mapping_version` bigint DEFAULT NULL,
  `next_mapping_version` bigint DEFAULT NULL,
  `previous_origin_confirmed` tinyint(1) NOT NULL,
  `changed_by_admin_id` bigint DEFAULT NULL,
  `changed_by` varchar(100) NOT NULL,
  `changed_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_smartstore_inventory_mapping_history_product` (`product_id`,`changed_at` DESC,`id` DESC),
  CONSTRAINT `fk_smartstore_inventory_mapping_history_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  CONSTRAINT `ck_smartstore_inventory_mapping_history_action` CHECK ((`action` in ('CREATED','UPDATED','ORIGIN_CHANGED','ENABLED','DISABLED','DELETED')))
) ENGINE=InnoDB;

CREATE TABLE `smartstore_order_action_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_order_id` varchar(30) NOT NULL,
  `action` varchar(40) NOT NULL,
  `status` varchar(20) NOT NULL,
  `request_summary` text,
  `result_code` varchar(100) DEFAULT NULL,
  `result_message` varchar(1000) DEFAULT NULL,
  `changed_by_admin_id` bigint DEFAULT NULL,
  `changed_by` varchar(100) NOT NULL,
  `requested_at` datetime(6) NOT NULL,
  `completed_at` datetime(6) DEFAULT NULL,
  `reconciliation_outcome` varchar(20) DEFAULT NULL,
  `reconciliation_note` varchar(500) DEFAULT NULL,
  `reconciled_by_admin_id` bigint DEFAULT NULL,
  `reconciled_by` varchar(100) DEFAULT NULL,
  `reconciled_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_smartstore_order_action_history_order` (`product_order_id`,`requested_at` DESC,`id` DESC),
  KEY `idx_smartstore_order_action_unresolved` (`reconciled_at`,`requested_at` DESC,`id` DESC),
  CONSTRAINT `ck_smartstore_order_action_history_action` CHECK ((`action` in ('INVENTORY_RESOLVED','ORDER_CONFIRMED','ORDER_DISPATCHED','ORDER_DELAYED','CANCEL_APPROVED','RETURN_APPROVED','RETURN_REJECTED','RETURN_HELD','RETURN_HOLD_RELEASED','RETURN_REQUESTED','EXCHANGE_DISPATCHED','EXCHANGE_COLLECTION_COMPLETED','EXCHANGE_REJECTED','EXCHANGE_HELD','EXCHANGE_HOLD_RELEASED','CANCEL_REQUESTED'))),
  CONSTRAINT `ck_smartstore_order_action_history_status` CHECK ((`status` in ('REQUESTED','SUCCEEDED','REJECTED','NOT_SENT','RESULT_UNKNOWN'))),
  CONSTRAINT `ck_smartstore_order_action_reconciliation` CHECK ((((`reconciliation_outcome` is null) and (`reconciliation_note` is null) and (`reconciled_by_admin_id` is null) and (`reconciled_by` is null) and (`reconciled_at` is null)) or ((`reconciliation_outcome` in ('APPLIED','NOT_APPLIED')) and (`reconciliation_note` is not null) and (`reconciled_by` is not null) and (`reconciled_at` is not null))))
) ENGINE=InnoDB;

CREATE TABLE `smartstore_order_mapping_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `product_variant_id` bigint DEFAULT NULL,
  `origin_product_no` bigint NOT NULL,
  `option_id` bigint DEFAULT NULL,
  `enabled` tinyint(1) NOT NULL,
  `closed_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_smartstore_order_mapping_history_variant` (`product_variant_id`),
  KEY `idx_smartstore_order_mapping_history_lookup` (`origin_product_no`,`option_id`,`closed_at`),
  KEY `idx_smartstore_order_mapping_history_product` (`product_id`,`origin_product_no`,`closed_at`),
  CONSTRAINT `fk_smartstore_order_mapping_history_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  CONSTRAINT `fk_smartstore_order_mapping_history_variant` FOREIGN KEY (`product_variant_id`) REFERENCES `product_variants` (`id`),
  CONSTRAINT `ck_smartstore_order_mapping_history_target` CHECK ((((`product_variant_id` is null) and (`option_id` is null)) or ((`product_variant_id` is not null) and (`option_id` is not null))))
) ENGINE=InnoDB;

CREATE TABLE `smartstore_order_sync_state` (
  `id` bigint NOT NULL,
  `last_changed_from` datetime(6) NOT NULL,
  `more_sequence` varchar(100) DEFAULT NULL,
  `processing_started_at` datetime(6) DEFAULT NULL,
  `integration_enabled` tinyint(1) DEFAULT NULL,
  `pending_activation_from` datetime(6) DEFAULT NULL,
  `row_version` bigint NOT NULL DEFAULT '0',
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  CONSTRAINT `ck_smartstore_order_sync_singleton` CHECK ((`id` = 1))
) ENGINE=InnoDB;

CREATE TABLE `smartstore_product_orders` (
  `product_order_id` varchar(30) NOT NULL,
  `order_id` varchar(30) NOT NULL,
  `origin_product_no` bigint NOT NULL,
  `item_no` bigint DEFAULT NULL,
  `product_id` bigint DEFAULT NULL,
  `product_variant_id` bigint DEFAULT NULL,
  `product_name` varchar(4000) NOT NULL,
  `product_option` varchar(4000) DEFAULT NULL,
  `delivery_info_enc` text,
  `product_order_status` varchar(40) NOT NULL,
  `place_order_status` varchar(20) DEFAULT NULL,
  `shipping_due_date` datetime(6) DEFAULT NULL,
  `expected_delivery_method` varchar(40) DEFAULT NULL,
  `delivery_company` varchar(40) DEFAULT NULL,
  `tracking_number` varchar(100) DEFAULT NULL,
  `unit_price` bigint DEFAULT NULL,
  `payment_amount` bigint DEFAULT NULL,
  `payment_commission` bigint DEFAULT NULL,
  `sale_commission` bigint DEFAULT NULL,
  `channel_commission` bigint DEFAULT NULL,
  `expected_settlement_amount` bigint DEFAULT NULL,
  `claim_type` varchar(40) DEFAULT NULL,
  `claim_status` varchar(40) DEFAULT NULL,
  `initial_quantity` int NOT NULL,
  `remain_quantity` int NOT NULL,
  `inventory_applied_quantity` int NOT NULL DEFAULT '0',
  `attention_reason` varchar(30) DEFAULT NULL,
  `last_changed_type` varchar(40) NOT NULL,
  `payment_date` datetime(6) DEFAULT NULL,
  `last_changed_at` datetime(6) NOT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `return_reviewed_remain_quantity` int DEFAULT NULL COMMENT '마지막 반품 검수 당시 잔여 주문 수량',
  `completed_return_quantity` int DEFAULT NULL,
  `reviewed_return_quantity` int NOT NULL DEFAULT '0',
  `restored_return_quantity` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`product_order_id`),
  KEY `idx_smartstore_order_recent` (`last_changed_at` DESC,`product_order_id`),
  KEY `idx_smartstore_order_attention` (`attention_reason`,`last_changed_at` DESC),
  CONSTRAINT `ck_smartstore_order_attention` CHECK (((`attention_reason` is null) or (`attention_reason` in ('MAPPING_REQUIRED','STOCK_SHORTAGE','RETURN_REVIEW','STATUS_REVIEW')))),
  CONSTRAINT `ck_smartstore_order_quantities` CHECK (((`initial_quantity` >= 0) and (`remain_quantity` >= 0) and (`inventory_applied_quantity` >= 0)))
) ENGINE=InnoDB;

CREATE TABLE `smartstore_settlement_entries` (
  `entry_key` varchar(255) NOT NULL,
  `product_order_id` varchar(30) DEFAULT NULL,
  `order_id` varchar(30) DEFAULT NULL,
  `product_order_type` varchar(50) NOT NULL,
  `settle_type` varchar(50) DEFAULT NULL,
  `product_name` varchar(4000) DEFAULT NULL,
  `pay_settle_amount` bigint NOT NULL,
  `total_pay_commission_amount` bigint DEFAULT NULL,
  `selling_interlock_commission_amount` bigint DEFAULT NULL,
  `benefit_settle_amount` bigint NOT NULL,
  `settle_expect_amount` bigint NOT NULL,
  `settle_basis_date` date DEFAULT NULL,
  `settle_expect_date` date DEFAULT NULL,
  `settle_complete_date` date DEFAULT NULL,
  `pay_date` date DEFAULT NULL,
  `reconciliation_status` varchar(30) NOT NULL,
  `reconciliation_reason` varchar(500) DEFAULT NULL,
  `fetched_at` datetime(6) NOT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`entry_key`),
  KEY `idx_smartstore_settlement_issue` (`reconciliation_status`,`fetched_at` DESC),
  CONSTRAINT `ck_smartstore_settlement_status` CHECK ((`reconciliation_status` in ('MATCHED','ORDER_NOT_FOUND','EXPECTED_AMOUNT_MISSING','AMOUNT_MISMATCH','NOT_APPLICABLE')))
) ENGINE=InnoDB;

CREATE TABLE `smartstore_settlement_sync_state` (
  `id` bigint NOT NULL,
  `next_pay_date` date NOT NULL,
  `processing_started_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `ck_smartstore_settlement_sync_singleton` CHECK ((`id` = 1))
) ENGINE=InnoDB;

CREATE TABLE `smartstore_stock_mappings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `product_variant_id` bigint DEFAULT NULL,
  `origin_product_no` bigint NOT NULL,
  `option_id` bigint DEFAULT NULL,
  `external_target_key` bigint GENERATED ALWAYS AS (coalesce(`option_id`,0)) STORED,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `retired` tinyint(1) NOT NULL DEFAULT '0',
  `internal_target_key` bigint GENERATED ALWAYS AS ((case when `retired` then NULL else coalesce(`product_variant_id`,0) end)) STORED,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_smartstore_stock_mapping_option` (`origin_product_no`,`external_target_key`),
  UNIQUE KEY `uk_smartstore_stock_mapping_product_variant` (`product_id`,`internal_target_key`),
  KEY `fk_smartstore_stock_mapping_variant` (`product_variant_id`),
  KEY `idx_smartstore_stock_mapping_product` (`product_id`,`enabled`),
  CONSTRAINT `fk_smartstore_stock_mapping_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  CONSTRAINT `fk_smartstore_stock_mapping_variant` FOREIGN KEY (`product_variant_id`) REFERENCES `product_variants` (`id`),
  CONSTRAINT `ck_smartstore_stock_mapping_target` CHECK ((((`product_variant_id` is null) and (`option_id` is null)) or ((`product_variant_id` is not null) and (`option_id` is not null))))
) ENGINE=InnoDB;

CREATE TABLE `smartstore_stock_syncs` (
  `product_id` bigint NOT NULL,
  `request_version` bigint NOT NULL,
  `status` varchar(20) NOT NULL,
  `attempt_count` int NOT NULL DEFAULT '0',
  `next_attempt_at` datetime(6) NOT NULL,
  `processing_started_at` datetime(6) DEFAULT NULL,
  `last_error` varchar(500) DEFAULT NULL,
  `synced_at` datetime(6) DEFAULT NULL,
  `row_version` bigint NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `generation` varchar(36) NOT NULL DEFAULT 'legacy',
  PRIMARY KEY (`product_id`),
  KEY `idx_smartstore_stock_sync_due` (`status`,`next_attempt_at`,`product_id`),
  KEY `idx_smartstore_stock_sync_reconciliation` (`status`,`synced_at`,`product_id`),
  CONSTRAINT `fk_smartstore_stock_sync_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  CONSTRAINT `ck_smartstore_stock_sync_status` CHECK ((`status` in ('PENDING','PROCESSING','SYNCED','FAILED')))
) ENGINE=InnoDB;

CREATE TABLE `user_social_accounts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `provider` varchar(20) NOT NULL,
  `provider_id_enc` varchar(1024) DEFAULT NULL COMMENT '소셜 provider ID AES-GCM 암호문; 기존 행은 다음 로그인에서 채움',
  `provider_id_hmac` char(64) NOT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_user_social_accounts_user_provider` (`user_id`,`provider`),
  UNIQUE KEY `uq_user_social_accounts_provider_identity` (`provider`,`provider_id_hmac`),
  CONSTRAINT `fk_user_social_accounts_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `chk_user_social_accounts_provider` CHECK ((`provider` in ('GOOGLE','NAVER','KAKAO')))
) ENGINE=InnoDB;

CREATE TABLE `workshop_profiles` (
  `id` bigint NOT NULL,
  `name` varchar(100) NOT NULL,
  `phone` varchar(30) DEFAULT NULL,
  `postal_code` varchar(20) DEFAULT NULL,
  `address_line1` varchar(200) DEFAULT NULL,
  `address_line2` varchar(200) DEFAULT NULL,
  `business_hours` varchar(1000) DEFAULT NULL,
  `map_url` varchar(500) DEFAULT NULL,
  `parking_info` varchar(1000) DEFAULT NULL,
  `business_registration_number` varchar(20) DEFAULT NULL,
  `representative_name` varchar(100) DEFAULT NULL,
  `email` varchar(254) DEFAULT NULL,
  `mail_order_registration_number` varchar(100) DEFAULT NULL,
  `introduction` varchar(2000) DEFAULT NULL,
  `kakao_talk_id` varchar(100) DEFAULT NULL,
  `naver_talk_url` varchar(500) DEFAULT NULL,
  `naver_blog_url` varchar(500) DEFAULT NULL,
  `instagram_url` varchar(500) DEFAULT NULL,
  `smart_store_url` varchar(500) DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `version` bigint NOT NULL DEFAULT '0' COMMENT '낙관적 락 버전',
  PRIMARY KEY (`id`),
  CONSTRAINT `chk_workshop_profiles_singleton` CHECK ((`id` = 1))
) ENGINE=InnoDB;

-- orders와 issued_coupons가 서로 참조하므로 두 테이블을 만든 뒤 연결한다.
ALTER TABLE `issued_coupons`
  ADD CONSTRAINT `fk_issued_coupon_used_order` FOREIGN KEY (`used_order_id`) REFERENCES `orders` (`id`);

-- 단일 행으로 유지하는 잠금·설정·공방 정보
INSERT INTO `admin_setup_lock` (`id`) VALUES (1);
INSERT INTO `data_key_rotation_lock` (`id`) VALUES (1);
INSERT INTO `image_media_reference_lock` (`id`) VALUES (1);
INSERT INTO `booking_calendar_settings` (`id`, `open_time`, `close_time`, `slot_interval_min`, `block_public_holidays`)
VALUES (1, '10:00:00', '19:00:00', 30, TRUE);
INSERT INTO `smartstore_order_sync_state` (`id`, `last_changed_from`)
VALUES (1, DATE_ADD(CURRENT_TIMESTAMP(6), INTERVAL 9 HOUR));
INSERT INTO `smartstore_settlement_sync_state` (`id`, `next_pay_date`)
VALUES (1, DATE_SUB(CURRENT_DATE, INTERVAL 6 DAY));
INSERT INTO `workshop_profiles` (`id`, `name`, `phone`, `address_line1`, `address_line2`, `map_url`,
                                 `business_registration_number`, `representative_name`, `email`,
                                 `mail_order_registration_number`, `introduction`, `kakao_talk_id`,
                                 `naver_talk_url`, `naver_blog_url`, `instagram_url`, `smart_store_url`)
VALUES (1, '해피갤러리', '010-9635-5608', '충북 충주시 계명대로 161', '1층',
        'https://m.place.naver.com/place/21668321', '303-11-87052', '홍지현', 'ssim1972@naver.com',
        '2011-충북 충주-127',
        '해피갤러리는 빈티지 가죽공예, 레진아트, 플루이드아트, 톨페인팅, 냅킨아트, 양말목공예, 하바리움, 위빙, POP 원데이클래스부터 자격증반, 창업반을 운영합니다.',
        'ssim1972', 'https://talk.naver.com/w4xufy', 'https://blog.naver.com/ssim1972',
        'https://www.instagram.com/happygallery_by/', 'https://smartstore.naver.com/happygallery');

package com.personal.happygallery.domain.error;

public class InventoryNotEnoughException extends HappyGalleryException {

    public InventoryNotEnoughException() {
        super(ErrorCode.INVENTORY_NOT_ENOUGH);
    }
}

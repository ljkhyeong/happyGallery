package com.personal.happygallery.common.error;

public class InventoryNotEnoughException extends HappyGalleryException {

    public InventoryNotEnoughException() {
        super(ErrorCode.INVENTORY_NOT_ENOUGH);
    }
}

package shop.personal.happyGallery.domain;

public enum OrderStatus {
	PLACED{
		@Override
		public OrderStatus pay() { return PAYED; }
	}, PAYED, PREPARING, SHIPPED, DELIVERED, CANCELED, REFUNDING;

	public OrderStatus pay() { throw cannot("pay"); }
	public OrderStatus prepare() { throw cannot("prepare"); }
	public OrderStatus ship() { throw cannot("ship"); }
	public OrderStatus deliver() { throw cannot("deliver"); }
	public OrderStatus cancel() { throw cannot("cancel"); }
	public OrderStatus refund() { throw cannot("refund"); }


	private UnsupportedOperationException cannot(String op) {
		return new UnsupportedOperationException(this + "상태에서는 " + op + " 불가");
	}
}

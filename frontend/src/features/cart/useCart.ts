import { useContext } from "react";
import { CartContext } from "./CartProvider";

export function useCart() {
  const cart = useContext(CartContext);
  if (!cart) {
    throw new Error("useCart must be used within CartProvider");
  }
  return cart;
}

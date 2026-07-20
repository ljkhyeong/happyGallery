import type { ButtonProps } from "react-bootstrap";
import { Link, type LinkProps } from "react-router-dom";

type LinkButtonProps = LinkProps & Pick<ButtonProps, "active" | "size" | "variant">;

export function LinkButton({
  active = false,
  className,
  size,
  variant = "primary",
  ...linkProps
}: LinkButtonProps) {
  const classes = [
    className,
    "btn",
    active && "active",
    variant && `btn-${variant}`,
    size && `btn-${size}`,
  ].filter(Boolean).join(" ");

  return <Link {...linkProps} role={linkProps.role ?? "button"} className={classes} />;
}

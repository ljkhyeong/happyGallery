import type { ButtonProps } from "react-bootstrap";
import { Link, type LinkProps } from "react-router";

type LinkButtonProps = LinkProps & Pick<ButtonProps, "size" | "variant">;

export function LinkButton({
  className,
  size,
  variant = "primary",
  ...linkProps
}: LinkButtonProps) {
  const classes = [
    className,
    "btn",
    variant && `btn-${variant}`,
    size && `btn-${size}`,
  ].filter(Boolean).join(" ");

  return <Link {...linkProps} className={classes} />;
}

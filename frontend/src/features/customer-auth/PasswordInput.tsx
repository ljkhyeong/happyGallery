import { useState, type InputHTMLAttributes } from "react";
import { Button, Form, InputGroup } from "react-bootstrap";

type Props = Omit<InputHTMLAttributes<HTMLInputElement>, "type" | "size" | "value"> & { value: string };

/**
 * 입력한 비밀번호를 잠깐 확인할 수 있는 칸. 보기 버튼 이름에 "비밀번호"를 넣지 않아
 * 라벨로 칸을 찾는 보조기기·테스트가 버튼과 헷갈리지 않게 한다.
 */
export function PasswordInput(props: Props) {
  const [visible, setVisible] = useState(false);
  return (
    <InputGroup className="password-input">
      <Form.Control {...props} type={visible ? "text" : "password"} />
      <Button
        type="button"
        variant="outline-secondary"
        aria-pressed={visible}
        onClick={() => setVisible((value) => !value)}
      >
        {visible ? "숨기기" : "보기"}
      </Button>
    </InputGroup>
  );
}

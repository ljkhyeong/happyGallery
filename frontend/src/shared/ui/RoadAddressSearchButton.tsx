import { useCallback, useEffect, useRef, useState } from "react";
import { Button, Modal, Spinner } from "react-bootstrap";
import { ErrorAlert } from "./ErrorAlert";

interface SelectedAddress {
  postalCode: string;
  roadAddress: string;
}

interface PostcodeData {
  zonecode: string;
  roadAddress: string;
  address: string;
}

type PostcodeConstructor = new (options: {
  oncomplete: (data: PostcodeData) => void;
  width: string;
  height: string;
}) => { embed: (container: HTMLElement) => void };

declare global {
  interface Window {
    kakao?: { Postcode?: PostcodeConstructor };
  }
}

let postcodeLoader: Promise<PostcodeConstructor> | undefined;

function loadPostcode(): Promise<PostcodeConstructor> {
  if (window.kakao?.Postcode) return Promise.resolve(window.kakao.Postcode);
  return postcodeLoader ??= new Promise<PostcodeConstructor>((resolve, reject) => {
    const script = document.createElement("script");
    const fail = () => {
      window.clearTimeout(timeout);
      script.remove();
      reject(new Error("주소 검색을 불러오지 못했습니다."));
    };
    const timeout = window.setTimeout(fail, 10_000);
    script.src = "https://t1.kakaocdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js";
    script.async = true;
    script.onload = () => {
      window.clearTimeout(timeout);
      if (window.kakao?.Postcode) resolve(window.kakao.Postcode);
      else fail();
    };
    script.onerror = fail;
    document.head.appendChild(script);
  }).catch((error: unknown) => {
    postcodeLoader = undefined;
    throw error;
  });
}

interface Props {
  onSelect: (address: SelectedAddress) => void;
}

function PostcodeSearch({ onSelect }: Props) {
  const container = useRef<HTMLDivElement>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<unknown>();

  useEffect(() => {
    let active = true;
    const element = container.current;
    loadPostcode().then((Postcode) => {
      if (!active || !element) return;
      new Postcode({
        oncomplete: (data) => {
          if (active) onSelect({ postalCode: data.zonecode, roadAddress: data.roadAddress || data.address });
        },
        width: "100%",
        height: "100%",
      }).embed(element);
      setLoading(false);
    }).catch((cause: unknown) => {
      if (active) {
        setError(cause);
        setLoading(false);
      }
    });
    return () => {
      active = false;
      element?.replaceChildren();
    };
  }, [onSelect]);

  return (
    <>
      {loading && <Spinner size="sm" aria-label="주소 검색 불러오는 중" />}
      <ErrorAlert error={error} />
      <div ref={container} hidden={Boolean(error)} style={{ height: "min(500px, 65vh)" }} />
      <p className="small text-muted-soft mt-2 mb-0">검색창을 닫고 주소를 직접 입력할 수도 있습니다.</p>
    </>
  );
}

export function RoadAddressSearchButton({ onSelect }: Props) {
  const [open, setOpen] = useState(false);
  const select = useCallback((address: SelectedAddress) => {
    onSelect(address);
    setOpen(false);
  }, [onSelect]);

  return (
    <>
      <Button type="button" variant="outline-dark" onClick={() => setOpen(true)}>주소 검색</Button>
      <Modal show={open} onHide={() => setOpen(false)} centered aria-labelledby="road-address-search-title">
        <Modal.Header closeButton>
          <Modal.Title id="road-address-search-title" className="fs-6">도로명주소 검색</Modal.Title>
        </Modal.Header>
        <Modal.Body>{open && <PostcodeSearch onSelect={select} />}</Modal.Body>
      </Modal>
    </>
  );
}

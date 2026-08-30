import { useState, type FormEvent } from "react";
import { useMutation } from "@tanstack/react-query";
import { Button, Form, InputGroup, ListGroup, Modal, Spinner } from "react-bootstrap";
import {
  searchRoadAddresses,
  type RoadAddressResponse,
} from "@/generated/api/roadAddress";
import { ErrorAlert } from "./ErrorAlert";

interface Props {
  onSelect: (address: RoadAddressResponse) => void;
}

export function RoadAddressSearchButton({ onSelect }: Props) {
  const [open, setOpen] = useState(false);
  const [keyword, setKeyword] = useState("");
  const search = useMutation({
    mutationFn: (searchKeyword: string) => searchRoadAddresses({ keyword: searchKeyword }),
  });

  const close = () => {
    setOpen(false);
    search.reset();
  };

  const submit = (event: FormEvent) => {
    event.preventDefault();
    const cleanKeyword = keyword.trim();
    if (cleanKeyword.length >= 2) search.mutate(cleanKeyword);
  };

  const select = (address: RoadAddressResponse) => {
    onSelect(address);
    close();
  };

  return (
    <>
      <Button type="button" variant="outline-dark" onClick={() => setOpen(true)}>
        주소 검색
      </Button>
      <Modal show={open} onHide={close} centered aria-labelledby="road-address-search-title">
        <Modal.Header closeButton>
          <Modal.Title id="road-address-search-title" className="fs-6">도로명주소 검색</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Form onSubmit={submit}>
            <Form.Label htmlFor="road-address-keyword">도로명 또는 건물명</Form.Label>
            <InputGroup className="mb-3">
              <Form.Control
                id="road-address-keyword"
                value={keyword}
                minLength={2}
                maxLength={100}
                autoFocus
                placeholder="예: 계명대로 161"
                onChange={(event) => setKeyword(event.target.value)}
              />
              <Button type="submit" variant="dark" disabled={keyword.trim().length < 2 || search.isPending}>
                {search.isPending ? <Spinner size="sm" aria-label="주소 검색 중" /> : "검색"}
              </Button>
            </InputGroup>
          </Form>

          <ErrorAlert error={search.error} />
          {search.isError && (
            <p className="small text-muted-soft mb-0">검색창을 닫고 주소를 직접 입력할 수 있습니다.</p>
          )}
          {search.data?.length === 0 && (
            <p className="small text-muted-soft mb-0">검색 결과가 없습니다. 도로명과 건물번호를 함께 입력해 보세요.</p>
          )}
          {search.data && search.data.length > 0 && (
            <ListGroup variant="flush">
              {search.data.map((address) => (
                <ListGroup.Item
                  key={`${address.postalCode}-${address.roadAddress}`}
                  action
                  type="button"
                  onClick={() => select(address)}
                >
                  <div className="fw-semibold">{address.roadAddress}</div>
                  <div className="small text-muted-soft">[{address.postalCode}] {address.jibunAddress}</div>
                  {address.buildingName && (
                    <div className="small text-muted-soft">{address.buildingName}</div>
                  )}
                </ListGroup.Item>
              ))}
            </ListGroup>
          )}
        </Modal.Body>
      </Modal>
    </>
  );
}

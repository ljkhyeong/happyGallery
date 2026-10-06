import { useState, type FormEvent } from "react";
import { useNavigate } from "react-router";
import { Search } from "lucide-react";

/**
 * 머리글 검색창. 작품 목록의 이름 검색(`keyword`)으로 보낸다.
 * 작품 목록의 도구 막대와 같은 검색 조건을 쓰므로 목록 화면에서 결과를 이어서 좁힐 수 있다.
 */
export function ProductSearchForm() {
  const navigate = useNavigate();
  const [keyword, setKeyword] = useState("");

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const normalized = keyword.trim().slice(0, 100);
    navigate(normalized ? `/products?${new URLSearchParams({ keyword: normalized })}` : "/products");
  };

  return (
    <form role="search" className="app-search" onSubmit={handleSubmit}>
      <label htmlFor="app-search-input" className="visually-hidden">작품 검색</label>
      <input
        id="app-search-input"
        type="search"
        value={keyword}
        onChange={(event) => setKeyword(event.target.value)}
        placeholder="가죽 카드지갑, 레진 키링"
        autoComplete="off"
        enterKeyHint="search"
      />
      <button type="submit" aria-label="검색하기">
        <Search size={20} aria-hidden="true" />
      </button>
    </form>
  );
}

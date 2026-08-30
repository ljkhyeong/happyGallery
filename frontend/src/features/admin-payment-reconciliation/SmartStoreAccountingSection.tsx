import { useState } from "react";
import { Alert, Button, Form, Table } from "react-bootstrap";
import type { SmartStoreAccountingReportResponse } from "@/generated/api/adminOperations";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { formatKRW } from "@/shared/lib";
import { ErrorAlert, LoadingSpinner } from "@/shared/ui";
import { fetchSmartStoreAccountingReport } from "./api";

interface Props {
  adminKey: string;
  onAuthError: () => void;
}

export function SmartStoreAccountingSection({ adminKey, onAuthError }: Props) {
  const initial = previousMonthRange();
  const [from, setFrom] = useState(initial.from);
  const [to, setTo] = useState(initial.to);
  const [range, setRange] = useState(initial);
  const query = useAdminQuery(onAuthError, {
    queryKey: ["admin", "smartstore-settlements", "accounting", range.from, range.to],
    queryFn: () => fetchSmartStoreAccountingReport(adminKey, range.from, range.to),
  });

  return <>
    <Form className="d-flex flex-wrap align-items-end gap-2 mb-3" onSubmit={(event) => {
      event.preventDefault();
      setRange({ from, to });
    }}>
      <Form.Group><Form.Label className="small">시작일</Form.Label>
        <Form.Control type="date" required value={from}
          onChange={(event) => setFrom(event.target.value)} />
      </Form.Group>
      <Form.Group><Form.Label className="small">종료일</Form.Label>
        <Form.Control type="date" required value={to}
          onChange={(event) => setTo(event.target.value)} />
      </Form.Group>
      <Button type="submit" variant="outline-primary" disabled={from > to}>회계 자료 조회</Button>
      {query.data && <Button type="button" variant="outline-success"
        onClick={() => downloadCsv(query.data)}>CSV 다운로드</Button>}
    </Form>
    {query.isLoading && <LoadingSpinner />}
    <ErrorAlert error={query.error} />
    {query.data && <>
      <Alert variant="light" className="small">
        일별 정산 {query.data.dailySettlements.length}건 · 수수료 {query.data.commissionDetails.length}건
        {` · 부가세 ${query.data.dailyVat.length}건 (제공 가능일 ${query.data.vatAvailableThrough}까지)`}
      </Alert>
      <h3 className="h6 mt-4">일별 정산</h3>
      <Table responsive hover size="sm" className="align-middle">
        <thead><tr><th>기준 기간</th><th>지급 예정일</th><th className="text-end">정산액</th>
          <th className="text-end">수수료</th><th className="text-end">보류액</th></tr></thead>
        <tbody>{query.data.dailySettlements.map((item, index) => <tr
          key={`${item.settleBasisStartDate}-${item.settleMethodType}-${index}`}>
          <td>{item.settleBasisStartDate} ~ {item.settleBasisEndDate}</td>
          <td>{item.settleExpectDate}</td>
          <td className="text-end">{formatKRW(item.settleAmount)}</td>
          <td className="text-end">{formatKRW(item.commissionSettleAmount)}</td>
          <td className="text-end">{formatKRW(item.payHoldbackAmount)}</td>
        </tr>)}</tbody>
      </Table>
      <h3 className="h6 mt-4">수수료 상세</h3>
      <Table responsive hover size="sm" className="align-middle">
        <thead><tr><th>상품 주문 번호</th><th>상품</th><th>수수료 유형</th>
          <th className="text-end">기준 금액</th><th className="text-end">수수료</th></tr></thead>
        <tbody>{query.data.commissionDetails.map((item, index) => <tr
          key={`${item.productOrderId}-${item.commissionType}-${index}`}>
          <td className="small">{item.productOrderId}</td><td>{item.productName ?? "-"}</td>
          <td>{item.commissionType}</td>
          <td className="text-end">{formatKRW(item.commissionBasisAmount)}</td>
          <td className="text-end">{formatKRW(item.commissionAmount)}</td>
        </tr>)}</tbody>
      </Table>
      <h3 className="h6 mt-4">일별 부가세</h3>
      <Table responsive hover size="sm" className="align-middle">
        <thead><tr><th>기준일</th><th className="text-end">총 매출</th>
          <th className="text-end">과세</th><th className="text-end">면세</th>
          <th className="text-end">카드</th></tr></thead>
        <tbody>{query.data.dailyVat.map((item) => <tr key={item.settleBasisDate}>
          <td>{item.settleBasisDate}</td><td className="text-end">{formatKRW(item.totalSalesAmount)}</td>
          <td className="text-end">{formatKRW(item.taxationSalesAmount)}</td>
          <td className="text-end">{formatKRW(item.taxExemptionSalesAmount)}</td>
          <td className="text-end">{formatKRW(item.creditCardAmount)}</td>
        </tr>)}</tbody>
      </Table>
    </>}
  </>;
}

function previousMonthRange() {
  const today = new Date();
  const first = new Date(today.getFullYear(), today.getMonth() - 1, 1);
  const last = new Date(today.getFullYear(), today.getMonth(), 0);
  return { from: localDate(first), to: localDate(last) };
}

function localDate(value: Date): string {
  const year = value.getFullYear();
  const month = String(value.getMonth() + 1).padStart(2, "0");
  const day = String(value.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function downloadCsv(report: SmartStoreAccountingReportResponse) {
  const rows: unknown[][] = [
    ["[일별 정산]"],
    ["기준 시작일", "기준 종료일", "지급 예정일", "지급 완료일", "정산액", "결제 정산액",
      "수수료 정산액", "혜택 정산액", "지급 보류액", "차감액", "차액", "정산 방식"],
    ...report.dailySettlements.map((item) => [
      item.settleBasisStartDate, item.settleBasisEndDate, item.settleExpectDate,
      item.settleCompleteDate, item.settleAmount, item.paySettleAmount,
      item.commissionSettleAmount, item.benefitSettleAmount, item.payHoldbackAmount,
      item.minusChargeAmount, item.differenceSettleAmount, item.settleMethodType,
    ]),
    [], ["[수수료 상세]"],
    ["주문 번호", "상품 주문 번호", "상품명", "정산 유형", "기준일", "수수료 유형", "기준 금액", "수수료"],
    ...report.commissionDetails.map((item) => [
      item.orderNo, item.productOrderId, item.productName, item.settleType,
      item.settleBasisDate, item.commissionType, item.commissionBasisAmount, item.commissionAmount,
    ]),
    [], ["[일별 부가세]"],
    ["기준일", "총 매출", "과세 매출", "면세 매출", "카드", "현금 소득공제",
      "현금 지출증빙", "현금 미발행", "기타"],
    ...report.dailyVat.map((item) => [
      item.settleBasisDate, item.totalSalesAmount, item.taxationSalesAmount,
      item.taxExemptionSalesAmount, item.creditCardAmount, item.cashInComeDeductionAmount,
      item.cashOutGoingEvidenceAmount, item.cashExclusionIssuanceAmount, item.otherAmount,
    ]),
  ];
  const csv = `\uFEFF${rows.map((row) => row.map(csvCell).join(",")).join("\r\n")}`;
  const url = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" }));
  const link = document.createElement("a");
  link.href = url;
  link.download = `smartstore-accounting-${report.from}-${report.to}.csv`;
  link.click();
  URL.revokeObjectURL(url);
}

function csvCell(value: unknown): string {
  const text = value === null || value === undefined ? "" : String(value);
  return `"${text.replaceAll('"', '""')}"`;
}

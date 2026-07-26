import { Container } from "react-bootstrap";
import { Link, useParams } from "react-router-dom";
import {
  resolvePolicyDocument,
  TERMS_POLICY_DOCUMENTS,
} from "@/features/policy-consent/policyDocuments";
import {
  TERMS_POLICY_VERSION,
  policyPath,
} from "@/features/policy-consent/policyVersions";

export function TermsPage() {
  const { version = TERMS_POLICY_VERSION } = useParams();
  const PolicyDocument = resolvePolicyDocument(TERMS_POLICY_DOCUMENTS, version);

  if (PolicyDocument) {
    return <PolicyDocument />;
  }

  return (
    <Container className="page-container legal-page" style={{ maxWidth: 860 }}>
      <header className="legal-page-header">
        <h1>이용약관</h1>
        <p>요청한 버전의 문서를 찾을 수 없습니다.</p>
      </header>
      <Link to={policyPath("terms", TERMS_POLICY_VERSION)}>현재 이용약관 보기</Link>
    </Container>
  );
}

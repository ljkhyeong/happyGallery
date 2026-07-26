import { Container } from "react-bootstrap";
import { Link, useParams } from "react-router-dom";
import {
  PRIVACY_POLICY_DOCUMENTS,
  resolvePolicyDocument,
} from "@/features/policy-consent/policyDocuments";
import {
  PRIVACY_POLICY_VERSION,
  policyPath,
} from "@/features/policy-consent/policyVersions";

export function PrivacyPolicyPage() {
  const { version = PRIVACY_POLICY_VERSION } = useParams();
  const PolicyDocument = resolvePolicyDocument(PRIVACY_POLICY_DOCUMENTS, version);

  if (PolicyDocument) {
    return <PolicyDocument />;
  }

  return (
    <Container className="page-container legal-page" style={{ maxWidth: 860 }}>
      <header className="legal-page-header">
        <h1>개인정보처리방침</h1>
        <p>요청한 버전의 문서를 찾을 수 없습니다.</p>
      </header>
      <Link to={policyPath("privacy", PRIVACY_POLICY_VERSION)}>
        현재 개인정보처리방침 보기
      </Link>
    </Container>
  );
}

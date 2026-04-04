/** Admin Bearer 인증 헤더를 생성한다. */
export function adminHeaders(token: string) {
  return { Authorization: `Bearer ${token}` };
}

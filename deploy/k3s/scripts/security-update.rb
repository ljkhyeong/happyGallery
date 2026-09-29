#!/usr/bin/env ruby
# Trivy의 수정 버전 중 현재 계열의 패치만 선택한다. 임의 Gradle 코드나 보고서 명령은 실행하지 않는다.
require 'json'
require 'rubygems'

module SecurityUpdate
  GROUPS = {
    'com.fasterxml.jackson.' => 'jackson-2-bom.version',
    'tools.jackson.' => 'jackson-bom.version',
    'org.apache.tomcat.' => 'tomcat.version',
    'io.netty:' => 'netty.version',
    'org.apache.httpcomponents.core5:' => 'httpcore5.version'
  }.freeze

  def self.plan(source, reports)
    updates = {}
    blocked = []
    reports.each do |report|
      report.fetch('Results').each do |result|
        Array(result['Vulnerabilities']).each do |finding|
          next unless %w[HIGH CRITICAL].include?(finding['Severity'])
          package = finding.fetch('PkgName')
          property = GROUPS.find { |prefix, _| package.start_with?(prefix) }&.last
          pattern = /ext\['#{Regexp.escape(property.to_s)}'\] = '([^']+)'/
          current = source.match(pattern)&.captures&.first
          installed = finding['InstalledVersion']
          fixed = finding.fetch('FixedVersion', '').split(',').map(&:strip)
          # Final 접미사를 포함해 숫자 세 자리 패치 버전만 취급한다.
          version = /\A\d+\.\d+\.\d+(?:\.Final)?\z/
          candidates = fixed.select do |value|
            current && current.match?(version) && value.match?(version) &&
              installed == current && value.split('.')[0, 2] == current.split('.')[0, 2] &&
              value.end_with?('.Final') == current.end_with?('.Final') &&
              Gem::Version.new(value) > Gem::Version.new(current)
          end
          if property && !candidates.empty?
            candidate = candidates.min_by { |value| Gem::Version.new(value) }
            previous = updates[property]
            updates[property] = candidate if !previous || Gem::Version.new(candidate) > Gem::Version.new(previous)
          else
            blocked << "#{finding['VulnerabilityID']}: #{package} — 관리 BOM·현재 버전 일치·동일 계열 수정판 조건 미충족"
          end
        end
      end
    end
    updates.each do |property, version|
      source = source.sub(/(ext\['#{Regexp.escape(property)}'\] = ')[^']+(')/) { "#{$1}#{version}#{$2}" }
    end
    [source, updates, blocked.uniq]
  end
end

if $PROGRAM_NAME == __FILE__
  source = File.read('build.gradle')
  reports = ARGV.map { |path| JSON.parse(File.read(path)) }
  abort 'Trivy 결과가 없습니다.' if reports.empty?
  updated, changes, blocked = SecurityUpdate.plan(source, reports)
  File.write('build.gradle', updated) unless changes.empty?
  body = "## 보안 업데이트\n\nTrivy가 제시한 동일 계열 수정 버전을 반영합니다. 자동 병합하지 않습니다.\n\n"
  changes.each { |property, version| body << "- #{property}: #{version}\n" }
  body << "자동 수정 가능한 의존성이 없습니다. Secret·OS EOL·검사 실행 오류는 원본 검사 로그를 확인하세요.\n" if changes.empty?
  body << "\n## 자동 처리하지 못한 항목\n\n" << (blocked.empty? ? "없음\n" : blocked.map { |line| "- #{line}\n" }.join)
  body << "\n## 검증\n\nSecurity update validation에서 빌드·기존 CI·백엔드와 프론트 이미지 보안 검사를 실행합니다. 결과 통과 전 병합하지 않습니다.\n"
  File.write('/tmp/security-update-body.md', body)
  File.open(ENV['GITHUB_STEP_SUMMARY'], 'a') { |f| f.write(body) } if ENV['GITHUB_STEP_SUMMARY']
  File.open(ENV.fetch('GITHUB_OUTPUT'), 'a') { |f| f.puts "changed=#{!changes.empty?}" }
end

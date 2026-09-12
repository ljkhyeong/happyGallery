#!/usr/bin/env ruby
# frozen_string_literal: true

require 'yaml'
require 'open3'

module RollingRelease
  # 변경이 없다는 것만 자동으로 확인한다. 호환 migration/API 변경은 별도 검토 대상이다.
  COMPATIBILITY_PATHS = %w[
    bootstrap/src/main/resources/db/migration
    bootstrap/src/main/java/com/personal/happygallery/bootstrap/migration
    bootstrap/src/main/resources/application.yml
    bootstrap/src/main/resources/application-prod.yml
    adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/security
    adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/customer/CustomerSessionBinder.java
    docs/PRD/0004_API_계약/openapi3.json
    build.gradle settings.gradle gradle
  ].freeze

  def self.documents(path)
    YAML.load_stream(File.read(path)).compact
  end

  def self.workload(documents, name)
    documents.find { |d| %w[Deployment StatefulSet].include?(d['kind']) && d.dig('metadata', 'name') == name } ||
      raise("기존 workload 기록이 없습니다: #{name}")
  end

  def self.check(repository, previous, candidate)
    old = documents(previous)
    fresh = documents(candidate)
    %w[app frontend].each do |name|
      refs = [old, fresh].map { |docs| workload(docs, name).dig('spec', 'template', 'spec', 'containers', 0, 'image') }
      revisions = refs.map do |ref|
        match = ref.to_s.match(/:([a-f0-9]{40})@sha256:[a-f0-9]{64}\z/)
        raise "#{name}의 40자리 commit SHA/digest 기록이 필요합니다." unless match
        match[1]
      end
      revisions.each do |revision|
        _, status = Open3.capture2e('git', '-C', repository, 'cat-file', '-e', "#{revision}^{commit}")
        raise "배포 이력을 비교할 Git commit이 없습니다: #{revision}" unless status.success?
      end
      paths, status = Open3.capture2e('git', '-C', repository, '-c', 'core.quotePath=false', 'diff', '--name-only', *revisions, '--', *COMPATIBILITY_PATHS)
      raise '배포 호환성 비교에 실패했습니다.' unless status.success?
      raise "DB·API·세션·기반 설정 변경은 호환성 검토 후 별도 배포가 필요합니다:\n#{paths}" unless paths.empty?
    end
    %w[mysql redis prometheus alertmanager grafana].each do |name|
      raise "#{name} 변경을 앱 롤링 배포와 함께 적용할 수 없습니다." unless
        workload(old, name)['spec'] == workload(fresh, name)['spec']
    end
    old_config = old.find { |d| d['kind'] == 'ConfigMap' && d.dig('metadata', 'name') == 'app-config' }
    new_config = fresh.find { |d| d['kind'] == 'ConfigMap' && d.dig('metadata', 'name') == 'app-config' }
    raise 'app-config 변경은 혼합 버전 호환성 검토가 필요합니다.' unless old_config == new_config
  end

  def self.extract_assets(manifest, phase)
    documents(manifest).select do |d|
      name = d.dig('metadata', 'name')
      case phase
      when 'storage' then d['kind'] == 'PersistentVolumeClaim' && name == 'frontend-assets'
      when 'server' then %w[Deployment Service NetworkPolicy].include?(d['kind']) &&
        %w[frontend-assets allow-ingress-to-assets].include?(name)
      when 'route' then d['kind'] == 'Ingress' && name == 'happygallery-assets'
      else raise "알 수 없는 단계: #{phase}"
      end
    end.each { |d| puts YAML.dump(d) }
  end
end

if $PROGRAM_NAME == __FILE__
  begin
    case ARGV.shift
    when 'check' then RollingRelease.check(*ARGV)
    when 'assets' then RollingRelease.extract_assets(*ARGV)
    else abort '사용법: rolling-release.rb check <repo> <이전 YAML> <새 YAML> | assets <YAML> <storage|server|route>'
    end
  rescue StandardError => error
    abort "오류: #{error.message}"
  end
end

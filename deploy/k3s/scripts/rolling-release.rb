#!/usr/bin/env ruby
# frozen_string_literal: true

require 'yaml'
require 'open3'
require 'json'
require 'strscan'
require_relative 'openapi-compatibility'

module RollingRelease
  # 계약·데이터 변경은 구조로 판정하고 인증·세션 변경은 코드 리뷰 기록을 확인한다.
  COMPATIBILITY_PATHS = %w[
    bootstrap/src/main/resources/db/migration
    bootstrap/src/main/java/com/personal/happygallery/bootstrap/migration
    bootstrap/src/main/resources/application.yml
    bootstrap/src/main/resources/application-prod.yml
    adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/security
    adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/customer/CustomerSessionBinder.java
    docs/PRD/0004_API_계약/openapi3.json
  ].freeze
  COMPATIBILITY_DECLARATION = 'deploy/k3s/rolling-compatibility.yml'
  COMPATIBILITY_CATEGORIES = %w[migration api runtime_config build].freeze
  # 업무 설정은 값 변경을 허용한다. 데이터 연결·저장소·보안 경계만 전환 대상으로 남긴다.
  PROTECTED_CONFIG_KEYS = %w[DB_URL REDIS_HOST REDIS_PORT MEDIA_STORAGE_PATH RATE_LIMIT_KEY_PREFIX
                             SPRING_PROFILES_ACTIVE HAPPYGALLERY_RUNTIME_MODE RATE_LIMIT_ENABLED
                             SESSION_SECURE_COOKIE FORWARD_HEADERS_STRATEGY MANAGEMENT_PORT
                             ACTUATOR_HEALTH_SHOW_DETAILS MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED].freeze
  PROTECTED_APPLICATION_PATHS = %w[spring.datasource.url spring.datasource.username spring.datasource.password
                                   spring.data.redis.host spring.data.redis.port spring.data.redis.password
                                   spring.session spring.security spring.profiles spring.config spring.flyway
                                   server.servlet.session app.runtime-mode app.key-rotation app.field-encryption app.admin app.guest-token.hmac-secret
                                   app.guest-token.previous-hmac-secret app.media.storage-path
                                   app.rate-limit.enabled app.rate-limit.key-prefix].freeze
  SQL_QUOTED_TOKEN = /'(?:''|\\.|[^'\\])*'|"(?:""|\\.|[^"\\])*"|\x60(?:\x60\x60|[^\x60])*\x60/m

  def self.documents(path)
    YAML.load_stream(File.read(path)).compact
  end

  def self.workload(documents, name)
    documents.find { |d| %w[Deployment StatefulSet].include?(d['kind']) && d.dig('metadata', 'name') == name } ||
      raise("기존 workload 기록이 없습니다: #{name}")
  end

  def self.git_blob(repository, revision, path)
    content, status = Open3.capture2e('git', '-C', repository, 'show', "#{revision}:#{path}")
    raise "Git 파일을 읽을 수 없습니다: #{revision}:#{path}" unless status.success?

    content
  end

  def self.git_diff_paths(repository, old_revision, new_revision)
    paths, status = Open3.capture2e(
      'git', '-C', repository, '-c', 'core.quotePath=false', 'diff', '--name-only', '--no-renames',
      old_revision, new_revision, '--', *COMPATIBILITY_PATHS
    )
    raise '배포 호환성 비교에 실패했습니다.' unless status.success?

    paths.lines.map(&:strip).reject(&:empty?).uniq.sort
  end

  def self.git_diff_statuses(repository, old_revision, new_revision)
    paths, status = Open3.capture2e(
      'git', '-C', repository, '-c', 'core.quotePath=false', 'diff', '--name-status', '--no-renames',
      old_revision, new_revision, '--', *COMPATIBILITY_PATHS
    )
    raise '배포 호환성 상태 비교에 실패했습니다.' unless status.success?

    paths.lines.each_with_object({}) do |line, result|
      change, path = line.strip.split("\t", 2)
      result[path] = change unless path.to_s.empty?
    end
  end

  def self.compatibility_declaration(repository, revision)
    raw = git_blob(repository, revision, COMPATIBILITY_DECLARATION)
    declaration = YAML.safe_load(raw, permitted_classes: [], aliases: false)
    unless declaration.is_a?(Hash) && declaration['version'] == 1 && declaration['mode'] == 'expand'
      raise '호환성 선언은 version: 1, mode: expand 형식이어야 합니다.'
    end
    raise '호환성 선언에는 검토 사유가 필요합니다.' if declaration['reason'].to_s.strip.empty?

    reviewed = declaration['reviewed']
    unless reviewed.is_a?(Hash) && (reviewed.keys - COMPATIBILITY_CATEGORIES).empty?
      raise "호환성 선언 카테고리는 #{COMPATIBILITY_CATEGORIES.join(', ')}만 사용할 수 있습니다."
    end

    paths = reviewed.each_with_object([]) do |(category, category_paths), result|
      unless COMPATIBILITY_CATEGORIES.include?(category) && category_paths.is_a?(Array) &&
             category_paths.all? { |path| path.is_a?(String) && !path.strip.empty? }
        raise "호환성 선언 경로 형식이 잘못되었습니다: #{category}"
      end
      result.concat(category_paths)
    end
    raise '호환성 선언 경로가 중복됩니다.' unless paths.uniq.length == paths.length

    [declaration, paths.sort]
  rescue Psych::Exception => error
    raise "호환성 선언 YAML을 읽을 수 없습니다: #{error.message}"
  end

  # SQL 문자열 안의 세미콜론과 주석은 문장 경계로 취급하지 않는다.
  def self.sql_statements(sql)
    scanner = StringScanner.new(sql)
    statements, current = [], +''
    until scanner.eos?
      if scanner.scan(/--(?=\s|$)[^\n]*|\#[^\n]*|\/\*(?![!+]).*?\*\//m)
        current << ' '
      elsif scanner.peek(2) == '/*'
        raise '실행형 SQL 주석 또는 닫히지 않은 주석은 자동 판정할 수 없습니다.'
      elsif (quoted = scanner.scan(SQL_QUOTED_TOKEN))
        current << quoted
      elsif scanner.scan(/;/)
        statements << current.strip unless current.strip.empty?
        current = +''
      else
        current << scanner.getch
      end
    end
    statements << current.strip unless current.strip.empty?
    statements
  end

  def self.expand_statement?(statement)
    identifier = '(?:[A-Za-z0-9_]+|\x60[^\x60]+\x60)'
    column = /\AALTER\s+TABLE\s+#{identifier}\s+ADD\s+(?:COLUMN\s+)?#{identifier}\s+[A-Z]+(?:\([0-9, ]+\))?(?:\s+UNSIGNED)?\s+NULL(?:\s+DEFAULT\s+NULL)?\z/i
    return true if column.match?(statement)

    scanner = StringScanner.new(statement)
    return false unless scanner.scan(/CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?#{identifier}\s*\(/i)
    depth = 1
    until scanner.eos? || depth.zero?
      next if scanner.scan(SQL_QUOTED_TOKEN)
      token = scanner.getch
      depth += 1 if token == '('
      depth -= 1 if token == ')'
    end
    # 정의 괄호 뒤의 AS SELECT 등을 새 테이블 정의로 잘못 허용하지 않는다.
    depth.zero? && scanner.rest.match?(/\A(?:\s+(?:ENGINE\s*=\s*\w+|(?:DEFAULT\s+)?CHARSET\s*=\s*\w+|(?:DEFAULT\s+)?CHARACTER\s+SET\s*=?\s*\w+|COLLATE\s*=\s*\w+))*\s*\z/i)
  end

  def self.validate_expand_migrations(repository, revision, paths, statuses)
    errors = []
    paths.grep(%r{\Abootstrap/src/(?:main/resources/db/migration|main/java/.+/migration)/}).each do |path|
      begin
        raise "적용 이력이 있는 migration 수정·삭제: #{path}" unless statuses[path] == 'A'
        raise "Java migration은 별도 전환 검토가 필요합니다: #{path}" unless path.end_with?('.sql')

        statements = sql_statements(git_blob(repository, revision, path))
        unsupported = statements.each_index.reject { |index| expand_statement?(statements[index]) }
        unless !statements.empty? && unsupported.empty?
          raise "migration 별도 검토 필요: #{path} (문장 #{unsupported.map { |i| i + 1 }.join(', ')}). 자동 허용: nullable 컬럼·새 테이블 추가"
        end
      rescue RuntimeError => error
        errors << "#{path}: #{error.message}"
      end
    end
    raise errors.join("\n") unless errors.empty?
  end

  def self.validate_additive_openapi(repository, old_revision, new_revision, paths)
    path = 'docs/PRD/0004_API_계약/openapi3.json'
    return unless paths.include?(path)

    old_spec = JSON.parse(git_blob(repository, old_revision, path))
    new_spec = JSON.parse(git_blob(repository, new_revision, path))
    errors = OpenapiCompatibility.new(old_spec, new_spec).differences
    raise "OpenAPI 비호환 변경:\n#{errors.join("\n")}" unless errors.empty?
  rescue JSON::ParserError => error
    raise "OpenAPI JSON을 읽을 수 없습니다: #{path}: #{error.message}"
  end

  def self.validate_reviewed_compatibility(repository, old_revision, new_revision, paths)
    # API는 계약 구조로 자동 판정한다. 이력이 남은 승인 경로는 후속 배포를 막지 않는다.
    review_paths = paths.select do |path|
      if path.end_with?('.yml')
        left, right = [old_revision, new_revision].map do |revision|
          YAML.safe_load(git_blob(repository, revision, path), permitted_classes: [], aliases: false)
        end
        changed_fields(left, right).any? do |field|
          PROTECTED_APPLICATION_PATHS.any? { |prefix| field == prefix || field.start_with?("#{prefix}.") || prefix.start_with?("#{field}.") }
        end
      else
        path.include?('/security/') || path.end_with?('/CustomerSessionBinder.java')
      end
    end
    errors = []
    begin
      unless review_paths.empty?
        _declaration, reviewed_paths = compatibility_declaration(repository, new_revision)
        missing = review_paths - reviewed_paths
        raise "호환성 검토 누락 경로: #{missing.join(', ')}" unless missing.empty?
      end
    rescue RuntimeError => error
      errors << "#{error.message}\n검토 필요 경로: #{review_paths.join(', ')}"
    end

    statuses = git_diff_statuses(repository, old_revision, new_revision)
    [-> { validate_expand_migrations(repository, new_revision, paths, statuses) },
     -> { validate_additive_openapi(repository, old_revision, new_revision, paths) }].each do |validation|
      begin
        validation.call
      rescue RuntimeError => error
        errors << error.message
      end
    end
    raise errors.join("\n") unless errors.empty?
  end

  def self.changed_fields(old, fresh, path = '')
    return [] if old == fresh
    if old.is_a?(Hash) && fresh.is_a?(Hash)
      (old.keys | fresh.keys).flat_map do |key|
        child = path.empty? ? key : "#{path}.#{key}"
        if old.key?(key) && fresh.key?(key)
          changed_fields(old[key], fresh[key], child)
        else
          [child]
        end
      end
    elsif old.is_a?(Array) && fresh.is_a?(Array) && old.length == fresh.length
      old.each_index.flat_map { |index| changed_fields(old[index], fresh[index], "#{path}[#{index}]") }
    else
      [path]
    end
  end

  def self.validate_app_config(old, fresh)
    raise 'app-config가 이전 또는 후보 manifest에 없습니다.' unless old && fresh
    left, right = old.fetch('data', {}), fresh.fetch('data', {})
    removed = left.keys - right.keys
    protected_changes = (left.keys | right.keys).select do |key|
      protected = PROTECTED_CONFIG_KEYS.include?(key) || key.match?(/PASSWORD|SECRET|ENCRYPT_KEY|HMAC_KEY/)
      protected && [left.key?(key), left[key]] != [right.key?(key), right[key]]
    end
    errors = []
    errors << "app-config 키 삭제: #{removed.join(', ')}" unless removed.empty?
    errors << "app-config 별도 전환 필요: #{protected_changes.join(', ')}" unless protected_changes.empty?
    %w[binaryData immutable].each do |key|
      errors << "app-config.#{key} 변경은 별도 검토가 필요합니다." unless old[key] == fresh[key]
    end
    raise errors.join("\n") unless errors.empty?
  end

  def self.workload_contract(document)
    spec = Marshal.load(Marshal.dump(document.fetch('spec')))
    template = spec.fetch('template')
    template['metadata'] ||= {}
    template['metadata'].delete('annotations')
    template.fetch('spec').fetch('containers').each do |container|
      container.delete('resources')
      %w[startupProbe readinessProbe livenessProbe].each do |key|
        # probe 삭제는 감지하면서 임계값·경로 조정은 허용한다.
        container[key] = true if container.key?(key)
      end
    end
    spec
  end

  def self.check(repository, previous, candidate)
    old = documents(previous)
    fresh = documents(candidate)
    errors, checked = [], []
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
      next if checked.include?(revisions)
      checked << revisions
      paths = git_diff_paths(repository, *revisions)
      begin
        validate_reviewed_compatibility(repository, *revisions, paths) unless paths.empty?
      rescue RuntimeError => error
        errors << error.message
      end
    end
    %w[mysql redis prometheus alertmanager grafana].each do |name|
      changed = changed_fields(workload_contract(workload(old, name)), workload_contract(workload(fresh, name)))
      errors << "#{name} 변경은 별도 전환 필요: #{changed.join(', ')}" unless changed.empty?
      if changed.empty? && workload(old, name)['spec'] != workload(fresh, name)['spec']
        warn "[happygallery] #{name} 리소스·probe·주석 변경 허용: 단일 인스턴스 재시작 중 일시 중단될 수 있습니다."
      end
    end
    old_config = old.find { |d| d['kind'] == 'ConfigMap' && d.dig('metadata', 'name') == 'app-config' }
    new_config = fresh.find { |d| d['kind'] == 'ConfigMap' && d.dig('metadata', 'name') == 'app-config' }
    begin
      validate_app_config(old_config, new_config)
    rescue RuntimeError => error
      errors << error.message
    end
    raise errors.join("\n") unless errors.empty?
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

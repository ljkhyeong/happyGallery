#!/usr/bin/env ruby
# frozen_string_literal: true

require 'yaml'
require 'open3'
require 'json'

module RollingRelease
  # 호환성 대상 변경은 후보 commit의 명시적인 expand 승인과 안전한 형태를 함께 확인한다.
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
  COMPATIBILITY_DECLARATION = 'deploy/k3s/rolling-compatibility.yml'
  COMPATIBILITY_CATEGORIES = %w[migration api runtime_config build].freeze

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

  def self.canonical_json(value)
    case value
    when Hash
      value.keys.sort.to_h { |key| [key, canonical_json(value[key])] }
    when Array
      value.map { |item| canonical_json(item) }
    else
      value
    end
  end

  def self.openapi_operation_additive?(old_operation, new_operation)
    return false unless old_operation.is_a?(Hash) && new_operation.is_a?(Hash)

    old_operation.all? do |key, value|
      new_operation.key?(key) && canonical_json(value) == canonical_json(new_operation[key])
    end && (new_operation.keys - old_operation.keys).all? do |key|
      key == 'parameters' && new_operation[key].is_a?(Array) &&
        new_operation[key].all? { |parameter| parameter.is_a?(Hash) && parameter['required'] != true }
    end
  end

  def self.openapi_path_item_additive?(old_path_item, new_path_item)
    return false unless old_path_item.is_a?(Hash) && new_path_item.is_a?(Hash)

    operation_keys = %w[get put post delete options head patch trace]
    old_path_item.all? do |key, value|
      next false unless new_path_item.key?(key)

      operation_keys.include?(key) ? openapi_operation_additive?(value, new_path_item[key]) :
        canonical_json(value) == canonical_json(new_path_item[key])
    end && (new_path_item.keys - old_path_item.keys).all? { |key| operation_keys.include?(key) }
  end

  def self.validate_expand_migrations(repository, revision, paths, statuses)
    paths.grep(%r{\Abootstrap/src/main/resources/db/migration/}).each do |path|
      unless statuses[path] == 'A'
        raise "migration은 새 nullable 컬럼 추가만 허용합니다: #{path}"
      end

      sql = git_blob(repository, revision, path).strip
      unless sql.match?(/\AALTER TABLE `?[A-Za-z0-9_]+`? ADD COLUMN `?[A-Za-z0-9_]+`? [A-Z]+(?:\([0-9, ]+\))? NULL;\z/i)
        raise "migration은 새 nullable 컬럼 추가만 허용합니다: #{path}"
      end
    end
  end

  def self.validate_additive_openapi(repository, old_revision, new_revision, paths)
    openapi_path = 'docs/PRD/0004_API_계약/openapi3.json'
    return unless paths.include?(openapi_path)

    old_spec = JSON.parse(git_blob(repository, old_revision, openapi_path))
    new_spec = JSON.parse(git_blob(repository, new_revision, openapi_path))
    old_paths = old_spec.fetch('paths', {})
    new_paths = new_spec.fetch('paths', {})
    removed_paths = old_paths.keys - new_paths.keys
    changed_paths = old_paths.keys.select { |path| !openapi_path_item_additive?(old_paths[path], new_paths[path]) }
    unless removed_paths.empty? && changed_paths.empty?
      raise "OpenAPI는 기존 경로를 변경·삭제할 수 없습니다: #{(removed_paths + changed_paths).uniq.join(', ')}"
    end

    old_spec.each do |key, value|
      next if key == 'paths'
      if key == 'components'
        old_components = value || {}
        new_components = new_spec.fetch('components', {})
        changed_components = old_components.keys.select do |component|
          next true unless new_components.key?(component)

          old_group = old_components[component]
          new_group = new_components[component]
          if old_group.is_a?(Hash)
            !new_group.is_a?(Hash) || old_group.keys.any? do |name|
              !new_group.key?(name) || canonical_json(old_group[name]) != canonical_json(new_group[name])
            end
          else
            canonical_json(old_group) != canonical_json(new_group)
          end
        end
        raise "OpenAPI는 기존 components를 변경·삭제할 수 없습니다: #{changed_components.join(', ')}" unless changed_components.empty?
      elsif !new_spec.key?(key) || canonical_json(value) != canonical_json(new_spec[key])
        raise "OpenAPI 기존 계약이 변경되었습니다: #{key}"
      end
    end
  rescue JSON::ParserError => error
    raise "OpenAPI JSON을 읽을 수 없습니다: #{error.message}"
  end

  def self.validate_reviewed_compatibility(repository, old_revision, new_revision, paths)
    _declaration, reviewed_paths = compatibility_declaration(repository, new_revision)
    missing = paths - reviewed_paths
    extra = reviewed_paths - paths
    unless missing.empty? && extra.empty?
      raise "호환성 선언 경로가 실제 변경과 일치하지 않습니다. 누락=#{missing.join(', ')} 추가=#{extra.join(', ')}"
    end

    statuses = git_diff_statuses(repository, old_revision, new_revision)
    validate_expand_migrations(repository, new_revision, paths, statuses)
    validate_additive_openapi(repository, old_revision, new_revision, paths)
  rescue RuntimeError => error
    if error.message.start_with?("Git 파일을 읽을 수 없습니다: #{new_revision}:#{COMPATIBILITY_DECLARATION}")
      raise "#{error.message}\n검토 필요 경로:\n#{paths.join("\n")}"
    end

    raise
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
      paths = git_diff_paths(repository, *revisions)
      validate_reviewed_compatibility(repository, *revisions, paths) unless paths.empty?
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

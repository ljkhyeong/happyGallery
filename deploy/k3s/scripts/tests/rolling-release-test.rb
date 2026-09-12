# frozen_string_literal: true

require 'minitest/autorun'
require 'tmpdir'
require 'fileutils'
require_relative '../rolling-release'

class RollingReleaseTest < Minitest::Test
  def setup
    @dir = Dir.mktmpdir('happygallery-rolling-')
    git('init', '-q')
    git('config', 'user.email', 'test@example.invalid')
    git('config', 'user.name', '배포 검사')
    change('application/task.java', 'old')
    @old_sha = commit
    change('application/task.java', 'new')
    @new_sha = commit
    @old = File.join(@dir, 'old.yaml')
    @new = File.join(@dir, 'new.yaml')
    write_manifest(@old, @old_sha)
    write_manifest(@new, @new_sha)
  end

  def teardown
    FileUtils.remove_entry(@dir)
  end

  def git(*args)
    output, status = Open3.capture2e('git', '-C', @dir, *args)
    raise output unless status.success?
    output.strip
  end

  def change(file, contents)
    path = File.join(@dir, file)
    FileUtils.mkdir_p(File.dirname(path))
    File.write(path, contents)
    git('add', file)
  end

  def commit
    git('commit', '-qm', '검사 변경')
    git('rev-parse', 'HEAD')
  end

  def write_manifest(path, revision, mysql_image: 'mysql@sha256:aaa')
    documents = %w[app frontend mysql redis prometheus alertmanager grafana].map do |name|
      image = %w[app frontend].include?(name) ? "localhost/#{name}:#{revision}@sha256:#{'b' * 64}" : name
      image = mysql_image if name == 'mysql'
      { 'kind' => name == 'mysql' ? 'StatefulSet' : 'Deployment', 'metadata' => { 'name' => name },
        'spec' => { 'template' => { 'spec' => { 'containers' => [{ 'name' => name, 'image' => image }] } } } }
    end
    File.write(path, documents.map { |document| YAML.dump(document) }.join)
  end

  def test_allows_application_change_with_unchanged_shared_contracts
    RollingRelease.check(@dir, @old, @new)
    assert true
  end

  def test_blocks_migration_api_and_session_changes
    %w[bootstrap/src/main/resources/db/migration/V184.sql
       docs/PRD/0004_API_계약/openapi3.json
       adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/security/Session.java].each do |path|
      change(path, 'incompatible')
      write_manifest(@new, commit)
      error = assert_raises(RuntimeError) { RollingRelease.check(@dir, @old, @new) }
      assert_includes error.message, path
    end
  end

  def test_blocks_infrastructure_change_and_missing_history
    write_manifest(@new, @new_sha, mysql_image: 'other-mysql')
    assert_match(/mysql 변경/, assert_raises(RuntimeError) { RollingRelease.check(@dir, @old, @new) }.message)
    write_manifest(@new, '0' * 40)
    assert_match(/Git commit/, assert_raises(RuntimeError) { RollingRelease.check(@dir, @old, @new) }.message)
  end

  def test_rendered_workloads_keep_ready_pods_and_shared_assets
    base = File.expand_path('../../base', __dir__)
    %w[app frontend frontend-assets].each do |name|
      documents = RollingRelease.documents(File.join(base, "#{name}.yaml"))
      deployment = RollingRelease.workload(documents, name)
      assert_equal({ 'type' => 'RollingUpdate', 'rollingUpdate' => { 'maxUnavailable' => 0, 'maxSurge' => 1 } }, deployment.dig('spec', 'strategy'))
      assert_operator deployment.dig('spec', 'minReadySeconds'), :>=, 10
      pod = deployment.dig('spec', 'template', 'spec')
      assert_operator pod['terminationGracePeriodSeconds'], :>=, 45
      assert pod['containers'].first['readinessProbe']
      assert_equal ['sh', '-ec', 'sleep 10'], pod['containers'].first.dig('lifecycle', 'preStop', 'exec', 'command')
    end
    assets = RollingRelease.documents(File.join(base, 'frontend-assets.yaml'))
    deployment = RollingRelease.workload(assets, 'frontend-assets')
    assert_equal '__FRONTEND_IMAGE__', deployment.dig('spec', 'template', 'spec', 'containers', 0, 'image')
    assert_equal '__FRONTEND_IMAGE__', deployment.dig('spec', 'template', 'spec', 'initContainers', 0, 'image')
    route = assets.find { |d| d['kind'] == 'Ingress' }
    assert_equal '/assets', route.dig('spec', 'rules', 0, 'http', 'paths', 0, 'path')
  end
end

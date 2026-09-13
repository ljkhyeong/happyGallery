# frozen_string_literal: true

require 'minitest/autorun'
require 'tmpdir'
require 'fileutils'
require 'json'
require_relative '../rolling-release'

class RollingReleaseTest < Minitest::Test
  def setup
    @dir = Dir.mktmpdir('happygallery-rolling-')
    git('init', '-q')
    git('config', 'user.email', 'test@example.invalid')
    git('config', 'user.name', '배포 검사')
    change('application/task.java', 'old')
    change('docs/PRD/0004_API_계약/openapi3.json', JSON.generate(
      'openapi' => '3.1.0',
      'info' => { 'title' => 'test', 'version' => '1' },
      'paths' => { '/health' => { 'get' => { 'responses' => { '200' => {} } } } },
      'components' => { 'schemas' => {} }
    ))
    change('build.gradle', 'plugins {}')
    change('bootstrap/src/main/resources/application.yml', "app:\n  mode: old\n")
    change('adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/security/customer/CustomerSecurityRoutes.java', 'old')
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

  def write_compatibility(reviewed)
    change('deploy/k3s/rolling-compatibility.yml', {
      'version' => 1,
      'mode' => 'expand',
      'reason' => '테스트 확장 변경',
      'reviewed' => reviewed
    }.to_yaml)
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

  def test_allows_reviewed_expand_changes
    migration = 'bootstrap/src/main/resources/db/migration/V184__track_shipment_lookup_attempts.sql'
    controller = 'adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/security/bot/BotProtectionController.java'
    dto = 'adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/security/bot/dto/BotProtectionResponse.java'
    routes = 'adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/security/customer/CustomerSecurityRoutes.java'
    openapi = 'docs/PRD/0004_API_계약/openapi3.json'
    application = 'bootstrap/src/main/resources/application.yml'
    build = 'build.gradle'
    change(migration, "ALTER TABLE fulfillments ADD COLUMN tracking_checked_at DATETIME(6) NULL;\n")
    change(controller, 'new controller')
    change(dto, 'new dto')
    change(routes, 'new routes')
    change(application, "app:\n  mode: new\n")
    change(build, 'plugins { id "java" }')
    change(openapi, JSON.generate(
      'openapi' => '3.1.0',
      'info' => { 'title' => 'test', 'version' => '1' },
      'paths' => {
        '/health' => {
          'get' => {
            'parameters' => [{ 'in' => 'header', 'name' => 'X-Bot-Token', 'required' => false }],
            'responses' => { '200' => {} }
          }
        },
        '/new' => { 'get' => { 'responses' => { '200' => {} } } }
      },
      'components' => { 'schemas' => { 'New' => {} } }
    ))
    write_compatibility(
      'migration' => [migration],
      'api' => [controller, dto, routes, openapi],
      'runtime_config' => [application],
      'build' => [build]
    )
    write_manifest(@new, commit)

    RollingRelease.check(@dir, @old, @new)
    assert true
  end

  def test_rejects_non_expand_migration
    migration = 'bootstrap/src/main/resources/db/migration/V185__unsafe.sql'
    change(migration, "ALTER TABLE fulfillments DROP COLUMN tracking_checked_at;\n")
    write_compatibility('migration' => [migration])
    write_manifest(@new, commit)

    error = assert_raises(RuntimeError) { RollingRelease.check(@dir, @old, @new) }
    assert_match(/nullable 컬럼 추가/, error.message)
  end

  def test_rejects_changed_existing_openapi_path
    openapi = 'docs/PRD/0004_API_계약/openapi3.json'
    change(openapi, JSON.generate(
      'openapi' => '3.1.0',
      'info' => { 'title' => 'test', 'version' => '1' },
      'paths' => {
        '/health' => {
          'get' => {
            'parameters' => [{ 'in' => 'header', 'name' => 'X-Bot-Token', 'required' => true }],
            'responses' => { '200' => {} }
          }
        }
      },
      'components' => { 'schemas' => {} }
    ))
    write_compatibility('api' => [openapi])
    write_manifest(@new, commit)

    error = assert_raises(RuntimeError) { RollingRelease.check(@dir, @old, @new) }
    assert_match(/기존 경로를 변경·삭제/, error.message)
  end

  def test_allows_openapi_documentation_change_on_existing_path
    openapi = 'docs/PRD/0004_API_계약/openapi3.json'
    change(openapi, JSON.generate(
      'openapi' => '3.1.0',
      'info' => { 'title' => 'test', 'version' => '1' },
      'paths' => {
        '/health' => {
          'get' => {
            'description' => '상태 확인 API',
            'responses' => { '200' => {} }
          }
        }
      },
      'components' => { 'schemas' => {} }
    ))
    write_compatibility('api' => [openapi])
    write_manifest(@new, commit)

    RollingRelease.check(@dir, @old, @new)
    assert true
  end

  def test_allows_optional_request_and_response_fields_on_existing_path
    openapi = 'docs/PRD/0004_API_계약/openapi3.json'
    change(openapi, JSON.generate(
      'openapi' => '3.1.0',
      'info' => { 'title' => 'test', 'version' => '1' },
      'paths' => {
        '/health' => {
          'get' => {
            'parameters' => [{
              'in' => 'query', 'name' => 'trace', 'required' => false,
              'schema' => { 'type' => 'string' }
            }],
            'responses' => {
              '200' => {
                'description' => '정상 응답',
                'content' => {
                  'application/json' => {
                    'schema' => {
                      'type' => 'object',
                      'properties' => { 'healthy' => { 'type' => 'boolean' } }
                    }
                  }
                }
              }
            }
          }
        }
      },
      'components' => { 'schemas' => {
        'Health' => {
          'type' => 'object',
          'properties' => { 'message' => { 'type' => 'string' } }
        }
      } }
    ))
    write_compatibility('api' => [openapi])
    write_manifest(@new, commit)

    RollingRelease.check(@dir, @old, @new)
    assert true
  end

  def test_allows_optional_fields_inside_existing_request_and_response_schemas
    old_operation = {
      'requestBody' => {
        'required' => false,
        'content' => {
          'application/json' => {
            'schema' => {
              'type' => 'object',
              'properties' => { 'name' => { 'type' => 'string' } }
            }
          }
        }
      },
      'responses' => {
        '200' => {
          'content' => {
            'application/json' => {
              'schema' => {
                'type' => 'object',
                'properties' => { 'id' => { 'type' => 'integer' } }
              }
            }
          }
        }
      }
    }
    new_operation = Marshal.load(Marshal.dump(old_operation))
    new_operation['requestBody']['content']['application/json']['schema']['properties']['phone'] = { 'type' => 'string' }
    new_operation['responses']['200']['content']['application/json']['schema']['properties']['status'] = { 'type' => 'string' }

    assert RollingRelease.openapi_operation_additive?(old_operation, new_operation)
  end

  def test_rejects_new_required_openapi_parameter
    openapi = 'docs/PRD/0004_API_계약/openapi3.json'
    change(openapi, JSON.generate(
      'openapi' => '3.1.0',
      'info' => { 'title' => 'test', 'version' => '1' },
      'paths' => {
        '/health' => {
          'get' => {
            'parameters' => [{
              'in' => 'query', 'name' => 'trace', 'required' => true,
              'schema' => { 'type' => 'string' }
            }],
            'responses' => { '200' => {} }
          }
        }
      },
      'components' => { 'schemas' => {} }
    ))
    write_compatibility('api' => [openapi])
    write_manifest(@new, commit)

    error = assert_raises(RuntimeError) { RollingRelease.check(@dir, @old, @new) }
    assert_match(/기존 경로를 변경·삭제/, error.message)
  end

  def test_rejects_unreviewed_compatibility_path
    build = 'build.gradle'
    change(build, 'plugins { id "java" }')
    write_compatibility('api' => [])
    write_manifest(@new, commit)

    error = assert_raises(RuntimeError) { RollingRelease.check(@dir, @old, @new) }
    assert_match(/호환성 선언 경로가 실제 변경과 일치/, error.message)
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

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
    documents << {
      'apiVersion' => 'v1',
      'kind' => 'ConfigMap',
      'metadata' => { 'name' => 'app-config' },
      'data' => {
        'SENTRY_RELEASE' => "happygallery@#{revision}",
        'PASS_TOTAL_PRICE' => revision == @old_sha ? '240000' : '120000',
        'ORDER_SHIPPING_FEE' => '0',
        'DB_URL' => 'jdbc:mysql://mysql/gallery',
        'STATIC_SETTING' => 'same'
      }
    }
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

  def test_source_check_detects_unreviewed_change_across_failed_pushes
    path = 'adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/security/Session.java'
    change(path, 'new session behavior')
    commit
    change('application/task.java', 'later unrelated change')
    candidate = commit
    error = assert_raises(RuntimeError) { RollingRelease.check_source(@dir, @old_sha, candidate) }
    assert_includes error.message, path

    write_compatibility('api' => [path])
    RollingRelease.check_source(@dir, @old_sha, commit)
  end

  def test_source_check_requires_available_commits
    error = assert_raises(RuntimeError) { RollingRelease.check_source(@dir, 'a' * 40, @new_sha) }
    assert_includes error.message, 'Git commit이 없습니다'
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
    assert_match(/migration 별도 검토 필요/, error.message)
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
    assert_match(/OpenAPI 비호환 변경/, error.message)
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
                      'required' => ['healthy'],
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

    assert_empty OpenapiCompatibility.new(old_operation, new_operation).differences
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
    assert_match(/OpenAPI 비호환 변경/, error.message)
  end

  def test_rejects_unreviewed_compatibility_path
    security = 'adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/security/Session.java'
    change(security, 'new session contract')
    write_compatibility('api' => [])
    write_manifest(@new, commit)

    error = assert_raises(RuntimeError) { RollingRelease.check(@dir, @old, @new) }
    assert_match(/호환성 검토 누락 경로/, error.message)
  end

  def test_blocks_infrastructure_change_and_missing_history
    write_manifest(@new, @new_sha, mysql_image: 'other-mysql')
    assert_match(/mysql 변경/, assert_raises(RuntimeError) { RollingRelease.check(@dir, @old, @new) }.message)
    write_manifest(@new, '0' * 40)
    assert_match(/Git commit/, assert_raises(RuntimeError) { RollingRelease.check(@dir, @old, @new) }.message)
  end

  def test_blocks_connection_config_change
    documents = RollingRelease.documents(@new)
    config = documents.find { |document| document.dig('metadata', 'name') == 'app-config' }
    config['data']['DB_URL'] = 'jdbc:mysql://different/gallery'
    File.write(@new, documents.map { |document| YAML.dump(document) }.join)

    error = assert_raises(RuntimeError) { RollingRelease.check(@dir, @old, @new) }
    assert_match(/app-config 별도 전환 필요: DB_URL/, error.message)
  end

  def update_manifest(path)
    documents = RollingRelease.documents(path)
    yield documents
    File.write(path, documents.map { |document| YAML.dump(document) }.join)
  end

  def test_allows_business_config_values_new_keys_and_metadata
    update_manifest(@new) do |documents|
      config = documents.find { |document| document.dig('metadata', 'name') == 'app-config' }
      config['data'].merge!('ORDER_SHIPPING_FEE' => '3000', 'STATIC_SETTING' => 'changed', 'NEW_FEATURE' => 'true')
      config['metadata']['labels'] = { 'owner' => 'ops' }
    end
    RollingRelease.check(@dir, @old, @new)
    assert true
  end

  def test_reports_deleted_config_keys
    update_manifest(@new) do |documents|
      documents.find { |d| d.dig('metadata', 'name') == 'app-config' }['data'].delete('ORDER_SHIPPING_FEE')
    end
    error = assert_raises(RuntimeError) { RollingRelease.check(@dir, @old, @new) }
    assert_includes error.message, 'app-config 키 삭제: ORDER_SHIPPING_FEE'
  end

  def test_allows_workload_tuning_but_reports_image_and_storage_changes
    update_manifest(@new) do |documents|
      mysql = RollingRelease.workload(documents, 'mysql')
      mysql['spec']['template']['metadata'] = { 'annotations' => { 'note' => 'tuned' } }
      mysql.dig('spec', 'template', 'spec', 'containers', 0)['resources'] = { 'limits' => { 'memory' => '2Gi' } }
    end
    RollingRelease.check(@dir, @old, @new)
    update_manifest(@new) do |documents|
      mysql = RollingRelease.workload(documents, 'mysql')
      mysql['spec']['volumeClaimTemplates'] = [{ 'metadata' => { 'name' => 'different' } }]
      mysql.dig('spec', 'template', 'spec', 'containers', 0)['image'] = 'other'
    end
    error = assert_raises(RuntimeError) { RollingRelease.check(@dir, @old, @new) }
    assert_includes error.message, 'containers[0].image'
    assert_includes error.message, 'volumeClaimTemplates'
  end

  def test_allows_probe_tuning_but_not_removal
    [@old, @new].each do |path|
      update_manifest(path) do |documents|
        RollingRelease.workload(documents, 'redis').dig('spec', 'template', 'spec', 'containers', 0)['readinessProbe'] =
          { 'tcpSocket' => { 'port' => 6379 }, 'periodSeconds' => path == @old ? 10 : 20 }
      end
    end
    RollingRelease.check(@dir, @old, @new)
    update_manifest(@new) do |documents|
      RollingRelease.workload(documents, 'redis').dig('spec', 'template', 'spec', 'containers', 0).delete('readinessProbe')
    end
    assert_includes assert_raises(RuntimeError) { RollingRelease.check(@dir, @old, @new) }.message, 'readinessProbe'
  end

  def test_allows_old_review_entries_on_later_deploy
    security = 'adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/security/Session.java'
    change(security, 'reviewed')
    write_compatibility('api' => [security, 'already-deployed.java'])
    write_manifest(@new, commit)
    RollingRelease.check(@dir, @old, @new)
    assert true
  end

  def test_allows_business_yaml_and_build_changes_without_declaration
    change('bootstrap/src/main/resources/application.yml', "app:\n  pass:\n    total-price: 130000\n")
    change('build.gradle', '// dependency update')
    write_manifest(@new, commit)
    RollingRelease.check(@dir, @old, @new)
    assert true
  end

  def test_requires_review_for_session_yaml_change
    change('bootstrap/src/main/resources/application.yml', "spring:\n  session:\n    timeout: 1d\n")
    write_manifest(@new, commit)
    assert_includes assert_raises(RuntimeError) { RollingRelease.check(@dir, @old, @new) }.message, 'application.yml'
  end

  def test_allows_commented_multi_statement_expand_migration
    migration = 'bootstrap/src/main/resources/db/migration/V185__expand.sql'
    change(migration, <<~SQL)
      -- nullable 컬럼 추가
      ALTER TABLE samples ADD COLUMN note VARCHAR(100) NULL;
      /* 새 테이블의 문자열 안에 있는 ; 와 -- 는 주석·문장 경계가 아니다. */
      CREATE TABLE extra_samples (id BIGINT PRIMARY KEY, note VARCHAR(100) DEFAULT 'a;--b');
    SQL
    write_manifest(@new, commit)
    RollingRelease.check(@dir, @old, @new)
    assert true
  end

  def test_rejects_destructive_second_statement_and_executable_comments
    migration = 'bootstrap/src/main/resources/db/migration/V185__unsafe.sql'
    [
      'ALTER TABLE samples ADD COLUMN note TEXT NULL; DROP TABLE samples;',
      'CREATE TABLE extra_samples (id BIGINT); /*!50000 DROP TABLE samples */;',
      'CREATE TABLE extra_samples (id BIGINT) AS SELECT id FROM samples;',
      'CREATE TABLE extra_samples (id BIGINT) AS SELECT (1);'
    ].each do |sql|
      change(migration, sql)
      write_manifest(@new, commit)
      assert_raises(RuntimeError) { RollingRelease.check(@dir, @old, @new) }
    end
  end

  def test_rejects_rewriting_applied_migration
    migration = 'bootstrap/src/main/resources/db/migration/V185__expand.sql'
    change(migration, 'CREATE TABLE samples (id BIGINT);')
    write_manifest(@old, commit)
    change(migration, 'CREATE TABLE samples (id INT);')
    write_manifest(@new, commit)
    assert_includes assert_raises(RuntimeError) { RollingRelease.check(@dir, @old, @new) }.message, '적용 이력이 있는 migration 수정·삭제'
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
